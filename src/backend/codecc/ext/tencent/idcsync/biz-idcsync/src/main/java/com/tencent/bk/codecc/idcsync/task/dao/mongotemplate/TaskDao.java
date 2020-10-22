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
 
package com.tencent.bk.codecc.idcsync.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.TaskFailRecordEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 任务操作持久类
 * 
 * @date 2020/8/24
 * @version V1.0
 */
@Repository
public class TaskDao 
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新任务错误码
     * @param taskId
     * @param taskFailRecordEntity
     */
    public void updateTaskFailRecord(Long taskId, TaskFailRecordEntity taskFailRecordEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        Update update = new Update();
        update.set("latest_scan_result", taskFailRecordEntity);
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
    }

}
