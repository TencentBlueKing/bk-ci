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

import com.tencent.bk.codecc.task.model.PipelineIdRelationshipEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 流水线维度构建持久层代码
 * 
 * @date 2020/10/27
 * @version V1.0
 */
@Repository
public class PipelineIdRelationDao 
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新流水线维度构建状态
     * @param pipelineId
     * @param triggerDate
     * @param status
     */
    public void updatePipelineIdRelationStatus(String pipelineId, LocalDate triggerDate, Integer status){
        Query query = new Query();
        query.addCriteria(Criteria.where("pipeline_id").is(pipelineId));
        query.addCriteria(Criteria.where("trigger_date").is(triggerDate));

        Update update = new Update();
        update.set("status", status);
        mongoTemplate.upsert(query, update, PipelineIdRelationshipEntity.class);
    }

    /**
     * 删除特定触发日期前的记录
     * @param triggerDate
     */
    public void deleteRecordsBeforeDate(LocalDate triggerDate){
        Query query = new Query();
        query.addCriteria(Criteria.where("trigger_date").lt(triggerDate));
        mongoTemplate.remove(query, PipelineIdRelationshipEntity.class);
    }

}
