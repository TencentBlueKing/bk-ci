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

import com.alibaba.fastjson.JSONReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.component.NewLintDefectTracingComponent;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.mongorepository.*;
import com.tencent.bk.codecc.defect.dao.mongotemplate.FileDefectGatherDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class LintDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    @Autowired
    private LintDefectRepository lintDefectRepository;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private BuildDefectService buildDefectService;
    @Autowired
    private NewLintDefectTracingComponent newLintDefectTracingComponent;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private FileDefectGatherRepository fileDefectGatherRepository;
    @Autowired
    private FileDefectGatherDao fileDefectGatherDao;

    @Override
    protected void uploadDefects(CommitDefectVO commitDefectVO, Map<String, ScmBlameVO> fileChangeRecordsMap, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        BuildEntity buildEntity = buildDao.getAndSaveBuildInfo(buildId);

        // 1.解析工具上报的告警文件，并做告警跟踪
        long beginTime = System.currentTimeMillis();
        List<LintFileEntity> gatherFileList = new ArrayList<>();
        Set<String> currentFileSet = parseDefectJsonFile(commitDefectVO, taskVO, buildEntity, fileChangeRecordsMap, codeRepoIdMap, gatherFileList);
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

        // 2.处理告警收敛
        processFileDefectGather(commitDefectVO, gatherFileList, fileChangeRecordsMap, currentFileSet, isFullScan, deleteFiles);

        // 查询所有告警
        beginTime = System.currentTimeMillis();
        List<LintFileEntity> allFileEntityList = lintDefectRepository.findByTaskIdAndToolName(taskId, toolName);
        log.info("find lint file list cost: {}, {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId, allFileEntityList.size());

        // 3.更新文件状态
        beginTime = System.currentTimeMillis();
        updateFileEntityStatus(allFileEntityList, currentFileSet, deleteFiles, isFullScan, buildEntity);
        log.info("update lint file list cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        String baseBuildId;
        if (toolBuildStackEntity == null)
        {
            ToolBuildInfoEntity toolBuildINfoEntity = toolBuildInfoRepository.findByTaskIdAndToolName(taskId, toolName);
            baseBuildId = toolBuildINfoEntity != null && StringUtils.isNotEmpty(toolBuildINfoEntity.getDefectBaseBuildId()) ? toolBuildINfoEntity.getDefectBaseBuildId() : "";
        }
        else
        {
            baseBuildId = StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId()) ? toolBuildStackEntity.getBaseBuildId() : "";
        }

        // 4.统计本次扫描的告警
        beginTime = System.currentTimeMillis();
        statistic(taskVO, toolName, buildId, baseBuildId, allFileEntityList);
        log.info("statistic cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 5.更新构建告警快照
        beginTime = System.currentTimeMillis();
        buildDefectService.updateBaseBuildDefects(taskId, toolName, baseBuildId, buildId, isFullScan, deleteFiles, currentFileSet);
        log.info("buildDefectService.updateBaseBuildDefectsAndClearTemp cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 6.保存质量红线数据
        beginTime = System.currentTimeMillis();
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);
        log.info("redLineReportService.saveRedLineData cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);
    }

    /**
     * 处理文件告警收敛
     * @param commitDefectVO
     * @param gatherFileList
     * @param fileChangeRecordsMap
     * @param currentFileSet
     * @param isFullScan
     * @param deleteFiles
     */
    private void processFileDefectGather(CommitDefectVO commitDefectVO,
                                         List<LintFileEntity> gatherFileList,
                                         Map<String, ScmBlameVO> fileChangeRecordsMap,
                                         Set<String> currentFileSet, boolean isFullScan, List<String> deleteFiles)
    {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        List<FileDefectGatherEntity> allGatherEntityList = fileDefectGatherRepository.findByTaskIdAndToolName(taskId, toolName);
        Map<String, FileDefectGatherEntity> allGatherEntityMap = allGatherEntityList.stream().collect(Collectors.toMap(
                gather -> StringUtils.isEmpty(gather.getRelPath()) ? gather.getFilePath() : gather.getRelPath(), Function.identity(), (k, v) -> v));
        List<FileDefectGatherEntity> needUpsertGatherEntityList = new ArrayList<>();

        long curTime = System.currentTimeMillis();
        if (CollectionUtils.isNotEmpty(gatherFileList))
        {
            FileDefectGatherVO fileDefectGatherVO = new FileDefectGatherVO();
            List<FileDefectGatherVO.GatherFile> gatherFileVOs = new ArrayList<>();
            int totalGatherDefects = 0;
            int totalGatherFiles = 0;
            for (LintFileEntity lintFileEntity : gatherFileList)
            {
                FileDefectGatherEntity gather = lintFileEntity.getGather();
                if (gather.isGatherDetail())
                {
                    fileDefectGatherVO.setFileName(lintFileEntity.getFilePath());
                }
                else
                {
                    needUpsertGatherEntityList.add(gather);
                    String filePath = lintFileEntity.getFilePath();
                    ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(lintFileEntity.getFilePath());
                    String relPath = fileLineAuthorInfo != null ? fileLineAuthorInfo.getFileRelPath() : null;
                    gather.setTaskId(taskId);
                    gather.setToolName(toolName);
                    gather.setStatus(ComConstants.DefectStatus.NEW.value());
                    gather.setFilePath(filePath);
                    gather.setRelPath(relPath);
                    FileDefectGatherEntity oldGather = allGatherEntityMap.get(StringUtils.isEmpty(relPath) ? filePath : relPath);
                    if (null == oldGather)
                    {
                        gather.setCreatedDate(curTime);
                    }
                    else
                    {
                        gather.setCreateTime(oldGather.getCreateTime());
                        gather.setUpdatedDate(curTime);
                    }

                    FileDefectGatherVO.GatherFile gatherFile = new FileDefectGatherVO.GatherFile();
                    BeanUtils.copyProperties(gather, gatherFile);
                    gatherFileVOs.add(gatherFile);

                    totalGatherDefects += gather.getTotal();
                    totalGatherFiles++;

                    currentFileSet.add(StringUtils.isEmpty(gather.getRelPath()) ? gather.getFilePath() : gather.getRelPath());

                    allGatherEntityMap.remove(StringUtils.isEmpty(relPath) ? filePath : relPath);
                }
            }
            fileDefectGatherVO.setGatherFileList(gatherFileVOs);
            fileDefectGatherVO.setDefectCount(totalGatherDefects);
            fileDefectGatherVO.setFileCount(totalGatherFiles);
            commitDefectVO.setMessage(GsonUtils.toJson(fileDefectGatherVO));
        }

        /**
         * 只处理打开状态的收敛文件：
         * 1、文件已删除，则设置为已修复状态
         * 2、全量扫描，且此次分析中没有上报，则设置为已修复状态
         * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
         */
        allGatherEntityMap.forEach((file, gatherEntity) ->
        {
            if (gatherEntity.getStatus() == ComConstants.DefectStatus.NEW.value() && (deleteFiles.contains(gatherEntity.getFilePath()) || isFullScan))
            {
                gatherEntity.setStatus(ComConstants.DefectStatus.FIXED.value());
                gatherEntity.setFixedTime(curTime);
                needUpsertGatherEntityList.add(gatherEntity);
            }
        });
        fileDefectGatherDao.upsertGatherFileListByPath(needUpsertGatherEntityList);
    }

    /**
     * lint类告警json文件格式：
     * [
     * {
     * "filePath": "/data/landun/workspace/game-lab-build/src/components/Ide/src/BlockEditor/spritelib/Actor.js",
     * "defectList":
     * [
     * {
     * "checker": "space-before-blocks",
     * "message": "Missing space before opening brace.",
     * "linenum": 26,
     * "pinpointHash": "6:XY7ATX3lBo7AElcG1C7cQ6cGzMcNGNAiGrQWM/4h25DehIacGACFILnQrWQGZ+Jf:hb3bhr2Q3ncU1HWMRChIXrLQrx3KZa"
     * }
     * ]
     * },
     * {
     * "filePath": "/data/landun/workspace/game-lab-build/src/components/Ide/src/BlockEditor/spritelib/Actor2.js",
     * "gather":
     * {
     * "total": 10000
     * }
     * }
     * ]
     *
     * @param commitDefectVO
     * @param taskVO
     * @param buildEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     * @return
     */
    private Set<String> parseDefectJsonFile(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO,
            BuildEntity buildEntity,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap,
            List<LintFileEntity> gatherFileList)
    {

        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 获取告警文件地址
        String fileIndex = scmJsonComponent.getDefectFileIndex(streamName, toolName, buildId);
        if (StringUtils.isEmpty(fileIndex))
        {
            log.warn("Can not find raw defect file:{}, {}, {}", streamName, toolName, buildId);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format("找不到的告警文件: %s, %s, %s", streamName, toolName, buildId), null);
        }

        File defectFile = new File(fileIndex);
        if (!defectFile.exists())
        {
            log.warn("文件不存在: {}", fileIndex);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format("找不到告警文件: %s", fileIndex), null);
        }
        // 获取规则列表
        List<CheckerDetailEntity> checkerDetailEntityList = checkerRepository.findByToolName(toolName);
        Map<String, Integer> checkerSeverityMap = checkerDetailEntityList.stream().collect(Collectors.toMap(CheckerDetailEntity::getCheckerKey, CheckerDetailEntity::getSeverity));

        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findByTaskId(taskId);
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null)
        {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        Set<String> filterPaths = getFilterPaths(taskVO);

        Set<String> currentFileSet = new HashSet<>();
        // 通过流式读json文件
        try (FileInputStream fileInputStram = new FileInputStream(defectFile);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStram, "UTF-8");
             JSONReader reader = new JSONReader(inputStreamReader))
        {
            reader.startArray();
            int cursor = 0;
            int chunkNo = 0;
            List<LintFileEntity> lintFileList = new ArrayList<>();
            List<Future> asyncResultList = new ArrayList<>();
            while (reader.hasNext())
            {
                LintFileEntity lintFileEntity = reader.readObject(LintFileEntity.class);
                if (CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                {
                    boolean valid = fillFileInfo(lintFileEntity, fileChangeRecordsMap, codeRepoIdMap, checkerSeverityMap);
                    if (valid)
                    {
                        currentFileSet.add(StringUtils.isEmpty(lintFileEntity.getRelPath()) ? lintFileEntity.getFilePath() : lintFileEntity.getRelPath());
                        lintFileList.add(lintFileEntity);
                        cursor += lintFileEntity.getDefectList().size();
                        if (cursor > MAX_PER_BATCH)
                        {
                            // 分批处理告警文件
                            processFileDefect(commitDefectVO, taskVO, lintFileList, filterPaths, buildEntity, chunkNo, transferAuthorList, asyncResultList);
                            cursor = 0;
                            lintFileList = new ArrayList<>();
                            chunkNo++;
                        }
                    }
                }
                else if (lintFileEntity.getGather() != null)
                {
                    gatherFileList.add(lintFileEntity);
                }
                else
                {
                    log.warn("file defectList is empty. {}, {}, {}", taskId, toolName, lintFileEntity.getFilePath());
                }
            }
            reader.endArray();

            if (lintFileList.size() > 0)
            {
                processFileDefect(commitDefectVO, taskVO, lintFileList, filterPaths, buildEntity, chunkNo, transferAuthorList, asyncResultList);
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
                    log.warn("handle file defect fail!{}", commitDefectVO, e);
                }
            });
        }
        catch (IOException e)
        {
            log.warn("Read defect file exception: {}", fileIndex, e);
        }

        return currentFileSet;
    }

    /**
     * 统计本次扫描的告警，需要统计的信息：
     * 1.本次分析遗留告警总数，文件总数，用于跟上一次分析的结果比较，得到最近一次分析结果，用于项目详情页展示，例如： 告警88247(↑38) 文件1796(↑0)
     * 2.当前遗留新告警数，历史告警数，用于数据报表统计每日告警遗留趋势图
     *
     * @param taskVO
     * @param toolName
     * @param buildId
     * @param baseBuildId
     * @param allFileEntityList
     */
    private void statistic(TaskDetailVO taskVO, String toolName, String buildId, String baseBuildId, List<LintFileEntity> allFileEntityList)
    {
        long taskId=taskVO.getTaskId();
        //计算总的告警数
        Long totalDefectCount = 0L;
        int defectCount = 0;
        int fileCount = 0;
        int newDefectCount = 0;
        int historyDefectCount = 0;
        int totalNewSerious = 0;
        int totalNewNormal = 0;
        int totalNewPrompt = 0;
        //modified by xxx 增加历史各严重等级告警的数量统计
        int totalSerious = 0;
        int totalNormal = 0;
        int totalPrompt = 0;
        Map<String, NotRepairedAuthorEntity> authorDefectMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(allFileEntityList))
        {
            // 查询新老告警判定时间
            long newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, toolName, taskVO);
            for (LintFileEntity fileEntity : allFileEntityList)
            {
                if(CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                {
                    totalDefectCount = totalDefectCount + fileEntity.getDefectList().size();
                }
                // 统计本次构建遗留的告警数量和文件数量，以及各作者告警数量
                if (fileEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                {
                    if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                    {
                        int defectCountInFile = 0;
                        for (LintDefectEntity lintDefectEntity : fileEntity.getDefectList())
                        {
                            if (lintDefectEntity.getStatus() != ComConstants.DefectStatus.NEW.value())
                            {
                                continue;
                            }
                            Long lineUpdateTime = lintDefectEntity.getLineUpdateTime();
                            if (lineUpdateTime == null)
                            {
                                lineUpdateTime = lintDefectEntity.getCreateTime();
                            }
                            if (lineUpdateTime >= newDefectJudgeTime)
                            {
                                // 获取作者告警数统计
                                NotRepairedAuthorEntity notRepairedAuthorEntity = null;
                                if (StringUtils.isNotEmpty(lintDefectEntity.getAuthor()))
                                {
                                    notRepairedAuthorEntity = authorDefectMap.get(lintDefectEntity.getAuthor());
                                    if (notRepairedAuthorEntity == null)
                                    {
                                        notRepairedAuthorEntity = new NotRepairedAuthorEntity();
                                        notRepairedAuthorEntity.setName(lintDefectEntity.getAuthor());
                                        authorDefectMap.put(lintDefectEntity.getAuthor(), notRepairedAuthorEntity);
                                    }
                                }

                                // 统计新增告警数
                                newDefectCount++;

                                // 统计新增严重告警数
                                if (ComConstants.SERIOUS == lintDefectEntity.getSeverity())
                                {
                                    totalNewSerious++;
                                    totalSerious++;
                                    if (notRepairedAuthorEntity != null)
                                    {
                                        notRepairedAuthorEntity.setSeriousCount(notRepairedAuthorEntity.getSeriousCount() + 1);
                                    }
                                }

                                // 统计新增一般告警数
                                else if (ComConstants.NORMAL == lintDefectEntity.getSeverity())
                                {
                                    totalNewNormal++;
                                    totalNormal++;
                                    if (notRepairedAuthorEntity != null)
                                    {
                                        notRepairedAuthorEntity.setNormalCount(notRepairedAuthorEntity.getNormalCount() + 1);
                                    }
                                }

                                // 统计新增提示告警数
                                else if (ComConstants.PROMPT_IN_DB == lintDefectEntity.getSeverity() || ComConstants.PROMPT == lintDefectEntity.getSeverity())
                                {
                                    totalNewPrompt++;
                                    totalPrompt++;
                                    if (notRepairedAuthorEntity != null)
                                    {
                                        notRepairedAuthorEntity.setPromptCount(notRepairedAuthorEntity.getPromptCount() + 1);
                                    }
                                }
                                // 统计用户新增告警数总和
                                if (notRepairedAuthorEntity != null)
                                    notRepairedAuthorEntity.setTotalCount(newDefectCount);
                            }
                            else
                            {
                                historyDefectCount++;
                                if(ComConstants.SERIOUS == lintDefectEntity.getSeverity())
                                {
                                    totalSerious++;
                                }
                                else if(ComConstants.NORMAL == lintDefectEntity.getSeverity())
                                {
                                    totalNormal++;
                                }
                                else if(ComConstants.PROMPT_IN_DB == lintDefectEntity.getSeverity() || ComConstants.PROMPT == lintDefectEntity.getSeverity())
                                {
                                    totalPrompt++;
                                }
                            }
                            defectCountInFile++;
                        }
                        if (defectCountInFile > 0)
                        {
                            fileCount++;
                            defectCount += defectCountInFile;
                        }
                    }
                }
            }
        }

        // 作者关联告警统计信息按告警数量排序
        List<NotRepairedAuthorEntity> authorDefects = Lists.newArrayList(authorDefectMap.values());
        Collections.sort(authorDefects, (o1, o2) -> Integer.compare(o2.getTotalCount(), o1.getTotalCount()));

        // 保存本次分析的统计情况
        int defectChange;
        int fileChange;
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);
        if (lastLintStatisticEntity == null)
        {
            defectChange = defectCount;
            fileChange = fileCount;
        }
        else
        {
            defectChange = defectCount - (lastLintStatisticEntity.getDefectCount() == null ? 0 : lastLintStatisticEntity.getDefectCount());
            fileChange = fileCount - (lastLintStatisticEntity.getFileCount() == null ? 0 : lastLintStatisticEntity.getFileCount());
        }

        LintStatisticEntity lintStatisticEntity = new LintStatisticEntity();
        lintStatisticEntity.setTaskId(taskId);
        lintStatisticEntity.setToolName(toolName);
        lintStatisticEntity.setFileCount(fileCount);
        lintStatisticEntity.setDefectCount(defectCount);
        lintStatisticEntity.setNewDefectCount(newDefectCount);
        lintStatisticEntity.setHistoryDefectCount(historyDefectCount);
        lintStatisticEntity.setDefectChange(defectChange);
        lintStatisticEntity.setFileChange(fileChange);
        lintStatisticEntity.setBuildId(buildId);
        lintStatisticEntity.setTotalNewNormal(totalNewNormal);
        lintStatisticEntity.setTotalNewPrompt(totalNewPrompt);
        lintStatisticEntity.setTotalNewSerious(totalNewSerious);
        lintStatisticEntity.setTotalNormal(totalNormal);
        lintStatisticEntity.setTotalPrompt(totalPrompt);
        lintStatisticEntity.setTotalSerious(totalSerious);
        lintStatisticEntity.setTotalDefectCount(totalDefectCount);
        lintStatisticEntity.setAuthorStatistic(authorDefects);

        long currentTime = System.currentTimeMillis();
        lintStatisticEntity.setTime(currentTime);
        lintStatisticRepository.save(lintStatisticEntity);

    }

    /**
     * 更新文件状态
     *
     * @param allFileEntityList
     * @param currentFileSet
     * @param deleteFiles
     * @param isFullScan
     * @param buildEntity
     */
    private void updateFileEntityStatus(List<LintFileEntity> allFileEntityList, Set<String> currentFileSet, List<String> deleteFiles, boolean isFullScan, BuildEntity buildEntity)
    {
        if (CollectionUtils.isNotEmpty(allFileEntityList))
        {
            List<LintFileEntity> needUpdateFileEntityList = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            for (LintFileEntity fileEntity : allFileEntityList)
            {
                String filePath = fileEntity.getFilePath();
                String relPath = fileEntity.getRelPath();

                // 是否是本次上报的告警文件
                boolean notCurrentBuildUpload = CollectionUtils.isEmpty(currentFileSet)
                        || !currentFileSet.contains(StringUtils.isEmpty(relPath) ? filePath : relPath);

                /**
                 * 只处理打开状态的告警文件：
                 * 1、文件已删除，则设置为已修复状态
                 * 2、全量扫描，且此次分析中没有上报，则设置为已修复状态
                 * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
                 */
                if (fileEntity.getStatus() == ComConstants.DefectStatus.NEW.value()
                        && (deleteFiles.contains(filePath) || (isFullScan && notCurrentBuildUpload)))
                {

                    //只有告警全部是已修复状态时，才会将文件状态设置为已修复，否则不会将文件状态设置为修复
                    Boolean updateFileFlag = true;
                    if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                    {
                        for (LintDefectEntity lintDefectEntity : fileEntity.getDefectList())
                        {
                            //只有待修复状态的告警才会设置为已修复
                            if (lintDefectEntity.getStatus() == ComConstants.DefectStatus.NEW.value())
                            {
                                lintDefectEntity.setStatus(lintDefectEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                                lintDefectEntity.setFixedTime(currentTime);
                                lintDefectEntity.setFixedBuildNumber(buildEntity.getBuildNo());
                                lintDefectEntity.setFixedRevision(fileEntity.getRevision());
                                lintDefectEntity.setFixedRepoId(fileEntity.getRepoId());
                                lintDefectEntity.setFixedBranch(fileEntity.getBranch());
                            }
                            //否则不动告警,并且只要有不为已修复状态的告警，则不更新文件状态
                            else if ((lintDefectEntity.getStatus() & ComConstants.DefectStatus.FIXED.value()) == 0)
                            {
                                updateFileFlag = false;
                            }
                        }
                    }

                    if (updateFileFlag)
                    {
                        log.info("need to update file status to fixed, file: {}", filePath);
                        fileEntity.setStatus(fileEntity.getStatus() | ComConstants.DefectStatus.FIXED.value());
                        fileEntity.setFixedTime(currentTime);
                    }
                    needUpdateFileEntityList.add(fileEntity);
                }
            }
            if (CollectionUtils.isNotEmpty(needUpdateFileEntityList))
            {
                lintDefectRepository.save(needUpdateFileEntityList);
            }
        }
    }

    private void processFileDefect(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO,
            List<LintFileEntity> currentLintFileList,
            Set<String> filterPath,
            BuildEntity buildEntity,
            int chunkNo,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
            List<Future> asyncResultList)
    {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        Set<String> filePathSet = currentLintFileList.stream()
                .map(lintFileEntity -> StringUtils.isEmpty(lintFileEntity.getRelPath()) ? lintFileEntity.getFilePath() : lintFileEntity.getRelPath())
                .collect(Collectors.toSet());

        List<LintFileEntity> preLintFileEntityList;
        if (StringUtils.isNotEmpty(currentLintFileList.get(0).getRelPath()))
        {
            preLintFileEntityList = lintDefectRepository.findByTaskIdAndToolNameAndRelPathIn(taskId, toolName, filePathSet);
        }
        else
        {
            preLintFileEntityList = lintDefectRepository.findByTaskIdAndToolNameAndFilePathIn(taskId, toolName, filePathSet);
        }

        // 告警跟踪聚类
        log.info("previous file:{}, current file:{}", preLintFileEntityList.size(), currentLintFileList.size());
        long beginTime = System.currentTimeMillis();
        Future<Boolean> asyncFuture = newLintDefectTracingComponent.defectTracing(
                commitDefectVO,
                taskVO,
                filterPath,
                buildEntity,
                chunkNo,
                preLintFileEntityList,
                currentLintFileList,
                transferAuthorList);

        asyncResultList.add(asyncFuture);
        log.info("async defec tracing(unfinish) cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, commitDefectVO.getBuildId());
    }

    /**
     * 填充文件的信息
     *
     * @param lintFileEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     * @param checkerSeverityMap
     * @return
     */
    private boolean fillFileInfo(LintFileEntity lintFileEntity,
                                 Map<String, ScmBlameVO> fileChangeRecordsMap,
                                 Map<String, RepoSubModuleVO> codeRepoIdMap,
                                 Map<String, Integer> checkerSeverityMap)
    {
        ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(lintFileEntity.getFilePath());
        if (fileLineAuthorInfo != null)
        {
            lintFileEntity.setFileUpdateTime(fileLineAuthorInfo.getFileUpdateTime());
            lintFileEntity.setRevision(fileLineAuthorInfo.getRevision());
            lintFileEntity.setUrl(fileLineAuthorInfo.getUrl());
            lintFileEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
            lintFileEntity.setBranch(fileLineAuthorInfo.getBranch());
            if (MapUtils.isNotEmpty(codeRepoIdMap))
            {
                //如果是svn用rootUrl关联
                if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
                {
                    if (codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl()) != null)
                    {
                        RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                        if(null != repoSubModuleVO){
                            lintFileEntity.setRepoId(repoSubModuleVO.getRepoId());
                        }
                    }
                }
                //其他用rootUrl关联
                else
                {
                    if (codeRepoIdMap.get(lintFileEntity.getUrl()) != null)
                    {
                        RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(lintFileEntity.getUrl());
                        if(null != repoSubModuleVO){
                            lintFileEntity.setRepoId(repoSubModuleVO.getRepoId());
                            if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                            {
                                lintFileEntity.setSubModule(repoSubModuleVO.getSubModule());
                            }
                        }
                    }
                }
            }
        }

        if (StringUtils.isEmpty(lintFileEntity.getSubModule()))
        {
            lintFileEntity.setSubModule("");
        }

        List<LintDefectEntity> defectList = lintFileEntity.getDefectList();

        // 填充文件内的告警的信息，其中如果告警的规则不属于已录入平台的规则，则移除告警
        defectList = defectList.stream().filter(defectEntity -> fillDefectInfo(defectEntity, fileLineAuthorInfo, checkerSeverityMap)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(defectList))
        {
            log.warn("file defectList is empty after filter. {}, {}, {}", lintFileEntity.getTaskId(), lintFileEntity.getToolName(), lintFileEntity.getFilePath());
            return false;
        }

        lintFileEntity.setDefectList(defectList);
        return true;
    }

    /**
     * 填充文件内的告警的信息，其中如果告警的规则不属于已录入平台的规则，则移除告警
     * @param defectEntity
     * @param fileLineAuthorInfo
     * @param checkerSeverityMap
     * @return
     */
    private boolean fillDefectInfo(LintDefectEntity defectEntity, ScmBlameVO fileLineAuthorInfo, Map<String, Integer> checkerSeverityMap)
    {
        Integer severity = checkerSeverityMap.get(defectEntity.getChecker());
        if (severity == null)
        {
            log.warn("Checker invalid! checker: {}", defectEntity.getChecker());
            return false;
        }
        defectEntity.setSeverity(severity);

        if (fileLineAuthorInfo != null)
        {
            List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
            if (CollectionUtils.isNotEmpty(changeRecords))
            {
                int defectLine = defectEntity.getLineNum();
                // 告警中的行号为0的改成1
                if (defectLine == 0)
                {
                    defectLine = 1;
                    defectEntity.setLineNum(defectLine);
                }
                for (ScmBlameChangeRecordVO changeRecord : changeRecords)
                {
                    boolean isFound = false;
                    List<Object> lines = changeRecord.getLines();
                    if (lines != null && lines.size() > 0)
                    {
                        for (Object line : lines)
                        {
                            if (line instanceof Integer && defectLine == (int) line)
                            {
                                isFound = true;
                            }
                            else if (line instanceof List)
                            {
                                List<Integer> lineScope = (List<Integer>) line;
                                if (CollectionUtils.isNotEmpty(lineScope) && lineScope.size() > 1)
                                {
                                    if (lineScope.get(0) <= defectLine && lineScope.get(lineScope.size() - 1) >= defectLine)
                                    {
                                        isFound = true;
                                    }
                                }
                            }
                            if (isFound)
                            {
                                defectEntity.setAuthor(changeRecord.getAuthor());
                                long lineUpdateTime = DateTimeUtils.getThirteenTimestamp(changeRecord.getLineUpdateTime());
                                defectEntity.setLineUpdateTime(lineUpdateTime);
                                break;
                            }
                        }
                    }
                    if (isFound)
                    {
                        break;
                    }
                }
            }
        }
        return true;
    }
}
