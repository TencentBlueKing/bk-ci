/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.GongfengStatProjEntity;
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
 * 工蜂度量数据存取对象
 *
 * @version V1.1
 * @date 2020/3/24
 */

@Repository
public class GongfengStatProjDao
{

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 插入和更新工蜂度量项目数据
     *
     * @param statProjectList entityList
     */
    public void upsertGongfengStatProjList(List<GongfengStatProjEntity> statProjectList)
    {
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GongfengStatProjEntity.class);
        if (CollectionUtils.isNotEmpty(statProjectList))
        {
            for (GongfengStatProjEntity statProjEntity : statProjectList)
            {
                Query query = new Query();
                Criteria criteria = Criteria.where("id").is(statProjEntity.getId());
                query.addCriteria(criteria);

                Update update = new Update();
                update.set("id", statProjEntity.getId())
                        .set("bg_id", statProjEntity.getBgId())
                        .set("org_paths", statProjEntity.getOrgPaths())
                        .set("path", statProjEntity.getPath())
                        .set("description", statProjEntity.getDescription())
                        .set("visibility", statProjEntity.getVisibility())
                        .set("visibility_level", statProjEntity.getVisibilityLevel())
                        .set("belong", statProjEntity.getBelong())
                        .set("owners", statProjEntity.getOwners())
                        .set("current_owners", statProjEntity.getCurrentOwners())
                        .set("current_owners_org_paths", statProjEntity.getCurrentOwnersOrgPaths())
                        .set("created_at", statProjEntity.getCreatedAt())
                        .set("creator", statProjEntity.getCreator())
                        .set("url", statProjEntity.getUrl())
                        .set("archived", statProjEntity.getArchived())
                        .set("is_sensitive", statProjEntity.getIsSensitive())
                        .set("sensitive_reason", statProjEntity.getSensitiveReason())
                        .set("public_visibility", statProjEntity.getPublicVisibility())
                        .set("synchronize_time", statProjEntity.getSynchronizeTime());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }


}
