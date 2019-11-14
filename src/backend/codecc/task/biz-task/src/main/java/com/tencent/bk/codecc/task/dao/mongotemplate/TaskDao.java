/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 任务持久层代码
 */
@Repository
public class TaskDao
{

    @Autowired
    private MongoTemplate mongoTemplate;

    public Boolean updateTask(long taskId, Long codeLang, String nameCn, List<String> taskOwner,
                              List<String> taskMember, String disableTime, int status, String userName)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId))
                .addCriteria(Criteria.where("status").is(TaskConstants.TaskStatus.ENABLE.value()));
        Update update = new Update();
        if (null != codeLang)
        {
            update.set("code_lang", codeLang);
        }
        if (StringUtils.isNotBlank(nameCn))
        {
            update.set("name_cn", nameCn);
        }
        if (!CollectionUtils.isEmpty(taskOwner))
        {
            update.set("task_owner", taskOwner);
        }
        if (!CollectionUtils.isEmpty(taskMember))
        {
            update.set("task_member", taskMember);
        }
        if (StringUtils.isNotBlank(disableTime))
        {
            update.set("disable_time", disableTime);
        }
        update.set("status", status);
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", userName);
        return mongoTemplate.updateMulti(query, update, TaskInfoEntity.class).isUpdateOfExisting();
    }


    public Boolean updateFilterPath(FilterPathInputVO pathInput, String userName)
    {
        Update update = new Update();
        if (StringUtils.isNotBlank(pathInput.getSvnRevision()))
        {
            update.set("svn_revision", pathInput.getSvnRevision());
        }
        if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(pathInput.getPathType()))
        {
            update.set("default_filter_path", pathInput.getDefaultFilterPath());
        }
        else
        {
            update.set("filter_path", pathInput.getFilterDir());
        }
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", userName);
        Query query = new Query(Criteria.where("task_id").is(pathInput.getTaskId()));
        return mongoTemplate.updateMulti(query, update, TaskInfoEntity.class).isUpdateOfExisting();
    }


    public Boolean updateEntity(TaskInfoEntity taskInfoEntity, String userName)
    {
        Update update = new Update();
        update.set("status", taskInfoEntity.getStatus());
        update.set("branch", taskInfoEntity.getBranch());
        update.set("repo_hash_id", taskInfoEntity.getRepoHashId());
        update.set("execute_date", taskInfoEntity.getExecuteDate());
        update.set("execute_time", taskInfoEntity.getExecuteTime());
        update.set("disable_time", taskInfoEntity.getDisableTime());
        update.set("disable_reason", taskInfoEntity.getDisableReason());
        update.set("last_disable_task_info", taskInfoEntity.getLastDisableTaskInfo());
        update.set("updated_by", userName);
        update.set("updated_date", System.currentTimeMillis());
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskInfoEntity.getTaskId()));
        return mongoTemplate.updateFirst(query, update, TaskInfoEntity.class).isUpdateOfExisting();
    }


}
