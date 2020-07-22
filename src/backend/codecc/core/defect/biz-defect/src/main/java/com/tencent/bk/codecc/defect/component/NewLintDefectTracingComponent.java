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

package com.tencent.bk.codecc.defect.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.*;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.BsonSerializationException;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 新的告警跟踪组件
 *
 * @version V1.0
 * @date 2020/4/26
 */
@Slf4j
@Component
public class NewLintDefectTracingComponent extends AbstractDefectTracingComponent<LintFileEntity>
{
    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private BuildDefectDao buildDefectDao;

    /**
     * 告警跟踪
     *
     * @return
     */
    @Override
    @Async("asyncLintDefectTracingExecutor")
    public Future<Boolean> defectTracing(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO, Set<String> filterPath,
            BuildEntity buildEntity,
            int chunkNo,
            List<LintFileEntity> originalFileList,
            List<LintFileEntity> currentFileList,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList)
    {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        String streamName = commitDefectVO.getStreamName();

        //1. 告警去重，去重没去完才需要聚类
        List<LintDefectEntity> defectList = distinctLintDefect(streamName, toolName, buildEntity.getBuildId(), originalFileList, currentFileList);
        log.info("distinct defect list: {}", defectList.size());

        //如果代码没有变化，本次上报的告警全部被去重完，则不需要做聚类，直接根据去重规则判断告警的状态
        if (CollectionUtils.isEmpty(defectList))
        {
            log.info("no file change since last check!");
            List<LintDefectEntity> upsertDefectList = updateOriginalDefectStatus(originalFileList, currentFileList, buildEntity);
            saveDefectFile(taskId, toolName, filterPath, buildEntity, originalFileList, currentFileList, upsertDefectList);
            return new AsyncResult<>(true);
        }

        //2. 拼装聚类入参
        List<AggregateDefectInputModel> defectHashList = defectList.stream().map(lintDefectEntity ->
                new AggregateDefectInputModel(
                        lintDefectEntity.getDefectId(),
                        lintDefectEntity.getChecker(),
                        lintDefectEntity.getPinpointHash(),
                        lintDefectEntity.getFilePath(),
                        lintDefectEntity.getRelPath())
        ).collect(Collectors.toList());

        //3. 做聚类，通过MQ分发到各台服务器上去做聚类，避免聚类都集中到一台服务器上导致服务器资源不足
        Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult = executeCluster(taskVO, toolName, buildEntity.getBuildId(), chunkNo, defectHashList);

        //4. 读取聚类输出文件，并转换告警状态
        List<LintDefectEntity> upsertDefectList = handleWithOutputModel(originalFileList, currentFileList, asyncResult, buildEntity, transferAuthorList);
        log.info("upsert defect list: {}", upsertDefectList.size());

        // 5.分批保存告警
        saveDefectFile(taskId, toolName, filterPath, buildEntity, originalFileList, currentFileList, upsertDefectList);

        return new AsyncResult<>(true);
    }

    /**
     * 代码没有变化，本次上报的告警全部被去重完，则不需要做聚类，直接根据去重规则判断告警的状态
     *
     * @param originalFileList
     * @param currentFileList
     * @param buildEntity
     * @return
     */
    private List<LintDefectEntity> updateOriginalDefectStatus(List<LintFileEntity> originalFileList, List<LintFileEntity> currentFileList, BuildEntity buildEntity)
    {
        Map<String, LintDefectEntity> currentFileMd5Map = currentFileList.stream()
                .filter(lintFileEntity -> lintFileEntity != null && CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                .map(LintFileEntity::getDefectList)
                .flatMap(Collection::parallelStream)
                .collect(Collectors.toMap(lintDefectEntity -> String.format("%s_%s_%s", lintDefectEntity.getFileMd5(), lintDefectEntity.getLineNum(), lintDefectEntity.getChecker()),
                        Function.identity(), (k, v) -> v));

        List<LintDefectEntity> originalDefectList = Lists.newArrayList();
        originalFileList.forEach(lintFileEntity ->
        {
            if (lintFileEntity != null && CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
            {
                lintFileEntity.getDefectList().forEach(defect ->
                {
                    if (StringUtils.isNotEmpty(defect.getPinpointHash()))
                    {
                        originalDefectList.add(defect);
                        /* 告警全部被去重，从去重的新告警中查找：
                         * 1.有找到新告警：
                         *   1.1 老告警是已修复，则变为重新打开
                         *   1.2 老告警是待修复，则将新告警的属性赋值给老告警
                         *   1.2 老告警是其他状态，则不变更直接上报（既然是被去重的，那么新告警和老告警的信息应该是一样的，所以不需要更新）
                         * 2.没找到新告警
                         *   2.1 将状态是NEW的老告警变成已修复
                         *   2.2 老告警是其他状态，则不变更直接上报
                         */
                        String fileMD5 = String.format("%s_%s_%s", defect.getFileMd5(), defect.getLineNum(), defect.getChecker());
                        if (currentFileMd5Map.keySet().contains(fileMD5))
                        {
                            if((defect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0)
                            {
                                reopenDefect(defect);
                            }
                            else if(defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                            {
                                LintDefectEntity newDefect = currentFileMd5Map.get(fileMD5);
                                if(null != newDefect)
                                {
                                    defect.setChecker(newDefect.getChecker());
                                    defect.setLineNum(newDefect.getLineNum());
                                    defect.setMessage(newDefect.getMessage());
                                    defect.setPinpointHash(newDefect.getPinpointHash());
                                }

                            }
                        }
                        else if (!currentFileMd5Map.keySet().contains(fileMD5) && defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                        {
                            fixDefect(buildEntity, defect, lintFileEntity);
                        }
                    }
                });
            }
        });
        return originalDefectList;
    }

    /**
     * 保存告警
     *
     * @param taskId
     * @param toolName
     * @param filterPath
     * @param buildEntity
     * @param originalFileList
     * @param currentFileList
     * @param upsertDefectList
     */
    protected void saveDefectFile(long taskId,
                                  String toolName,
                                  Set<String> filterPath,
                                  BuildEntity buildEntity,
                                  List<LintFileEntity> originalFileList,
                                  List<LintFileEntity> currentFileList,
                                  List<LintDefectEntity> upsertDefectList)
    {
        long beginTime = System.currentTimeMillis();
        log.info("begin saveDefectFile: taskId:{}, toolName:{}, buildId:{}", taskId, toolName, buildEntity.getBuildId());
        // 告警按文件分组
        Map<String, List<LintDefectEntity>> defectGroupByPathMap = upsertDefectList.stream()
                .collect(Collectors.groupingBy(defect -> StringUtils.isEmpty(defect.getRelPath()) ? defect.getFilePath() : defect.getRelPath()));

        AtomicInteger defectCount = new AtomicInteger();
        long curTime = System.currentTimeMillis();
        List<BuildDefectEntity> buildDefectEntityList = new ArrayList<>();
        List<LintFileEntity> finalFileEntityList = new ArrayList<>();

        Map<String, LintFileEntity> originalFileMap = originalFileList.stream()
                .collect(Collectors.toMap(file -> StringUtils.isEmpty(file.getRelPath()) ? file.getFilePath() : file.getRelPath(), Function.identity()));

        // 由于originalFileList是根据currentFileList的文件列表查出来的，所以originalFileList是currentFileList的子集，所以遍历currentFileList来入库
        currentFileList.forEach(currentFile ->
        {
            String path = StringUtils.isEmpty(currentFile.getRelPath()) ? currentFile.getFilePath() : currentFile.getRelPath();

            List<LintDefectEntity> clusterDefectList = null;
            if (defectGroupByPathMap.get(path) != null)
            {
                clusterDefectList = defectGroupByPathMap.get(path);
            }

            // 已存在的告警文件，把新文件的信息更新到老告警文件
            if (originalFileMap.get(path) != null)
            {
                LintFileEntity originalFile = originalFileMap.get(path);
                originalFile.setStatus(ComConstants.TaskFileStatus.NEW.value());
                originalFile.setMd5(currentFile.getMd5());
                originalFile.setRelPath(currentFile.getRelPath());
                originalFile.setBranch(currentFile.getBranch());
                originalFile.setUrl(currentFile.getUrl());
                originalFile.setFilePath(currentFile.getFilePath());
                originalFile.setRepoId(currentFile.getRepoId());
                originalFile.setRevision(currentFile.getRevision());
                originalFile.setSubModule(currentFile.getSubModule());
                originalFile.setFileUpdateTime(currentFile.getFileUpdateTime());
                currentFile = originalFile;
            }
            // 本次新增文件，设置创建时间
            else
            {
                currentFile.setStatus(ComConstants.TaskFileStatus.NEW.value());
                currentFile.setCreateTime(curTime);
            }

            if (CollectionUtils.isNotEmpty(clusterDefectList))
            {
                currentFile.setDefectList(clusterDefectList);
            }

            Set<String> authorSet = new TreeSet<>();
            Set<String> checkerSet = new TreeSet<>();
            currentFile.getDefectList().forEach(defect ->
            {
                checkerSet.add(defect.getChecker());
                if (StringUtils.isNotEmpty(defect.getAuthor()))
                {
                    authorSet.add(defect.getAuthor());
                }

            });
            currentFile.setCheckerList(checkerSet);
            currentFile.setAuthorList(authorSet);
            currentFile.setDefectCount(currentFile.getDefectList().size());
            finalFileEntityList.add(currentFile);

            // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
            checkMaskByPath(currentFile, filterPath, curTime);
            BuildDefectEntity buildDefectEntity = wrappedFileDefectSnatshot(taskId, toolName, buildEntity, currentFile);
            if (buildDefectEntity != null)
            {
                buildDefectEntityList.add(buildDefectEntity);
            }

            defectCount.addAndGet(currentFile.getDefectCount());
        });

        log.info("save defect trace result: taskId:{}, toolName:{}, buildId:{}, fileCount:{}, defectCount:{}", taskId, toolName, buildEntity.getBuildId(), finalFileEntityList.size(), defectCount.get());
        try
        {
            lintDefectDao.upsertDefectListByPath(taskId, toolName, finalFileEntityList);
        }
        catch (BsonSerializationException e)
        {
            log.info("lint defect size larger than 16M! task id: {}", taskId);
        }
        buildDefectDao.upsertByFilePath(buildDefectEntityList);
        log.info("end saveDefectFile, cost:{}, taskId:{}, toolName:{}, buildId:{}", System.currentTimeMillis() - beginTime, taskId, toolName, buildEntity.getBuildId());
    }

    /**
     * 组装文件告警的快照
     *
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param lintFileEntity
     * @return
     */
    private BuildDefectEntity wrappedFileDefectSnatshot(long taskId, String toolName, BuildEntity buildEntity, LintFileEntity lintFileEntity)
    {
        Set<String> fileDefectIds = lintFileEntity.getDefectList().stream()
                .filter(defect -> defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                .map(LintDefectEntity::getDefectId)
                .collect(Collectors.toSet());

        BuildDefectEntity buildDefectEntity = null;
        if (CollectionUtils.isNotEmpty(fileDefectIds))
        {
            buildDefectEntity = new BuildDefectEntity();
            buildDefectEntity.setTaskId(taskId);
            buildDefectEntity.setToolName(toolName);
            buildDefectEntity.setBuildId(buildEntity.getBuildId());
            buildDefectEntity.setBuildNum(buildEntity.getBuildNo());
            buildDefectEntity.setFileRelPath(lintFileEntity.getRelPath());
            buildDefectEntity.setFilePath(lintFileEntity.getFilePath());
            buildDefectEntity.setFileDefectIds(fileDefectIds);
        }

        return buildDefectEntity;
    }

    /**
     * 读取聚类输出文件，并转换告警状态
     *
     * @param originalFileList
     * @param currentFileList
     * @param asyncResult
     * @param buildEntity
     * @param transferAuthorList
     * @return
     */
    protected List<LintDefectEntity> handleWithOutputModel(
            List<LintFileEntity> originalFileList,
            List<LintFileEntity> currentFileList,
            Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult,
            BuildEntity buildEntity, List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList)
    {
        long beginTime = System.currentTimeMillis();
        List<LintDefectEntity> upsertDefectList = new ArrayList<>();
        if (asyncResult != null)
        {
            String outputFile = asyncResult.getFirst();
            log.info("begin handleWithOutputModel: {}", outputFile);
            AsyncRabbitTemplate.RabbitConverterFuture<Boolean> asyncMsgFuture = asyncResult.getSecond();
            try
            {
                if (asyncMsgFuture.get())
                {
                    log.info("return true: {}", outputFile);

                    // 检查聚类output文件是否存在
                    checkOutputFileExists(outputFile);

                    String outputDefects = ScmJsonComponent.readFileContent(outputFile);
                    if (StringUtils.isEmpty(outputDefects))
                    {
                        log.info("empty output defects! output file : {}", outputFile);
                        return upsertDefectList;
                    }
                    List<AggregateDefectOutputModel> outputDefectList = JsonUtil.INSTANCE.to(outputDefects, new TypeReference<List<AggregateDefectOutputModel>>()
                    {
                    });
                    log.info("clustered defect list: {}", outputDefectList.size());

                    if (CollectionUtils.isNotEmpty(outputDefectList))
                    {
                        Map<String, LintDefectEntity> defectMap = new HashMap<>();
                        Map<String, LintDefectEntity> originalDefectMap = originalFileList.stream().map(LintFileEntity::getDefectList)
                                .flatMap(Collection::parallelStream).collect(Collectors.toMap(LintDefectEntity::getDefectId, Function.identity(), (k, v) -> v));
                        defectMap.putAll(originalDefectMap);

                        Map<String, LintDefectEntity> currentDefectMap = currentFileList.stream().map(LintFileEntity::getDefectList)
                                .flatMap(Collection::parallelStream).collect(Collectors.toMap(LintDefectEntity::getDefectId, Function.identity(), (k, v) -> v));
                        defectMap.putAll(currentDefectMap);

                        Map<String, LintFileEntity> originalFileMap = originalFileList.stream()
                                .collect(Collectors.toMap(file -> StringUtils.isEmpty(file.getRelPath()) ? file.getFilePath() : file.getRelPath(), Function.identity()));

                        //将聚类输出格式改为defectEntity
                        List<List<LintDefectEntity>> clusteredDefectList = outputDefectList.stream().map(AggregateDefectOutputModel::getDefects).map(aggregateDefectInputModels ->
                                aggregateDefectInputModels.stream().map(aggregateDefectInputModel -> defectMap.get(aggregateDefectInputModel.getId())).collect(Collectors.toList())
                        ).collect(Collectors.toList());

                        Map<String, LintDefectEntity> currentFileMd5Map = currentDefectMap.values().stream()
                                .filter(lintDefectEntity -> StringUtils.isNotEmpty(lintDefectEntity.getFileMd5()) && StringUtils.isNotBlank(lintDefectEntity.getChecker()))
                                .collect(Collectors.toMap(lintDefectEntity -> String.format("%s_%s_%s",
                                        lintDefectEntity.getFileMd5(), lintDefectEntity.getLineNum(), lintDefectEntity.getChecker()),
                                        Function.identity(), (k, v) -> v));
                        log.info("current file md5 map size: {}", currentFileMd5Map.size());

                        clusteredDefectList.forEach(lintDefectList ->
                        {
                            //将聚类输出分为新告警和历史告警
                            Map<Boolean, List<LintDefectEntity>> partitionedDefects = lintDefectList.stream().collect(Collectors.groupingBy(LintDefectEntity::getNewDefect));
                            List<LintDefectEntity> newDefectList = partitionedDefects.get(true);
                            List<LintDefectEntity> oldDefectList = partitionedDefects.get(false);

                            /* 聚类分组中只有老告警，从去重的新告警中查找：
                             * 1.有找到新告警：
                             *   1.1 老告警是已修复，则变为重新打开
                             *   1.2 老告警是待修复，则要将新告警的属性赋值，并上报
                             *   1.2 老告警是其他状态，则不变更直接上报（既然是被去重的，那么新告警和老告警的信息应该是一样的，所以不需要更新）
                             * 2.没找到新告警
                             *   2.1 将状态是NEW的老告警变成已修复
                             *   2.2 老告警是其他状态，则不变更直接上报
                             */
                            if (CollectionUtils.isEmpty(newDefectList))
                            {
                                oldDefectList.forEach(oldDefect ->
                                {
                                    String fileMD5 = String.format("%s_%s_%s", oldDefect.getFileMd5(), oldDefect.getLineNum(), oldDefect.getChecker());
                                    if (currentFileMd5Map.keySet().contains(fileMD5))
                                    {
                                        if((oldDefect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0)
                                        {
                                            reopenDefect(oldDefect);
                                        }
                                        else if(oldDefect.getStatus() == ComConstants.DefectStatus.NEW.value())
                                        {
                                            LintDefectEntity newDefect = currentFileMd5Map.get(fileMD5);
                                            if(null != newDefect)
                                            {
                                                oldDefect.setChecker(newDefect.getChecker());
                                                oldDefect.setLineNum(newDefect.getLineNum());
                                                oldDefect.setMessage(newDefect.getMessage());
                                                oldDefect.setPinpointHash(newDefect.getPinpointHash());
                                            }
                                        }
                                    }
                                    else if (!currentFileMd5Map.keySet().contains(fileMD5) && oldDefect.getStatus() == ComConstants.DefectStatus.NEW.value())
                                    {
                                        String path = StringUtils.isEmpty(oldDefect.getRelPath()) ? oldDefect.getFilePath() : oldDefect.getRelPath();
                                        fixDefect(buildEntity, oldDefect, originalFileMap.get(path));
                                    }
                                });
                                upsertDefectList.addAll(oldDefectList);
                            }
                            else
                            {
                                /* 先按行号对新旧告警列表排序，然后依序一一对应当做同一个告警，遍历新告警列表：
                                 * 1.有对应老告警：
                                 *   1.1 老告警是已修复，则变为重新打开
                                 *   1.2 老告警是其他状态，则不变更状态直接上报
                                 * 2.无对应老告警
                                 *   2.1 告警是首次创建的告警
                                 * 3.老告警列表比新告警多，部分老告警没有对应的新告警
                                 *   3.1 将状态是NEW的老告警变成已修复
                                 *   3.2 老告警是其他状态，则不变更直接上报
                                 */
                                newDefectList.sort(Comparator.comparingInt(LintDefectEntity::getLineNum));
                                if (CollectionUtils.isNotEmpty(oldDefectList))
                                {
                                    oldDefectList.sort(Comparator.comparingInt(LintDefectEntity::getLineNum));
                                }
                                for (int i = 0; i < newDefectList.size(); i++)
                                {
                                    LintDefectEntity newDefect = newDefectList.get(i);
                                    LintDefectEntity selectedOldDefect = null;
                                    if (CollectionUtils.isNotEmpty(oldDefectList) && oldDefectList.size() > i)
                                    {
                                        selectedOldDefect = oldDefectList.get(i);
                                    }
                                    if (selectedOldDefect != null)
                                    {
                                        selectedOldDefect.setChecker(newDefect.getChecker());
                                        selectedOldDefect.setLineNum(newDefect.getLineNum());
                                        selectedOldDefect.setMessage(newDefect.getMessage());
                                        selectedOldDefect.setPinpointHash(newDefect.getPinpointHash());
                                        if ((selectedOldDefect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0)
                                        {
                                            reopenDefect(selectedOldDefect);
                                        }
                                        if (StringUtils.isEmpty(selectedOldDefect.getAuthor()))
                                        {
                                            selectedOldDefect.setAuthor(newDefect.getAuthor());
                                        }
                                    }
                                    else
                                    {
                                        selectedOldDefect = newDefect;
                                        if (null != buildEntity)
                                        {
                                            selectedOldDefect.setCreateBuildNumber(buildEntity.getBuildNo());
                                        }
                                        selectedOldDefect.setCreateTime(System.currentTimeMillis());
                                        selectedOldDefect.setStatus(ComConstants.DefectStatus.NEW.value());

                                        // 作者转换
                                        if (CollectionUtils.isNotEmpty(transferAuthorList))
                                        {
                                            for (TransferAuthorEntity.TransferAuthorPair trasferAuthorPair : transferAuthorList)
                                            {
                                                String author = selectedOldDefect.getAuthor();
                                                if (StringUtils.isNotEmpty(author) && author.equalsIgnoreCase(trasferAuthorPair.getSourceAuthor()))
                                                {
                                                    selectedOldDefect.setAuthor(trasferAuthorPair.getTargetAuthor());
                                                }
                                            }
                                        }
                                    }
                                    upsertDefectList.add(selectedOldDefect);
                                }

                                // 老告警比新告警多出来的那部分告警变成已修复
                                if (CollectionUtils.isNotEmpty(oldDefectList) && oldDefectList.size() > newDefectList.size())
                                {
                                    List<LintDefectEntity> closeOldDefectList = oldDefectList.subList(newDefectList.size() - 1, oldDefectList.size());
                                    upsertDefectList.addAll(closeOldDefectList);
                                    closeOldDefectList.forEach(defect ->
                                    {
                                        if (defect.getStatus() == ComConstants.DefectStatus.NEW.value())
                                        {
                                            String path = StringUtils.isEmpty(defect.getRelPath()) ? defect.getFilePath() : defect.getRelPath();
                                            fixDefect(buildEntity, defect, originalFileMap.get(path));
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
                else
                {
                    log.warn("return false: {}", outputFile);
                }
            }
            catch (InterruptedException | ExecutionException e)
            {
                log.warn("wait cluster exception: {}", outputFile, e);
            }
        }
        log.info("end handleWithOutputModel, cost: {}", System.currentTimeMillis() - beginTime);
        return upsertDefectList;
    }

    /**
     * 入库前检测屏蔽路径
     *
     * @param lintFileEntity
     * @param filterPath
     * @param curTime
     */
    private void checkMaskByPath(LintFileEntity lintFileEntity, Set<String> filterPath, long curTime)
    {
        String relPath = lintFileEntity.getRelPath();
        String filePath = lintFileEntity.getFilePath();

        // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
        if ((lintFileEntity.getStatus() & ComConstants.TaskFileStatus.PATH_MASK.value()) == 0
                && PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPath))
        {
            boolean isAllPathMark = true;
            for (LintDefectEntity defect : lintFileEntity.getDefectList())
            {
                // 不是已修复状态的告警才需要设置为被路径屏蔽（修复状态和其他非new状态互斥）
                if ((defect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0)
                {
                    isAllPathMark = false;
                }
                else
                {
                    defect.setStatus(defect.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
                    defect.setExcludeTime(curTime);
                }
            }

            // 文件中所有告警都是已屏蔽，文件状态才是已屏蔽
            if (isAllPathMark)
            {
                lintFileEntity.setStatus(lintFileEntity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
                lintFileEntity.setExcludeTime(curTime);
            }
        }
        // 如果已经是被路径屏蔽的，但是实质没有被路径屏蔽，则要把屏蔽状态去掉
        else if (!PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPath))
        {
            if ((lintFileEntity.getStatus() & ComConstants.TaskFileStatus.PATH_MASK.value()) > 0)
            {
                lintFileEntity.setStatus(lintFileEntity.getStatus() - ComConstants.TaskFileStatus.PATH_MASK.value());
            }

            lintFileEntity.getDefectList().forEach(defect ->
            {
                if ((defect.getStatus() & ComConstants.DefectStatus.PATH_MASK.value()) > 0)
                {
                    defect.setStatus(lintFileEntity.getStatus() - ComConstants.DefectStatus.PATH_MASK.value());
                }
            });
        }
    }

    /**
     * 告警去重后，得到并集的告警清单
     *
     * @param streamName
     * @param toolName
     * @param buildId
     * @param originalLintFile
     * @param currentLintFile
     * @return
     */
    private List<LintDefectEntity> distinctLintDefect(
            String streamName,
            String toolName,
            String buildId,
            List<LintFileEntity> originalLintFile,
            List<LintFileEntity> currentLintFile)
    {
        Map<String, String> fileMd5Map = getFIleMd5Map(streamName, toolName, buildId);

        Map<String, String> relPathMap = new HashMap<>();
        currentLintFile.forEach(lintFileEntity ->
        {
            if (lintFileEntity != null)
            {
                lintFileEntity.setMd5(fileMd5Map.get(lintFileEntity.getFilePath()));
                relPathMap.put(lintFileEntity.getRelPath(), lintFileEntity.getFilePath());
                if (CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                {
                    lintFileEntity.getDefectList().forEach(defect ->
                    {
                        defect.setFileMd5(lintFileEntity.getMd5());
                        defect.setRelPath(lintFileEntity.getRelPath());
                        defect.setFilePath(lintFileEntity.getFilePath());
                        defect.setDefectId(ObjectId.get().toString());
                        defect.setNewDefect(true);
                        if (0 == defect.getStatus())
                        {
                            defect.setStatus(ComConstants.DefectStatus.NEW.value());
                        }
                    });
                }
            }
        });

        originalLintFile.forEach(lintFileEntity ->
        {
            String filePath = relPathMap.get(lintFileEntity.getRelPath());
            if (CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
            {
                lintFileEntity.getDefectList().forEach(defect ->
                {
                    defect.setFileMd5(lintFileEntity.getMd5());
                    defect.setRelPath(lintFileEntity.getRelPath());
                    defect.setFilePath(StringUtils.isEmpty(filePath) ? lintFileEntity.getFilePath() : filePath);
                    defect.setNewDefect(false);
                    //第一次告警跟踪，原有id为空需要兼容
                    if (StringUtils.isEmpty(defect.getDefectId()))
                    {
                        defect.setDefectId(ObjectId.get().toString());
                    }
                    if (0 == defect.getStatus())
                    {
                        defect.setStatus(ComConstants.DefectStatus.NEW.value());
                    }
                });
            }
        });

        // 过滤掉pinpointHash为空的告警，用于兼容（原来的没有上报的告警也要进行）
        List<LintDefectEntity> originalDefectList = originalLintFile.stream()
                .map(lintFileEntity -> lintFileEntity.getDefectList())
                .flatMap(Collection::parallelStream)
                .filter(lintDefectEntity -> StringUtils.isNotEmpty(lintDefectEntity.getPinpointHash()))
                .collect(Collectors.toList());

        Set<String> originalFileMd5Set = originalDefectList.stream()
                .filter(lintDefectEntity -> StringUtils.isNotEmpty(lintDefectEntity.getFileMd5()))
                .map(lintDefectEntity -> String.format("%s_%s_%s", lintDefectEntity.getFileMd5(), lintDefectEntity.getLineNum(), lintDefectEntity.getChecker()))
                .collect(Collectors.toSet());

        //去重：md5、行号、规则一样的，直接去掉
        List<LintDefectEntity> currDefectList = currentLintFile.stream()
                .filter(lintFileEntity -> lintFileEntity != null && CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                .map(lintFileEntity -> lintFileEntity.getDefectList())
                .flatMap(Collection::parallelStream)
                .filter(lintDefectEntity -> StringUtils.isNotEmpty(lintDefectEntity.getPinpointHash())
                        && !originalFileMd5Set.contains(String.format("%s_%s_%s", lintDefectEntity.getFileMd5(), lintDefectEntity.getLineNum(), lintDefectEntity.getChecker())))
                .collect(Collectors.toList());

        List<LintDefectEntity> finalDefectFile = new ArrayList();

        log.info("originalDefectList size: {}", originalDefectList.size());
        log.info("currDefectList size: {}", currDefectList.size());
        // 本次上报的都被去重了，表示文件完全没有变化，则不需要做聚类，避免空耗
        if (CollectionUtils.isNotEmpty(currDefectList))
        {
            finalDefectFile.addAll(currDefectList);
            if (CollectionUtils.isNotEmpty(originalDefectList))
            {
                finalDefectFile.addAll(originalDefectList);
            }
        }

        return finalDefectFile;
    }


    /**
     * 将告警设置为已修复
     *
     * @param buildEntity
     * @param defect
     * @param fileEntity
     */
    protected void fixDefect(BuildEntity buildEntity, LintDefectEntity defect, LintFileEntity fileEntity)
    {
        defect.setStatus(defect.getStatus() | ComConstants.DefectStatus.FIXED.value());
        defect.setFixedTime(System.currentTimeMillis());
        if (null != buildEntity)
        {
            defect.setFixedBuildNumber(buildEntity.getBuildNo());
        }
        if (fileEntity != null)
        {
            defect.setFixedRevision(fileEntity.getRevision());
            defect.setFixedRepoId(fileEntity.getRepoId());
            defect.setFixedBranch(fileEntity.getBranch());
        }

    }

    /**
     * 重新打开告警
     *
     * @param oldDefect
     */
    protected void reopenDefect(LintDefectEntity oldDefect)
    {
        oldDefect.setStatus(ComConstants.DefectStatus.NEW.value());
        oldDefect.setFixedTime(null);
        oldDefect.setFixedBuildNumber(null);
        oldDefect.setFixedRevision(null);
        oldDefect.setFixedRepoId(null);
        oldDefect.setFixedBranch(null);
    }

}
