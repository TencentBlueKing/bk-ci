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

import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetProjectRelationshipModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 规则集关联Dao
 *
 * @version V1.0
 * @date 2020/10/12
 */
@Repository
public class CheckerSetRelationshipDao {

    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据规则集ID集合、蓝盾项目ID获取关联列表
     *
     * @param checkerSetIds 规则集ID集合
     * @param projectId     蓝盾项目ID
     * @return list
     */
    public List<CheckerSetProjectRelationshipModel> findByCheckerSetIdInAndProjectId(Set<String> checkerSetIds,
            String projectId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("checker_set_id").in(checkerSetIds).and("project_id").is(projectId));

        return mongoTemplate
                .find(query, CheckerSetProjectRelationshipModel.class, "t_checker_set_project_relationship");
    }

}
