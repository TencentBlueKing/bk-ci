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

package com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel;
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model;
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel;
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil;
import com.tencent.bk.codecc.apiquery.vo.DefectQueryReqVO;
import com.tencent.bk.codecc.apiquery.vo.LintFileVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * lint类工具告警持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Repository
public class LintDefectDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @return list
     */
    public List<LintStatisticModel> findStatByTaskIdInAndToolIs(Collection<Long> taskIdSet, String toolName) {
        // 以taskId tooName进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));
        // 根据时间倒序排列
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        // 以taskId进行分组，并取第一个的字段
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("time").as("time")
                .first("defect_count").as("defect_count")
                .first("file_count").as("file_count")
                .first("new_defect_count").as("new_defect_count")
                .first("history_defect_count").as("history_defect_count")
                .first("total_new_serious").as("total_new_serious")
                .first("total_new_normal").as("total_new_normal")
                .first("total_new_prompt").as("total_new_prompt")
                .first("total_serious").as("total_serious")
                .first("total_normal").as("total_normal")
                .first("total_prompt").as("total_prompt")
                .first("total_defect_count").as("total_defect_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<LintStatisticModel> queryResult =
                mongoTemplate.aggregate(agg, "t_lint_statistic", LintStatisticModel.class);
        return queryResult.getMappedResults();
    }


    /**
     * 以规则维度统计告警数
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @return list
     */
    public List<LintDefectV2Model> findDefectByGroupChecker(Collection<Long> taskIdSet, String toolName,
            Integer status, Long startTime, Long endTime) {
        MatchOperation match = Aggregation
                .match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName).and("status").is(status));

        // 选填筛选
        List<Criteria> criteriaList = Lists.newArrayList();
        if (startTime != 0 && endTime != 0) {
            // 按告警状态匹配对应时间字段
            String timeField = ConvertUtil.timeField4DefectStatus(status);
            criteriaList.add(Criteria.where(timeField).gte(startTime).lte(endTime));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        MatchOperation afterMatch = Aggregation.match(criteria);

        SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "task_id");

        // 以规则为维度统计告警数
        GroupOperation group = Aggregation.group("task_id", "tool_name", "checker")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("checker").as("checker")
                .first("severity").as("severity")
                .count().as("line_num");

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, afterMatch, group, sort).withOptions(options);

        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Model.class).getMappedResults();
    }


    /**
     * 批量查询告警列表
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @param timeField 时间筛选字段 create_time,fixed_time,ignore_time,exclude_time
     * @return list
     */
    public List<DefectStatModel> batchQueryDefect(Collection<Long> taskIdSet, String toolName, List<Integer> status,
            String timeField, Long startTime, Long endTime) {
        // 索引筛选1
        MatchOperation match1 = getIndexMatchOperation(taskIdSet, toolName, status);

        // 选填筛选2
        MatchOperation match2 = getFilterMatchOperation(timeField, startTime, endTime);

        GroupOperation group = Aggregation.group("task_id", "severity")
                .first("task_id").as("task_id")
                .first("severity").as("severity")
                .count().as("count");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match1, match2, group).withOptions(options);

        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", DefectStatModel.class).getMappedResults();
    }

    /**
     * 批量统计各状态告警数
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @param timeField 时间筛选字段 create_time,fixed_time,ignore_time,exclude_time
     * @return list
     */
    public List<DefectStatModel> batchStatDefect(Collection<Long> taskIdSet, String toolName, List<Integer> status,
            String timeField, Long startTime, Long endTime) {
        // 索引筛选1
        MatchOperation match1 = getIndexMatchOperation(taskIdSet, toolName, status);

        // 选填筛选2
        MatchOperation match2 = getFilterMatchOperation(timeField, startTime, endTime);

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .count().as("count");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match1, match2, group).withOptions(options);

        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", DefectStatModel.class).getMappedResults();
    }

    /**
     * 索引筛选
     */
    @NotNull
    private MatchOperation getIndexMatchOperation(Collection<Long> taskIdSet, String toolName, List<Integer> status) {
        List<Criteria> criteriaList = Lists.newArrayList();
        criteriaList.add(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));

        if (CollectionUtils.isNotEmpty(status)) {
            criteriaList.add(Criteria.where("status").in(status));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return Aggregation.match(criteria);
    }

    /**
     * 非索引筛选
     */
    @NotNull
    private MatchOperation getFilterMatchOperation(String timeField, Long startTime, Long endTime) {
        List<Criteria> criteriaList = Lists.newArrayList();
        if (StringUtils.isNotBlank(timeField)) {
            criteriaList.add(Criteria.where(timeField).gte(startTime).lte(endTime));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return Aggregation.match(criteria);
    }


    @NotNull
    private Criteria getQueryCriteria(long taskId,
            DefectQueryReqVO defectQueryReqVO,
            Set<String> defectIdSet,
            Set<String> pkgChecker,
            long newDefectJudgeTime) {
        String checker = defectQueryReqVO.getChecker();
        String author = defectQueryReqVO.getAuthor();
        Set<String> fileList = defectQueryReqVO.getFileList();
        Set<String> conditionSeverity = defectQueryReqVO.getSeverity();
        Set<String> conditionDefectType = defectQueryReqVO.getDefectType();
        Set<Integer> condStatusList = defectQueryReqVO.getStatus();
        String startTimeStr = defectQueryReqVO.getStartCreateTime();
        String endTimeStr = defectQueryReqVO.getEndCreateTime();

        Criteria andCriteria = new Criteria();
        andCriteria.and("task_id").is(taskId).and("tool_name").is(defectQueryReqVO.getToolName());

        // 1.buildId对应的告警ID集合过滤
        if (CollectionUtils.isNotEmpty(defectIdSet)) {
            Set<ObjectId> entityIdSet = defectIdSet.stream().map(ObjectId::new).collect(Collectors.toSet());
            andCriteria.and("_id").in(entityIdSet);
        }

        // 2.状态过滤
        if (CollectionUtils.isNotEmpty(condStatusList)) {
            Set<Integer> condStatusSet = condStatusList.stream().map(it -> it | ComConstants.DefectStatus.NEW.value())
                    .collect(Collectors.toSet());
            andCriteria.and("status").in(condStatusSet);
        }

        // 3.告警作者过滤
        if (StringUtils.isNotEmpty(author)) {
            andCriteria.and("author").is(author);
        }

        // 4.严重程度过滤
        if (CollectionUtils.isNotEmpty(conditionSeverity)) {
            if (conditionSeverity.remove(String.valueOf(ComConstants.PROMPT))) {
                conditionSeverity.add(String.valueOf(ComConstants.PROMPT_IN_DB));
            }

            if (!conditionSeverity.containsAll(
                    Sets.newHashSet(String.valueOf(ComConstants.SERIOUS), String.valueOf(ComConstants.NORMAL),
                            String.valueOf(ComConstants.PROMPT_IN_DB)))) {
                Set<Integer> severitySet = conditionSeverity.stream().map(Integer::valueOf).collect(Collectors.toSet());
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

            minTime = Math.max(minTime, startTime);
            maxTime = Math.min(maxTime, endTime);
        }

        if (minTime != 0) {
            andCriteria.and("line_update_time").gte(minTime);
        }
        if (maxTime != 0) {
            andCriteria.and("line_update_time").lt(newDefectJudgeTime);
        }

        return andCriteria;
    }

    public Page<LintFileVO> findDefectFilePageByCondition(long taskId,
            DefectQueryReqVO queryWarningReq,
            Set<String> defectIdSet,
            Set<String> pkgChecker,
            long newDefectJudgeTime,
            int pageNum,
            int pageSize,
            String sortField,
            String sortType) {
        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(taskId, queryWarningReq, defectIdSet, pkgChecker, newDefectJudgeTime);
        MatchOperation match = Aggregation.match(criteria);

        // 以filePath进行分组，计算文件总数
        GroupOperation group = Aggregation.group("task_id", "tool_name", "file_path");
        CountOperation count = Aggregation.count().as("file_count");
        Aggregation agg1 = Aggregation.newAggregation(match, group, count)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintStatisticModel> result =
                mongoTemplate.aggregate(agg1, "t_lint_defect_v2", LintStatisticModel.class);
        List<LintStatisticModel> totalResult = result.getMappedResults();

        if (CollectionUtils.isEmpty(totalResult)) {
            return new Page<>(0, pageNum, pageSize, 0, new ArrayList<>());
        }

        int total = totalResult.get(0).getFileCount();

        // 以filePath进行分组
        GroupOperation group1 = Aggregation.group("task_id", "tool_name", "file_path")
                .last("file_name").as("fileName")
                .last("file_path").as("filePath")
                .last("file_update_time")
                .as("fileUpdateTime").count().as("defectCount")
                .addToSet("checker").as("checkerList")
                .addToSet("severity").as("severityList")
                .addToSet("author").as("authorList");

        // 默认按文件名排列
        if (StringUtils.isEmpty(sortField)) {
            sortField = "fileName";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC.name();
        }
        SortOperation sort = Aggregation.sort(Sort.Direction.valueOf(sortType), sortField);
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageSize * pageNum));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation agg2 = Aggregation.newAggregation(match, group1, sort, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintFileVO> queryResult =
                mongoTemplate.aggregate(agg2, "t_lint_defect_v2", LintFileVO.class);
        List<LintFileVO> lintFileVOList = queryResult.getMappedResults();
        lintFileVOList.forEach(lintFileVO -> {
            Set<Integer> severityList = lintFileVO.getSeverityList();
            if (CollectionUtils.isNotEmpty(severityList)
                    && severityList.remove(ComConstants.PROMPT_IN_DB)) {
                severityList.add(ComConstants.PROMPT);
            }
        });
        final int pages = (int) Math.ceil(total / (double) pageSize);
        return new Page<>(total, pageNum + 1, pageSize, pages, lintFileVOList);
    }


    /**
     * 根据条件查询文件列表
     *
     * @param taskId             任务ID
     * @param defectQueryReqVO   请求体
     * @param defectIdSet        告警ID集合
     * @param newDefectJudgeTime 新旧告警区分时间
     * @param filedMap           设置需要返回或者过滤的字段
     * @return page
     */
    public Page<LintDefectV2Model> findDefectPageByCondition(long taskId, DefectQueryReqVO defectQueryReqVO,
            Set<String> defectIdSet, Set<String> pkgChecker, long newDefectJudgeTime, Map<String, Boolean> filedMap,
            Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {

        Criteria criteria = getQueryCriteria(taskId, defectQueryReqVO, defectIdSet, pkgChecker, newDefectJudgeTime);

        long count = mongoTemplate.count(new Query(criteria), "t_lint_defect_v2");
        int totalPage = 0;
        if (count > 0) {
            totalPage = ((int)count + pageSize - 1) / pageSize;
        }

        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField)) {
            if (sortType.isAscending()) {
                sortType = Sort.Direction.DESC;
            } else {
                sortType = Sort.Direction.ASC;
            }
        }

        ProjectionOperation project = Aggregation.project(filedMap.keySet().toArray(new String[0]));

        SortOperation sort = Aggregation.sort(sortType, sortField);
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNum * pageSize));
        LimitOperation limit = Aggregation.limit(Long.valueOf(pageSize));

        AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, project, limit)
                .withOptions(options);

        AggregationResults<LintDefectV2Model> queryResults =
                mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Model.class);

        return new Page<>(count, pageNum + 1, pageSize, totalPage, queryResults.getMappedResults());
    }

    /**
     * 批量分页获取告警列表
     *
     * @param status    告警状态
     * @param timeField 时间筛选字段 create_time,fixed_time,ignore_time,exclude_time
     * @param fieldSet  指定获取字段
     * @return list
     */
    public List<LintDefectV2Model> batchQueryDefectPage(Collection<Long> taskIdSet, String toolName,
            List<Integer> status, String timeField, Long startTime, Long endTime, @NotNull Set<String> fieldSet,
            @NotNull Pageable pageable) {
        // 索引筛选
        MatchOperation indexMatch = getIndexMatchOperation(taskIdSet, toolName, status);
        // 普通选填筛选
        MatchOperation filterMatch = getFilterMatchOperation(timeField, startTime, endTime);
        // 获取指定字段
        ProjectionOperation project = Aggregation.project(fieldSet.toArray(new String[0]));

        int pageSize = pageable.getPageSize();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageable.getPageNumber() * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);

        AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg =
                Aggregation.newAggregation(indexMatch, filterMatch, sort, skip, project, limit).withOptions(options);
        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Model.class).getMappedResults();
    }

}
