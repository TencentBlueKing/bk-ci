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
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetTaskRelationshipModel;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 规则集数据DAO
 *
 * @version V2.0
 * @date 2020/5/12
 */
@Repository
public class CheckerSetDao {
    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据任务ID集合获取关联的规则集
     *
     * @param taskIdSet 任务ID集合
     * @return list
     */
    public List<CheckerSetTaskRelationshipModel> findByTaskId(Collection<Long> taskIdSet) {
        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteria.andOperator(Criteria.where("task_id").in(taskIdSet));
        }

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria)).withOptions(options);

        AggregationResults<CheckerSetTaskRelationshipModel> queryResults =
                mongoTemplate.aggregate(agg, "t_checker_set_task_relationship", CheckerSetTaskRelationshipModel.class);
        return queryResults.getMappedResults();
    }


    /**
     * 按规则集ID集合获取规则集详情列表
     *
     * @param checkerSetIdList 规则集ID集合
     * @param withProps        是否查询checkerProps字段
     * @return list
     */
    public List<CheckerSetModel> findByCheckerSetIdList(Collection<String> checkerSetIdList, Boolean withProps) {
        List<CheckerSetModel> checkerSetModelList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(checkerSetIdList)) {
            return checkerSetModelList;
        }

        Criteria criteria = new Criteria();
        criteria.andOperator(Criteria.where("checker_set_id").in(checkerSetIdList));
        // 简化查询字段
        List<String> queryFieldList = Lists.newArrayList("checker_set_id", "checker_set_name", "code_lang",
                "checker_set_lang", "scope", "version", "creator", "create_time", "last_update_time", "checker_count",
                "task_usage", "enable", "sort_weight", "project_id", "description", "catagories", "legacy", "official");
        if (withProps) {
            queryFieldList.add("checker_props");
        }
        ProjectionOperation project = Aggregation.project(queryFieldList.toArray(new String[0]));

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria), project).withOptions(options);

        AggregationResults<CheckerSetModel> queryResults =
                mongoTemplate.aggregate(agg, "t_checker_set", CheckerSetModel.class);
        return queryResults.getMappedResults();

    }


    /**
     * 根据规则集id和版本查询规则集
     */
    public CheckerSetModel findByCheckerSetIdAndVersion(String checkerSetId, int version) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("checker_set_id").is(checkerSetId)
                        .and("version").is(version)
        );
        return mongoTemplate.findOne(query, CheckerSetModel.class, "t_checker_set");
    }

    public CheckerSetModel findLatestVersionByCheckerSetId(String checkerSetId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("checker_set_id").is(checkerSetId))
                .with(new Sort(Sort.Direction.DESC,"version"));

        return mongoTemplate.findOne(query, CheckerSetModel.class, "t_checker_set");
    }

}
