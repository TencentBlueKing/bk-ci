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
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogModel;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 任务分析记录持久类
 *
 * @version V2.0
 * @date 2020/5/14
 */
@Repository
public class TaskLogDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 批量获取任务的工具最近分析[成功]的记录
     *
     * @param taskIds   任务ID集合
     * @param toolName  工具名
     * @return list
     */
    public List<TaskLogModel> findLastTaskLogList(Set<Long> taskIds, String toolName) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 添加查询条件  指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        // 工具名筛选
        criteriaList.add(Criteria.where("tool_name").ne(ComConstants.Tool.CLOC.name()));
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }

        // 分析成功的
        criteriaList.add(Criteria.where("curr_step").in(Lists
                .newArrayList(ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.Step4MutliTool.COMMIT.value()))
                .and("flag").is(ComConstants.StepFlag.SUCC.value()));

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        MatchOperation match = Aggregation.match(criteria);
        // 根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        // 以任务ID和构件号进行分组
        GroupOperation group = Aggregation.group("task_id", "build_num")
                .first("task_id").as("task_id")
                .first("curr_step").as("curr_step")
                .first("flag").as("flag")
                .first("start_time").as("start_time")
                .first("end_time").as("end_time")
                .first("build_num").as("build_num")
                .first("tool_name").as("tool_name");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);

        AggregationResults<TaskLogModel> queryResult = mongoTemplate.aggregate(agg, "t_task_log", TaskLogModel.class);
        return queryResult.getMappedResults();
    }


    /**
     * 批量获取任务分析次数
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @return list
     */
    public List<TaskLogModel> findTaskAnalyzeCount(Set<Long> taskIds, String toolName) {
        List<Criteria> criteriaList = Lists.newArrayList();

        // 添加查询条件  指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        // 工具名筛选
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        criteriaList.add(Criteria.where("tool_name").ne(ComConstants.Tool.CLOC.name()));

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        MatchOperation match = Aggregation.match(criteria);

        // 按任务ID分组统计分析次数
        GroupOperation group = Aggregation.group("task_id").first("task_id").as("task_id").count().as("flag");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);
        AggregationResults<TaskLogModel> queryResult = mongoTemplate.aggregate(agg, "t_task_log", TaskLogModel.class);
        return queryResult.getMappedResults();
    }


    /**
     * 批量获取任务的工具最近分析[成功]的记录
     *
     * @param taskIds   任务ID集合
     * @param startTime 工具名
     * @return list
     */
    public List<TaskLogModel> findLastTaskLogByTime(List<Long> taskIds, Long startTime, Long endTime) {
        List<Criteria> criteriaList = Lists.newArrayList();

        // 指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        // 工具名筛选
        criteriaList.add(Criteria.where("tool_name").ne(ComConstants.Tool.CLOC.name()));

        // 分析成功的
        criteriaList.add(Criteria.where("curr_step").in(Lists
                .newArrayList(ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.Step4MutliTool.COMMIT.value()))
                .and("flag").is(ComConstants.StepFlag.SUCC.value()));

        if (startTime != null) {
            criteriaList.add(Criteria.where("start_time").gte(startTime));
        }
        if (endTime != null) {
            criteriaList.add(Criteria.where("end_time").lte(endTime));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        MatchOperation match = Aggregation.match(criteria);
        // 根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        // 以任务ID和构件号进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("build_num").as("build_num")
                .first("curr_step").as("curr_step")
                .first("flag").as("flag")
                .first("start_time").as("start_time")
                .first("end_time").as("end_time")
                .first("tool_name").as("tool_name");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);

        AggregationResults<TaskLogModel> queryResult = mongoTemplate.aggregate(agg, "t_task_log", TaskLogModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 获取一次构建的分析记录
     * @param taskId 任务id
     * @param toolName 工具名
     * @param buildId 构建id
     * @return TaskLogModel
     */
    public TaskLogModel findByBuildId(Long taskId, String toolName, String buildId) {
        Query query = new BasicQuery(new Document());
        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("build_id").is(buildId)
        );

        return mongoTemplate.findOne(query, TaskLogModel.class, "t_task_log");
    }


    /**
     * 批量获取任务的工具最新分析记录
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @return list
     */
    public List<TaskLogModel> batchFindLastTaskLogByTool(List<Long> taskIds, String toolName) {
        // 添加查询条件
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds).and("tool_name").is(toolName));
        // 根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        // 以task_id进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("curr_step").as("curr_step")
                .first("flag").as("flag")
                .first("start_time").as("start_time");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(match, sort, group).withOptions(options);

        AggregationResults<TaskLogModel> queryResult =
                mongoTemplate.aggregate(aggregation, "t_task_log", TaskLogModel.class);
        return queryResult.getMappedResults();
    }

}
