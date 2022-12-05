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

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.IgnoreCheckerEntity;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 配置忽略规则包持久层
 *
 * @version V1.0
 * @date 2019/6/6
 */
@Repository
public class IgnoreCheckerDao
{

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新忽略规则信息
     *
     * @param entity
     * @return
     */
    public void upsertIgnoreChecker(IgnoreCheckerEntity entity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(entity.getTaskId()))
                .addCriteria(Criteria.where("tool_name").is(entity.getToolName()));
        Update update = new Update();
        update.set("close_default_checkers", entity.getCloseDefaultCheckers());
        update.set("open_non_default_checkers", entity.getOpenNonDefaultCheckers());
        mongoTemplate.upsert(query, update, IgnoreCheckerEntity.class);
    }


    /**
     * 查询关闭默认规则包的任务ID
     *
     * @param toolName 工具名称
     * @param checkers 规则集合
     * @return task id list
     */
    public List<IgnoreCheckerEntity> queryCloseDefaultCheckers(String toolName, Collection<String> checkers)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("close_default_checkers", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        Criteria criteria = new Criteria();
        criteria.and("tool_name").is(toolName);
        criteria.and("close_default_checkers").all(checkers);

        query.addCriteria(criteria);

        return mongoTemplate.find(query, IgnoreCheckerEntity.class);
    }


}
