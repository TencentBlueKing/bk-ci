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

import com.tencent.bk.codecc.task.model.CustomProjEntity;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 查询个性化工程持久类
 * 
 * @date 2020/5/19
 * @version V1.0
 */
@Repository
public class CustomProjDao 
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据url和分支查询
     * @param customProjSource
     * @param url
     * @param branch
     * @return
     */
    public CustomProjEntity findByGongfengIdAndUrlAndBranch(String customProjSource, Integer gongfengProjectId,
                                                            String url, String branch, String logicRepo) {
        Document fieldsObj = new Document();
        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("custom_proj_source").is(customProjSource));
        if (null != gongfengProjectId) {
            query.addCriteria(Criteria.where("gongfeng_project_id").is(gongfengProjectId));
        } else {
            query.addCriteria(Criteria.where("url").is(url));
        }
        if (StringUtils.isNotBlank(branch)) {
            query.addCriteria(Criteria.where("branch").is(branch));
        }
        if (StringUtils.isNotBlank(logicRepo)) {
            query.addCriteria(Criteria.where("logic_repo").is(logicRepo));
        }
        return mongoTemplate.findOne(query, CustomProjEntity.class, "t_customized_project");
    }
}
