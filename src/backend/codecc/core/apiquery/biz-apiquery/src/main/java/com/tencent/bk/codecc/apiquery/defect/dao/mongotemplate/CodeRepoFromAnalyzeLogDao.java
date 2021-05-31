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

import com.tencent.bk.codecc.apiquery.task.model.CodeRepoFromAnalyzeLogModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CodeRepoFromAnalyzeLogDao
 *
 * @version V1.0
 * @date 2021/1/25
 */
@Repository
public class CodeRepoFromAnalyzeLogDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据taskIds获取代码仓库地址
     *
     * @param taskIds 任务id集合
     * @return list
     */
    public List<CodeRepoFromAnalyzeLogModel> getCodeRepoListByTaskIds(List<Long> taskIds) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds));

        UnwindOperation unwind = Aggregation.unwind("code_repo_list");

        // 以taskId进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .addToSet("code_repo_list.url").as("url");
        Aggregation agg = Aggregation.newAggregation(match, unwind, group);

        AggregationResults<CodeRepoFromAnalyzeLogModel> queryResult =
                mongoTemplate.aggregate(agg, "t_code_repo_from_analyzelog", CodeRepoFromAnalyzeLogModel.class);
        return queryResult.getMappedResults();
    }

}
