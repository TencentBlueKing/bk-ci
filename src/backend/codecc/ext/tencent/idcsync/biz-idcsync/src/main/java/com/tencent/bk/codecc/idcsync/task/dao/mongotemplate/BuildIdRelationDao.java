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

import com.tencent.bk.codecc.task.model.BuildIdRelationshipEntity;
import com.tencent.bk.codecc.task.model.TaskFailRecordEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;


/**
 * 构建id映射表持久类
 * 
 * @date 2020/8/24
 * @version V1.0
 */
@Repository
public class BuildIdRelationDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新映射表状态及错误码
     * @param buildId
     * @param status
     * @param taskFailRecordEntity
     */
    public void updateRelationStatus(String buildId,
                                     Integer status,
                                     TaskFailRecordEntity taskFailRecordEntity,
                                     Long elapseTime,
                                     Long taskId,
                                     String buildNum) {
        Query query = new Query();
        query.addCriteria(Criteria.where("build_id").is(buildId));
        Update update = new Update();
        update.set("status", status);
        update.set("build_num", buildNum);
        if (null != taskFailRecordEntity) {
            update.set("scan_err_code", taskFailRecordEntity);
        }
        if (null != elapseTime) {
            update.set("elapse_time", elapseTime);
        }
        if (null != taskId) {
            update.set("task_id", taskId);
        }
        mongoTemplate.updateFirst(query, update, BuildIdRelationshipEntity.class);
    }
}
