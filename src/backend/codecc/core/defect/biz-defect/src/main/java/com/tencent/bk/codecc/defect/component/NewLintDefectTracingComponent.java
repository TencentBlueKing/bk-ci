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
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.util.PathUtils;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
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
public class NewLintDefectTracingComponent extends AbstractDefectTracingComponent<LintDefectV2Entity> {
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private DefectIdGenerator defectIdGenerator;

    /**
     * 告警跟踪
     *
     * @return
     */
    @Deprecated
    @Async("asyncLintDefectTracingExecutor")
    public Future<Boolean> defectTracing(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO, Set<String> filterPathSet,
            BuildEntity buildEntity,
            int chunkNo,
            List<LintDefectV2Entity> originalDefectList,
            List<LintDefectV2Entity> currentDefectList,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        String streamName = commitDefectVO.getStreamName();

        //1. 告警去重，去重没去完才需要聚类
        List<LintDefectV2Entity> defectList = distinctLintDefect(streamName, toolName, buildEntity.getBuildId(),
                originalDefectList, currentDefectList);
        log.info("distinct defect list: {}", defectList.size());

        //如果代码没有变化，本次上报的告警全部被去重完，则不需要做聚类，直接根据去重规则判断告警的状态
        if (CollectionUtils.isEmpty(defectList)) {
            log.info("no file change since last check!");
            List<LintDefectV2Entity> upsertDefectList = updateOriginalDefectStatus(originalDefectList,
                    currentDefectList,
                    new HashSet<>(taskVO.getWhitePaths()),
                    filterPathSet,
                    buildEntity,
                    transferAuthorList);
            saveDefectFile(taskId,
                    toolName,
                    filterPathSet,
                    new HashSet<>(taskVO.getWhitePaths()),
                    buildEntity,
                    upsertDefectList);

            return new AsyncResult<>(true);
        }

        //2. 拼装聚类入参
        List<AggregateDefectInputModel> defectHashList = defectList.stream().map(lintDefectEntity ->
                new AggregateDefectInputModel(
                        lintDefectEntity.getEntityId(),
                        lintDefectEntity.getChecker(),
                        lintDefectEntity.getPinpointHash(),
                        lintDefectEntity.getFilePath(),
                        lintDefectEntity.getRelPath())
        ).collect(Collectors.toList());

        //3. 做聚类，通过MQ分发到各台服务器上去做聚类，避免聚类都集中到一台服务器上导致服务器资源不足
        /*Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult = executeCluster(taskVO,
                toolName, buildEntity.getBuildId(), chunkNo, defectHashList);*/

        //4. 读取聚类输出文件，并转换告警状态
        /*List<LintDefectV2Entity> upsertDefectList = handleWithOutputModel(originalDefectList, currentDefectList,
                asyncResult, buildEntity, transferAuthorList);
        log.info("upsert defect list: {}", upsertDefectList.size());*/

        // 5.分批保存告警
        /*saveDefectFile(taskId,
                toolName,
                filterPathSet,
                taskVO.getPathList(),
                buildEntity,
                upsertDefectList);*/

        return new AsyncResult<>(true);
    }

    /**
     * 代码没有变化，本次上报的告警全部被去重完，则不需要做聚类，直接根据去重规则判断告警的状态
     *
     * @param originalDefectList
     * @param currentDefectList
     * @param buildEntity
     * @param transferAuthorList
     * @return
     */
    @Deprecated
    private List<LintDefectV2Entity> updateOriginalDefectStatus(List<LintDefectV2Entity> originalDefectList,
                                                                List<LintDefectV2Entity> currentDefectList,
                                                                Set<String> pathList,
                                                                Set<String> filterPaths,
                                                                BuildEntity buildEntity,
                                                                List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList)
    {
        Map<String, LintDefectV2Entity> currentFileMd5Map = currentDefectList.stream()
                .collect(Collectors.toMap(defect -> String.format("%s_%s_%s", defect.getFileMd5(),
                        defect.getLineNum(), defect.getChecker()), Function.identity(), (k, v) -> v));

        List<LintDefectV2Entity> upsertDefectList = new ArrayList<>();
        originalDefectList.forEach(defect -> {
            if (StringUtils.isNotEmpty(defect.getPinpointHash())) {
                /* 告警全部被去重，从去重的新告警中查找：
                 * 1.有找到新告警：
                 *   1.1 老告警是已修复，则变为重新打开
                 *   1.2 老告警是待修复，则将新告警的属性赋值给老告警
                 *   1.3 老告警是其他状态，则不变更直接上报（既然是被去重的，那么新告警和老告警的信息应该是一样的，所以不需要更新）
                 * 2.没找到新告警
                 *   2.1 将状态是NEW的老告警变成已修复
                 *   2.2 老告警是其他状态，则不变更直接上报
                 */
                String fileMD5 = String.format("%s_%s_%s", defect.getFileMd5(), defect.getLineNum(),
                        defect.getChecker());
                LintDefectV2Entity newDefect = currentFileMd5Map.get(fileMD5);
                if (newDefect != null) {
                    if ((defect.getStatus() & DefectStatus.FIXED.value()) > 0
                            || (defect.getStatus() & DefectStatus.PATH_MASK.value()) > 0) {
                        reopenDefect(defect);
                        updateOldDefectInfo(defect, newDefect, transferAuthorList);
                        upsertDefectList.add(defect);
                    } else if (defect.getStatus() == DefectStatus.NEW.value()) {
                        updateOldDefectInfo(defect, newDefect, transferAuthorList);
                        upsertDefectList.add(defect);
                    }
                } else if (defect.getStatus() == DefectStatus.NEW.value()) {
                    fixDefect(defect, buildEntity);
                    upsertDefectList.add(defect);
                } else if (checkMaskByPath(defect, filterPaths, pathList, System.currentTimeMillis())) {
                    updateOldDefectInfo(defect, newDefect, transferAuthorList);
                    upsertDefectList.add(defect);
                }
            }
        });
        return upsertDefectList;
    }

    /**
     * 保存告警
     *
     * @param taskId
     * @param toolName
     * @param filterPathSet
     * @param buildEntity
     * @param upsertDefectList
     */
    protected void saveDefectFile(long taskId,
                                  String toolName,
                                  Set<String> filterPathSet,
                                  Set<String> pathList,
                                  BuildEntity buildEntity,
                                  List<LintDefectV2Entity> upsertDefectList) {
        long beginTime = System.currentTimeMillis();
        log.info("begin saveDefectFile: taskId:{}, toolName:{}, buildId:{}", taskId, toolName,
                buildEntity.getBuildId());

        if (CollectionUtils.isNotEmpty(upsertDefectList)) {
            List<LintDefectV2Entity> needGenerateDefectIdList = new ArrayList<>();
            long curTime = System.currentTimeMillis();
            upsertDefectList.forEach(defect ->
            {
                defect.setTaskId(taskId);
                defect.setToolName(toolName);
                defect.setUpdatedDate(curTime);
                checkMaskByPath(defect, filterPathSet, pathList, curTime);

                if (StringUtils.isEmpty(defect.getId())) {
                    needGenerateDefectIdList.add(defect);
                }
            });

            // 初始化告警ID
            if (CollectionUtils.isNotEmpty(needGenerateDefectIdList)) {
                int increment = needGenerateDefectIdList.size();
                Long currMaxId = defectIdGenerator.generateDefectId(taskId, toolName, increment);
                AtomicLong currMinIdAtom = new AtomicLong(currMaxId - increment + 1);
                needGenerateDefectIdList.forEach(defect -> defect.setId(String.valueOf(currMinIdAtom.getAndIncrement())));
            }

            log.info("save defect trace result: taskId:{}, toolName:{}, buildId:{}, defectCount:{}", taskId, toolName,
                    buildEntity.getBuildId(), upsertDefectList.size());
            lintDefectV2Repository.saveAll(upsertDefectList);
        }
        log.info("end saveDefectFile, cost:{}, taskId:{}, toolName:{}, buildId:{}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildEntity.getBuildId());
    }

    /**
     * 读取聚类输出文件，并转换告警状态
     *
     * @see LintDefectCommitComponent#postHandleDefectList(List, BuildEntity, List)
     * @param originalDefectList
     * @param currentDefectList
     * @param asyncResult
     * @param buildEntity
     * @param transferAuthorList
     * @return
     */
    @Deprecated
    protected List<LintDefectV2Entity> handleWithOutputModel(
            List<LintDefectV2Entity> originalDefectList,
            List<LintDefectV2Entity> currentDefectList,
            Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult,
            BuildEntity buildEntity, List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        long beginTime = System.currentTimeMillis();
        List<LintDefectV2Entity> upsertDefectList = new ArrayList<>();
        if (asyncResult != null) {
            String outputFile = asyncResult.getFirst();
            log.info("begin handleWithOutputModel: {}", outputFile);
            AsyncRabbitTemplate.RabbitConverterFuture<Boolean> asyncMsgFuture = asyncResult.getSecond();
            try {
                if (asyncMsgFuture.get()) {
                    log.info("return true: {}", outputFile);

                    // 检查聚类output文件是否存在
                    checkOutputFileExists(outputFile);

                    String outputDefects = ScmJsonComponent.readFileContent(outputFile);
                    if (StringUtils.isEmpty(outputDefects)) {
                        log.info("empty output defects! output file : {}", outputFile);
                        return upsertDefectList;
                    }
                    List<AggregateDefectOutputModel> outputDefectList = JsonUtil.INSTANCE.to(outputDefects,
                            new TypeReference<List<AggregateDefectOutputModel>>() {
                            });
                    log.info("clustered defect list: {}", outputDefectList.size());

                    if (CollectionUtils.isNotEmpty(outputDefectList)) {
                        Map<String, LintDefectV2Entity> defectMap = new HashMap<>();
                        Map<String, LintDefectV2Entity> originalDefectMap = originalDefectList.stream()
                                .collect(Collectors.toMap(LintDefectV2Entity::getEntityId, Function.identity(),
                                        (k, v) -> v));
                        defectMap.putAll(originalDefectMap);

                        Map<String, LintDefectV2Entity> currentDefectMap = currentDefectList.stream()
                                .collect(Collectors.toMap(LintDefectV2Entity::getEntityId, Function.identity(),
                                        (k, v) -> v));
                        defectMap.putAll(currentDefectMap);

                        //将聚类输出格式改为defectEntity
                        List<List<LintDefectV2Entity>> clusteredDefectList = outputDefectList.stream()
                                .map(AggregateDefectOutputModel::getDefects)
                                .map(aggregateDefectInputModels -> aggregateDefectInputModels.stream()
                                        .map(aggregateInputModel -> defectMap.get(aggregateInputModel.getId()))
                                        .collect(Collectors.toList())
                                ).collect(Collectors.toList());

                        Map<String, LintDefectV2Entity> currentFileMd5Map = currentDefectMap.values().stream()
                                .filter(defect -> StringUtils.isNotEmpty(defect.getFileMd5())
                                        && StringUtils.isNotBlank(defect.getChecker()))
                                .collect(Collectors.toMap(defect -> String.format("%s_%s_%s", defect.getFileMd5(),
                                        defect.getLineNum(), defect.getChecker()),
                                        Function.identity(), (k, v) -> v));
                        log.info("current file md5 map size: {}", currentFileMd5Map.size());

                        clusteredDefectList.forEach(defectList ->
                        {
                            //将聚类输出分为新告警和历史告警
                            Map<Boolean, List<LintDefectV2Entity>> partitionedDefects = defectList.stream()
                                    .collect(Collectors.groupingBy(LintDefectV2Entity::getNewDefect));
                            List<LintDefectV2Entity> newDefectList = partitionedDefects.get(true);
                            List<LintDefectV2Entity> oldDefectList = partitionedDefects.get(false);

                            /* 聚类分组中只有老告警，从去重的新告警中查找：
                             * 1.有找到新告警：
                             *   1.1 老告警是已修复，则变为重新打开
                             *   1.2 老告警是待修复，则要将新告警的属性赋值，并上报
                             *   1.3 老告警是其他状态，则不变更直接上报（既然是被去重的，那么新告警和老告警的信息应该是一样的，所以不需要更新）
                             * 2.没找到新告警
                             *   2.1 将状态是NEW的老告警变成已修复
                             *   2.2 老告警是其他状态，则不变更直接上报
                             */
                            if (CollectionUtils.isEmpty(newDefectList)) {
                                oldDefectList.forEach(oldDefect ->
                                {
                                    String fileMD5 = String.format("%s_%s_%s", oldDefect.getFileMd5(),
                                            oldDefect.getLineNum(), oldDefect.getChecker());
                                    LintDefectV2Entity newDefect = currentFileMd5Map.get(fileMD5);
                                    if (newDefect != null) {
                                        if ((oldDefect.getStatus() & DefectStatus.FIXED.value()) > 0) {
                                            reopenDefect(oldDefect);
                                            updateOldDefectInfo(oldDefect, newDefect, transferAuthorList);
                                            upsertDefectList.add(oldDefect);
                                        } else if (oldDefect.getStatus() == DefectStatus.NEW.value()) {
                                            // 用新告警的信息更新老告警信息
                                            updateOldDefectInfo(oldDefect, newDefect, transferAuthorList);
                                            upsertDefectList.add(oldDefect);
                                        }
                                    } else if (newDefect == null
                                            && oldDefect.getStatus() == DefectStatus.NEW.value()) {
                                        fixDefect(oldDefect, buildEntity);
                                        upsertDefectList.add(oldDefect);
                                    }
                                });
                            } else {
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
                                newDefectList.sort(Comparator.comparingInt(LintDefectV2Entity::getLineNum));
                                if (CollectionUtils.isNotEmpty(oldDefectList)) {
                                    oldDefectList.sort(Comparator.comparingInt(LintDefectV2Entity::getLineNum));
                                }
                                for (int i = 0; i < newDefectList.size(); i++) {
                                    LintDefectV2Entity newDefect = newDefectList.get(i);
                                    LintDefectV2Entity selectedOldDefect = null;
                                    if (CollectionUtils.isNotEmpty(oldDefectList) && oldDefectList.size() > i) {
                                        selectedOldDefect = oldDefectList.get(i);
                                    }
                                    if (selectedOldDefect != null) {
                                        // 用新告警的信息更新老告警信息
                                        updateOldDefectInfo(selectedOldDefect, newDefect, transferAuthorList);

                                        if ((selectedOldDefect.getStatus()
                                                & DefectStatus.FIXED.value()) > 0) {
                                            reopenDefect(selectedOldDefect);
                                        }
                                        if (StringUtils.isEmpty(selectedOldDefect.getAuthor())) {
                                            selectedOldDefect.setAuthor(newDefect.getAuthor());
                                        }
                                    } else {
                                        selectedOldDefect = newDefect;
                                        selectedOldDefect.setCreateTime(System.currentTimeMillis());
                                        selectedOldDefect.setStatus(DefectStatus.NEW.value());
                                        if (null != buildEntity) {
                                            selectedOldDefect.setCreateBuildNumber(buildEntity.getBuildNo());
                                        }
                                        // 作者转换
                                        transferAuthor(selectedOldDefect, transferAuthorList);
                                    }
                                    upsertDefectList.add(selectedOldDefect);
                                }

                                // 老告警比新告警多出来的那部分告警变成已修复
                                if (CollectionUtils.isNotEmpty(oldDefectList)
                                        && oldDefectList.size() > newDefectList.size()) {
                                    List<LintDefectV2Entity> closeOldDefectList =
                                            oldDefectList.subList(newDefectList.size(), oldDefectList.size());
                                    closeOldDefectList.forEach(defect ->
                                    {
                                        if (defect.getStatus() == DefectStatus.NEW.value()) {
                                            fixDefect(defect, buildEntity);
                                            upsertDefectList.add(defect);
                                        }
                                    });
                                }
                            }
                        });
                    }
                } else {
                    log.warn("return false: {}", outputFile);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.warn("wait cluster exception: {}", outputFile, e);
            }
        }
        log.info("end handleWithOutputModel, cost: {}", System.currentTimeMillis() - beginTime);
        return upsertDefectList;
    }

    /**
     * 用新告警的信息更新老告警信息
     *
     * @param selectedOldDefect
     * @param newDefect
     * @param transferAuthorList
     */
    private void updateOldDefectInfo(LintDefectV2Entity selectedOldDefect, LintDefectV2Entity newDefect,
                                     List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        if (newDefect == null) {
            return;
        }
        selectedOldDefect.setChecker(newDefect.getChecker());
        selectedOldDefect.setLineNum(newDefect.getLineNum());
        selectedOldDefect.setMessage(newDefect.getMessage());
        selectedOldDefect.setPinpointHash(newDefect.getPinpointHash());
        selectedOldDefect.setLineUpdateTime(newDefect.getLineUpdateTime());
        selectedOldDefect.setFilePath(newDefect.getFilePath());
        selectedOldDefect.setRelPath(newDefect.getRelPath());
        selectedOldDefect.setUrl(newDefect.getUrl());
        selectedOldDefect.setRepoId(newDefect.getRepoId());
        selectedOldDefect.setRevision(newDefect.getRevision());
        selectedOldDefect.setBranch(newDefect.getBranch());
        selectedOldDefect.setSubModule(newDefect.getSubModule());
        selectedOldDefect.setFileUpdateTime(newDefect.getFileUpdateTime());
        selectedOldDefect.setFileMd5(newDefect.getFileMd5());
        if (StringUtils.isEmpty(selectedOldDefect.getAuthor())) {
            selectedOldDefect.setAuthor(newDefect.getAuthor());

            // 作者转换
            transferAuthor(selectedOldDefect, transferAuthorList);
        }
    }

    /**
     * 入库前检测屏蔽路径
     *
     * @param lintDefectV2Entity
     * @param filterPaths
     * @param curTime
     */
    private boolean checkMaskByPath(LintDefectV2Entity lintDefectV2Entity,
                                    Set<String> filterPaths,
                                    Set<String> pathList,
                                    long curTime) {
        String relPath = lintDefectV2Entity.getRelPath();
        String filePath = lintDefectV2Entity.getFilePath();

        // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
        if ((lintDefectV2Entity.getStatus() & ComConstants.TaskFileStatus.PATH_MASK.value()) == 0
                && (lintDefectV2Entity.getStatus() & DefectStatus.FIXED.value()) == 0
                && (PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPaths)
                || (CollectionUtils.isNotEmpty(pathList)
                && !PathUtils.checkIfMaskByPath(filePath, pathList)))) {
            lintDefectV2Entity.setStatus(lintDefectV2Entity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
            lintDefectV2Entity.setExcludeTime(curTime);
            return true;
        }
        // 如果已经是被路径屏蔽的，但是实质没有被路径屏蔽，则要把屏蔽状态去掉
        else if ((CollectionUtils.isEmpty(pathList)
                || PathUtils.checkIfMaskByPath(filePath, pathList))
                && !PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPaths)
                && (lintDefectV2Entity.getStatus() & ComConstants.TaskFileStatus.PATH_MASK.value()) > 0) {
            lintDefectV2Entity.setStatus(lintDefectV2Entity.getStatus() - ComConstants.TaskFileStatus.PATH_MASK.value());
            return true;
        }
        return false;
    }

    /**
     * 告警去重后，得到并集的告警清单
     *
     * @see LintDefectCommitComponent#preHandleDefectList(String, String, String, List, List, Map)
     * @param streamName
     * @param toolName
     * @param buildId
     * @param originalDefectList
     * @param currentDefectList
     * @return
     */
    @Deprecated
    private List<LintDefectV2Entity> distinctLintDefect(
            String streamName,
            String toolName,
            String buildId,
            List<LintDefectV2Entity> originalDefectList,
            List<LintDefectV2Entity> currentDefectList) {
        Map<String, String> fileMd5Map = getFIleMd5Map(streamName, toolName, buildId);

        Map<String, String> relPathMap = new HashMap<>();
        currentDefectList.forEach(defect ->
        {
            if (StringUtils.isNotEmpty(defect.getRelPath())) {
                relPathMap.put(defect.getRelPath(), defect.getFilePath());
            }
            defect.setFileMd5(fileMd5Map.get(defect.getFilePath()));
            defect.setEntityId(ObjectId.get().toString());
            defect.setNewDefect(true);
            if (0 == defect.getStatus()) {
                defect.setStatus(DefectStatus.NEW.value());
            }
        });

        // 过滤掉pinpointHash为空的告警，用于兼容（原来的没有上报的告警也要进行）
        originalDefectList =
                originalDefectList.stream().filter(defect -> StringUtils.isNotEmpty(defect.getPinpointHash()))
                        .map(defect -> {
                            defect.setNewDefect(false);
                            if (0 == defect.getStatus()) {
                                defect.setStatus(DefectStatus.NEW.value());
                            }
                            String filePath = relPathMap.get(defect.getRelPath());
                            defect.setFilePath(StringUtils.isEmpty(filePath) ? defect.getFilePath() : filePath);
                            return defect;
                        }).collect(Collectors.toList());

        Set<String> originalFileMd5Set = originalDefectList.stream()
                .filter(lintDefectEntity -> StringUtils.isNotEmpty(lintDefectEntity.getFileMd5()))
                .map(lintDefectEntity -> String.format("%s_%s_%s", lintDefectEntity.getFileMd5(),
                        lintDefectEntity.getLineNum(), lintDefectEntity.getChecker()))
                .collect(Collectors.toSet());

        //去重：md5、行号、规则一样的，直接去掉
        List<LintDefectV2Entity> currDefectList = currentDefectList.stream()
                .filter(lintDefectEntity -> StringUtils.isNotEmpty(lintDefectEntity.getPinpointHash())
                        && !originalFileMd5Set.contains(String.format("%s_%s_%s",
                        lintDefectEntity.getFileMd5(),
                        lintDefectEntity.getLineNum(),
                        lintDefectEntity.getChecker())))
                .collect(Collectors.toList());

        List<LintDefectV2Entity> finalDefectList = new ArrayList();

        log.info("originalDefectList size: {}", originalDefectList.size());
        log.info("currDefectList size: {}", currDefectList.size());
        // 本次上报的都被去重了，表示文件完全没有变化，则不需要做聚类，避免空耗
        if (CollectionUtils.isNotEmpty(currDefectList)) {
            finalDefectList.addAll(currDefectList);
            if (CollectionUtils.isNotEmpty(originalDefectList)) {
                finalDefectList.addAll(originalDefectList);
            }
        }

        return finalDefectList;
    }

    private void transferAuthor(LintDefectV2Entity selectedOldDefect,
                                List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        if (CollectionUtils.isNotEmpty(transferAuthorList)) {
            for (TransferAuthorEntity.TransferAuthorPair trasferAuthorPair : transferAuthorList) {
                String author = selectedOldDefect.getAuthor();
                if (StringUtils.isNotEmpty(author) && author.equalsIgnoreCase(trasferAuthorPair.getSourceAuthor())) {
                    selectedOldDefect.setAuthor(trasferAuthorPair.getTargetAuthor());
                }
            }
        }
    }

    /**
     * 将告警设置为已修复
     *
     * @param defect
     * @param buildEntity
     */
    protected void fixDefect(LintDefectV2Entity defect, BuildEntity buildEntity) {
        defect.setStatus(defect.getStatus() | DefectStatus.FIXED.value());
        defect.setFixedTime(System.currentTimeMillis());
        if (null != buildEntity) {
            defect.setFixedBuildNumber(buildEntity.getBuildNo());
        }
    }

    /**
     * 重新打开告警
     *
     * @param oldDefect
     */
    protected void reopenDefect(LintDefectV2Entity oldDefect) {
        oldDefect.setStatus(DefectStatus.NEW.value());
        oldDefect.setFixedTime(null);
        oldDefect.setFixedBuildNumber(null);
    }

}
