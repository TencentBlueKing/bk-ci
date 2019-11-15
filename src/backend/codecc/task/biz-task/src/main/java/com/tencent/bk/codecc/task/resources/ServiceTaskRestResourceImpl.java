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

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.*;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;

/**
 * 服务间任务管理接口
 *
 * @version V1.0
 * @date 2019/5/2
 */
@RestResource
public class ServiceTaskRestResourceImpl implements ServiceTaskRestResource
{
    @Autowired
    private TaskService taskService;

    @Autowired
    @Qualifier("pipelineTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Override
    public Result<TaskBaseVO> getTaskInfo(String nameEn)
    {
        return new Result<>(taskService.getTaskInfo(nameEn));
    }

    @Override
    public Result<TaskBaseVO> getTaskToolList(long taskId)
    {
        return new Result<>(taskService.getTaskToolList(taskId));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoById(Long taskId)
    {
        return new Result<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public Result<Boolean> updateTask(TaskDetailVO taskDetailVO, String userName)
    {
        return new Result<>(taskRegisterService.updateTaskFromPipeline(taskDetailVO, userName));
    }

    @Override
    public Result<TaskIdVO> registerPipelineTask(TaskDetailVO taskDetailVO, String projectId, String userName)
    {
        taskDetailVO.setProjectId(projectId);
        return new Result<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public Result<Boolean> stopTask(Long taskId, String disabledReason, String userName)
    {
        return new Result<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public Result<Boolean> checkTaskExists(Long taskId)
    {
        return new Result<>(taskService.checkTaskExists(taskId));
    }

    @Override
    public Result<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache()
    {
        return new Result<>(taskService.getToolMetaListFromCache());
    }

    @Override
    public Result<TaskDetailVO> getPipelineTask(String pipelineId)
    {
        return new Result<>(taskService.getTaskInfoByPipelineId(pipelineId));
    }


}
