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
import com.tencent.devops.common.api.pojo.CodeCCResult;
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
    public CodeCCResult<TaskBaseVO> getTaskInfo(String nameEn)
    {
        return new CodeCCResult<>(taskService.getTaskInfo(nameEn));
    }

    @Override
    public CodeCCResult<TaskBaseVO> getTaskToolList(long taskId)
    {
        return new CodeCCResult<>(taskService.getTaskToolList(taskId));
    }

    @Override
    public CodeCCResult<TaskDetailVO> getTaskInfoById(Long taskId)
    {
        return new CodeCCResult<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public CodeCCResult<Boolean> updateTask(TaskDetailVO taskDetailVO, String userName)
    {
        return new CodeCCResult<>(taskRegisterService.updateTaskFromPipeline(taskDetailVO, userName));
    }

    @Override
    public CodeCCResult<TaskIdVO> registerPipelineTask(TaskDetailVO taskDetailVO, String projectId, String userName)
    {
        taskDetailVO.setProjectId(projectId);
        return new CodeCCResult<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public CodeCCResult<Boolean> stopTask(Long taskId, String disabledReason, String userName)
    {
        return new CodeCCResult<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public CodeCCResult<Boolean> checkTaskExists(Long taskId)
    {
        return new CodeCCResult<>(taskService.checkTaskExists(taskId));
    }

    @Override
    public CodeCCResult<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache()
    {
        return new CodeCCResult<>(taskService.getToolMetaListFromCache());
    }

    @Override
    public CodeCCResult<TaskDetailVO> getPipelineTask(String pipelineId)
    {
        return new CodeCCResult<>(taskService.getTaskInfoByPipelineId(pipelineId));
    }


}
