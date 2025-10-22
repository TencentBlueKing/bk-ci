/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.quality.pojo.request.QualityReviewRequest
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

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
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "指定草稿版本（为调试构建）", required = false)
        @QueryParam("version")
        version: Int?
    ): Result<BuildManualStartupInfo>

    @Operation(summary = "手动启动流水线")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/manualStartup")
    @Path("/{projectId}/{pipelineId}/")
    fun manualStartup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "启动参数", required = true)
        values: Map<String, String>,
        @Parameter(description = "指定草稿版本（为调试构建）", required = false)
        @QueryParam("version")
        version: Int?
    ): Result<BuildId>

    @Operation(summary = "手动停止流水线")
    @DELETE
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stop")
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun manualShutdown(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>

    @Operation(summary = "重试流水线-重试或者跳过失败插件")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/retry")
    @Path("/{projectId}/{pipelineId}/{buildId}/retry")
    fun retry(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "要重试的原子任务ID", required = false)
        @QueryParam("taskId")
        taskId: String? = null,
        @Parameter(description = "仅重试所有失败Job", required = false)
        @QueryParam("failedContainer")
        failedContainer: Boolean? = false,
        @Parameter(description = "跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）", required = false)
        @QueryParam("skip")
        skipFailedTask: Boolean? = false
    ): Result<BuildId>

    @Operation(summary = "质量红线人工审核")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/qualityGateReview/{action}")
    fun manualQualityGateReview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "步骤Id", required = true)
        @PathParam("elementId")
        elementId: String,
        @Parameter(description = "动作", required = true)
        @PathParam("action")
        action: ManualReviewAction,
        @Parameter(description = "红线ID", required = false)
        request: QualityReviewRequest? = null
    ): Result<Boolean>

    @Operation(summary = "人工审核")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/review")
    fun manualReview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "原子ID", required = true)
        @PathParam("elementId")
        elementId: String,
        @Parameter(description = "审核信息", required = true)
        params: ReviewParam
    ): Result<Boolean>

    @Operation(summary = "人工审核(new)")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/toReview")
    fun goToReview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "步骤Id", required = true)
        @PathParam("elementId")
        elementId: String
    ): Result<ReviewParam>

    @Operation(summary = "触发审核")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/review")
    fun buildTriggerReview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否通过审核", required = true)
        @QueryParam("approve")
        approve: Boolean,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @Operation(summary = "手动触发启动阶段")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stages/{stageId}/manualStart")
    fun manualStartStage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "阶段ID", required = true)
        @PathParam("stageId")
        stageId: String,
        @Parameter(description = "取消执行", required = false)
        @QueryParam("cancel")
        cancel: Boolean?,
        @Parameter(description = "审核请求体", required = false)
        reviewRequest: StageReviewRequest? = null
    ): Result<Boolean>

    @Operation(summary = "获取流水线构建历史-new")
    @GET
    @Path("/{projectId}/{pipelineId}/history/new")
    fun getHistoryBuildNew(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "代码库别名", required = false)
        @QueryParam("materialAlias")
        materialAlias: List<String>?,
        @Parameter(description = "代码库URL", required = false)
        @QueryParam("materialUrl")
        materialUrl: String?,
        @Parameter(description = "分支", required = false)
        @QueryParam("materialBranch")
        materialBranch: List<String>?,
        @Parameter(description = "commitId", required = false)
        @QueryParam("materialCommitId")
        materialCommitId: String?,
        @Parameter(description = "commitMessage", required = false)
        @QueryParam("materialCommitMessage")
        materialCommitMessage: String?,
        @Parameter(description = "状态", required = false)
        @QueryParam("status")
        status: List<BuildStatus>?,
        @Parameter(description = "触发方式", required = false)
        @QueryParam("trigger")
        trigger: List<StartType>?,
        @Parameter(description = "排队于-开始时间(时间戳形式)", required = false)
        @QueryParam("queueTimeStartTime")
        queueTimeStartTime: Long?,
        @Parameter(description = "排队于-结束时间(时间戳形式)", required = false)
        @QueryParam("queueTimeEndTime")
        queueTimeEndTime: Long?,
        @Parameter(description = "开始于-开始时间(时间戳形式)", required = false)
        @QueryParam("startTimeStartTime")
        startTimeStartTime: Long?,
        @Parameter(description = "开始于-结束时间(时间戳形式)", required = false)
        @QueryParam("startTimeEndTime")
        startTimeEndTime: Long?,
        @Parameter(description = "结束于-开始时间(时间戳形式)", required = false)
        @QueryParam("endTimeStartTime")
        endTimeStartTime: Long?,
        @Parameter(description = "结束于-结束时间(时间戳形式)", required = false)
        @QueryParam("endTimeEndTime")
        endTimeEndTime: Long?,
        @Parameter(description = "耗时最小值", required = false)
        @QueryParam("totalTimeMin")
        totalTimeMin: Long?,
        @Parameter(description = "耗时最大值", required = false)
        @QueryParam("totalTimeMax")
        totalTimeMax: Long?,
        @Parameter(description = "备注", required = false)
        @QueryParam("remark")
        remark: String?,
        @Parameter(description = "构件号起始", required = false)
        @QueryParam("buildNoStart")
        buildNoStart: Int?,
        @Parameter(description = "构件号结束", required = false)
        @QueryParam("buildNoEnd")
        buildNoEnd: Int?,
        @Parameter(description = "构建信息", required = false)
        @QueryParam("buildMsg")
        buildMsg: String?,
        @Parameter(description = "指定调试数据", required = false)
        @QueryParam("debug")
        debug: Boolean? = null,
        @Parameter(description = "触发代码库", required = false)
        @QueryParam("triggerAlias")
        triggerAlias: List<String>?,
        @Parameter(description = "触发分支", required = false)
        @QueryParam("triggerBranch")
        triggerBranch: List<String>?,
        @Parameter(description = "触发人", required = false)
        @QueryParam("triggerUser")
        triggerUser: List<String>?
    ): Result<BuildHistoryPage<BuildHistory>>

    @Operation(summary = "获取流水线构建参数")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/parameters")
    fun getBuildParameters(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<List<BuildParameters>>
}
