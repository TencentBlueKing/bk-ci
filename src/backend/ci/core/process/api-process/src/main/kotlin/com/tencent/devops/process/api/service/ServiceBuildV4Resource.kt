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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildTaskPauseInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_BUILD"], description = "服务-构建资源")
@Path("/service/buildsV4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceBuildV4Resource {

    @ApiOperation("获取流水线手动启动参数")
    @GET
    @Path("/{projectId}/manualStartupInfo")
    fun manualStartupInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<BuildManualStartupInfo>

    @ApiOperation("获取流水线构建历史")
    @GET
    @Path("/{projectId}/history")
    fun getHistoryBuild(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<BuildHistoryPage<BuildHistory>>

    @ApiOperation("手动启动流水线")
    @POST
    @Path("/")
    fun manualStartupNew(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("手动指定构建版本参数", required = false)
        @QueryParam("buildNo")
        buildNo: Int? = null,
        @ApiParam("启动类型", required = false)
        @QueryParam("startType")
        startType: StartType
    ): Result<BuildId>

    @ApiOperation("重试流水线-重试或者跳过失败插件")
    @POST
    @Path("/{projectId}/{buildId}/retry")
    fun retry(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("要重试或跳过的插件ID，或者StageId", required = false)
        @QueryParam("taskId")
        taskId: String? = null,
        @ApiParam("仅重试所有失败Job", required = false)
        @QueryParam("failedContainer")
        failedContainer: Boolean? = false,
        @ApiParam("跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）", required = false)
        @QueryParam("skip")
        skipFailedTask: Boolean? = false,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否忽略人工触发", required = false)
        @QueryParam("checkManualStartup")
        checkManualStartup: Boolean? = false
    ): Result<BuildId>

    @ApiOperation("手动停止流水线")
    @DELETE
    @Path("/{projectId}/{buildId}/")
    fun manualShutdown(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @ApiOperation("获取构建详情")
    @GET
    @Path("/{projectId}/{buildId}/detail")
    fun getBuildDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<ModelDetail>

    @ApiOperation("获取构建详情")
    @GET
    @Path("/{projectId}/{buildId}/status")
    fun getBuildStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<BuildHistoryWithVars>

    @ApiOperation("获取构建中的变量值")
    @POST
    @Path("/{projectId}/{buildId}/variables")
    fun getBuildVariableValue(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode = ChannelCode.BS,
        @ApiParam("变量名列表", required = true)
        variableNames: List<String>
    ): Result<Map<String, String>>

    @ApiOperation("手动触发启动阶段")
    @POST
    @Path("/projects/{projectId}/builds/{buildId}/stages/{stageId}/manualStart")
    fun manualStartStage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("阶段ID", required = true)
        @PathParam("stageId")
        stageId: String,
        @ApiParam("取消执行", required = false)
        @QueryParam("cancel")
        cancel: Boolean?,
        @ApiParam("审核请求体", required = false)
        reviewRequest: StageReviewRequest? = null
    ): Result<Boolean>

    @ApiOperation("操作暂停插件")
    @POST
    @Path("/projects/{projectId}/builds/{buildId}/execution/pause")
    fun executionPauseAtom(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        taskPauseExecute: BuildTaskPauseInfo
    ): Result<Boolean>
}
