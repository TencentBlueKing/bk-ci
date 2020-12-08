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

import com.tencent.bk.codecc.defect.model.CheckerConfigEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * 代码仓库url的持久化
 *
 * @version V1.0
 * @date 2019/10/25
 */
@Repository
public class ToolBuildStackDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void upsert(ToolBuildStackEntity toolBuildStackEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(toolBuildStackEntity.getTaskId())
                .and("tool_name").is(toolBuildStackEntity.getToolName())
                .and("build_id").is(toolBuildStackEntity.getBuildId()));

        Update update = new Update();
        update.set("task_id", toolBuildStackEntity.getTaskId())
                .set("tool_name", toolBuildStackEntity.getToolName())
                .set("build_id", toolBuildStackEntity.getBuildId())
                .set("base_build_id", toolBuildStackEntity.getBaseBuildId())
                .set("full_scan", toolBuildStackEntity.isFullScan())
                .set("delete_files", toolBuildStackEntity.getDeleteFiles());
        mongoTemplate.upsert(query, update, ToolBuildStackEntity.class);
    }

    public void batchUpsert(Collection<ToolBuildStackEntity> toolBuildStackEntitys)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ToolBuildStackEntity.class);
        if (CollectionUtils.isNotEmpty(toolBuildStackEntitys))
        {
            for (ToolBuildStackEntity entity : toolBuildStackEntitys)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(entity.getTaskId())
                        .and("tool_name").is(entity.getToolName())
                        .and("build_id").is(entity.getBuildId()));
                Update update = new Update();
                update.set("task_id", entity.getTaskId())
                        .set("tool_name", entity.getToolName())
                        .set("build_id", entity.getBuildId())
                        .set("base_build_id", entity.getBaseBuildId())
                        .set("full_scan", entity.isFullScan())
                        .set("delete_files", entity.getDeleteFiles());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }
}
