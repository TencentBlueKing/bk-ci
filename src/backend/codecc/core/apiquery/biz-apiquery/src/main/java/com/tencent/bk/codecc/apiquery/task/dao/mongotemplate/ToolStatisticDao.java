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

package com.tencent.bk.codecc.apiquery.task.dao.mongotemplate;

import com.tencent.bk.codecc.apiquery.task.model.ToolStatisticModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 工具统计持久层
 *
 * @version V2.0
 * @date 2020/4/26
 */
@Repository
public class ToolStatisticDao {
    @Autowired
    @Qualifier("taskMongoTemplate")
    private MongoTemplate mongoTemplate;

    /**
     * 根据时间、来源、工具名 获取工具数、活跃工具数
     *
     * @param dates      时间集合
     * @param createFrom 来源
     * @param toolName   工具名
     * @return list
     */
    public List<ToolStatisticModel> findToolCountByCreateFromAndTimeAndToolName(List<String> dates,
            Set<String> createFrom, String toolName) {
        // 筛选条件
        Criteria criteria = getToolSearchCriteria(dates, createFrom, toolName);
        // 排序(按时间升序)
        SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "date");
        ProjectionOperation project = Aggregation.project("date", "tool_count", "active_count");
        // 分组
        GroupOperation group = null;
        // 如果是非开源
        if (createFrom.size() != 1) {
            group = Aggregation.group("date").first("date").as("date").sum("tool_count").as("tool_count")
                    .sum("active_count").as("active_count");
        }
        Aggregation aggregation;
        if (group == null) {
            aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, project);
        } else {
            aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, project, group);
        }
        AggregationResults<ToolStatisticModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_statistic", ToolStatisticModel.class);
        return queryResults.getMappedResults();
    }

    /**
     * 根据时间、来源、工具名 获取工具分析次数
     *
     * @param dates      时间集合
     * @param createFrom 来源
     * @param toolName   工具名
     * @return list
     */
    public List<ToolStatisticModel> findToolAnalyzeCount(List<String> dates, Set<String> createFrom,
            String toolName) {
        // 筛选条件
        Criteria criteria = getToolSearchCriteria(dates, createFrom, toolName);
        // 排序(按时间升序)
        SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "date");
        ProjectionOperation project = Aggregation.project("date", "analyze_count");
        // 分组
        GroupOperation group = null;
        // 如果是非开源
        if (createFrom.size() != 1) {
            group = Aggregation.group("date").first("date").as("date")
                    .sum("analyze_count").as("analyze_count");
        }
        Aggregation aggregation;
        if (group == null) {
            aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, project);
        } else {
            aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, group);
        }
        AggregationResults<ToolStatisticModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_tool_statistic", ToolStatisticModel.class);
        return queryResults.getMappedResults();
    }

    /**
     * 工具数 工具活跃数 工具分析次数 筛选条件
     *
     * @param dates
     * @param createFrom
     * @param toolName
     * @return
     */
    @NotNull
    private Criteria getToolSearchCriteria(List<String> dates, Set<String> createFrom, String toolName) {
        // 筛选条件
        List<Criteria> criteriaList = new ArrayList<>();
        // 时间
        if (CollectionUtils.isNotEmpty(dates)) {
            criteriaList.add(Criteria.where("date").in(dates));
        }
        //工具名
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        // 来源
        criteriaList.add(Criteria.where("data_from").in(createFrom));

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return criteria;
    }
}
