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
import com.tencent.bk.codecc.defect.component.NewLintDefectTracingComponent;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.FileDefectGatherRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.FileDefectGatherDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.git.GitRepoApiService;
import com.tencent.bk.codecc.defect.service.statistic.LintDefectStatisticService;
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
@Component("lintDefectCommitConsumer")
@Slf4j
public class LintDefectCommitConsumer extends AbstractDefectCommitConsumer
{
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private BuildDefectService buildDefectService;
    @Autowired
    private NewLintDefectTracingComponent newLintDefectTracingComponent;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private FileDefectGatherRepository fileDefectGatherRepository;
    @Autowired
    private FileDefectGatherDao fileDefectGatherDao;
    @Autowired
    private GitRepoApiService gitRepoApiService;
    @Autowired
    private LintDefectStatisticService lintDefectStatisticService;

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
        List<LintFileV2Entity> gatherFileList = new ArrayList<>();
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
        List<LintDefectV2Entity> allNewDefectList = lintDefectV2Repository.findByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());
        log.info("find lint file list cost: {}, {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId, allNewDefectList.size());

        // 3.更新文件状态
        beginTime = System.currentTimeMillis();
        updateFileEntityStatus(allNewDefectList, currentFileSet, deleteFiles, isFullScan, buildEntity);
        log.info("update lint file list cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 4.统计本次扫描的告警
        beginTime = System.currentTimeMillis();
        lintDefectStatisticService.statistic(taskVO, toolName, buildId, toolBuildStackEntity, allNewDefectList);
        log.info("statistic cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 5.更新构建告警快照
        beginTime = System.currentTimeMillis();
        buildDefectService.saveLintBuildDefect(taskId, toolName, buildEntity, allNewDefectList);
        log.info("saveBuildDefect cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 6.保存质量红线数据
        beginTime = System.currentTimeMillis();
        redLineReportService.saveRedLineData(taskVO, toolName, buildId);
        log.info("redLineReportService.saveRedLineData cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 7.回写工蜂mr信息
        beginTime = System.currentTimeMillis();
        gitRepoApiService.addLintGitCodeAnalyzeComment(taskVO, buildEntity.getBuildId(), buildEntity.getBuildNo(), toolName, currentFileSet);
        log.info("gitRepoApiService.addLintGitCodeAnalyzeComment cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);
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
                                         List<LintFileV2Entity> gatherFileList,
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
            for (LintFileV2Entity lintFileEntity : gatherFileList)
            {
                FileDefectGatherEntity gather = lintFileEntity.getGather();
                if (gather.isGatherDetail())
                {
                    fileDefectGatherVO.setFileName(lintFileEntity.getFile());
                }
                else
                {
                    needUpsertGatherEntityList.add(gather);
                    String filePath = lintFileEntity.getFile();
                    ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(lintFileEntity.getFile());
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
            List<LintFileV2Entity> gatherFileList)
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
            Set<String> eachBatchFilePathSet = new HashSet<>();
            List<LintDefectV2Entity> lintDefectList = new ArrayList<>();
            List<Future> asyncResultList = new ArrayList<>();
            while (reader.hasNext())
            {
                LintFileV2Entity lintFileEntity = reader.readObject(LintFileV2Entity.class);
                if (CollectionUtils.isNotEmpty(lintFileEntity.getDefects()))
                {
                    // 填充文件内的告警的信息，其中如果告警的规则不属于已录入平台的规则，则移除告警
                    List<LintDefectV2Entity> tmpDefectList = lintFileEntity.getDefects().stream()
                            .filter(defect -> fillDefectInfo(defect, lintFileEntity.getFile(), fileChangeRecordsMap.get(lintFileEntity.getFile()), codeRepoIdMap, checkerSeverityMap))
                            .collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(tmpDefectList))
                    {
                        log.warn("file defectList is empty after filter. {}, {}, {}", taskId, toolName, lintFileEntity.getFile());
                        continue;
                    }
                    String filePath = StringUtils.isEmpty(tmpDefectList.get(0).getRelPath()) ? lintFileEntity.getFile() : tmpDefectList.get(0).getRelPath();
                    currentFileSet.add(filePath);
                    eachBatchFilePathSet.add(filePath);
                    lintDefectList.addAll(tmpDefectList);
                    cursor += tmpDefectList.size();
                    if (cursor > MAX_PER_BATCH)
                    {
                        // 分批处理告警文件
                        processFileDefect(commitDefectVO, taskVO, lintDefectList, eachBatchFilePathSet, filterPaths, buildEntity, chunkNo, transferAuthorList, asyncResultList);
                        cursor = 0;
                        lintDefectList = new ArrayList<>();
                        eachBatchFilePathSet = new HashSet<>();
                        chunkNo++;
                    }
                }
                else if (lintFileEntity.getGather() != null)
                {
                    gatherFileList.add(lintFileEntity);
                }
                else
                {
                    log.warn("file defectList is empty. {}, {}, {}", taskId, toolName, lintFileEntity.getFile());
                }
            }
            reader.endArray();

            if (lintDefectList.size() > 0)
            {
                processFileDefect(commitDefectVO, taskVO, lintDefectList, eachBatchFilePathSet, filterPaths, buildEntity, chunkNo, transferAuthorList, asyncResultList);
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
     * 更新告警状态
     *  @param allDefectEntityList
     * @param currentFileSet
     * @param deleteFiles
     * @param isFullScan
     * @param buildEntity
     */
    private void updateFileEntityStatus(List<LintDefectV2Entity> allDefectEntityList, Set<String> currentFileSet, List<String> deleteFiles, boolean isFullScan, BuildEntity buildEntity)
    {
        if (CollectionUtils.isNotEmpty(allDefectEntityList))
        {
            List<LintDefectV2Entity> needUpdateDefectList = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            for (LintDefectV2Entity defect : allDefectEntityList)
            {
                String filePath = defect.getFilePath();
                String relPath = defect.getRelPath();

                // 是否是本次上报的告警文件
                boolean notCurrentBuildUpload = CollectionUtils.isEmpty(currentFileSet)
                        || !currentFileSet.contains(StringUtils.isEmpty(relPath) ? filePath : relPath);

                /**
                 * 1、文件已删除，则设置为已修复状态
                 * 2、全量扫描，且此次分析中没有上报，则设置为已修复状态
                 * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
                 */
                if (deleteFiles.contains(filePath) || (isFullScan && notCurrentBuildUpload))
                {
                    defect.setStatus(defect.getStatus() | ComConstants.DefectStatus.FIXED.value());
                    defect.setFixedTime(currentTime);
                    defect.setFixedBuildNumber(buildEntity.getBuildNo());
                    needUpdateDefectList.add(defect);
                }
            }
            if (CollectionUtils.isNotEmpty(needUpdateDefectList))
            {
                lintDefectV2Repository.save(needUpdateDefectList);
            }
        }
    }

    private void processFileDefect(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO,
            List<LintDefectV2Entity> currentDefectEntityList,
            Set<String> filePathSet,
            Set<String> filterPathSet,
            BuildEntity buildEntity,
            int chunkNo,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
            List<Future> asyncResultList)
    {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();

        List<LintDefectV2Entity> preDefectEntityList;
        if (StringUtils.isNotEmpty(currentDefectEntityList.get(0).getRelPath()))
        {
            preDefectEntityList = lintDefectV2Repository.findByTaskIdAndToolNameAndRelPathIn(taskId, toolName, filePathSet);
        }
        else
        {
            preDefectEntityList = lintDefectV2Repository.findByTaskIdAndToolNameAndFilePathIn(taskId, toolName, filePathSet);
        }

        // 告警跟踪聚类
        log.info("previous defect count:{}, current defect count:{}", preDefectEntityList.size(), currentDefectEntityList.size());
        long beginTime = System.currentTimeMillis();
        Future<Boolean> asyncFuture = newLintDefectTracingComponent.defectTracing(
                commitDefectVO,
                taskVO,
                filterPathSet,
                buildEntity,
                chunkNo,
                preDefectEntityList,
                currentDefectEntityList,
                transferAuthorList);

        asyncResultList.add(asyncFuture);
        log.info("async defec tracing(unfinish) cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, commitDefectVO.getBuildId());
    }

    /**
     * 填充文件内的告警的信息，其中如果告警的规则不属于已录入平台的规则，则移除告警
     *
     * @param defectEntity
     * @param filePath
     * @param fileLineAuthorInfo
     * @param codeRepoIdMap
     * @param checkerSeverityMap
     * @return
     */
    private boolean fillDefectInfo(LintDefectV2Entity defectEntity,
                                   String filePath,
                                   ScmBlameVO fileLineAuthorInfo,
                                   Map<String, RepoSubModuleVO> codeRepoIdMap,
                                   Map<String, Integer> checkerSeverityMap)
    {
        defectEntity.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1)
        {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        defectEntity.setFileName(filePath.substring(fileNameIndex + 1));
        Integer severity = checkerSeverityMap.get(defectEntity.getChecker());
        if (severity == null)
        {
            log.warn("Checker invalid! checker: {}", defectEntity.getChecker());
            return false;
        }
        defectEntity.setSeverity(severity);

        if (fileLineAuthorInfo != null)
        {
            setFileInfo(defectEntity, fileLineAuthorInfo, codeRepoIdMap);

            setAuthor(defectEntity, fileLineAuthorInfo);
        }

        if (StringUtils.isEmpty(defectEntity.getSubModule()))
        {
            defectEntity.setSubModule("");
        }

        return true;
    }

    private void setFileInfo(LintDefectV2Entity defectEntity, ScmBlameVO fileLineAuthorInfo, Map<String, RepoSubModuleVO> codeRepoIdMap)
    {
        defectEntity.setFileUpdateTime(fileLineAuthorInfo.getFileUpdateTime());
        defectEntity.setRevision(fileLineAuthorInfo.getRevision());
        defectEntity.setUrl(fileLineAuthorInfo.getUrl());
        defectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
        defectEntity.setBranch(fileLineAuthorInfo.getBranch());
        if (MapUtils.isNotEmpty(codeRepoIdMap))
        {
            //如果是svn用rootUrl关联
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType()))
            {
                if (codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl()) != null)
                {
                    RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                    if (null != repoSubModuleVO)
                    {
                        defectEntity.setRepoId(repoSubModuleVO.getRepoId());
                    }
                }
            }
            //其他用rootUrl关联
            else
            {
                if (codeRepoIdMap.get(defectEntity.getUrl()) != null)
                {
                    RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(defectEntity.getUrl());
                    if (null != repoSubModuleVO)
                    {
                        defectEntity.setRepoId(repoSubModuleVO.getRepoId());
                        if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule()))
                        {
                            defectEntity.setSubModule(repoSubModuleVO.getSubModule());
                        }
                    }
                }
            }
        }
    }

    private void setAuthor(LintDefectV2Entity defectEntity, ScmBlameVO fileLineAuthorInfo)
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
}
