package com.tencent.bk.codecc.defect.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.dao.mongorepository.StatDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.StatStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.StatDefectDao;
import com.tencent.bk.codecc.defect.model.DefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.StatDefectEntity;
import com.tencent.bk.codecc.defect.model.StatStatisticEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.api.ServiceToolMetaRestResource;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.util.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("statDefectCommitConsumer")
public class StatDefectCommitConsumer extends AbstractDefectCommitConsumer {

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    private Client client;
    @Autowired
    private StatDefectDao statDefectDao;
    @Autowired
    private StatStatisticRepository statStatisticRepository;
    @Autowired
    private StatDefectRepository statDefectRepository;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 判断增量还是全量
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository
                .findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

        // 获取统计类工具自定义参数
        Result<ToolMetaDetailVO> res = client.get(ServiceToolMetaRestResource.class).obtainToolDetail(toolName);
        if (res == null || res.isNotOk() || res.getData() == null) {
            throw new CodeCCException(
                    String.format("obtain tool meta info fail: toolName: %s, taskId: %s, buildId: %s", toolName,
                            taskId, buildId));
        }

        ToolMetaDetailVO.CustomToolInfo customToolInfo = res.getData().getCustomToolInfo();

        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        log.info("stat defect json: {}", defectListJson);
        DefectJsonFileEntity<Map<String, String>> defectJsonFileEntity = JsonUtil.INSTANCE
                .to(defectListJson, new TypeReference<DefectJsonFileEntity<Map<String, String>>>() {
                });

        List<StatDefectEntity> defectEntityList = new ArrayList<>(defectJsonFileEntity.getDefects().size());
        // 设置自定义参数
        log.warn("stat tool {} get empty custom info, taskId: {}, buildId: {} {}", toolName, taskId, buildId,
                defectEntityList);
        defectJsonFileEntity.getDefects()
                .forEach(defectInfo -> {
                    StatDefectEntity defectEntity = new StatDefectEntity(taskId, toolName);
                    customToolInfo.getCustomToolParam()
                            .keySet()
                            .forEach(field -> defectEntity.append(field, defectInfo.get(field)));
                    defectEntityList.add(defectEntity);
                });

        // 如果是全量的话，先设置所有数据失效位
        if (isFullScan) {
            statDefectDao.batchDisableStatInfo(taskId, toolName);
        }

        // 保存本次信息
        statDefectRepository.saveAll(defectEntityList);

        statisticCommit(defectEntityList, commitDefectVO, customToolInfo.getCustomToolDimension(), isFullScan);
    }

    /**
     * 告警信息统计入库
     *
     * @param defectEntityList 告警列表
     * @param commitDefectVO 告警参数
     * @param customToolDimension 自定义维度
     * @param isFullScan 全量标志
     */
    private void statisticCommit(List<StatDefectEntity> defectEntityList, CommitDefectVO commitDefectVO
            , Map<String, String> customToolDimension, boolean isFullScan) {

        long taskId = commitDefectVO.getTaskId();
        String buildId = commitDefectVO.getBuildId();
        String toolName = commitDefectVO.getToolName();

        Map<String, List<StatDefectEntity>> statisticMap = defectEntityList.stream()
                .collect(Collectors.groupingBy(
                        statDefectEntity -> statDefectEntity.get(customToolDimension.get("dimension")).toString()));

        List<StatStatisticEntity> statisticEntityList = new ArrayList<>();
        String STAT_LOCK_PREFIX = "lock.stat.statistic:";
        long LOCK_TIMEOUT = 10L;
        RedisLock lock = new RedisLock(redisTemplate, String.format("%s%s.%s", STAT_LOCK_PREFIX, taskId, toolName),
                LOCK_TIMEOUT);
        try {
            lock.lock();

            if (CollectionUtils.isEmpty(defectEntityList)) {
                log.warn("stat {} defect is empty, taskId {} | buildId {}, scanType {}", toolName, taskId, buildId,
                        isFullScan);
                // 获取上一次统计数据
                StatStatisticEntity lastEntity = statStatisticRepository
                        .findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
                // 全量扫描时结果为空就放一个值为 0 的 Doc 进去，如果是增量并且上一次构建不存在的话也按照全量逻辑
                if (isFullScan || lastEntity == null) {
                    StatStatisticEntity statStatisticEntity = new StatStatisticEntity(taskId, toolName, buildId);
                    statStatisticEntity.append("single", 0);
                    statStatisticEntity.append("multiple", 0);
                    statStatisticRepository.save(statStatisticEntity);
                } else {
                    // 上一次构建存在的话按照增量逻辑入库
                    List<StatStatisticEntity> lastEntityList = statStatisticRepository
                            .findByTaskIdAndBuildIdAndToolName(taskId, lastEntity.getString("build_id"), toolName);
                    lastEntityList.forEach(statStatisticEntity -> {
                        statStatisticEntity.setBuildId(buildId);
                        statStatisticEntity.setEntityId(ObjectId.get().toString());
                    });
                    statStatisticRepository.saveAll(lastEntityList);
                }
            } else {
                // 遍历按维度划分的List数据
                statisticMap.keySet().forEach(conf -> {
                    StatStatisticEntity statStatisticEntity = new StatStatisticEntity(taskId, toolName, buildId);
                    // 注入维度字段值
                    statStatisticEntity.append(customToolDimension.get("dimension"), conf);

                    // 按维度统计
                    statisticMap.get(conf).forEach(statDefectEntity -> {

                        // 计算要统计的字段
                        customToolDimension.keySet().stream()
                                .filter(key -> !key.equals("dimension"))
                                .forEach(dimension -> {
                                    switch (dimension) {
                                        // single 代表统计数据条数
                                        case "single": {
                                            if (statStatisticEntity.get(dimension) == null) {
                                                statStatisticEntity.append(dimension, 1);
                                            } else {
                                                statStatisticEntity
                                                        .append(dimension,
                                                                statStatisticEntity.getInteger(dimension) + 1);
                                            }
                                            break;
                                        }
                                        // multiple 代表统计数据字段值之和
                                        case "multiple": {
                                            if (statStatisticEntity.get(dimension) == null) {
                                                statStatisticEntity.append(dimension, statDefectEntity.get(dimension));
                                            } else {
                                                statStatisticEntity.append(dimension,
                                                        statDefectEntity.getInteger(dimension) + statStatisticEntity
                                                                .getInteger(dimension));
                                            }
                                            break;
                                        }
                                        default:
                                            break;
                                    }
                                });

                    });

                    statisticEntityList.add(statStatisticEntity);
                });

                // 增量/全量写入
                if (!isFullScan) {
                    // 获取上一次统计数据
                    StatStatisticEntity lastEntity = statStatisticRepository
                            .findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);

                    // 第一次构建
                    if (lastEntity == null) {
                        statStatisticRepository.saveAll(statisticEntityList);
                        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
                        return;
                    }

                    List<StatStatisticEntity> lastEntityList = statStatisticRepository
                            .findByTaskIdAndBuildIdAndToolName(taskId, lastEntity.getString("build_id"), toolName);
                    Map<String, List<StatStatisticEntity>> currMap = statisticEntityList.stream()
                            .collect(Collectors.groupingBy(
                                    statStatisticEntity -> statStatisticEntity
                                            .getString(customToolDimension.get("dimension"))));

                    // 上一次统计数据按维度更新到本次统计数据中
                    lastEntityList.forEach(lastStatStatisticEntity -> {
                        List<StatStatisticEntity> dimensionCurrEntityList = currMap
                                .get(String.valueOf(lastStatStatisticEntity.get(customToolDimension.get("dimension"))));
                        log.info("bug fix {} {}", currMap,
                                String.valueOf(lastStatStatisticEntity.get(customToolDimension.get("dimension"))));
                        if (dimensionCurrEntityList != null && !dimensionCurrEntityList.isEmpty()) {
                            customToolDimension.keySet().stream()
                                    .filter(key -> !key.equals("dimension"))
                                    .forEach(dimension -> dimensionCurrEntityList.get(0).append(dimension,
                                            Double.parseDouble(String.valueOf(lastStatStatisticEntity.get(dimension)))
                                                    + Double.parseDouble(
                                                    String.valueOf(dimensionCurrEntityList.get(0).get(dimension)))));
                        } else {
                            lastStatStatisticEntity.setEntityId(ObjectId.get().toString());
                            lastStatStatisticEntity.setBuildId(buildId);
                            statisticEntityList.add(lastStatStatisticEntity);
                        }
                    });
                }
                statStatisticRepository.saveAll(statisticEntityList);
            }
        } finally {
            lock.unlock();
        }

        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }
}