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
 
package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.BuildIdRelationshipEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 构建id映射持久类
 * 
 * @date 2020/8/27
 * @version V1.0
 */
@Repository
public class BuildIdRelationDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据查询条件更新commit_id
     * @param buildId
     * @param commitId
     */
    public void updateCommitId(String buildId, String commitId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("build_id").is(buildId));
        Update update = new Update();
        update.set("commit_id", commitId);
        mongoTemplate.updateFirst(query, update, BuildIdRelationshipEntity.class);
    }
}
