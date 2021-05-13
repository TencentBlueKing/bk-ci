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

import com.tencent.bk.codecc.apiquery.defect.model.MetricsModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 度量数据Dao
 *
 * @version V1.0
 * @date 2021/3/23
 */

@Repository
public class MetricsDao {

    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 获取度量数据
     *
     * @param taskIds  任务id集合
     * @param buildIds 构建id集合
     * @return list
     */
    public List<MetricsModel> findByTaskIdAndBuildId(Collection<Long> taskIds, Collection<String> buildIds) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").in(taskIds).and("build_id").in(buildIds));
        return mongoTemplate.find(query, MetricsModel.class, "t_metrics");
    }

}
