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

package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class LintDefectV2Dao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public void batchUpdateDefectAuthor(long taskId, List<LintDefectV2Entity> defectList)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            defectList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", defectEntity.getAuthor());
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的exclude位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusExcludeBit(long taskId, List<LintDefectV2Entity> defectList)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            defectList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("exclude_time", defectEntity.getExcludeTime());
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    public List<LintDefectV2Entity> findDefectsByFilePath(Long taskId, String toolName, Set<Integer> excludeStatusSet, Set<String> filterPaths)
    {
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("status", true);
        fieldsObj.put("exclude_time", true);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);

        query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").nin(excludeStatusSet));

        Criteria orOperator = new Criteria();
        filterPaths.forEach(file -> orOperator.orOperator(Criteria.where("file_path").regex(file)));
        query.addCriteria(orOperator);

        return mongoTemplate.find(query, LintDefectV2Entity.class);
    }

}
