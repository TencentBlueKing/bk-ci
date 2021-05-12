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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.apiquery.task.model.ToolElapseTimeModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工具分析耗时统计持久层
 *
 * @version V1.0
 * @date 2021/1/15
 */

@Repository
public class ToolElapseTimeDao {

    @Autowired
    @Qualifier("taskMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 按工具/来源多选 查询工具分析耗时
     *
     * @param dates        日期列表
     * @param createFrom   数据来源
     * @param scanStatType 扫描统计类型
     * @param toolNames    工具名
     * @return list
     */
    public List<ToolElapseTimeModel> findByConditions(Collection<String> dates, Collection<String> createFrom,
            String scanStatType, Collection<String> toolNames) {
        // 查询条件
        List<Criteria> criteriaList = Lists.newArrayList();
        // 日期
        if (CollectionUtils.isNotEmpty(dates)) {
            criteriaList.add(Criteria.where("date").in(dates));
        }
        // 来源 enum DefectStatType
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").in(createFrom));
        }
        // 是否超快增量 enum ScanStatType
        if (StringUtils.isNotEmpty(scanStatType)) {
            criteriaList.add(Criteria.where("scan_stat_type").is(scanStatType));
        }
        // 工具名
        if (CollectionUtils.isNotEmpty(toolNames)) {
            criteriaList.add(Criteria.where("tool_name").in(toolNames));
        }

        Criteria criteria = new Criteria();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        GroupOperation group = null;
        if (createFrom == null || createFrom.size() > 1) {
            group = Aggregation.group("date", "tool_name", "scan_stat_type")
                    .first("date").as("date")
                    .first("tool_name").as("tool_name")
                    .first("scan_stat_type").as("scan_stat_type")
                    .sum("total_elapse_time").as("total_elapse_time")
                    .sum("succ_analyze_count").as("succ_analyze_count")
                    .sum("fail_analyze_count").as("fail_analyze_count");
        }
        Aggregation agg;
        if (null == group) {
            agg = Aggregation.newAggregation(Aggregation.match(criteria));
        } else {
            agg = Aggregation.newAggregation(Aggregation.match(criteria), group);
        }

        AggregationResults<ToolElapseTimeModel> queryResults =
                mongoTemplate.aggregate(agg, "t_tool_elapse_time", ToolElapseTimeModel.class);

        return queryResults.getMappedResults();
    }

}
