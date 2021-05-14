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

import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel;
import com.tencent.devops.common.constant.ComConstants.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * cloc代码统计持久类
 *
 * @version V1.0
 * @date 2020/4/9
 */
@Repository
public class CLOCStatisticsDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;



    /**
     * 按任务ID批量获取最新构建ID
     *
     * @param taskIds 任务ID集合
     * @return build id
     */
    public List<CLOCStatisticModel> queryLastBuildIdByTaskIds(Collection<Long> taskIds) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds)
                .and("tool_name").is(Tool.CLOC.name()));

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "_id");

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id");

        Aggregation agg = Aggregation.newAggregation(match, sort, group);
        AggregationResults<CLOCStatisticModel> queryResult =
                mongoTemplate.aggregate(agg, "t_cloc_statistic", CLOCStatisticModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 按任务ID和最新构建ID统计总代码数
     *
     * @param taskIds      任务ID集合
     * @param lastBuildIds 最新构建ID集合
     * @return list
     */
    public List<CLOCStatisticModel> batchStatClocStatisticByTaskId(Collection<Long> taskIds,
            List<String> lastBuildIds) {
        MatchOperation match =
                Aggregation.match(Criteria.where("task_id").in(taskIds)
                        .and("build_id").in(lastBuildIds)
                        .and("tool_name").is(Tool.CLOC.name()));

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .sum("sum_code").as("sum_code")
                .sum("sum_blank").as("sum_blank")
                .sum("sum_comment").as("sum_comment");

        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CLOCStatisticModel> queryResult =
                mongoTemplate.aggregate(agg, "t_cloc_statistic", CLOCStatisticModel.class);
        return queryResult.getMappedResults();
    }

}
