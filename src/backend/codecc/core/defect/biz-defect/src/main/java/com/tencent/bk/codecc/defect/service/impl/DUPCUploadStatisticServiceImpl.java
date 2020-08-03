/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.DUPCUploadStatisticService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.service.RedLineReportService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.DUPCScanSummaryVO;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.UploadDUPCStatisticVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.kafka.KafkaClient;
import com.tencent.devops.common.kafka.KafkaTopic;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @version V1.0
 * @date 2019/6/3
 */
@Slf4j
@Service
public class DUPCUploadStatisticServiceImpl implements DUPCUploadStatisticService
{
    private static Logger logger = LoggerFactory.getLogger(DUPCUploadStatisticServiceImpl.class);

    /**
     * 字符串锁前缀
     */
    private static final String UPDATE_DUPC_DEFECTS_LOCK_KEY = "UPDATE_DUPC_DEFECTS:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 20L;

    @Autowired
    public TaskAnalysisDao taskAnalysisDao;

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private BuildDefectService buildDefectService;

    @Autowired
    private RedLineReportService redLineReportService;

    @Autowired
    private Client client;

    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;

    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private DUPCDefectDao dupcDefectDao;

    @Autowired
    private KafkaClient kafkaClient;

    @Override
    public CodeCCResult uploadStatistic(UploadDUPCStatisticVO uploadStatisticVO)
    {
        // 调用task模块的接口获取任务信息
        Long taskId = uploadStatisticVO.getTaskId();

        // 更新告警方法的状态
        RedisLock lock = new RedisLock(redisTemplate,
                UPDATE_DUPC_DEFECTS_LOCK_KEY + taskId + ComConstants.SEPARATOR_SEMICOLON + ComConstants.Tool.DUPC.name(),
                LOCK_TIMEOUT);
        try
        {
            // 加分布式锁
            lock.lock();
            updateDefectStatus(uploadStatisticVO);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }

        return new CodeCCResult(CommonMessageCode.SUCCESS, "upload DUPC analysis statistic ok");
    }

    /**
     * 保存本次上报文件的告警数据统计数据
     *
     * @param taskId
     * @param buildId
     * @param existCount
     * @param scanSummary
     * @param baseBuildId
     */
    private void saveStatisticResult(long taskId, String buildId, int existCount, DUPCScanSummaryVO scanSummary, String baseBuildId, int superHighCount,
                                     int highCount, int mediumCount)
    {
        long dupLineCount = scanSummary.getDupLineCount();
        long rawlineCount = scanSummary.getRawlineCount();
        float dupRate = 0.00F;
        if (rawlineCount != 0)
        {
            dupRate = (float) dupLineCount * 100 / rawlineCount;
        }

        DUPCStatisticEntity baseStatisticEntity = dupcStatisticRepository.findByTaskIdAndBuildId(taskId, baseBuildId);
        DUPCStatisticEntity statisticEntity = new DUPCStatisticEntity();
        if (baseStatisticEntity != null)
        {
            statisticEntity.setDefectChange(existCount - baseStatisticEntity.getDefectCount());
            statisticEntity.setDupRateChange(dupRate - baseStatisticEntity.getDupRate());
            statisticEntity.setLastDefectCount(baseStatisticEntity.getDefectCount());
            statisticEntity.setLastDupRate(baseStatisticEntity.getDupRate());
        }
        else
        {
            statisticEntity.setDefectChange(existCount);
            statisticEntity.setDupRateChange(dupRate);
            statisticEntity.setLastDefectCount(0);
            statisticEntity.setLastDupRate(0.0F);
        }

        statisticEntity.setDefectCount(existCount);
        statisticEntity.setDefectChange(existCount - (statisticEntity.getLastDefectCount() == null ? 0 : statisticEntity.getLastDefectCount()));
        statisticEntity.setDupRate(dupRate);
        statisticEntity.setDupRateChange(dupRate - (statisticEntity.getLastDupRate() == null ? 0 : statisticEntity.getLastDupRate()));
        statisticEntity.setTime(System.currentTimeMillis());
        statisticEntity.setTaskId(taskId);
        statisticEntity.setToolName(ComConstants.Tool.DUPC.name());
        statisticEntity.setBuildId(buildId);
        statisticEntity.setSuperHighCount(superHighCount);
        statisticEntity.setHighCount(highCount);
        statisticEntity.setMediumCount(mediumCount);

        DUPCScanSummaryEntity dupcScanSummary = new DUPCScanSummaryEntity();
        BeanUtils.copyProperties(scanSummary, dupcScanSummary);
        statisticEntity.setDupcScanSummary(dupcScanSummary);
        dupcStatisticRepository.save(statisticEntity);

        // 获取最近5天重复率趋势
        List<DupcChartTrendEntity> dupcChart = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(ComConstants.Tool.DUPC.name(), ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        DupcDataReportRspVO dupcDataReportRspVO = (DupcDataReportRspVO) dataReportBizService
                .getDataReport(taskId, ComConstants.Tool.DUPC.name(), 5, null, null);
        if (dupcDataReportRspVO != null)
        {
            //按日期排序
            dupcDataReportRspVO.getChartTrendList().getDucpChartList().sort(Comparator.comparing(DupcChartTrendVO::getDate));

            //重复率值保留两位小数
            dupcDataReportRspVO.getChartTrendList().getDucpChartList().forEach(dupcChartTrendVO ->
            {
                BigDecimal averageDupc = new BigDecimal(dupcChartTrendVO.getDupc());
                dupcChartTrendVO.setDupc(averageDupc.setScale(2, BigDecimal.ROUND_HALF_DOWN).floatValue());
            });

            dupcChart.addAll(dupcDataReportRspVO.getChartTrendList().getDucpChartList().stream().map(dupcChartTrendVO ->
            {
                DupcChartTrendEntity dupcChartTrendEntity = new DupcChartTrendEntity();
                BeanUtils.copyProperties(dupcChartTrendVO, dupcChartTrendEntity);
                return dupcChartTrendEntity;
            }).
                    collect(Collectors.toList()));
        }
        statisticEntity.setDupcChart(dupcChart);
        dupcStatisticRepository.save(statisticEntity);

    }

    private void updateDefectStatus(UploadDUPCStatisticVO uploadStatisticVO)
    {
        // TODO 清除重复告警，后续删除
        List<DUPCDefectEntity> dupFiles = dupcDefectRepository.findByTaskIdWithoutBlockList(uploadStatisticVO.getTaskId());
        Set<String> tempTaskIdAndRelPath = new HashSet<>();
        Set<DUPCDefectEntity> dupEntities = new HashSet<>();
        for (DUPCDefectEntity file : dupFiles)
        {
            if (!tempTaskIdAndRelPath.add(file.getTaskId() + "_" + file.getRelPath()))
            {
                dupEntities.add(file);
            }
        }
        dupcDefectRepository.delete(dupEntities);

        // 获取本次构建上报的文件列表并入库
        long taskId = uploadStatisticVO.getTaskId();
        String buildId = uploadStatisticVO.getBuildId();
        Set<String> currentBuildFileRelPaths = Sets.newHashSet();

        //获取任务信息
        TaskDetailVO taskDetailVO = getTaskDetail(taskId);

        List<BuildDefectEntity> buildDefectEntity = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.DUPC.name(), buildId);
        if (CollectionUtils.isNotEmpty(buildDefectEntity))
        {
            List<DUPCDefectEntity> currentBuildDupcFiles = Lists.newArrayList();
            for (BuildDefectEntity currentBuildDefect : buildDefectEntity)
            {
                currentBuildDupcFiles.add(currentBuildDefect.getTempDupcDefectFile());
                currentBuildFileRelPaths.add(currentBuildDefect.getFileRelPath());

                // 清除临时文件
                currentBuildDefect.setTempDupcDefectFile(null);
            }

            // 保存本次上报的告警
            if (CollectionUtils.isNotEmpty(currentBuildDupcFiles))
            {
                //对入库告警再做一次屏蔽路径过滤
                List<String> filterPath = new ArrayList<>();
                if(CollectionUtils.isNotEmpty(taskDetailVO.getFilterPath()))
                {
                    filterPath.addAll(taskDetailVO.getFilterPath());
                }
                if(CollectionUtils.isNotEmpty(taskDetailVO.getDefaultFilterPath()))
                {
                    filterPath.addAll(taskDetailVO.getDefaultFilterPath());
                }
                if(CollectionUtils.isNotEmpty(filterPath))
                {
                    currentBuildDupcFiles.forEach(dupcDefectEntity -> {
                        try{
                            if(StringUtils.isNotBlank(dupcDefectEntity.getRelPath()) &&
                                    PathUtils.checkIfMaskByPath(dupcDefectEntity.getRelPath(), new HashSet<>(filterPath)))
                            {
                                dupcDefectEntity.setStatus(dupcDefectEntity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
                            }
                        } catch (Exception e)
                        {
                            log.info("invalid regex expression for dupc, expression: task id: {}", taskId);
                        }

                    });
                }

                dupcDefectDao.upsertDupcDefect(currentBuildDupcFiles);

                // 上报告警信息到数据平台
//                pushToKafka(currentBuildDupcFiles);
            }

            // 清除临时文件
            buildDefectRepository.save(buildDefectEntity);
        }


        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.DUPC.name());
        float sh = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        float h = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        int existCount = 0;
        int superHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        List<DUPCDefectEntity> updateDefects = Lists.newArrayList();
        List<DUPCDefectEntity> defectList = dupcDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        logger.info("dupc statistic build: {}", buildId);
        if (CollectionUtils.isNotEmpty(defectList))
        {
            Iterator<DUPCDefectEntity> it = defectList.iterator();
            // 更新告警状态，并统计告警数量
            while (it.hasNext())
            {
                DUPCDefectEntity defectEntity = it.next();
                logger.info("dupc fileRelPath: {}", defectEntity.getRelPath());
                if (CollectionUtils.isEmpty(currentBuildFileRelPaths) || !currentBuildFileRelPaths.contains(defectEntity.getRelPath()))
                {
                    long curTime = System.currentTimeMillis();
                    defectEntity.setStatus(null == defectEntity.getStatus() ? 0 : defectEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                    defectEntity.setFixedTime(curTime);
                    defectEntity.setLastUpdateTime(curTime);
                    updateDefects.add(defectEntity);
                }

                if (ComConstants.DefectStatus.NEW.value() == defectEntity.getStatus())
                {
                    existCount++;
                    float dupcRate = convertDupRate2Float(defectEntity.getDupRate());
                    if (MapUtils.isNotEmpty(riskConfigMap))
                    {
                        if (dupcRate >= m && dupcRate < h)
                        {
                            mediumCount++;
                        }
                        else if (dupcRate >= h && dupcRate < sh)
                        {
                            highCount++;
                        }
                        else if (dupcRate >= sh)
                        {
                            superHighCount++;
                        }
                    }
                }
            }

            // 更新告警状态
            if (CollectionUtils.isNotEmpty(updateDefects))
            {
                dupcDefectDao.upsertDupcDefect(updateDefects);
            }
        }
        logger.debug("existCount-->{}", existCount);

        // 保存本次上报文件的告警数据统计数据
        ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, ComConstants.Tool.DUPC.name());
        String baseBuildId = toolBuildInfoEntity != null && StringUtils.isNotEmpty(toolBuildInfoEntity.getDefectBaseBuildId())
                ? toolBuildInfoEntity.getDefectBaseBuildId() : "";
        saveStatisticResult(taskId, buildId, existCount, uploadStatisticVO.getScanSummary(), baseBuildId, superHighCount, highCount, mediumCount);

        // 更新构建告警快照
        buildDefectService.updateBaseBuildDefectsAndClearTemp(taskId, ComConstants.Tool.DUPC.name(), null, buildId,
                true, null, null);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskDetailVO, ComConstants.Tool.DUPC.name(), buildId);
    }

    private TaskDetailVO getTaskDetail(long taskId)
    {
        CodeCCResult<TaskDetailVO> taskDetailVOCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskDetailVOCodeCCResult.isNotOk() || null == taskDetailVOCodeCCResult.getData())
        {
            logger.error("task info is empty! task id: {}: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        TaskDetailVO taskDetailVO = taskDetailVOCodeCCResult.getData();
        return taskDetailVO;
    }

    /**
     * 将重复率的百分数转换成浮点数
     * 工具侧上报的文件代码重复率是带%号的，比如12.57%，所以要先去除掉后面的百分号比较
     *
     * @param dupRateStr
     * @return
     */
    private float convertDupRate2Float(String dupRateStr)
    {
        float dupRate = 0;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(dupRateStr))
        {
            dupRate = Float.valueOf(dupRateStr.substring(0, dupRateStr.length() - 1));
        }
        return dupRate;
    }

    private void pushToKafka(List<DUPCDefectEntity> dupcDefectEntityList) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (dupcDefectEntityList.size() > 0) {
            dupcDefectEntityList.forEach(dupcDefectEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(dupcDefectEntity);
                String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                map.put("washTime", dateString);
                mapList.add(map);
            });

        }

        try {
            kafkaClient.send(KafkaTopic.SINGLE_DUPC_STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(mapList));
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
