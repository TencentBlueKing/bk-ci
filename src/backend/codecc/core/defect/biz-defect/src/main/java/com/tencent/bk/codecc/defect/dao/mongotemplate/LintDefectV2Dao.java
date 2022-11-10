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

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import static com.tencent.devops.common.constant.ComConstants.MASK_STATUS;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.LintDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.bk.codecc.defect.vo.LintStatisticVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class LintDefectV2Dao {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoPageHelper mongoPageHelper;

    /**
     * 根据条件查询文件列表
     *
     * @param taskId
     * @param defectQueryReqVO
     * @param defectIdSet
     * @param newDefectJudgeTime
     * @param filedMap           设置需要返回或者过滤的字段
     * @param toolNameSet
     * @return
     */
    public List<LintDefectV2Entity> findDefectByCondition(long taskId, DefectQueryReqVO defectQueryReqVO,
                                                          Set<String> defectIdSet, Set<String> pkgChecker,
                                                          long newDefectJudgeTime, Map<String, Boolean> filedMap,
                                                          List<String> toolNameSet) {
        Query query = getQueryByCondition(taskId, defectQueryReqVO, defectIdSet, pkgChecker,
                newDefectJudgeTime, filedMap, toolNameSet);

        return mongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 根据条件查询文件列表
     *
     * @param taskId
     * @param defectQueryReqVO
     * @param defectIdSet
     * @param newDefectJudgeTime
     * @param filedMap           设置需要返回或者过滤的字段
     * @return
     */
    // NOCC:ParameterNumber(设计如此:)
    public Page<LintDefectV2Entity> findDefectPageByCondition(long taskId, DefectQueryReqVO defectQueryReqVO,
                                                              Set<String> defectIdSet, Set<String> pkgChecker,
                                                              long newDefectJudgeTime, Map<String, Boolean> filedMap,
                                                              Integer pageNum, Integer pageSize, String sortField,
                                                              Sort.Direction sortType,
                                                              List<String> toolNameSet) {
        Query query = getQueryByCondition(taskId, defectQueryReqVO, defectIdSet, pkgChecker,
                newDefectJudgeTime, filedMap, toolNameSet);

        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

        // createBuildNumber在mongodb中是String类型在没有collation支持下，无法正常敏感规则排序
        // 目前暂不考虑升级jar以支持collation特性
        if ("createBuildNumber".equals(sortField)) {
            sortField = "createTime";
        }

        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField)) {
            if (sortType.isAscending()) {
                sortType = Sort.Direction.DESC;
            } else {
                sortType = Sort.Direction.ASC;
            }
        }

        // 把前端传入的小驼峰排序字段转换为小写下划线的数据库字段名
        String sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField);
        List<Sort.Order> sortList =
                new ArrayList<>(Collections.singletonList(new Sort.Order(sortType, sortFieldInDb)));

        // 如果按文件名排序，那么还要再按行号排序
        if ("fileName".equals(sortField)) {
            sortList.add(new Sort.Order(sortType, "line_num"));
        }

        Page<LintDefectV2Entity> pageResult = mongoPageHelper.pageQuery(query,
                LintDefectV2Entity.class, pageSize, pageNum, sortList);
        return pageResult;
    }

    @NotNull
    protected Query getQueryByCondition(long taskId,
                                        DefectQueryReqVO defectQueryReqVO,
                                        Set<String> defectIdSet,
                                        Set<String> pkgChecker,
                                        long newDefectJudgeTime,
                                        Map<String, Boolean> filedMap,
                                        List<String> toolNameSet) {
        Query query = new BasicQuery(new Document());
        if (MapUtils.isNotEmpty(filedMap)) {
            Document fieldsObj = new Document();
            filedMap.forEach((filed, isNeedReturn) -> fieldsObj.put(filed, isNeedReturn));
            query = new BasicQuery(new Document(), fieldsObj);
        }
        Criteria criteria = getQueryCriteria(taskId, defectQueryReqVO, defectIdSet, pkgChecker,
                newDefectJudgeTime, toolNameSet);
        query.addCriteria(criteria);
        return query;
    }

    @NotNull
    private Criteria getQueryCriteria(long taskId,
                                      DefectQueryReqVO defectQueryReqVO,
                                      Set<String> defectIdSet,
                                      Set<String> pkgChecker,
                                      long newDefectJudgeTime,
                                      List<String> toolNameSet) {
        String checker = defectQueryReqVO.getChecker();
        String author = defectQueryReqVO.getAuthor();
        Set<String> fileList = defectQueryReqVO.getFileList();
        Set<String> conditionSeverity = defectQueryReqVO.getSeverity();
        Set<String> conditionDefectType = defectQueryReqVO.getDefectType();
        Set<String> condStatusList = defectQueryReqVO.getStatus();
        String startTimeStr = defectQueryReqVO.getStartCreateTime();
        String endTimeStr = defectQueryReqVO.getEndCreateTime();

        Criteria andCriteria = new Criteria();
        andCriteria.and("task_id").is(taskId);
        if (CollectionUtils.isNotEmpty(toolNameSet)) {
            andCriteria.and("tool_name").in(toolNameSet);
        } else {
            andCriteria.and("tool_name").is(defectQueryReqVO.getToolName());
        }

        // 1.buildId对应的告警ID集合过滤
        if (CollectionUtils.isNotEmpty(defectIdSet)) {
            Set<ObjectId> entityIdSet = defectIdSet.stream().map(ObjectId::new).collect(Collectors.toSet());
            andCriteria.and("_id").in(entityIdSet);
        }

        // 2.状态过滤
        if (CollectionUtils.isNotEmpty(condStatusList)) {
            // 若是快照查，选中待修复就必须补偿上已修复；
            if (StringUtils.isNotEmpty(defectQueryReqVO.getBuildId())) {
                String newStatusStr = String.valueOf(DefectStatus.NEW.value());
                String fixedStatusStr = String.valueOf(DefectStatus.FIXED.value());
                if (condStatusList.contains(newStatusStr)) {
                    condStatusList.add(newStatusStr);
                    condStatusList.add(fixedStatusStr);
                } else {
                    // 快照查，不存在已修复
                    condStatusList.remove(fixedStatusStr);
                }
            }
            Set<Integer> condStatusSet = condStatusList.stream()
                    .map(it -> Integer.parseInt(it) | DefectStatus.NEW.value())
                    .collect(Collectors.toSet());
            andCriteria.and("status").in(condStatusSet);
        }

        // 3.告警作者过滤
        if (StringUtils.isNotEmpty(author)) {
            andCriteria.and("author").is(author);
        }

        // 4.严重程度过滤
        if (CollectionUtils.isNotEmpty(conditionSeverity)) {
            if (conditionSeverity.contains(String.valueOf(ComConstants.PROMPT))) {
                conditionSeverity.add(String.valueOf(ComConstants.PROMPT_IN_DB));
            }

            if (!conditionSeverity.containsAll(Sets.newHashSet(String.valueOf(ComConstants.SERIOUS),
                    String.valueOf(ComConstants.NORMAL), String.valueOf(ComConstants.PROMPT_IN_DB)))) {
                Set<Integer> severitySet =
                        conditionSeverity.stream().map(Integer::valueOf).collect(Collectors.toSet());
                andCriteria.and("severity").in(severitySet);
            }
        }

        // 5.规则类型过滤
        if (pkgChecker == null) {
            pkgChecker = new HashSet<>();
        }
        if (StringUtils.isNotEmpty(checker)) {
            pkgChecker.add(checker);
        }
        if (CollectionUtils.isNotEmpty(pkgChecker)) {
            andCriteria.and("checker").in(pkgChecker);
        }

        // 6.路径过滤
        if (CollectionUtils.isNotEmpty(fileList)) {
            List<Criteria> criteriaList = new ArrayList<>();
            fileList.forEach(file -> criteriaList.add(Criteria.where("file_path").regex(file)));
            andCriteria.andOperator(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        long minTime = 0L;
        long maxTime = 0L;
        // 7.新旧告警过滤
        if (newDefectJudgeTime != 0 && CollectionUtils.isNotEmpty(conditionDefectType)) {
            if (conditionDefectType.contains(String.valueOf(ComConstants.DefectType.NEW.value()))) {
                minTime = newDefectJudgeTime;
            } else if (conditionDefectType.contains(String.valueOf(ComConstants.DefectType.HISTORY.value()))) {
                maxTime = newDefectJudgeTime;
            }
        }

        // 8.按日期过滤
        if (StringUtils.isNotEmpty(startTimeStr)) {
            long startTime = DateTimeUtils.getTimeStamp(startTimeStr + " 00:00:00");

            long endTime = StringUtils.isEmpty(endTimeStr) ? System.currentTimeMillis() :
                    DateTimeUtils.getTimeStamp(endTimeStr + " 23:59:59");

            minTime = minTime < startTime ? startTime : minTime;
            maxTime = (maxTime == 0 || maxTime > endTime) ? endTime : maxTime;
        }

        if (minTime != 0 && maxTime == 0) {
            andCriteria.and("line_update_time").gte(minTime);
        }
        if (minTime == 0 && maxTime != 0) {
            andCriteria.and("line_update_time").lt(maxTime);
        }
        if (minTime != 0 && maxTime != 0) {
            andCriteria.and("line_update_time").lt(maxTime).gte(minTime);
        }

        return andCriteria;
    }


    public void batchUpdateDefectAuthor(long taskId, List<LintDefectV2Entity> defectList, String newAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id")
                        .is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", newAuthor);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的ignore位
     *
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     * @param ignoreAuthor
     */
    public void batchUpdateDefectStatusIgnoreBit(long taskId, List<LintDefectV2Entity> defectList,
                                                 int ignoreReasonType, String ignoreReason, String ignoreAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id")
                        .is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("ignore_time", currTime);
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                update.set("ignore_author", ignoreAuthor);
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    public void batchMarkDefect(long taskId, List<LintDefectV2Entity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id")
                        .is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    public Page<LintFileVO> findDefectFilePageByCondition(long taskId,
                                                          DefectQueryReqVO queryWarningReq,
                                                          Set<String> defectIdSet,
                                                          Set<String> pkgChecker,
                                                          long newDefectJudgeTime,
                                                          int pageNum,
                                                          int pageSize,
                                                          String sortField,
                                                          Sort.Direction sortType,
                                                          List<String> toolNameSet) {
        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(taskId, queryWarningReq, defectIdSet, pkgChecker,
                newDefectJudgeTime, toolNameSet);
        MatchOperation match = Aggregation.match(criteria);

        // 以filePath进行分组，计算文件总数
        GroupOperation group = Aggregation.group("task_id", "tool_name", "file_path");
        CountOperation count = Aggregation.count().as("fileCount");
        Aggregation agg1 = Aggregation.newAggregation(match, group, count)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintStatisticVO> result =
                mongoTemplate.aggregate(agg1, "t_lint_defect_v2", LintStatisticVO.class);
        List<LintStatisticVO> totalResult = result.getMappedResults();

        if (CollectionUtils.isEmpty(totalResult)) {
            return new Page<>(0, pageNum, pageSize, 0, new ArrayList<>());
        }

        int total = totalResult.get(0).getFileCount();

        // 以filePath进行分组
        GroupOperation group1 = Aggregation.group("task_id", "tool_name", "file_path")
                .last("file_name").as("fileName")
                .last("file_path").as("filePath")
                .last("file_update_time").as("fileUpdateTime")
                .count().as("defectCount")
                .addToSet("checker").as("checkerList")
                .addToSet("severity").as("severityList")
                .addToSet("author").as("authorList");

        // 默认按文件名排列
        if (StringUtils.isEmpty(sortField)) {
            sortField = "fileName";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }
        SortOperation sort = Aggregation.sort(sortType, sortField);
        SkipOperation skip = Aggregation
                .skip(Long.valueOf(pageSize * ((pageNum <= 0 ? MongoPageHelper.FIRST_PAGE_NUM : pageNum) - 1)));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation agg2 = Aggregation.newAggregation(match, group1, sort, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintFileVO> queryResult =
                mongoTemplate.aggregate(agg2, "t_lint_defect_v2", LintFileVO.class);
        List<LintFileVO> filelist = queryResult.getMappedResults();
        filelist.forEach(lintFileVO -> {
            Set<Integer> severityList = lintFileVO.getSeverityList();
            if (CollectionUtils.isNotEmpty(severityList) && severityList.remove(ComConstants.PROMPT_IN_DB)) {
                severityList.add(ComConstants.PROMPT);
            }
        });
        final Integer pages = (int) Math.ceil(total / (double) pageSize);
        final Page<LintFileVO> pageResult = new Page<>(total, pageNum, pageSize, pages, filelist);
        return pageResult;
    }

    /**
     * 根据状态过滤后获取规则，处理人、文件路径
     *
     * @param taskId
     * @param toolNameSet
     * @param statusSet
     * @return
     */
    public List<LintFileVO> getCheckerAuthorPathForPageInit(long taskId, List<String> toolNameSet,
                                                            Set<String> statusSet) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").is(taskId).and("tool_name").in(toolNameSet);

        // 状态过滤
        if (CollectionUtils.isNotEmpty(statusSet)) {
            Set<Integer> condStatusSet = statusSet.stream().map(it -> Integer.valueOf(it) | DefectStatus.NEW.value())
                    .collect(Collectors.toSet());
            criteria.and("status").in(condStatusSet);
        }
        MatchOperation match = Aggregation.match(criteria);

        // 以filePath进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "file_path")
                .last("file_path").as("filePath")
                .last("url").as("url")
                .last("rel_path").as("relPath")
                .addToSet("checker").as("checkerList")
                .addToSet("author").as("authorList");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintFileVO> queryResult =
                mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintFileVO.class);

        return queryResult.getMappedResults();
    }

    /**
     * 根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
     *
     * @param taskId
     * @param queryWarningReq
     * @param defectIdSet
     * @param pkgChecker
     * @param toolNameSet
     * @return
     */
    public List<LintDefectGroupStatisticVO> statisticByStatus(long taskId, DefectQueryReqVO queryWarningReq,
                                                              Set<String> defectIdSet, Set<String> pkgChecker,
                                                              List<String> toolNameSet) {
        // 只需要查状态为待修复，已修复，已忽略的告警
        Set<String> needQueryStatusSet = Sets.newHashSet(
                String.valueOf(DefectStatus.NEW.value()),
                String.valueOf(DefectStatus.NEW.value() | DefectStatus.FIXED.value()),
                String.valueOf(DefectStatus.NEW.value() | DefectStatus.IGNORE.value()));
        needQueryStatusSet.addAll(MASK_STATUS);
        queryWarningReq.setStatus(needQueryStatusSet);

        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(taskId, queryWarningReq, defectIdSet, pkgChecker,
                0, toolNameSet);
        MatchOperation match = Aggregation.match(criteria);

        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "status")
                .last("status").as("status")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintDefectGroupStatisticVO> queryResult = mongoTemplate.aggregate(agg,
                "t_lint_defect_v2", LintDefectGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }


    /**
     * 根据规则、处理人、快照、路径、日期、状态过滤后计算各严重级别告警数
     *
     * @param taskId
     * @param queryWarningReq
     * @param defectIdSet
     * @param pkgChecker
     * @param toolNameSet
     * @return
     */
    public List<LintDefectGroupStatisticVO> statisticBySeverity(long taskId, DefectQueryReqVO queryWarningReq,
                                                                Set<String> defectIdSet, Set<String> pkgChecker,
                                                                List<String> toolNameSet) {
        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(taskId, queryWarningReq, defectIdSet, pkgChecker,
                0, toolNameSet);
        MatchOperation match = Aggregation.match(criteria);

        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "severity")
                .last("severity").as("severity")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintDefectGroupStatisticVO> queryResult =
                mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }

    /**
     * 根据规则、处理人、快照、路径、日期、状态过滤后计算各严重级别告警数
     *
     * @param taskId
     * @param queryWarningReq
     * @param defectIdSet
     * @param pkgChecker
     * @param toolNameSet
     * @return
     */
    public List<LintDefectGroupStatisticVO> statisticByDefectType(long taskId, DefectQueryReqVO queryWarningReq,
                                                                  Set<String> defectIdSet, Set<String> pkgChecker,
                                                                  List<String> toolNameSet, long newDefectJudgeTime,
                                                                  int defectType) {
        // 根据查询条件过滤
        Set<String> oldDefectType = queryWarningReq.getDefectType();
        queryWarningReq.setDefectType(Sets.newHashSet(String.valueOf(defectType)));
        Criteria criteria = getQueryCriteria(taskId, queryWarningReq, defectIdSet, pkgChecker,
                newDefectJudgeTime, toolNameSet);
        queryWarningReq.setDefectType(oldDefectType);

        MatchOperation match = Aggregation.match(criteria);

        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintDefectGroupStatisticVO> queryResult = mongoTemplate
                .aggregate(agg, "t_lint_defect_v2", LintDefectGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }
}
