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
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectInputModel;
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModel;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
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
public class NewCCNDefectTracingComponent extends AbstractDefectTracingComponent<CCNDefectEntity> {
    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    /**
     * 告警跟踪
     *
     * @return
     */
    @Async("asyncCcnDefectTracingExecutor")
    @Deprecated
    public Future<Boolean> defectTracing(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO, Set<String> filterPaths,
            BuildEntity buildEntity,
            int chunkNo,
            List<CCNDefectEntity> originalDefectList,
            List<CCNDefectEntity> currentDefectList,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        String streamName = commitDefectVO.getStreamName();

        //1. 告警去重，去重没去完才需要聚类
        List<CCNDefectEntity> defectList = distinctLintDefect(streamName, toolName, buildEntity.getBuildId(),
                originalDefectList, currentDefectList);
        log.info("distinct defect list: {}", defectList.size());

        //如果代码没有变化，直接检查是否被路径屏蔽，然后将original入库
        if (CollectionUtils.isEmpty(defectList)) {
            Set<String> pathList = new HashSet<>();
            if (CollectionUtils.isNotEmpty(taskVO.getWhitePaths())) {
                pathList.addAll(taskVO.getWhitePaths());
            }
            List<CCNDefectEntity> upsertDefectList = updateOriginalDefectStatus(originalDefectList, currentDefectList,
                    filterPaths, pathList, buildEntity, transferAuthorList);
            saveDefects(taskId, toolName, filterPaths, pathList, buildEntity, upsertDefectList);

            return new AsyncResult<>(true);
        }

        //2. 拼装聚类入参
        List<AggregateDefectInputModel> defectHashList = defectList.stream().map(ccnDefectEntity ->
                new AggregateDefectInputModel(
                        ccnDefectEntity.getEntityId(),
                        "",
                        ccnDefectEntity.getPinpointHash(),
                        ccnDefectEntity.getFilePath(),
                        ccnDefectEntity.getRelPath())
        ).collect(Collectors.toList());

        //3. 做聚类，通过MQ分发到各台服务器上去做聚类，避免聚类都集中到一台服务器上导致服务器资源不足
//        Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult = executeCluster(taskVO,
//                toolName, buildEntity.getBuildId(), chunkNo, defectHashList);

        //4. 读取聚类输出文件，并转换告警状态
//        List<CCNDefectEntity> upsertDefectList = handleWithOutputModel(originalDefectList, currentDefectList,
//                asyncResult, buildEntity, transferAuthorList);
//        log.info("upsert defect list: {}", upsertDefectList.size());

        // 5.分批保存告警
//        saveDefects(taskId, toolName, filterPaths, buildEntity, upsertDefectList);

        return new AsyncResult<>(true);
    }

    /**
     * 代码没有变化，本次上报的告警全部被去重完，则不需要做聚类，直接根据去重规则判断告警的状态
     *
     * @param originalDefectList
     * @param currentDefectList
     * @param filterPaths
     * @param buildEntity
     * @param transferAuthorList
     * @return
     */
    @Deprecated
    private List<CCNDefectEntity> updateOriginalDefectStatus(
                                                     List<CCNDefectEntity> originalDefectList,
                                                     List<CCNDefectEntity> currentDefectList,
                                                     Set<String> filterPaths,
                                                     Set<String> pathList,
                                                     BuildEntity buildEntity,
                                                     List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList)
    {
        Map<String, CCNDefectEntity> currentDefectMd5Map = currentDefectList.stream()
                .filter(ccnDefectEntity -> StringUtils.isNotEmpty(ccnDefectEntity.getMd5()))
                .collect(Collectors.toMap(defect -> String.format("%s_%s_%s", defect.getMd5(),
                        defect.getFunctionName(), defect.getStartLines()),
                        Function.identity(), (k, v) -> v));

        List<CCNDefectEntity> upsertDefectList = new ArrayList<>();

        /* 聚类分组中只有老告警，从去重的新告警中查找：
         * 1.有找到新告警：
         *   1.1 老告警是已修复，则变为重新打开
         *   1.2 老告警是其他状态，则不变更直接上报（既然是被去重的，那么新告警和老告警的信息应该是一样的，所以不需要更新）
         * 2.没找到新告警
         *   2.1 将状态是NEW的老告警变成已修复
         *   2.2 老告警是其他状态，则不变更直接上报
         */
        originalDefectList.forEach(oldDefect ->
        {
            String fileMD5 = String.format("%s_%s_%s", oldDefect.getMd5(), oldDefect.getFunctionName(),
                    oldDefect.getStartLines());
            CCNDefectEntity newDefect = currentDefectMd5Map.get(fileMD5);
            if (newDefect != null && (oldDefect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0) {
                reopenDefect(oldDefect);

                // 用新告警的信息更新老告警信息
                updateOldDefectInfo(oldDefect, newDefect, transferAuthorList);
                upsertDefectList.add(oldDefect);
            } else if (newDefect == null && oldDefect.getStatus() == ComConstants.DefectStatus.NEW.value()) {
                fixDefect(oldDefect, buildEntity);
                upsertDefectList.add(oldDefect);
            } else if (checkMaskByPath(oldDefect, filterPaths, pathList, System.currentTimeMillis())) {

                // 用新告警的信息更新老告警信息
                updateOldDefectInfo(oldDefect, newDefect, transferAuthorList);
                upsertDefectList.add(oldDefect);
            }
        });

        return upsertDefectList;
    }

    /**
     * 保存告警
     *
     * @param taskId
     * @param toolName
     * @param filterPaths
     * @param buildEntity
     * @param upsertDefectList
     */
    protected void saveDefects(long taskId,
                   String toolName,
                   Set<String> filterPaths,
                   Set<String> pathList,
                   BuildEntity buildEntity,
                   List<CCNDefectEntity> upsertDefectList)
    {
        long beginTime = System.currentTimeMillis();
        log.info("begin saveDefectFile: taskId:{}, toolName:{}, buildId:{}", taskId, toolName,
                buildEntity.getBuildId());

        long curTime = System.currentTimeMillis();
        if (CollectionUtils.isNotEmpty(upsertDefectList)) {
            upsertDefectList.forEach(ccnDefectEntity ->
            {
                ccnDefectEntity.setTaskId(taskId);
                ccnDefectEntity.setUpdatedDate(curTime);
                checkMaskByPath(ccnDefectEntity, filterPaths, pathList, curTime);
            });

            log.info("save defect trace result: taskId:{}, toolName:{}, buildId:{}, defectCount:{}", taskId, toolName,
                    buildEntity.getBuildId(), upsertDefectList.size());
            ccnDefectRepository.saveAll(upsertDefectList);
        }
        log.info("end saveDefectFile, cost:{}, taskId:{}, toolName:{}, buildId:{}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildEntity.getBuildId());
    }

    /**
     * 读取聚类输出文件，并转换告警状态
     *
     * @see CCNDefectCommitComponent#postHandleDefectList(List, BuildEntity, List)
     * @param originalDefectList
     * @param currentDefectList
     * @param asyncResult
     * @param buildEntity
     * @param transferAuthorList
     * @return
     */
    @Deprecated
    protected List<CCNDefectEntity> handleWithOutputModel(
            List<CCNDefectEntity> originalDefectList,
            List<CCNDefectEntity> currentDefectList,
            Pair<String, AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResult,
            BuildEntity buildEntity, List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        long beginTime = System.currentTimeMillis();
        List<CCNDefectEntity> upsertDefectList = new ArrayList<>();
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
                        Map<String, CCNDefectEntity> defectMap = new HashMap<>();
                        Map<String, CCNDefectEntity> originalDefectMap = originalDefectList.stream()
                                .collect(Collectors.toMap(CCNDefectEntity::getEntityId, Function.identity(),
                                        (k, v) -> v));
                        defectMap.putAll(originalDefectMap);

                        Map<String, CCNDefectEntity> currentDefectMap = currentDefectList.stream()
                                .collect(Collectors.toMap(CCNDefectEntity::getEntityId, Function.identity(),
                                        (k, v) -> v));
                        defectMap.putAll(currentDefectMap);

                        //将聚类输出格式改为defectEntity
                        List<List<CCNDefectEntity>> clusteredDefectList =
                                outputDefectList.stream().map(AggregateDefectOutputModel::getDefects)
                                        .map(aggregateDefectInputModels -> aggregateDefectInputModels.stream()
                                                .map(aggregateDefectInputModel ->
                                                        defectMap.get(aggregateDefectInputModel.getId()))
                                                .collect(Collectors.toList())
                                        ).collect(Collectors.toList());

                        Map<String, CCNDefectEntity> currentDefectMd5Map = currentDefectList.stream()
                                .filter(ccnDefectEntity -> StringUtils.isNotEmpty(ccnDefectEntity.getMd5()))
                                .collect(Collectors.toMap(defect -> String.format("%s_%s_%s", defect.getMd5(),
                                        defect.getFunctionName(), defect.getStartLines()),
                                        Function.identity(), (k, v) -> v));

                        clusteredDefectList.forEach(defectList ->
                        {
                            //将聚类输出分为新告警和历史告警
                            Map<Boolean, List<CCNDefectEntity>> partitionedDefects =
                                    defectList.stream().collect(Collectors.groupingBy(CCNDefectEntity::getNewDefect));
                            List<CCNDefectEntity> newDefectList = partitionedDefects.get(true);
                            List<CCNDefectEntity> oldDefectList = partitionedDefects.get(false);

                            /* 聚类分组中只有老告警，从去重的新告警中查找：
                             * 1.有找到新告警：
                             *   1.1 老告警是已修复，则变为重新打开
                             *   1.2 老告警是其他状态，则不变更直接上报（既然是被去重的，那么新告警和老告警的信息应该是一样的，所以不需要更新）
                             * 2.没找到新告警
                             *   2.1 将状态是NEW的老告警变成已修复
                             *   2.2 老告警是其他状态，则不变更直接上报
                             */
                            if (CollectionUtils.isEmpty(newDefectList)) {
                                oldDefectList.forEach(oldDefect ->
                                {
                                    String fileMD5 = String.format("%s_%s_%s", oldDefect.getMd5(),
                                            oldDefect.getFunctionName(), oldDefect.getStartLines());
                                    CCNDefectEntity newDefect = currentDefectMd5Map.get(fileMD5);
                                    if (newDefect != null
                                            && (oldDefect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0) {
                                        reopenDefect(oldDefect);

                                        // 用新告警的信息更新老告警信息
                                        updateOldDefectInfo(oldDefect, newDefect, transferAuthorList);
                                        upsertDefectList.add(oldDefect);
                                    } else if (newDefect == null
                                            && oldDefect.getStatus() == ComConstants.DefectStatus.NEW.value()) {
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
                                newDefectList.sort(Comparator.comparingInt(CCNDefectEntity::getStartLines));
                                if (CollectionUtils.isNotEmpty(oldDefectList)) {
                                    oldDefectList.sort(Comparator.comparingInt(CCNDefectEntity::getStartLines));
                                }
                                for (int i = 0; i < newDefectList.size(); i++) {
                                    CCNDefectEntity newDefect = newDefectList.get(i);
                                    CCNDefectEntity oldDefect = null;
                                    if (CollectionUtils.isNotEmpty(oldDefectList) && oldDefectList.size() > i) {
                                        oldDefect = oldDefectList.get(i);
                                    }
                                    if (oldDefect != null) {
                                        // 用新告警的信息更新老告警信息
                                        updateOldDefectInfo(oldDefect, newDefect, transferAuthorList);

                                        if ((oldDefect.getStatus() & ComConstants.DefectStatus.FIXED.value()) > 0) {
                                            reopenDefect(oldDefect);
                                        }
                                        if (StringUtils.isEmpty(oldDefect.getAuthor())) {
                                            oldDefect.setAuthor(newDefect.getAuthor());
                                        }
                                    } else {
                                        oldDefect = newDefect;
                                        if (null != buildEntity) {
                                            oldDefect.setCreateBuildNumber(buildEntity.getBuildNo());
                                        }
                                        oldDefect.setCreateTime(System.currentTimeMillis());
                                        oldDefect.setStatus(ComConstants.DefectStatus.NEW.value());

                                        // 作者转换
                                        transferAuthor(oldDefect, transferAuthorList);
                                    }

                                    upsertDefectList.add(oldDefect);
                                }

                                // 老告警比新告警多出来的那部分告警变成已修复
                                if (CollectionUtils.isNotEmpty(oldDefectList)
                                        && oldDefectList.size() > newDefectList.size()) {
                                    List<CCNDefectEntity> closeOldDefectList =
                                            oldDefectList.subList(newDefectList.size(), oldDefectList.size());
                                    closeOldDefectList.forEach(defect ->
                                    {
                                        if (defect.getStatus() == ComConstants.DefectStatus.NEW.value()) {
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
    private void updateOldDefectInfo(CCNDefectEntity selectedOldDefect, CCNDefectEntity newDefect,
                                     List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList) {
        if (newDefect == null) {
            return;
        }
        selectedOldDefect.setCcn(newDefect.getCcn());
        selectedOldDefect.setFunctionName(newDefect.getFunctionName());
        selectedOldDefect.setLongName(newDefect.getLongName());
        selectedOldDefect.setConditionLines(newDefect.getConditionLines());
        selectedOldDefect.setFilePath(newDefect.getFilePath());
        selectedOldDefect.setStartLines(newDefect.getStartLines());
        selectedOldDefect.setEndLines(newDefect.getEndLines());
        selectedOldDefect.setTotalLines(newDefect.getTotalLines());
        selectedOldDefect.setPinpointHash(newDefect.getPinpointHash());
        selectedOldDefect.setMd5(newDefect.getMd5());
        selectedOldDefect.setLatestDateTime(newDefect.getLatestDateTime());
        selectedOldDefect.setRelPath(newDefect.getRelPath());
        selectedOldDefect.setUrl(newDefect.getUrl());
        selectedOldDefect.setRepoId(newDefect.getRepoId());
        selectedOldDefect.setRevision(newDefect.getRevision());
        selectedOldDefect.setBranch(newDefect.getBranch());
        selectedOldDefect.setSubModule(newDefect.getSubModule());
        if (StringUtils.isEmpty(selectedOldDefect.getAuthor())) {
            selectedOldDefect.setAuthor(newDefect.getAuthor());

            // 作者转换
            transferAuthor(selectedOldDefect, transferAuthorList);
        }
    }

    /**
     * 入库前检测屏蔽路径
     *
     * @param ccnDefectEntity
     * @param filterPaths
     * @param curTime
     * @return
     */
    private boolean checkMaskByPath(CCNDefectEntity ccnDefectEntity,
                                    Set<String> filterPaths,
                                    Set<String> pathList,
                                    long curTime) {
        String relPath = ccnDefectEntity.getRelPath();
        String filePath = ccnDefectEntity.getFilePath();

        // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
        if ((ccnDefectEntity.getStatus() & ComConstants.DefectStatus.PATH_MASK.value()) == 0
                && (ccnDefectEntity.getStatus() & ComConstants.DefectStatus.FIXED.value()) == 0
                && (PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPaths)
                || (CollectionUtils.isNotEmpty(pathList)
                && !PathUtils.checkIfMaskByPath(filePath, pathList)))) {
            ccnDefectEntity.setStatus(ccnDefectEntity.getStatus() | ComConstants.DefectStatus.PATH_MASK.value());
            ccnDefectEntity.setExcludeTime(curTime);
            return true;
        }
        // 如果已经是被路径屏蔽的，但是实质没有被路径屏蔽，则要把屏蔽状态去掉
        else if ((CollectionUtils.isEmpty(pathList)
                || PathUtils.checkIfMaskByPath(filePath, pathList))
                && !PathUtils.checkIfMaskByPath(StringUtils.isNotEmpty(relPath) ? relPath : filePath, filterPaths)
                && (ccnDefectEntity.getStatus() & ComConstants.DefectStatus.PATH_MASK.value()) > 0) {
            ccnDefectEntity.setStatus(ccnDefectEntity.getStatus() - ComConstants.DefectStatus.PATH_MASK.value());
            return true;
        }
        return false;
    }

    /**
     * 告警去重后，得到并集的告警清单
     *
     * @see CCNDefectCommitComponent#preHandleDefectList(String, String, String, List, List, Map)
     * @param streamName
     * @param toolName
     * @param buildId
     * @param preDefectList
     * @param currentDefectList
     * @return
     */
    @Deprecated
    private List<CCNDefectEntity> distinctLintDefect(
            String streamName,
            String toolName,
            String buildId,
            List<CCNDefectEntity> preDefectList,
            List<CCNDefectEntity> currentDefectList) {
        Map<String, String> fileMd5Map = getFIleMd5Map(streamName, toolName, buildId);

        Map<String, String> relPathMap = new HashMap<>();
        currentDefectList.forEach(ccnDefectEntity ->
        {
            if (ccnDefectEntity != null) {
                if (StringUtils.isNotEmpty(ccnDefectEntity.getRelPath())) {
                    relPathMap.put(ccnDefectEntity.getRelPath(), ccnDefectEntity.getFilePath());
                }
                ccnDefectEntity.setMd5(fileMd5Map.get(ccnDefectEntity.getFilePath()));
                ccnDefectEntity.setEntityId(ObjectId.get().toString());
                ccnDefectEntity.setNewDefect(true);
                if (0 == ccnDefectEntity.getStatus()) {
                    ccnDefectEntity.setStatus(ComConstants.DefectStatus.NEW.value());
                }
            }
        });

        // 过滤掉pinpointHash为空的告警，用于兼容（原来的没有上报的告警也要进行）
        List<CCNDefectEntity> originalDefectList = preDefectList.stream()
                .filter(defect -> StringUtils.isNotEmpty(defect.getPinpointHash()))
                .map(defect ->
                {
                    defect.setNewDefect(false);
                    if (0 == defect.getStatus()) {
                        defect.setStatus(ComConstants.DefectStatus.NEW.value());
                    }
                    String filePath = relPathMap.get(defect.getRelPath());
                    if (StringUtils.isNotEmpty(filePath)) {
                        defect.setFilePath(filePath);
                    }
                    return defect;
                }).collect(Collectors.toList());

        Set<String> originalFileMd5Set = originalDefectList.stream()
                .filter(ccnDefectEntity -> StringUtils.isNotEmpty(ccnDefectEntity.getMd5()))
                .map(ccnDefectEntity -> String.format("%s_%s_%s", ccnDefectEntity.getMd5(),
                        ccnDefectEntity.getFunctionName(), ccnDefectEntity.getStartLines()))
                .collect(Collectors.toSet());

        //去重：md5、方法名、开行号一样的，直接去掉
        List<CCNDefectEntity> currDefectList = currentDefectList.stream()
                .filter(ccnDefectEntity -> StringUtils.isNotEmpty(ccnDefectEntity.getPinpointHash())
                        && !originalFileMd5Set.contains(String.format("%s_%s_%s", ccnDefectEntity.getMd5(),
                        ccnDefectEntity.getFunctionName(), ccnDefectEntity.getStartLines())))
                .collect(Collectors.toList());

        List<CCNDefectEntity> finalDefectList = new ArrayList();

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

    private void transferAuthor(CCNDefectEntity selectedOldDefect,
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
    protected void fixDefect(CCNDefectEntity defect, BuildEntity buildEntity) {
        defect.setStatus(defect.getStatus() | ComConstants.DefectStatus.FIXED.value());
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
    protected void reopenDefect(CCNDefectEntity oldDefect) {
        oldDefect.setStatus(ComConstants.DefectStatus.NEW.value());
        oldDefect.setFixedTime(null);
        oldDefect.setFixedBuildNumber(null);
    }

}
