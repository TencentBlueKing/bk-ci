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

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.CodeCCHeaderKt.*;

/**
 * 任务接口
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"USER_TASK"}, description = "任务管理接口")
@Path("/user/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskRestResource
{

    @ApiOperation("获取任务清单")
    @Path("/")
    @GET
    Result<TaskListVO> getTaskList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user
    );

    @ApiOperation("获取任务基本信息清单")
    @Path("/base")
    @GET
    Result<TaskListVO> getTaskBaseList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user);


    @ApiOperation("获取任务信息")
    @Path("/taskInfo")
    @GET
    Result<TaskBaseVO> getTaskInfo();


    @ApiOperation("获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTask(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );

    @ApiOperation("获取任务信息概览")
    @Path("/overview/{taskId}")
    @GET
    Result<TaskOverviewVO> getTaskOverview(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId);

    @ApiOperation("从持续集成平台注册新任务")
    @Path("/")
    @POST
    Result<TaskIdVO> registerDevopsTask(
            @ApiParam(value = "任务信息", required = true)
            @Valid
                    TaskDetailVO taskDetailVO,
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("校验任务英文名是否重复")
    @Path("/duplicate/streamName/{nameEn}")
    @GET
    Result<Boolean> checkDuplicateStream(
            @ApiParam(value = "任务英文名", required = true)
            @PathParam("nameEn")
                    String streamName);


    @ApiOperation("修改定时任务信息")
    @Path("/timing")
    @PUT
    Result<Boolean> modifyTimeAnalysisTask(
            @ApiParam(value = "定时分析信息", required = true)
                    TimeAnalysisReqVO timeAnalysisReqVO,
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @ApiOperation("修改任务")
    @Path("/")
    @PUT
    Result<Boolean> updateTask(
            //@Valid
            @ApiParam(value = "任务更新信息", required = true)
                    TaskUpdateVO taskUpdateVO,
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("添加路径屏蔽")
    @Path("/filter/path")
    @POST
    Result<Boolean> addFilterPath(
            @ApiParam(value = "任务信息", required = true)
            @Valid
                    FilterPathInputVO filterPathInput,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("删除路径屏蔽")
    @Path("/filter/path")
    @DELETE
    Result<Boolean> deleteFilterPath(
            @ApiParam(value = "删除路径", required = true)
            @QueryParam("path")
                    String path,
            @ApiParam(value = "路径类型", required = true)
            @QueryParam("pathType")
                    String pathType,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("路径屏蔽列表")
    @Path("/filter/path/{taskId}")
    @GET
    Result<FilterPathOutVO> filterPath(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId
    );


    @ApiOperation("路径屏蔽树")
    @Path("/filter/path/tree")
    @GET
    Result<TreeNodeTaskVO> filterPathTree(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId
    );


    @ApiOperation("启用任务")
    @Path("/start")
    @PUT
    Result<Boolean> startTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("获得任务状态")
    @Path("/status")
    @GET
    Result<TaskStatusVO> getTaskStatus(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId);


    @ApiOperation("停用任务")
    @Path("/stop")
    @PUT
    Result<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "停用原因", required = true)
                    String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName
    );


    @ApiOperation("获取代码库配置信息")
    @Path("/code/lib")
    @GET
    Result<TaskCodeLibraryVO> getCodeLibrary(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId
    );


    @ApiOperation("更新代码库配置信息")
    @Path("/code/lib")
    @PUT
    Result<Boolean> updateCodeLibrary(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @Valid
            @ApiParam(value = "代码库信息", required = true)
                    TaskCodeLibraryVO taskCodeLibrary
    );


    @ApiOperation("触发立即分析")
    @Path("/execute")
    @POST
    Result<Boolean> executeTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "是否首次触发")
            @QueryParam("isFirstTrigger")
                    String isFirstTrigger,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);

    @ApiOperation("获取任务成员清单")
    @Path("/memberList")
    @GET
    Result<TaskMemberVO> getTaskMemberAndAdmin(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId
    );


}
