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
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class TaskLogOverviewDao {

    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 查询任务维度的分析次数
     *
     * @param taskIds   任务ID集合
     * @param status    分析状态
     * @param startTime 开始范围
     * @param endTime   结束范围
     * @return long
     */
    public Long queryTaskAnalyzeCount(Collection<Long> taskIds, Integer status, Long startTime, Long endTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").in(taskIds));
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (startTime != null) {
            query.addCriteria(Criteria.where("start_time").gte(startTime).lte(endTime));
        }

        return mongoTemplate.count(query, "t_task_log_overview");
    }

    /**
     * 按任务ID分组统计时间范围内的分析次数
     *
     * @param taskIds   任务ID集合
     * @param status    分析状态
     * @param startTime 开始范围
     * @param endTime   结束范围
     * @return list
     */
    public List<TaskLogOverviewModel> statAnalyzeCountByTaskIds(Collection<Long> taskIds, Integer status,
            Long startTime, Long endTime) {
        MatchOperation match = getIndexMatchOpera(taskIds, status, startTime, endTime);

        // 以任务ID进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .count().as("build_count");

        Aggregation agg = Aggregation.newAggregation(match, group);

        AggregationResults<TaskLogOverviewModel> queryResult =
                mongoTemplate.aggregate(agg, "t_task_log_overview", TaskLogOverviewModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 按任务ID获取时间范围内的最新分析状态
     *
     * @param taskIds   任务ID集合
     * @param status    分析状态
     * @return list
     */
    public List<TaskLogOverviewModel> findLatestAnalyzeStatus(Collection<Long> taskIds, Integer status) {
        List<Criteria> criteriaList = Lists.newArrayList();

        // 指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }

        // 筛选分析状态 enum ScanStatus
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        MatchOperation match = Aggregation.match(criteria);
        // 根据开始时间倒序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        // 以任务ID进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id")
                .first("build_num").as("build_num")
                .first("start_time").as("start_time")
                .first("status").as("status");

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);
        AggregationResults<TaskLogOverviewModel> queryResult =
                mongoTemplate.aggregate(agg, "t_task_log_overview", TaskLogOverviewModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 按任务ID分组获取时间范围内的build id
     *
     * @param taskIds   任务ID集合
     * @param status    分析状态
     * @param startTime 开始范围
     * @param endTime   结束范围
     * @return list
     */
    public List<TaskLogOverviewModel> findBuildIdsByStartTime(Collection<Long> taskIds, Integer status,
            Long startTime, Long endTime) {
        MatchOperation match = getIndexMatchOpera(taskIds, status, startTime, endTime);

        // 以开始时间倒序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");

        // 以任务ID进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id");

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);

        AggregationResults<TaskLogOverviewModel> queryResult =
                mongoTemplate.aggregate(agg, "t_task_log_overview", TaskLogOverviewModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 组装索引筛选条件
     * @return match
     */
    @NotNull
    private MatchOperation getIndexMatchOpera(Collection<Long> taskIds, Integer status, Long startTime, Long endTime) {
        List<Criteria> criteriaList = Lists.newArrayList();

        // 指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }

        // 筛选分析状态 enum ScanStatus
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        // 筛选执行时间
        if (startTime != null && endTime != null) {
            criteriaList.add(Criteria.where("start_time").gte(startTime).lte(endTime));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        return Aggregation.match(criteria);
    }
}
