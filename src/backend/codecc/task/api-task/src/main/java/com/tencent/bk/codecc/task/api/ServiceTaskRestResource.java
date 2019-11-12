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

package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.*;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.*;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"SERVICE_TASK"}, description = "任务管理接口")
@Path("/service/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTaskRestResource
{
    @ApiOperation("获取任务信息")
    @Path("/taskInfo")
    @GET
    Result<TaskBaseVO> getTaskInfo(
            @ApiParam(value = "任务英文名", required = true)
            @QueryParam("nameEn")
                    String nameEn);

    @ApiOperation("获取任务已接入工具列表")
    @Path("/tools")
    @GET
    Result<TaskBaseVO> getTaskToolList(
            @ApiParam(value = "任务ID", required = true)
            @QueryParam("taskId")
                    long taskId);


    @ApiOperation("从流水线注册任务")
    @Path("/")
    @POST
    Result<TaskIdVO> registerPipelineTask(
            @ApiParam(value = "任务详细信息", required = true)
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "当前项目", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoById(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );


    @ApiOperation("修改任务信息")
    @Path("/")
    @PUT
    Result<Boolean> updateTask(
            @ApiParam(value = "任务修改信息", required = true)
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );

    @ApiOperation("停用任务")
    @Path("/{taskId}")
    @DELETE
    Result<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "停用原因", required = true)
            @QueryParam("disabledReason")
                    String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );

    @ApiOperation("检查任务是否存在")
    @Path("/exists/{taskId}")
    @GET
    Result<Boolean> checkTaskExists(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );


    @ApiOperation("获取所有的基础工具信息")
    @Path("/tool/meta")
    @GET
    Result<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache();



    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/task/info/{pipelineId}")
    @GET
    Result<TaskDetailVO> getPipelineTask(
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId
    );

}
