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

import com.tencent.bk.codecc.task.model.GrayTaskCategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 灰度任务分类持久
 *
 * @version V1.0
 * @date 2021/1/4
 */
@Repository
public class GrayTaskCategoryDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据项目id和工蜂仓库id插入或更新灰度表
     * @param grayTaskCategoryEntity
     * @param user
     */
    public void upsertByProjectIdAndGongfengProjectId(GrayTaskCategoryEntity grayTaskCategoryEntity, String user) {
        Query query = new Query();
        query.addCriteria(Criteria.where("project_id").is(grayTaskCategoryEntity.getProjectId()));
        query.addCriteria(Criteria.where("gongfeng_project_id").is(grayTaskCategoryEntity.getGongfengProjectId()));
        Update update = new Update();
        update.set("project_id", grayTaskCategoryEntity.getProjectId());
        update.set("pipeline_id", grayTaskCategoryEntity.getPipelineId());
        update.set("task_id", grayTaskCategoryEntity.getTaskId());
        update.set("gongfeng_project_id", grayTaskCategoryEntity.getGongfengProjectId());
        update.set("category", grayTaskCategoryEntity.getCategory());
        update.set("status", grayTaskCategoryEntity.getStatus());
        mongoTemplate.upsert(query, update, GrayTaskCategoryEntity.class);
    }


    /**
     * 更新项目状态
     * @param projectId
     * @param pipelineId
     * @param status
     */
    public void updateStatus(String projectId, String pipelineId, Integer status) {
        Query query = new Query();
        query.addCriteria(Criteria.where("project_id").is(projectId))
                .addCriteria(Criteria.where("pipeline_id").is(pipelineId));
        Update update = new Update();
        update.set("status", status);
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", "CodeCC");
        mongoTemplate.updateFirst(query, update, GrayTaskCategoryEntity.class);
    }

}
