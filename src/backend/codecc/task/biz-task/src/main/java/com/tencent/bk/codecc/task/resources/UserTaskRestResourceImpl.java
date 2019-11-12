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

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserTaskRestResource;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExAction;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 任务清单资源实现
 *
 * @version V1.0
 * @date 2019/4/23
 */
@RestResource
public class UserTaskRestResourceImpl implements UserTaskRestResource
{
    @Autowired
    private TaskService taskService;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    @Qualifier("devopsTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PathFilterService pathFilterService;


    @Override
    public Result<TaskListVO> getTaskList(String projectId, String user)
    {
        return new Result<>(taskService.getTaskList(projectId, user));
    }

    @Override
    public Result<TaskListVO> getTaskBaseList(String projectId, String user)
    {
        return new Result<>(taskService.getTaskBaseList(projectId, user));
    }


    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<TaskBaseVO> getTaskInfo()
    {
        return new Result<>(taskService.getTaskInfo());
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<TaskDetailVO> getTask(Long taskId)
    {
        return new Result<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<TaskOverviewVO> getTaskOverview(Long taskId)
    {
        return new Result<>(taskService.getTaskOverview(taskId));
    }


    @Override
    public Result<TaskIdVO> registerDevopsTask(TaskDetailVO taskDetailVO, String projectId, String userName)
    {
        taskDetailVO.setProjectId(projectId);
        return new Result<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public Result<Boolean> checkDuplicateStream(String streamName)
    {
        return new Result<>(taskRegisterService.checkeIsStreamRegistered(streamName));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String projectId, String userName)
    {
        return new Result<>(taskService.updateTask(taskUpdateVO, taskId, userName));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> modifyTimeAnalysisTask(TimeAnalysisReqVO timeAnalysisReqVO, long taskId, String userName)
    {
        return new Result<>(taskRegisterService.modifyTimeAnalysisTask(timeAnalysisReqVO.getExecuteDate(), timeAnalysisReqVO.getExecuteTime(),
                taskId, userName));
    }


    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> addFilterPath(FilterPathInputVO filterPathInput, String userName)
    {
        return new Result<>(pathFilterService.addFilterPaths(filterPathInput, userName));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> deleteFilterPath(String path, String pathType, Long taskId, String userName)
    {
        return new Result<>(pathFilterService.deleteFilterPath(path, pathType, taskId, userName));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<FilterPathOutVO> filterPath(Long taskId)
    {
        return new Result<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<TreeNodeTaskVO> filterPathTree(Long taskId)
    {
        return new Result<>(pathFilterService.filterPathTree(taskId));
    }

    @Override
    public Result<Boolean> startTask(Long taskId, String userName)
    {
        return new Result<>(taskService.startTask(taskId, userName));
    }

    @Override
    public Result<TaskStatusVO> getTaskStatus(Long taskId)
    {
        return new Result<>(taskService.getTaskStatus(taskId));
    }


    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> stopTask(Long taskId, String disabledReason, String userName)
    {
        return new Result<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<TaskCodeLibraryVO> getCodeLibrary(Long taskId)
    {
        return new Result<>(taskService.getCodeLibrary(taskId));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> updateCodeLibrary(Long taskId, String userName, TaskCodeLibraryVO taskCodeLibrary)
    {
        return new Result<>(taskService.updateCodeLibrary(taskId, userName, taskCodeLibrary));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<Boolean> executeTask(long taskId, String isFirstTrigger,
                                       String userName)
    {
        return new Result<>(taskService.manualExecuteTask(taskId, isFirstTrigger, userName));
    }

    @Override
    @AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
    public Result<TaskMemberVO> getTaskMemberAndAdmin(long taskId, String projectId)
    {
        return new Result<>(taskService.getTaskMemberAndAdmin(taskId, projectId));
    }


}
