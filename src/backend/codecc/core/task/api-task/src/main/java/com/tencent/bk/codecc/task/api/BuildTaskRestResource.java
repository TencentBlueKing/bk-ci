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
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.devops.common.api.pojo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.*;

/**
 * 构建机任务接口
 *
 * @version V1.0
 * @date 2019/7/21
 */
@Api(tags = {"BUILD_TASK"}, description = "任务管理接口")
@Path("/build/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskRestResource
{

    @ApiOperation("获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoById(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId
    );

    @ApiOperation("获取任务信息")
    @Path("/streamName/{streamName}")
    @GET
    Result<TaskDetailVO> getTaskInfoByStreamName(
            @ApiParam(value = "流名称（也即任务英文名）", required = true)
            @PathParam(value = "streamName")
                    String streamName
    );

    @ApiOperation("获取任务信息")
    @Path("/pipeline/{pipelineId}")
    @GET
    Result<PipelineTaskVO> getTaskInfoByPipelineId(
        @ApiParam(value = "流水线id", required = true)
        @PathParam(value = "pipelineId")
            String pipelineId,
        @ApiParam(value = "用户id", required = true)
        @QueryParam(value = "userId")
            String userId
    );

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

    @ApiOperation("保存定制化报告信息")
    @Path("/report")
    @POST
    Result<Boolean> updateTaskReportInfo(
            @ApiParam(value = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @ApiParam(value = "通知信息", required = true)
                    NotifyCustomVO notifyCustomVO);

    @ApiOperation("修改任务扫描触发配置")
    @Path("/taskId/{taskId}/scanConfiguration")
    @POST
    Result<Boolean> updateScanConfiguration(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
                    Long taskId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @ApiParam(value = "定时分析信息", required = true)
                    ScanConfigurationVO scanConfigurationVO
    );

    @ApiOperation("添加路径屏蔽")
    @Path("/add/filter/path")
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
    @Path("/del/filter")
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

    @ApiOperation("更新code.yml的路径屏蔽")
    @Path("/code/yml/filter/update")
    @POST
    Result<Boolean> codeYmlFilterPath(
        @ApiParam(value = "任务ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
        @ApiParam(value = "当前用户", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
        @ApiParam(value = "当前用户", required = true)
            CodeYmlFilterPathVO codeYmlFilterPathVO
    );

    @ApiOperation("获取工具platform信息")
    @Path("/toolConfig/info")
    @GET
    Result<ToolConfigPlatformVO> getToolConfigInfo(
            @ApiParam(value = "任务ID", required = true)
            @QueryParam("taskId")
                    Long taskId,
            @ApiParam(value = "工具名称", required = true)
            @QueryParam("toolName")
                    String toolName
    );

    @ApiOperation("发送开始任务信号")
    @Path("/startSignal/taskId/{taskId}/buildId/{buildId}")
    @GET
    Result<Boolean> sendStartTaskSignal(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "构件号id", required = true)
            @PathParam("buildId")
            String buildId);

    @ApiOperation("触发立即分析")
    @Path("/execute")
    @POST
    Result<Boolean> executeTask(
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "是否首次触发")
            @QueryParam("isFirstTrigger")
                    String isFirstTrigger,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);

    @ApiOperation("添加路径白名单")
    @Path("/path")
    @POST
    Result<Boolean> addWhitePath(
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @ApiParam(value = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @ApiParam(value = "任务信息", required = true)
                    List<String> pathList
    );
}
