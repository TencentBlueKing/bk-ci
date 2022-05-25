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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.RiskConfigCache;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.BsonSerializationException;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DUPC告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("dupcDefectCommitConsumer")
@Slf4j
public class DUPCDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    @Autowired
    private RiskConfigCache riskConfigCache;
    @Autowired
    private DUPCDefectRepository dupcDefectRepository;
    @Autowired
    private DUPCDefectDao dupcDefectDao;
    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;
    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(commitDefectVO.getStreamName());

        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<DUPCDefectJsonFileEntity<DUPCDefectEntity>>()
        {
        });

        //获取风险系数值
        Map<String, String> riskConfigMap = riskConfigCache.getRiskConfig(ComConstants.Tool.DUPC.name());
        float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        List<DUPCDefectEntity> oldDefectList = dupcDefectRepository.findByTaskIdWithoutBlockList(taskId);
        Map<String, DUPCDefectEntity> oldDefectMap;
        if(CollectionUtils.isNotEmpty(oldDefectList)){
            oldDefectMap = oldDefectList.stream()
                    .collect(Collectors.toMap(defect -> StringUtils.isEmpty(defect.getRelPath()) ? defect.getFilePath() : defect.getRelPath(), Function.identity(), (k, v) -> v));
        } else{
            oldDefectMap = new HashMap<>();
        }

        long curTime = System.currentTimeMillis();

        List<DUPCDefectEntity> defectList = defectJsonFileEntity.getDefects();
        if (CollectionUtils.isNotEmpty(defectList))
        {
            Set<String> filterPaths = filterPathService.getFilterPaths(taskVO, toolName);

            defectList.forEach(dupcDefectEntity ->
            {
                // 填充告警信息
                fillDefectInfo(dupcDefectEntity, commitDefectVO, curTime, fileChangeRecordsMap, codeRepoIdMap);

                Set<String> pathSet = new HashSet<>();
                if (CollectionUtils.isNotEmpty(taskVO.getWhitePaths())) {
                    pathSet.addAll(taskVO.getWhitePaths());
                }
                // 更新告警状态
                updateDefectStatus(dupcDefectEntity, oldDefectMap, filterPaths, pathSet, curTime, m);
            });
        }
        try{
            dupcDefectDao.upsertDupcDefect(defectList);
        } catch (BsonSerializationException e){
            log.info("dupc defect size larger than 16M! task id: {}", commitDefectVO.getTaskId());
        }

        // 余下的是本次没有上报的，需要标志为已修复
        List<DUPCDefectEntity> fixDefectList = Lists.newArrayList();
        if (oldDefectMap.size() > 0)
        {
            oldDefectMap.values().forEach(oldDefect ->
            {
                if (ComConstants.DefectStatus.NEW.value() == oldDefect.getStatus())
                {
                    oldDefect.setStatus(oldDefect.getStatus() | ComConstants.DefectStatus.FIXED.value());
                    oldDefect.setFixedTime(curTime);
                    fixDefectList.add(oldDefect);
                }
            });
        }
        dupcDefectDao.batchFixDefect(taskId, fixDefectList);

        // 保存本次上报文件的告警数据统计数据
        ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, ComConstants.Tool.DUPC.name());
        String baseBuildId = toolBuildInfoEntity != null && StringUtils.isNotEmpty(toolBuildInfoEntity.getDefectBaseBuildId())
                ? toolBuildInfoEntity.getDefectBaseBuildId() : "";

        statistic(defectList, defectJsonFileEntity, taskId, buildId, baseBuildId, riskConfigMap, taskVO, toolName);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 保存质量红线数据
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);
    }

    /**
     * 更新告警状态
     * @param dupcDefectEntity
     * @param oldDefectMap
     * @param filterPaths
     * @param curTime
     * @param m
     */
    private void updateDefectStatus(DUPCDefectEntity dupcDefectEntity,
                                    Map<String, DUPCDefectEntity> oldDefectMap,
                                    Set<String> filterPaths,
                                    Set<String> pathSet,
                                    long curTime,
                                    float m)
    {
        String path = StringUtils.isEmpty(dupcDefectEntity.getRelPath()) ? dupcDefectEntity.getFilePath() : dupcDefectEntity.getRelPath();
        DUPCDefectEntity oldDefect = oldDefectMap.get(path);
        // 已经存在的风险文件
        if (oldDefect != null)
        {
            Integer oldStatus = oldDefect.getStatus();

            // 如果风险文件状态是new且风险系数低于基线，将文件置为已修复
            if (oldStatus == ComConstants.DefectStatus.NEW.value() && dupcDefectEntity.getDupRateValue() < m)
            {
                dupcDefectEntity.setStatus(ComConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(curTime);
            }
            // 如果风险文件状态是closed且风险系数高于基线，将文件置为new
            else if ((oldStatus & ComConstants.DefectStatus.FIXED.value()) > 0 && dupcDefectEntity.getDupRateValue() >= m)
            {
                dupcDefectEntity.setStatus(oldStatus - ComConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(null);
            }
            else
            {
                dupcDefectEntity.setStatus(oldStatus);
            }
            dupcDefectEntity.setEntityId(oldDefect.getEntityId());

            // 余下的是本次没有上报的，需要标志为已修复
            oldDefectMap.remove(path);
        }
        // 新增的风险文件
        else
        {
            dupcDefectEntity.setCreateTime(curTime);
            if (dupcDefectEntity.getDupRateValue() < m)
            {
                dupcDefectEntity.setStatus(ComConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(curTime);
            }
            else
            {
                dupcDefectEntity.setStatus(ComConstants.DefectStatus.NEW.value());
            }
        }

        // 检查是否被路径屏蔽
        checkMaskByPath(dupcDefectEntity, filterPaths, pathSet, curTime);
    }

    /**
     * 保存本次上报文件的告警数据统计数据
     * @param defectList
     * @param defectJsonFileEntity
     * @param taskId
     * @param buildId
     * @param baseBuildId
     * @param riskConfigMap
     */
    private void statistic(List<DUPCDefectEntity> defectList,
                           DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity,
                           long taskId, String buildId, String baseBuildId, Map<String, String> riskConfigMap,
                           TaskDetailVO taskVO, String toolName)
    {
        float sh = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        float h = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        int existCount = 0;
        int superHighCount = 0;
        int highCount = 0;
        int mediumCount = 0;

        // 新增统计项
        int newSuperHighCount = 0;
        int newHighCount = 0;
        int newMediumCount = 0;
        Map<String, DUPCNotRepairedAuthorEntity> newAuthorMap = Maps.newHashMap();
        Map<String, DUPCNotRepairedAuthorEntity> existAuthorMap = Maps.newHashMap();

        log.info("dupc statistic build: {}", buildId);
        if (CollectionUtils.isNotEmpty(defectList))
        {
            long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskVO);
            Iterator<DUPCDefectEntity> it = defectList.iterator();

            // 更新告警状态，并统计告警数量
            while (it.hasNext())
            {
                DUPCDefectEntity defectEntity = it.next();

                if (ComConstants.DefectStatus.NEW.value() != defectEntity.getStatus()) {
                    continue;
                }

                existCount++;
                float dupcRate = defectEntity.getDupRateValue();
                int riskVal = getRiskFactorVal(dupcRate, sh, h, m);
                // scm文件修改时间有可能为空，当null时则认为是新告警处理
                boolean isNewDefectByJudgeTime = defectEntity.getFileChangeTime() == null
                        || defectEntity.getFileChangeTime() >= newDefectJudgeTime;

                // 逻辑区分新旧告警
                if (isNewDefectByJudgeTime) {
                    if (ComConstants.RiskFactor.SH.value() == riskVal) {
                        newSuperHighCount++;
                    } else if (ComConstants.RiskFactor.H.value() == riskVal) {
                        newHighCount++;
                    } else if (ComConstants.RiskFactor.M.value() == riskVal) {
                        newMediumCount++;
                    }
                } else {
                    if (dupcRate >= m && dupcRate < h) {
                        mediumCount++;
                    } else if (dupcRate >= h && dupcRate < sh) {
                        highCount++;
                    } else if (dupcRate >= sh) {
                        superHighCount++;
                    }
                }

                if (StringUtils.isEmpty(defectEntity.getAuthorList())) {
                    continue;
                }

                Set<String> authorSet = null;
                try {
                    authorSet = Sets.newHashSet(StringUtils.split(defectEntity.getAuthorList(), ";"));
                } catch (Exception e) {
                    log.error("get dupc author list fail, source string: {}", defectEntity.getAuthorList(), e);
                }

                if (CollectionUtils.isEmpty(authorSet)) {
                    continue;
                }

                for (String author : authorSet) {
                    String name = author.trim();
                    DUPCNotRepairedAuthorEntity authorEntity;
                    if (isNewDefectByJudgeTime) {
                        authorEntity = newAuthorMap.get(name);
                        if (authorEntity == null) {
                            authorEntity = new DUPCNotRepairedAuthorEntity();
                            authorEntity.setName(name);
                            newAuthorMap.put(name, authorEntity);
                        }
                    } else {
                        authorEntity = existAuthorMap.get(name);
                        if (authorEntity == null) {
                            authorEntity = new DUPCNotRepairedAuthorEntity();
                            authorEntity.setName(name);
                            existAuthorMap.put(name, authorEntity);
                        }
                    }

                    if (ComConstants.RiskFactor.SH.value() == riskVal) {
                        authorEntity.setSuperHighCount(authorEntity.getSuperHighCount() + 1);
                    } else if (ComConstants.RiskFactor.H.value() == riskVal) {
                        authorEntity.setHighCount(authorEntity.getHighCount() + 1);
                    } else if (ComConstants.RiskFactor.M.value() == riskVal) {
                        authorEntity.setMediumCount(authorEntity.getMediumCount() + 1);
                    }
                }
            }
        }
        log.debug("existCount-->{}", existCount);

        long dupLineCount = defectJsonFileEntity.getDupLineCount();
        long rawlineCount = defectJsonFileEntity.getTotalLineCount();
        float dupRate = 0.00F;
        if (rawlineCount != 0)
        {
            dupRate = (float) dupLineCount * 100 / rawlineCount;
        }

        DUPCStatisticEntity baseStatisticEntity = dupcStatisticRepository.findFirstByTaskIdAndBuildId(taskId, baseBuildId);
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
        statisticEntity.setNewSuperHighCount(newSuperHighCount);
        statisticEntity.setNewHighCount(newHighCount);
        statisticEntity.setNewMediumCount(newMediumCount);
        statisticEntity.setNewAuthorStatistic(Lists.newArrayList(newAuthorMap.values()));
        statisticEntity.setExistAuthorStatistic(Lists.newArrayList(existAuthorMap.values()));

        DUPCScanSummaryEntity dupcScanSummary = new DUPCScanSummaryEntity();
        dupcScanSummary.setRawlineCount(rawlineCount);
        dupcScanSummary.setDupLineCount(dupLineCount);
        statisticEntity.setDupcScanSummary(dupcScanSummary);
        dupcStatisticRepository.save(statisticEntity);

        // 获取最近5天重复率趋势
        List<DupcChartTrendEntity> dupcChart = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(ComConstants.Tool.DUPC.name(),
                ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
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

    private void fillDefectInfo(DUPCDefectEntity dupcDefectEntity,
                                CommitDefectVO commitDefectVO,
                                long curTime,
                                Map<String, ScmBlameVO> fileChangeRecordsMap,
                                Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        //设置基础信息
        dupcDefectEntity.setTaskId(commitDefectVO.getTaskId());
        dupcDefectEntity.setToolName(commitDefectVO.getToolName());
        String dupRateStr = dupcDefectEntity.getDupRate();
        float dupRate = Float.valueOf(StringUtils.isEmpty(dupRateStr) ? "0" : dupRateStr.substring(0, dupRateStr.length() - 1));
        dupcDefectEntity.setDupRateValue(dupRate);
        dupcDefectEntity.setLastUpdateTime(curTime);

        //设置相应文件路径及代码库信息
        ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(dupcDefectEntity.getFilePath());
        if (fileLineAuthorInfo != null)
        {
            dupcDefectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
            dupcDefectEntity.setUrl(fileLineAuthorInfo.getUrl());
            dupcDefectEntity.setFileChangeTime(fileLineAuthorInfo.getFileUpdateTime());
            dupcDefectEntity.setRevision(fileLineAuthorInfo.getRevision());
            dupcDefectEntity.setBranch(fileLineAuthorInfo.getBranch());
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
            {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                if(null != repoSubModuleVO){
                    dupcDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                }
            }
            else
            {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getUrl());
                if(null != repoSubModuleVO){
                    dupcDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                    if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                    {
                        dupcDefectEntity.setSubModule(repoSubModuleVO.getSubModule());
                    }
                }

            }

            // 设置作者信息
            setAuthor(dupcDefectEntity, fileLineAuthorInfo);
        }

        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList))
        {
            blockList.forEach(block -> block.setSourceFile(dupcDefectEntity.getFilePath()));
        }

    }

    private void setAuthor(DUPCDefectEntity dupcDefectEntity, ScmBlameVO fileLineAuthorInfo)
    {
        List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList) && CollectionUtils.isNotEmpty(changeRecords))
        {
            // 获取各文件代码行对应的作者信息映射
            Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = getLineAuthorMap(changeRecords);
            if (null != lineAuthorMap)
            {
                blockList.forEach(codeBlockEntity ->
                {
                    Long functionLastUpdateTime = 0L;
                    for (long i = codeBlockEntity.getStartLines(); i <= codeBlockEntity.getEndLines(); i++)
                    {
                        ScmBlameChangeRecordVO recordVO = lineAuthorMap.get(Integer.valueOf(String.valueOf(i)));
                        if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime)
                        {
                            functionLastUpdateTime = recordVO.getLineUpdateTime();
                            codeBlockEntity.setAuthor(ToolParamUtils.trimUserName(recordVO.getAuthor()));
                            codeBlockEntity.setLatestDatetime(recordVO.getLineUpdateTime());
                        }
                    }
                });
            }
            //设置作者清单
            dupcDefectEntity.setAuthorList(blockList.stream().map(CodeBlockEntity::getAuthor).
                    filter(StringUtils::isNotBlank).distinct().reduce((o1, o2) -> String.format("%s;%s", o1, o2)).orElse(""));
        }
    }

    /**
     * 入库前检测屏蔽路径
     *  @param dupcDefectEntity
     * @param filterPaths
     * @param curTime
     * @return
     */
    private boolean checkMaskByPath(DUPCDefectEntity dupcDefectEntity,
                                    Set<String> filterPaths,
                                    Set<String> pathSet,
                                    long curTime) {
        String relPath = dupcDefectEntity.getRelPath();
        String filePath = dupcDefectEntity.getFilePath();

        // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
        if ((dupcDefectEntity.getStatus() & ComConstants.TaskFileStatus.PATH_MASK.value()) == 0
                && (dupcDefectEntity.getStatus() & ComConstants.DefectStatus.FIXED.value()) == 0
                && (PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPaths)
                || (CollectionUtils.isNotEmpty(pathSet)
                && !PathUtils.checkIfMaskByPath(filePath, pathSet))))
        {
            dupcDefectEntity.setStatus(dupcDefectEntity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
            dupcDefectEntity.setExcludeTime(curTime);
            return true;
        }
        return false;
    }

    /**
     * 根据重复率获取风险等级枚举值
     */
    private int getRiskFactorVal(float dupcRate, float sh, float h, float m) {
        if (dupcRate >= sh) {
            return ComConstants.RiskFactor.SH.value();
        } else if (dupcRate < sh && dupcRate >= h) {
            return ComConstants.RiskFactor.H.value();
        } else if (dupcRate < h && dupcRate >= m) {
            return ComConstants.RiskFactor.M.value();
        } else {
            return ComConstants.RiskFactor.L.value();
        }
    }
}
