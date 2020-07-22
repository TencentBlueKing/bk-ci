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
import com.tencent.bk.codecc.defect.component.CCNDefectTracingComponent;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.*;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.CCNUploadStatisticVO;
import com.tencent.bk.codecc.defect.vo.ChartAverageVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
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
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
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
public class CCNUploadStatisticServiceImpl implements CCNUploadStatisticService
{
    /**
     * 字符串锁前缀
     */
    private static final String UPDATE_CCN_DEFECTS_LOCK_KEY = "UPDATE_CCN_DEFECTS:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 20L;

    @Autowired
    public TaskAnalysisDao taskAnalysisDao;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;

    @Autowired
    private Client client;

    @Autowired
    private BuildDefectService buildDefectService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedLineReportService redLineReportService;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;

    @Autowired
    private KafkaClient kafkaClient;

    @Autowired
    private CCNDefectTracingComponent ccnDefectTracingComponent;

    @Autowired
    private BuildDao buildDao;

    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;

    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private CheckerService checkerService;

    @Override
    public CodeCCResult uploadStatistic(CCNUploadStatisticVO uploadStatisticVO)
    {
        log.info("upload ccn statistic data. uploadStatisticVO: {}", JsonUtil.INSTANCE.toJson(uploadStatisticVO));
        // 调用task模块的接口获取任务信息
        Long taskId = uploadStatisticVO.getTaskId();

        // 更新告警方法的状态和构建告警快照
        RedisLock lock = new RedisLock(redisTemplate, UPDATE_CCN_DEFECTS_LOCK_KEY + taskId + ComConstants.SEPARATOR_SEMICOLON + ComConstants.Tool.CCN.name(),
                LOCK_TIMEOUT);
        try
        {
            // 加分布式锁
            lock.lock();
            updateDefectStatusAndBuildDefects(taskId, uploadStatisticVO);
        }
        finally
        {
            if (lock != null)
            {
                lock.unlock();
            }
        }

        return new CodeCCResult(CommonMessageCode.SUCCESS, "upload CCN analysis statistic ok");
    }

    /**
     * 根据分析版本号判断告警是否已经被修复
     *
     * @param taskId
     * @param uploadStatisticVO
     * @return
     */
    private void updateDefectStatusAndBuildDefects(long taskId, CCNUploadStatisticVO uploadStatisticVO)
    {
        // 获取本次构建上报的文件列表并入库
        String buildId = uploadStatisticVO.getBuildId();
        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);

        // 查询临时存储的已删除文件列表并入库 modified by xxxxx xxxxxx 确认其他工具是否也需要
        CodeRepoInfoEntity codeRepoInfoEntity = codeRepoRepository.findByTaskIdAndBuildId(taskId, buildId);
        if (codeRepoInfoEntity != null && CollectionUtils.isNotEmpty(codeRepoInfoEntity.getTempDeleteFiles()))
        {
            toolBuildInfoDao.updateDeleteFiles(taskId, ComConstants.Tool.CCN.name(), codeRepoInfoEntity.getTempDeleteFiles(), DefectConstants.UpdateToolDeleteFileType.ADD);
        }

        // 获取工具侧上报的已删除文件
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.CCN.name(), buildId);
        List<String> deleteFiles;
        if (toolBuildStackEntity != null && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles()))
        {
            deleteFiles = toolBuildStackEntity.getDeleteFiles();
        }
        else
        {
            deleteFiles = Lists.newArrayList();
        }

        TaskDetailVO taskDetailVO = getTaskDetail(taskId);

        // 获取本次是增量还是全量扫描
        boolean isFullScan = toolBuildStackEntity != null ? toolBuildStackEntity.isFullScan() : true;

        Set<String> currentBuildRelPaths = Sets.newHashSet();
        List<CCNDefectEntity> ccnDefectList = ccnDefectRepository.findByTaskId(taskId);
        List<BuildDefectEntity> buildDefectEntity = buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.CCN.name(), buildId);
        if (CollectionUtils.isNotEmpty(buildDefectEntity))
        {
            List<CCNDefectEntity> currentBuildCcnDefects = Lists.newArrayList();
            for (BuildDefectEntity currentBuildDefect : buildDefectEntity)
            {
                currentBuildCcnDefects.add(currentBuildDefect.getTempCcnDefect());
                currentBuildRelPaths.add(currentBuildDefect.getFileRelPath());
            }

            // 如果是增量扫描，要获取本次上报文件的告警列表，如果是全量扫描，则直接使用现有所有告警
            List<CCNDefectEntity> previousCcnDefects = Lists.newArrayList();
            if (isFullScan)
            {
                previousCcnDefects = ccnDefectList;
            }
            else
            {
                if (CollectionUtils.isNotEmpty(ccnDefectList))
                {
                    for (CCNDefectEntity previousCcnDefect : ccnDefectList)
                    {
                        if (currentBuildRelPaths.contains(previousCcnDefect.getRelPath()))
                        {
                            previousCcnDefects.add(previousCcnDefect);
                        }
                    }
                }
            }

            // 做告警跟踪产生圈复杂度告警唯一entityId
            log.info("currentBuildCcnDefects size:{}", currentBuildCcnDefects.size());
            log.info("previousCcnDefects size:{}", previousCcnDefects.size());
            List<CCNDefectEntity> finalCcnDefects = ccnDefectTracingComponent.defectTracing(taskDetailVO, ComConstants.Tool.CCN.name(), buildEntity,
                    previousCcnDefects, currentBuildCcnDefects);
            log.info("finalCcnDefects size:{}", finalCcnDefects.size());
            // 保存本次上报的告警
            if (CollectionUtils.isNotEmpty(finalCcnDefects))
            {
                //根据屏蔽逻辑处理入库告警
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
                    finalCcnDefects.forEach(ccnDefectEntity -> {
                        try{
                            if(StringUtils.isNotBlank(ccnDefectEntity.getRelPath()) &&
                                    PathUtils.checkIfMaskByPath(ccnDefectEntity.getRelPath(), new HashSet<>(filterPath)))
                            {
                                ccnDefectEntity.setStatus(ccnDefectEntity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
                            }
                        } catch (Exception e)
                        {
                            log.info("invalid regex expression for ccn, expression: task id: {}", taskId);
                        }

                    });
                }

                ccnDefectRepository.deleteByTaskIdIsAndPinpointHashIsNull(taskId);
                ccnDefectRepository.save(finalCcnDefects);

                // 上报数据到数据平台
//                pushToKafka(finalCcnDefects);
            }

            // 更新快照中的告警entityId
            if (CollectionUtils.isNotEmpty(buildDefectEntity))
            {
                for (BuildDefectEntity currentBuildDefect : buildDefectEntity)
                {
                    // 保存告警跟踪处理过后生成的告警entityId
                    currentBuildDefect.setDefectId(currentBuildDefect.getTempCcnDefect().getEntityId());

                    // 清除临时文件存储
                    currentBuildDefect.setTempCcnDefect(null);
                }
            }


            // 清除临时文件存储
            buildDefectRepository.save(buildDefectEntity);
        }

        // 获取各严重级别定义
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
        if (riskConfigMap == null)
        {
            log.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        // 获取超标圈复杂度阈值，优先从规则里面取，取不到从个性化参数里面取，再取不到就是用默认值
        int ccnThreshold = getCcnThreshold(taskDetailVO, ComConstants.Tool.CCN.name());

        int ccnBeyondThresholdSum = 0;
        int existCount = 0;
        int superHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        List<CCNDefectEntity> needUpdateDefectList = Lists.newArrayList();
        ccnDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(ccnDefectList))
        {
            // 更新告警状态，并统计告警数量
            Iterator<CCNDefectEntity> it = ccnDefectList.iterator();
            while (it.hasNext())
            {
                CCNDefectEntity defectEntity = it.next();

                /**
                 * 1、如果文件已删除，则设置为已修复状态
                 * 2、如果是全量扫描，且此次分析中没有上报，则设置为已修复状态
                 * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
                 */
                if (defectEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                {
                    boolean notCurrentBuildUpload = CollectionUtils.isEmpty(currentBuildRelPaths)
                            || !currentBuildRelPaths.contains(defectEntity.getRelPath());
                    if (deleteFiles.contains(defectEntity.getFilePath()) || (isFullScan && notCurrentBuildUpload))
                    {
                        long curTime = System.currentTimeMillis();
                        defectEntity.setStatus(defectEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                        defectEntity.setFixedTime(curTime);
                        defectEntity.setLatestDateTime(curTime);
                        defectEntity.setFixedBuildNumber(buildEntity.getBuildNo());
                        needUpdateDefectList.add(defectEntity);
                    }
                }

                // 统计遗留告警数
                if (defectEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                {
                    existCount++;

                    int ccn = defectEntity.getCcn();
                    if (ccn >= m && ccn < h)
                    {
                        mediumCount++;
                    }
                    else if (ccn >= h && ccn < sh)
                    {
                        highCount++;
                    }
                    else if (ccn >= sh)
                    {
                        superHighCount++;
                    }
                    else if (ccn < m)
                    {
                        lowCount++;
                    }

                    // 计算超标复杂度
                    int diff = ccn - ccnThreshold;
                    if (diff > 0)
                    {
                        ccnBeyondThresholdSum += diff;
                    }
                }
            }

            // 更新告警文件状态
            if (CollectionUtils.isNotEmpty(needUpdateDefectList))
            {
                ccnDefectRepository.save(needUpdateDefectList);
            }
        }
        log.info("existCount-->{}", existCount);

        // 更新构建告警快照
        String baseBuildId = toolBuildStackEntity != null && StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity.getBaseBuildId() : "";
        buildDefectService.updateBaseBuildDefectsAndClearTemp(taskId, ComConstants.Tool.CCN.name(), baseBuildId, buildId,
                isFullScan, deleteFiles, currentBuildRelPaths);

        // 保存本次构建遗留告警统计数据
        float averageCCN = Float.valueOf(uploadStatisticVO.getAverageCCN());
        saveStatisticResult(taskId, uploadStatisticVO.getBuildId(), existCount, averageCCN, baseBuildId, superHighCount,
                highCount, mediumCount, lowCount, ccnBeyondThresholdSum);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskDetailVO, ComConstants.Tool.CCN.name(), buildId);
    }

    /**
     * @param taskId
     * @param buildId
     * @param existCount
     * @param averageCCN
     */
    private void saveStatisticResult(long taskId, String buildId, int existCount, float averageCCN, String baseBuildId, int superHighCount, int highCount,
            int mediumCount, int lowCount, int ccnBeyondThresholdSum)
    {
        CCNStatisticEntity baseBuildCcnStatistic = ccnStatisticRepository.findByTaskIdAndBuildId(taskId, baseBuildId);

        CCNStatisticEntity newCcnStatistic = new CCNStatisticEntity();
        if (baseBuildCcnStatistic != null)
        {
            newCcnStatistic.setDefectChange(existCount - (baseBuildCcnStatistic.getDefectCount() == null ? 0 : baseBuildCcnStatistic.getDefectCount()));
            newCcnStatistic.setAverageCCNChange(averageCCN - (baseBuildCcnStatistic.getAverageCCN() == null ? 0 : baseBuildCcnStatistic.getAverageCCN()));
            newCcnStatistic.setLastDefectCount(baseBuildCcnStatistic.getDefectCount());
            newCcnStatistic.setLastAverageCCN(baseBuildCcnStatistic.getAverageCCN());
        }
        else
        {
            newCcnStatistic.setDefectChange(existCount);
            newCcnStatistic.setAverageCCNChange(averageCCN);
            newCcnStatistic.setLastDefectCount(0);
            newCcnStatistic.setLastAverageCCN(0.0F);
        }
        newCcnStatistic.setDefectCount(existCount);
        newCcnStatistic.setAverageCCN(averageCCN);
        newCcnStatistic.setTime(System.currentTimeMillis());
        newCcnStatistic.setTaskId(taskId);
        newCcnStatistic.setToolName(ComConstants.Tool.CCN.name());
        newCcnStatistic.setBuildId(buildId);
        newCcnStatistic.setSuperHighCount(superHighCount);
        newCcnStatistic.setHighCount(highCount);
        newCcnStatistic.setMediumCount(mediumCount);
        newCcnStatistic.setLowCount(lowCount);
        newCcnStatistic.setCcnBeyondThresholdSum(ccnBeyondThresholdSum);
        ccnStatisticRepository.save(newCcnStatistic);

        // 获取最近5日平均圈复杂度趋势数据，由于需要使用最新统计结果，所以先保存再获取趋势数据然后再次保存
        List<ChartAverageEntity> averageList = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(ComConstants.Tool.CCN.name(),
                ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        CCNDataReportRspVO ccnDataReportRspVO = (CCNDataReportRspVO) dataReportBizService
                .getDataReport(taskId, ComConstants.Tool.CCN.name(), 5, null, null);
        if (ccnDataReportRspVO != null)
        {
            //平均圈复杂度按日期从早到晚排序
            ccnDataReportRspVO.getChartAverageList().getAverageList().sort(Comparator.comparing(ChartAverageVO::getDate));

            //平均圈复杂度图表数值保留两位小数
            ccnDataReportRspVO.getChartAverageList().getAverageList().forEach(chartAverageVO ->
            {
                BigDecimal averageCcnBd = new BigDecimal(chartAverageVO.getAverageCCN());
                chartAverageVO.setAverageCCN(averageCcnBd.setScale(2, BigDecimal.ROUND_HALF_DOWN).floatValue());
            });

            averageList.addAll(ccnDataReportRspVO.getChartAverageList().getAverageList().stream().map(chartAverageVO ->
            {
                ChartAverageEntity chartAverageEntity = new ChartAverageEntity();
                BeanUtils.copyProperties(chartAverageVO, chartAverageEntity);
                return chartAverageEntity;
            }).collect(Collectors.toList()));
        }
        newCcnStatistic.setAverageList(averageList);
        ccnStatisticRepository.save(newCcnStatistic);

    }

    private int getCcnThreshold(TaskDetailVO taskVO, String toolName)
    {
        long taskId = taskVO.getTaskId();
        int ccnThreshold = ComConstants.DEFAULT_CCN_THRESHOLD;
        AnalyzeConfigInfoVO analyzeConfigInfoVO = new AnalyzeConfigInfoVO();
        analyzeConfigInfoVO.setTaskId(taskId);
        analyzeConfigInfoVO.setMultiToolType(toolName);
        analyzeConfigInfoVO = checkerService.getTaskCheckerConfig(analyzeConfigInfoVO);
        List<OpenCheckerVO> openCheckers = analyzeConfigInfoVO.getOpenCheckers();
        if (CollectionUtils.isNotEmpty(openCheckers) && CollectionUtils.isNotEmpty(openCheckers.get(0).getCheckerOptions()))
        {
            String ccnThresholdStr = openCheckers.get(0).getCheckerOptions().get(0).getCheckerOptionValue();
            ccnThreshold = StringUtils.isEmpty(ccnThresholdStr) ? ComConstants.DEFAULT_CCN_THRESHOLD : Integer.valueOf(ccnThresholdStr.trim());
        }
        else
        {
            List<ToolConfigInfoVO> toolConfigInfoList = taskVO.getToolConfigInfoList();
            for (ToolConfigInfoVO toolConfigInfoVO : toolConfigInfoList)
            {
                if (ComConstants.Tool.CCN.name().equals(toolConfigInfoVO.getToolName()) && StringUtils.isNotEmpty(toolConfigInfoVO.getParamJson()))
                {
                    JSONObject paramJson = new JSONObject(toolConfigInfoVO.getParamJson());
                    if (paramJson.has(ComConstants.KEY_CCN_THRESHOLD))
                    {
                        String ccnThresholdStr = paramJson.getString(ComConstants.KEY_CCN_THRESHOLD);
                        ccnThreshold = StringUtils.isEmpty(ccnThresholdStr) ? ComConstants.DEFAULT_CCN_THRESHOLD : Integer.valueOf(ccnThresholdStr.trim());
                    }
                    break;
                }
            }
        }
        return ccnThreshold;
    }

    private TaskDetailVO getTaskDetail(long taskId)
    {
        CodeCCResult<TaskDetailVO> taskDetailVOCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskDetailVOCodeCCResult.isNotOk() || null == taskDetailVOCodeCCResult.getData())
        {
            log.error("task info is empty! task id: {}: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        TaskDetailVO taskDetailVO = taskDetailVOCodeCCResult.getData();
        return taskDetailVO;
    }

    private void pushToKafka(List<CCNDefectEntity> ccnDefectEntityList) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (ccnDefectEntityList.size() > 0) {
            ccnDefectEntityList.forEach(ccnDefectEntity -> {
                Map<String, Object> map = JsonUtil.INSTANCE.toMap(ccnDefectEntity);
                String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                map.put("washTime", dateString);
                mapList.add(map);
            });

        }

        try {
            kafkaClient.send(KafkaTopic.SINGLE_CCN_STATISTIC_TOPIC, JsonUtil.INSTANCE.toJson(mapList));
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
