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
import com.tencent.bk.codecc.apiquery.task.model.ToolAnalyzeStatModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 工具配置持久层
 *
 * @version V1.0
 * @date 2021/01/26
 */
@Repository
public class AnalyzeCountStatDao {

    @Autowired
    @Qualifier("taskMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据来源、时间查询工具执行统计数据
     * @param dates         日期集合
     * @param createFromReq 来源
     */
    public List<ToolAnalyzeStatModel> getToolAnalyzeStat(List<String> dates, Set<String> createFromReq) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(dates)) {
            criteriaList.add(Criteria.where("date").in(dates));
        }
        if (CollectionUtils.isNotEmpty(createFromReq)) {
            criteriaList.add(Criteria.where("data_from").in(createFromReq));
        }
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria));
        AggregationResults<ToolAnalyzeStatModel> queryResults =
                mongoTemplate.aggregate(aggregation, "t_analyze_count_stat", ToolAnalyzeStatModel.class);
        return queryResults.getMappedResults();
    }


    /**
     * 查找符合条件的taskId集合
     *
     * @param toolName   工具名
     * @param createFrom 来源
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return list
     */
    public List<ToolAnalyzeStatModel> getToolAnalyzeTaskIds(String toolName, Set<String> createFrom, String startTime,
            String endTime) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        if (CollectionUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").in(createFrom));
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            criteriaList.add(Criteria.where("date").gte(startTime).lte(endTime));
        }
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, ToolAnalyzeStatModel.class, "t_analyze_count_stat");
    }
}
