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

package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.quality.pojo.request.QualityReviewRequest
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "APP_PIPELINE_BUILD", description = "app流水线相关接口")
@Path("/app/pipelineBuild")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface AppPipelineBuildResource {

    @Operation(summary = "获取流水线手动启动参数")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/manualStartupInfo")
    @Path("/{projectId}/{pipelineId}/manualStartupInfo")
    fun manualStartupInfo(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<BuildManualStartupInfo>

    @Operation(summary = "手动启动流水线")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/manualStartup")
    @Path("/{projectId}/{pipelineId}/")
    fun manualStartup(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "启动参数", required = true)
        values: Map<String, String>
    ): Result<BuildId>

    @Operation(summary = "手动停止流水线")
    @DELETE
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stop")
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun manualShutdown(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>

    @Operation(summary = "重试流水线-重试或者跳过失败插件")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/retry")
    @Path("/{projectId}/{pipelineId}/{buildId}/retry")
    fun retry(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(name = "要重试的原子任务ID", required = false)
        @QueryParam("taskId")
        taskId: String? = null,
        @Parameter(name = "仅重试所有失败Job", required = false)
        @QueryParam("failedContainer")
        failedContainer: Boolean? = false,
        @Parameter(name = "跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）", required = false)
        @QueryParam("skip")
        skipFailedTask: Boolean? = false
    ): Result<BuildId>

    @Operation(summary = "质量红线人工审核")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/qualityGateReview/{action}")
    fun manualQualityGateReview(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(name = "步骤Id", required = true)
        @PathParam("elementId")
        elementId: String,
        @Parameter(name = "动作", required = true)
        @PathParam("action")
        action: ManualReviewAction,
        @Parameter(name = "红线ID", required = false)
        request: QualityReviewRequest? = null
    ): Result<Boolean>

    @Operation(summary = "人工审核")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/review")
    fun manualReview(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(name = "原子ID", required = true)
        @PathParam("elementId")
        elementId: String,
        @Parameter(name = "审核信息", required = true)
        params: ReviewParam
    ): Result<Boolean>

    @Operation(summary = "人工审核(new)")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/toReview")
    fun goToReview(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(name = "步骤Id", required = true)
        @PathParam("elementId")
        elementId: String
    ): Result<ReviewParam>

    @Operation(summary = "触发审核")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/review")
    fun buildTriggerReview(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(name = "是否通过审核", required = true)
        @QueryParam("approve")
        approve: Boolean,
        @Parameter(name = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @Operation(summary = "手动触发启动阶段")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stages/{stageId}/manualStart")
    fun manualStartStage(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(name = "阶段ID", required = true)
        @PathParam("stageId")
        stageId: String,
        @Parameter(name = "取消执行", required = false)
        @QueryParam("cancel")
        cancel: Boolean?,
        @Parameter(name = "审核请求体", required = false)
        reviewRequest: StageReviewRequest? = null
    ): Result<Boolean>

    @Operation(summary = "获取流水线构建历史-new")
    @GET
    @Path("/{projectId}/{pipelineId}/history/new")
    fun getHistoryBuildNew(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(name = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(name = "代码库别名", required = false)
        @QueryParam("materialAlias")
        materialAlias: List<String>?,
        @Parameter(name = "代码库URL", required = false)
        @QueryParam("materialUrl")
        materialUrl: String?,
        @Parameter(name = "分支", required = false)
        @QueryParam("materialBranch")
        materialBranch: List<String>?,
        @Parameter(name = "commitId", required = false)
        @QueryParam("materialCommitId")
        materialCommitId: String?,
        @Parameter(name = "commitMessage", required = false)
        @QueryParam("materialCommitMessage")
        materialCommitMessage: String?,
        @Parameter(name = "状态", required = false)
        @QueryParam("status")
        status: List<BuildStatus>?,
        @Parameter(name = "触发方式", required = false)
        @QueryParam("trigger")
        trigger: List<StartType>?,
        @Parameter(name = "排队于-开始时间(时间戳形式)", required = false)
        @QueryParam("queueTimeStartTime")
        queueTimeStartTime: Long?,
        @Parameter(name = "排队于-结束时间(时间戳形式)", required = false)
        @QueryParam("queueTimeEndTime")
        queueTimeEndTime: Long?,
        @Parameter(name = "开始于-开始时间(时间戳形式)", required = false)
        @QueryParam("startTimeStartTime")
        startTimeStartTime: Long?,
        @Parameter(name = "开始于-结束时间(时间戳形式)", required = false)
        @QueryParam("startTimeEndTime")
        startTimeEndTime: Long?,
        @Parameter(name = "结束于-开始时间(时间戳形式)", required = false)
        @QueryParam("endTimeStartTime")
        endTimeStartTime: Long?,
        @Parameter(name = "结束于-结束时间(时间戳形式)", required = false)
        @QueryParam("endTimeEndTime")
        endTimeEndTime: Long?,
        @Parameter(name = "耗时最小值", required = false)
        @QueryParam("totalTimeMin")
        totalTimeMin: Long?,
        @Parameter(name = "耗时最大值", required = false)
        @QueryParam("totalTimeMax")
        totalTimeMax: Long?,
        @Parameter(name = "备注", required = false)
        @QueryParam("remark")
        remark: String?,
        @Parameter(name = "构件号起始", required = false)
        @QueryParam("buildNoStart")
        buildNoStart: Int?,
        @Parameter(name = "构件号结束", required = false)
        @QueryParam("buildNoEnd")
        buildNoEnd: Int?,
        @Parameter(name = "构建信息", required = false)
        @QueryParam("buildMsg")
        buildMsg: String?
    ): Result<BuildHistoryPage<BuildHistory>>
}
