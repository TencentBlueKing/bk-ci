/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.component.NewCCNDefectTracingComponent;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.FileCCNRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.FileCCNDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.vo.CCNDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.ChartAverageVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * CCN告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("ccnDefectCommitConsumer")
@Slf4j
public class CCNDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    @Autowired
    private FileCCNRepository fileCCNRepository;
    @Autowired
    private FileCCNDao fileCCNDao;
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private NewCCNDefectTracingComponent newCCNDefectTracingComponent;
    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    private BuildDefectService buildDefectService;
    @Autowired
    private CheckerService checkerService;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(commitDefectVO.getStreamName());
        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(commitDefectVO.getBuildId());

        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        CCNDefectJsonFileEntity<CCNDefectEntity> defectJsonFileEntity = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<CCNDefectJsonFileEntity<CCNDefectEntity>>()
        {
        });

        // 1.解析工具上报的告警文件，并做告警跟踪
        long beginTime = System.currentTimeMillis();
        Set<String> currentFileSet = parseDefectJsonFile(commitDefectVO, defectJsonFileEntity, taskVO, buildEntity, fileChangeRecordsMap, codeRepoIdMap);
        log.info("parseDefectJsonFile cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        // 判断本次是增量还是全量扫描
        boolean isFullScan = toolBuildStackEntity != null ? toolBuildStackEntity.isFullScan() : true;

        // 获取工具侧上报的已删除文件
        List<String> deleteFiles;
        if (toolBuildStackEntity != null && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles()))
        {
            deleteFiles = toolBuildStackEntity.getDeleteFiles();
        }
        else
        {
            deleteFiles = Lists.newArrayList();
        }

        // 查询所有状态为NEW的告警
        beginTime = System.currentTimeMillis();
        List<CCNDefectEntity> allNewDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        log.info("find lint file list cost: {}, {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId, allNewDefectList.size());

        // 2.更新告警状态
        updateDefectEntityStatus(allNewDefectList, currentFileSet, deleteFiles, isFullScan, buildEntity);

        String baseBuildId = toolBuildStackEntity != null && StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity.getBaseBuildId() : "";

        // 3.计算总平均圈复杂度
        float averageCCN = calculateAverageCCN(taskId, defectJsonFileEntity, isFullScan, deleteFiles);

        // 4.统计本次扫描的告警
        beginTime = System.currentTimeMillis();
        statistic(taskVO, toolName, averageCCN, buildId, baseBuildId, allNewDefectList);
        log.info("statistic cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 5.更新构建告警快照
        beginTime = System.currentTimeMillis();
        buildDefectService.updateBaseBuildDefects(taskId, toolName, baseBuildId, buildId, isFullScan, deleteFiles, currentFileSet);
        log.info("buildDefectService.updateBaseBuildDefectsAndClearTemp cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 6.保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, ComConstants.Tool.CCN.name(), buildId);
    }

    /**
     * 统计本次扫描的告警
     * @param taskVO
     * @param toolName
     * @param averageCCN
     * @param buildId
     * @param baseBuildId
     * @param allNewDefectList
     */
    private void statistic(TaskDetailVO taskVO, String toolName, float averageCCN, String buildId, String baseBuildId, List<CCNDefectEntity> allNewDefectList)
    {
        long taskId = taskVO.getTaskId();
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
        int ccnThreshold = getCcnThreshold(taskVO, toolName);

        int ccnBeyondThresholdSum = 0;
        int existCount = 0;
        int superHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        if (CollectionUtils.isNotEmpty(allNewDefectList))
        {
            // 更新告警状态，并统计告警数量
            Iterator<CCNDefectEntity> it = allNewDefectList.iterator();
            while (it.hasNext())
            {
                CCNDefectEntity defectEntity = it.next();

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
        }
        log.info("existCount-->{}", existCount);

        CCNStatisticEntity baseBuildCcnStatistic = ccnStatisticRepository.findByTaskIdAndBuildId(taskVO.getTaskId(), baseBuildId);

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
        newCcnStatistic.setToolName(toolName);
        newCcnStatistic.setBuildId(buildId);
        newCcnStatistic.setSuperHighCount(superHighCount);
        newCcnStatistic.setHighCount(highCount);
        newCcnStatistic.setMediumCount(mediumCount);
        newCcnStatistic.setLowCount(lowCount);
        newCcnStatistic.setCcnBeyondThresholdSum(ccnBeyondThresholdSum);
        ccnStatisticRepository.save(newCcnStatistic);

        // 获取最近5日平均圈复杂度趋势数据，由于需要使用最新统计结果，所以先保存再获取趋势数据然后再次保存
        List<ChartAverageEntity> averageList = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        CCNDataReportRspVO ccnDataReportRspVO = (CCNDataReportRspVO) dataReportBizService
                .getDataReport(taskId, toolName, 5, null, null);
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

    /**
     * 更新告警状态
     * @param allNewDefectList
     * @param currentFileSet
     * @param deleteFiles
     * @param isFullScan
     * @param buildEntity
     */
    private void updateDefectEntityStatus(List<CCNDefectEntity> allNewDefectList,
                                          Set<String> currentFileSet,
                                          List<String> deleteFiles,
                                          boolean isFullScan,
                                          BuildEntity buildEntity)
    {
        List<CCNDefectEntity> needUpdateDefectList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(allNewDefectList))
        {
            long curTime = System.currentTimeMillis();
            allNewDefectList.forEach(defectEntity ->
            {
                String filePath = defectEntity.getFilePath();
                String relPath = defectEntity.getRelPath();

                // 是否是本次上报的告警文件
                boolean notCurrentBuildUpload = CollectionUtils.isEmpty(currentFileSet)
                        || !currentFileSet.contains(StringUtils.isEmpty(relPath) ? filePath : relPath);

                /**
                 * 1、文件已删除，则设置为已修复状态
                 * 2、全量扫描，且此次分析中没有上报，则设置为已修复状态
                 * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
                 */
                if ((deleteFiles.contains(filePath) || (isFullScan && notCurrentBuildUpload)))
                {
                    defectEntity.setStatus(defectEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                    defectEntity.setFixedTime(curTime);
                    defectEntity.setLatestDateTime(curTime);
                    defectEntity.setFixedBuildNumber(buildEntity.getBuildNo());
                    needUpdateDefectList.add(defectEntity);
                }
            });

            if (CollectionUtils.isNotEmpty(needUpdateDefectList))
            {
                ccnDefectRepository.save(needUpdateDefectList);
            }
        }
    }

    /**
     * 解析工具上报的告警文件，并做告警跟踪
     * @param commitDefectVO
     * @param defectJsonFileEntity
     * @param taskVO
     * @param buildEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     * @return
     */
    private Set<String> parseDefectJsonFile(CommitDefectVO commitDefectVO,
                                            CCNDefectJsonFileEntity<CCNDefectEntity> defectJsonFileEntity,
                                            TaskDetailVO taskVO,
                                            BuildEntity buildEntity,
                                            Map<String, ScmBlameVO> fileChangeRecordsMap,
                                            Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        // 获取作者转换关系
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findByTaskId(commitDefectVO.getTaskId());
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null)
        {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        // 获取屏蔽路径
        Set<String> filterPaths = getFilterPaths(taskVO);

        List<CCNDefectEntity> currentDefectList = defectJsonFileEntity.getDefects();
        log.info("current all defect list:{}", currentDefectList.size());

        // 填充告警的代码仓库信息
        currentDefectList.forEach(ccnDefectEntity -> fillDefectInfo(ccnDefectEntity, fileChangeRecordsMap, codeRepoIdMap));

        Map<String, List<CCNDefectEntity>> currentDefectGroup = currentDefectList.stream()
                .collect(Collectors.groupingBy(defect -> StringUtils.isEmpty(defect.getRelPath()) ? defect.getFilePath() : defect.getRelPath()));

        int chunkNo = 0;
        List<Future> asyncResultList = new ArrayList<>();
        List<CCNDefectEntity> partDefectList = new ArrayList<>();
        Set<String> filePathSet = new HashSet<>();
        for (Map.Entry<String, List<CCNDefectEntity>> entry : currentDefectGroup.entrySet())
        {
            String path = entry.getKey();
            List<CCNDefectEntity> defectList = entry.getValue();
            partDefectList.addAll(defectList);
            filePathSet.add(path);
            if (partDefectList.size() > MAX_PER_BATCH)
            {
                processDefects(commitDefectVO, taskVO, partDefectList, filePathSet, transferAuthorList, filterPaths, buildEntity, chunkNo, asyncResultList);
                chunkNo++;
                partDefectList = new ArrayList<>();
                filePathSet = new HashSet<>();
            }
        }

        if (partDefectList.size() > 0)
        {
            processDefects(commitDefectVO, taskVO, partDefectList, filePathSet, transferAuthorList, filterPaths, buildEntity, chunkNo, asyncResultList);
        }

        // 直到所有的异步处理否都完成了，才继续往下走
        asyncResultList.forEach(asyncResult ->
        {
            try
            {
                asyncResult.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                log.warn("handle defect fail! {}", commitDefectVO, e);
            }
        });

        return currentDefectGroup.keySet();
    }

    private void processDefects(CommitDefectVO commitDefectVO,
                                TaskDetailVO taskVO,
                                List<CCNDefectEntity> currentDefectList,
                                Set<String> filePathSet,
                                List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
                                Set<String> filterPaths,
                                BuildEntity buildEntity,
                                int chunkNo,
                                List<Future> asyncResultList)
    {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        List<CCNDefectEntity> preDefectList;
        if (StringUtils.isNotEmpty(currentDefectList.get(0).getRelPath()))
        {
            preDefectList = ccnDefectRepository.findByTaskIdAndRelPathIn(taskId, filePathSet);
        }
        else
        {
            preDefectList = ccnDefectRepository.findByTaskIdAndFilePathIn(taskId, filePathSet);
        }

        // 做告警跟踪产生圈复杂度告警唯一entityId
        log.info("previous defect count:{}, current defect count:{}", preDefectList.size(), currentDefectList.size());
        long beginTime = System.currentTimeMillis();
        Future<Boolean> asyncFuture = newCCNDefectTracingComponent.defectTracing(commitDefectVO,
                taskVO,
                filterPaths,
                buildEntity,
                chunkNo,
                preDefectList,
                currentDefectList,
                transferAuthorList);
        asyncResultList.add(asyncFuture);
        log.info("async defec tracing(unfinish) cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, commitDefectVO.getBuildId());
    }

    /**
     * 计算平均圈复杂度
     *
     * @param taskId
     * @param defectJsonFileEntity
     * @param isFullScan
     * @param deleteFiles
     * @return
     */
    private float calculateAverageCCN(long taskId, CCNDefectJsonFileEntity<CCNDefectEntity> defectJsonFileEntity, boolean isFullScan, List<String> deleteFiles)
    {
        // 如果本次是全量扫描，则要清除已有的文件圈复杂度列表，如果是增量扫描，则要先清理待删除文件
        if (isFullScan)
        {
            fileCCNRepository.deleteByTaskId(taskId);
        }
        else
        {
            // 获取删除文件列表
            if (CollectionUtils.isNotEmpty(deleteFiles))
            {
                fileCCNRepository.deleteByTaskIdIsAndFilePathIn(taskId, deleteFiles);
            }
        }

        // 保存本次上报的文件圈复杂度列表
        if (CollectionUtils.isNotEmpty(defectJsonFileEntity.getFilesTotalCCN()))
        {
            defectJsonFileEntity.getFilesTotalCCN().forEach(fileCCNEntity -> fileCCNEntity.setTaskId(taskId));
            fileCCNDao.upsertFileCCNList(defectJsonFileEntity.getFilesTotalCCN());
        }

        // 统计平均圈复杂度
        float averageCCN = 0;
        BigDecimal fileCount = BigDecimal.ZERO;
        BigDecimal totalCCN = BigDecimal.ZERO;
        List<FileCCNEntity> fileCCNEntities = fileCCNRepository.findByTaskId(taskId);
        for (FileCCNEntity fileCCNEntity : fileCCNEntities)
        {
            try
            {
                totalCCN = totalCCN.add(BigDecimal.valueOf(Double.parseDouble(fileCCNEntity.getTotalCCNCount())));
            }
            catch (Exception e)
            {
                log.error("totalCCNCount convert to double fail! fileCCNEntity{}", JsonUtil.INSTANCE.toJson(fileCCNEntity), e);
                continue;
            }
            fileCount = fileCount.add(BigDecimal.ONE);
        }
        log.info("file count is : {}", fileCount);

        // 避免除以0报错
        if (fileCount.compareTo(BigDecimal.ZERO) > 0)
        {
            averageCCN = totalCCN.divide(fileCount, 2, BigDecimal.ROUND_CEILING).floatValue();
        }

        return averageCCN;
    }

    /**
     * 填充告警的代码仓库信息
     *
     * @param ccnDefectEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     */
    private void fillDefectInfo(CCNDefectEntity ccnDefectEntity, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(ccnDefectEntity.getFilePath());
        if (fileLineAuthorInfo != null)
        {
            // 获取作者信息
            setAuthor(ccnDefectEntity, fileLineAuthorInfo);

            ccnDefectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
            ccnDefectEntity.setUrl(fileLineAuthorInfo.getUrl());
            ccnDefectEntity.setBranch(fileLineAuthorInfo.getBranch());
            ccnDefectEntity.setRevision(fileLineAuthorInfo.getRevision());
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
            {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                if(null != repoSubModuleVO){
                    ccnDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                }
            }
            else
            {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getUrl());
                if (repoSubModuleVO != null)
                {
                    ccnDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                    if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                    {
                        ccnDefectEntity.setSubModule(repoSubModuleVO.getSubModule());
                    }
                    else
                    {
                        ccnDefectEntity.setSubModule("");
                    }
                }
            }
        }
    }

    /**
     * 获取告警作者，取函数涉及的所有行中的最新修改作者作为告警作者
     *
     * @param ccnDefectEntity
     * @param fileLineAuthorInfo
     */
    private void setAuthor(CCNDefectEntity ccnDefectEntity, ScmBlameVO fileLineAuthorInfo)
    {
        List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
        if (CollectionUtils.isNotEmpty(changeRecords))
        {
            Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = getLineAuthorMap(changeRecords);

            // 获取函数涉及的所有行中的最新修改作者作为告警作者
            long functionLastUpdateTime = 0;
            for (int i = ccnDefectEntity.getStartLines(); i <= ccnDefectEntity.getEndLines(); i++)
            {
                if (lineAuthorMap != null)
                {
                    ScmBlameChangeRecordVO recordVO = lineAuthorMap.get(i);
                    if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime)
                    {
                        functionLastUpdateTime = recordVO.getLineUpdateTime();
                        ccnDefectEntity.setAuthor(recordVO.getAuthor());
                        ccnDefectEntity.setLatestDateTime(recordVO.getLineUpdateTime());
                    }
                }
            }
        }
    }
}
