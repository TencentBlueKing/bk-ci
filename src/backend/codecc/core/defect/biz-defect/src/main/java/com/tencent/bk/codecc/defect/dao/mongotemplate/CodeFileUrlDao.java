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
 
package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码仓库url的持久化
 * 
 * @date 2019/10/25
 * @version V1.0
 */
@Repository
public class CodeFileUrlDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void upsert(long taskId, List<CodeFileUrlEntity> codeFileUrlEntityList)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CodeFileUrlEntity.class);
        if (CollectionUtils.isNotEmpty(codeFileUrlEntityList))
        {
            for (CodeFileUrlEntity entity : codeFileUrlEntityList)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)
                        .and("file_path").is(entity.getFile()));
                Update update = new Update();
                update.set("task_id", taskId)
                        .set("file_path", entity.getFile())
                        .set("url", entity.getUrl())
                        .set("version", entity.getVersion())
                        .set("scm_type", entity.getScmType());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }

}
