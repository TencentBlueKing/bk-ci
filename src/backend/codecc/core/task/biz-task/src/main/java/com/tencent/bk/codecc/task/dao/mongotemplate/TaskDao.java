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

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

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
            update.set("test_source_filter_path", pathInput.getTestSourceFilterPath());
            update.set("auto_gen_filter_path", pathInput.getAutoGenFilterPath());
            update.set("third_party_filter_path", pathInput.getThirdPartyFilterPath());
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
        update.set("os_type", taskInfoEntity.getOsType());
        update.set("build_env", taskInfoEntity.getBuildEnv());
        update.set("alias_name", taskInfoEntity.getAliasName());
        update.set("project_build_type", taskInfoEntity.getProjectBuildType());
        update.set("project_build_command", taskInfoEntity.getProjectBuildCommand());
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

    /**
     * 查询事业群下的部门ID集合
     *
     * @param bgId       事业群ID
     * @param createFrom 任务创建来源
     * @return deptList
     */
    public List<TaskInfoEntity> queryDeptId(Integer bgId, String createFrom)
    {
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("dept_id", true);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);

        if (bgId != null)
        {
            query.addCriteria(Criteria.where("bg_id").is(bgId));
        }
        if (StringUtils.isNotEmpty(createFrom))
        {
            query.addCriteria(Criteria.where("create_from").is(createFrom));
        }

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 多条件查询任务列表
     *
     * @param status     任务状态
     * @param bgId       事业群ID
     * @param deptIds    部门ID列表（多选）
     * @param taskIds    任务ID列表（批量）
     * @param createFrom 创建来源（多选）
     * @return task list
     */
    public List<TaskInfoEntity> queryTaskInfoEntityList(Integer status, Integer bgId, Collection<Integer> deptIds,
            Collection<Long> taskIds, Collection<String> createFrom)
    {
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("execute_time", false);
        fieldsObj.put("execute_date", false);
        fieldsObj.put("timer_expression", false);
        fieldsObj.put("last_disable_task_info", false);
        fieldsObj.put("default_filter_path", false);
        fieldsObj.put("workspace_id", false);
        fieldsObj.put("tool_config_info_list", false);
        fieldsObj.put("compile_plat", false);
        fieldsObj.put("run_plat", false);

        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);
        // 任务状态筛选
        if (status != null)
        {
            query.addCriteria(Criteria.where("status").is(status));
        }
        // 指定批量任务
        if (!CollectionUtils.isEmpty(taskIds))
        {
            query.addCriteria(Criteria.where("task_id").in(taskIds));
        }
        // 事业群ID筛选
        if (bgId != null && bgId != 0)
        {
            query.addCriteria(Criteria.where("bg_id").is(bgId));
        }
        // 部门ID筛选
        if (!CollectionUtils.isEmpty(deptIds))
        {
            query.addCriteria(Criteria.where("dept_id").in(deptIds));
        }
        // 创建来源筛选
        if (!CollectionUtils.isEmpty(createFrom))
        {
            query.addCriteria(Criteria.where("create_from").in(createFrom));
        }

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }


    /**
     * 更新组织架构信息
     *
     * @param taskInfoEntity entity
     * @return result
     */
    public Boolean updateOrgInfo(@NotNull TaskInfoEntity taskInfoEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskInfoEntity.getTaskId()));

        Update update = new Update();
        update.set("bg_id", taskInfoEntity.getBgId());
        update.set("dept_id", taskInfoEntity.getDeptId());
        update.set("center_id", taskInfoEntity.getCenterId());

        return mongoTemplate.updateFirst(query, update, TaskInfoEntity.class).isUpdateOfExisting();
    }

    /**
     * 根据自定义条件获取taskId信息
     * @param customParam 匹配自定义参数（is(customParam) in(customParam)不能为 null 或者 empty
     *
     * @param nCustomParam 不匹配自定义参数
     */
    public List<TaskInfoEntity> queryTaskInfoByCustomParam(Map<String, Object> customParam,
            Map<String, Object> nCustomParam) {
        if (customParam == null || customParam.isEmpty()) {
            throw new IllegalArgumentException("查询条件不能为空");
        }

        Criteria criteria;
        List<String> fields = Lists.newArrayList(customParam.keySet());
        if (customParam.get(fields.get(0)) instanceof Collection) {
            criteria = Criteria.where(fields.get(0)).in(customParam.get(fields.get(0)));
        } else {
            criteria = Criteria.where(fields.get(0)).is(customParam.get(fields.get(0)));
        }

        fields.stream()
                .skip(1)
                .forEach(field -> {
                    if (customParam.get(field) instanceof Collection) {
                        criteria.and(field).in(customParam.get(field));
                    } else {
                        criteria.and(field).is(customParam.get(field));
                    }
                });

        fields = Lists.newArrayList(nCustomParam.keySet());
        fields.forEach(field -> {
            if (nCustomParam.get(field) instanceof Collection) {
                criteria.and(field).nin(nCustomParam.get(field));
            } else {
                criteria.and(field).ne(nCustomParam.get(field));
            }
        });

        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, TaskInfoEntity.class);
    }
}
