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

package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 告警持久类
 *
 * @version V1.0
 * @date 2019/9/29
 */

@Repository
public class DefectDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 按规则统计指定状态告警数
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @param checkers  规则集合
     * @return list
     */
    public List<CheckerStatisticEntity> findStatByTaskIdAndToolChecker(Collection<Long> taskIdSet, String toolName,
            List<Integer> status, Collection<String> checkers) {
        // 索引筛选
        MatchOperation matchIdx = Aggregation
                .match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName).and("status").in(status));
        // 普通筛选
        MatchOperation matchAft = Aggregation.match(Criteria.where("checker_name").in(checkers));

        GroupOperation group =
                Aggregation.group("checker_name").first("checker_name").as("id").count().as("defect_count");
        Aggregation agg = Aggregation.newAggregation(matchIdx, matchAft, group);
        return mongoTemplate.aggregate(agg, "t_defect", CheckerStatisticEntity.class).getMappedResults();
    }

}
