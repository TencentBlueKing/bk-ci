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

import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogGroupEntity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 分析记录复杂查询持久代码
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Repository
public class TaskLogDao
{

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<TaskLogGroupEntity> findFirstByTaskIdOrderByStartTime(long taskId, Set<String> toolSet)
    {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId).and("tool_name").in(toolSet));
        //根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("tool_name")
                .first("task_id").as("taskId")
                .first("tool_name").as("toolName")
                .first("elapse_time").as("elapseTime")
                .first("end_time").as("endTime")
                .first("start_time").as("startTime")
                .first("build_num").as("buildNum")
                .first("build_id").as("buildId");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<TaskLogGroupEntity> queryResult = mongoTemplate.aggregate(agg, "t_task_log", TaskLogGroupEntity.class);
        return queryResult.getMappedResults();
    }


    public List<TaskLogEntity> findFirstByTaskIdAndBuildIdOrderbyStartTime(long taskId, String buildId)
    {
        //添加查询条件
        MatchOperation matchOperation = Aggregation.match(Criteria.where("task_id").is(taskId).and("build_id").is(buildId));
        //根据时间排序
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.DESC, "start_time");
        //以toolName进行分组
        GroupOperation groupOperation = Aggregation.group("tool_name")
                        .first("stream_name").as("stream_name")
                        .first("task_id").as("task_id")
                        .first("tool_name").as("tool_name")
                        .first("curr_step").as("curr_step")
                        .first("flag").as("flag")
                        .first("start_time").as("start_time")
                        .first("end_time").as("end_time")
                        .first("elapse_time").as("elapse_time")
                        .first("pipeline_id").as("pipeline_id")
                        .first("build_id").as("build_id")
                        .first("build_num").as("build_num")
                        .first("trigger_from").as("trigger_from")
                        .first("version_time").as("version_time")
                        .first("step_array").as("step_array");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, sortOperation, groupOperation);

        AggregationResults<TaskLogEntity> queryResult = mongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 批量获取任务的工具最新分析记录
     *
     * @param taskIds  任务ID集合
     * @param toolName 工具名
     * @return list
     */
    public List<TaskLogEntity> findLastTaskLogByTool(Set<Long> taskIds, String toolName)
    {
        // 添加查询条件
        MatchOperation match = Aggregation.match(Criteria.where("tool_name").is(toolName).and("task_id").in(taskIds));
        // 根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        // 以toolName进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("curr_step").as("curr_step")
                .first("flag").as("flag")
                .first("start_time").as("start_time")
                .first("end_time").as("end_time")
                .first("build_num").as("build_num");
        Aggregation aggregation = Aggregation.newAggregation(match, sort, group);

        AggregationResults<TaskLogEntity> queryResult =
                mongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }


    /**
     * 获取时间区间内的所有任务的所有分析记录
     *
     * @param startTime 开始分析时间
     * @param endTime   分析结束时间
     * @return list
     */
    public List<TaskLogEntity> findTaskLogByTime(Set<Long> taskIdSet, Long startTime, Long endTime)
    {
        // 添加查询条件（在时间区间范围内）
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet)
                .and("start_time").gt(startTime).and("end_time").lt(endTime));

        // 根据任务ID排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "task_id");

        Aggregation aggregation = Aggregation.newAggregation(match, sort);

        AggregationResults<TaskLogEntity> queryResult =
                mongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 获取最后十次构建的分析记录
     *
     * @param taskId
     * @param limitSize
     * @return list
     */
    public List<TaskLogEntity> findLatestBuild(Long taskId, int limitSize) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId));
        // 以taskId进行分组
        GroupOperation group = Aggregation.group("build_id")
                .first("build_id").as("build_id");
        LimitOperation limit = new LimitOperation(limitSize);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        Aggregation agg = Aggregation.newAggregation(match, sort, group, limit);

        AggregationResults<TaskLogEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_task_log", TaskLogEntity.class);
        List<String> buildIds = queryResult.getMappedResults()
                .stream()
                .map(TaskLogEntity::getBuildId)
                .collect(Collectors.toList());
        return mongoTemplate.find(new Query(Criteria.where("build_id").in(buildIds)), TaskLogEntity.class);
    }
}
