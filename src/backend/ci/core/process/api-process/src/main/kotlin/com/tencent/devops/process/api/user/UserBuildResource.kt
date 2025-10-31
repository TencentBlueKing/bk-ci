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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.process.enums.HistorySearchType
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildStageProgressInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.BuildRecordInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "USER_BUILD", description = "用户-构建资源")
@Path("/user/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserBuildResource {

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

    @Operation(summary = "获取流水线构建参数")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/parameters")
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

    @Operation(summary = "手动启动流水线")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/start")
    @Path("/{projectId}/{pipelineId}")
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
        @Parameter(description = "手动指定构建版本参数", required = false)
        @QueryParam("buildNo")
        buildNo: Int? = null,
        @Parameter(description = "触发审核人列表", required = false)
        @QueryParam("triggerReviewers")
        triggerReviewers: List<String>? = null,
        @Parameter(description = "指定草稿版本（为调试构建）", required = false)
        @QueryParam("version")
        version: Int? = null
    ): Result<BuildId>

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
        @Parameter(description = "要重试或跳过的插件ID，或者StageId", required = false)
        @QueryParam("taskId")
        taskId: String? = null,
        @Parameter(description = "仅重试所有失败Job", required = false)
        @QueryParam("failedContainer")
        failedContainer: Boolean? = false,
        @Parameter(description = "跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）", required = false)
        @QueryParam("skip")
        skipFailedTask: Boolean? = false
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

    @Operation(summary = "人工审核")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/elements/{elementId}/review")
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
        @Parameter(description = "步骤Id", required = true)
        @PathParam("elementId")
        elementId: String,
        @Parameter(description = "审核信息", required = true)
        params: ReviewParam
    ): Result<Boolean>

    @Operation(summary = "人工审核(new)")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/elements/{elementId}/toReview")
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

    @Operation(summary = "获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    @Path("/{projectId}/{pipelineId}/{buildId}/detail")
    fun getBuildDetail(
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
    ): Result<ModelDetail>

    @Operation(summary = "根据执行次数获取构建详情")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/record")
    fun getBuildRecordByExecuteCount(
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
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<ModelRecord>

    @Operation(summary = "获取构建多次执行信息")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/recordInfo")
    fun getBuildRecordInfo(
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
    ): Result<List<BuildRecordInfo>>

    @Operation(summary = "获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/buildNo/{buildNo}/detail")
    @Path("/{projectId}/{pipelineId}/detail/{buildNo}")
    fun getBuildDetailByBuildNo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建BuildNo", required = true)
        @PathParam("buildNo")
        buildNo: Int,
        @Parameter(description = "查看指定版本调试数据", required = false, example = "false")
        @QueryParam("version")
        debugVersion: Int? = null
    ): Result<ModelDetail>

    @Operation(summary = "获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/buildNo/{buildNo}/detail")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/record/{buildNum}")
    fun getBuildRecordByBuildNum(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建序号（buildNum）", required = true)
        @PathParam("buildNum")
        buildNum: Int,
        @Parameter(description = "查看指定版本调试数据", required = false, example = "false")
        @QueryParam("version")
        debugVersion: Int? = null,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<ModelRecord>

    @Operation(summary = "获取已完成的最新构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/latestFinished")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/latestFinished")
    fun goToLatestFinishedBuild(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Response

    @Operation(summary = "获取流水线构建历史")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/history")
    @Path("/{projectId}/{pipelineId}/history")
    fun getHistoryBuild(
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
        @Parameter(description = "是否检测权限", required = false, example = "true")
        @QueryParam("checkPermission")
        checkPermission: Boolean?,
        @Parameter(description = "查看指定版本调试数据", required = false, example = "false")
        @QueryParam("version")
        debugVersion: Int? = null
    ): Result<BuildHistoryPage<BuildHistory>>

    @Operation(summary = "获取流水线构建历史-new")
    @GET
    // @Path("/{projectId}/{pipelineId}/history/new")
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
        @Parameter(description = "源材料代码库别名", required = false)
        @QueryParam("materialAlias")
        materialAlias: List<String>?,
        @Parameter(description = "代码库URL", required = false)
        @QueryParam("materialUrl")
        materialUrl: String?,
        @Parameter(description = "源材料分支", required = false)
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
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false,
        @Parameter(description = "指定调试数据", required = false)
        @QueryParam("debug")
        debug: Boolean? = null,
        @Parameter(description = "触发代码库", required = false)
        @QueryParam("triggerAlias")
        triggerAlias: List<String>?,
        @Parameter(description = "触发分支", required = false)
        @QueryParam("triggerBranch")
        triggerBranch: List<String>?,
        @Parameter(description = "触发方式", required = false)
        @QueryParam("triggerUser")
        triggerUser: List<String>?
    ): Result<BuildHistoryPage<BuildHistory>>

    @Operation(summary = "修改流水线备注")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/updateRemark")
    @Path("/{projectId}/{pipelineId}/{buildId}/updateRemark")
    fun updateBuildHistoryRemark(
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
        @Parameter(description = "备注信息", required = true)
        remark: BuildHistoryRemark?
    ): Result<Boolean>

    @Operation(summary = "获取流水线构建中的查询条件-状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionStatus")
    @Path("/{projectId}/{pipelineId}/historyCondition/status")
    fun getHistoryConditionStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<IdValue>>

    @Operation(summary = "获取流水线构建中的查询条件-触发方式")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionTrigger")
    @Path("/{projectId}/{pipelineId}/historyCondition/trigger")
    fun getHistoryConditionTrigger(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<IdValue>>

    @Operation(summary = "获取流水线构建中的查询条件-代码库")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionRepo")
    @Path("/{projectId}/{pipelineId}/historyCondition/repo")
    fun getHistoryConditionRepo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "查看指定版本调试数据", required = false, example = "false")
        @QueryParam("version")
        debugVersion: Int? = null,
        @Parameter(description = "搜索分支关键字", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "搜索类型, 触发/源材料", required = false)
        @QueryParam("type")
        type: HistorySearchType?
    ): Result<List<String>>

    @Operation(summary = "获取流水线构建中的查询条件-分支")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionBranchName")
    @Path("/{projectId}/{pipelineId}/historyCondition/branchName")
    fun getHistoryConditionBranch(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "仓库", required = false)
        @QueryParam("alias")
        alias: List<String>?,
        @Parameter(description = "查看指定版本调试数据", required = false, example = "false")
        @QueryParam("debugVersion")
        debugVersion: Int? = null,
        @Parameter(description = "搜索分支关键字", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "搜索类型,触发/源材料", required = false)
        @QueryParam("type")
        type: HistorySearchType?
    ): Result<List<String>>

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
        approve: Boolean
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

    @Operation(summary = "操作暂停插件")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/taskIds/{taskId}/execution/pause")
    fun executionPauseAtom(
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
        @Parameter(description = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @Parameter(description = "待执行插件元素", required = false)
        element: Element?,
        @Parameter(description = "执行类型, true 继续, false 停止", required = true)
        @QueryParam("isContinue")
        isContinue: Boolean,
        @Parameter(description = "stageId", required = true)
        @QueryParam("stageId")
        stageId: String,
        @Parameter(description = "containerId", required = true)
        @QueryParam("containerId")
        containerId: String
    ): Result<Boolean>

    @Operation(summary = "尝试将异常导致流水线中断的继续运转下去（结果可能是：失败结束 or 继续运行）")
    @PUT
    @Path("/{projectId}/{pipelineId}/try_fix_stuck_builds")
    fun tryFinishStuckBuilds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128, required = true)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 64, required = true)
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        @BkField(minLength = 32, maxLength = 34, required = true)
        pipelineId: String,
        @Parameter(description = "要操作的构建ID列表[最大50个]", required = true)
        @BkField(required = true)
        buildIds: Set<String>
    ): Result<Boolean>

    @Operation(summary = "获取阶段执行进度")
    @GET
    @Path("/{projectId}/{pipelineId}/getStageProgressRate/")
    fun getStageProgressRate(
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
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "阶段ID", required = true)
        @QueryParam("stageId")
        stageId: String
    ): Result<BuildStageProgressInfo>

    @Operation(summary = "回放指定构建任务的触发事件")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/replayByBuild")
    fun replayByBuild(
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
        @Parameter(description = "强制触发", required = false)
        @QueryParam("forceTrigger")
        forceTrigger: Boolean? = false
    ): Result<BuildId>
}
