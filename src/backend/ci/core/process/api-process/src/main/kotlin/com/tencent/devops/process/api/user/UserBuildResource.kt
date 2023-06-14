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
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["USER_BUILD"], description = "用户-构建资源")
@Path("/user/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserBuildResource {

    @ApiOperation("获取流水线手动启动参数")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/manualStartupInfo")
    @Path("/{projectId}/{pipelineId}/manualStartupInfo")
    fun manualStartupInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<BuildManualStartupInfo>

    @ApiOperation("获取流水线构建参数")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/parameters")
    @Path("/{projectId}/{pipelineId}/{buildId}/parameters")
    fun getBuildParameters(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<List<BuildParameters>>

    @ApiOperation("手动启动流水线")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/start")
    @Path("/{projectId}/{pipelineId}")
    fun manualStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>,
        @ApiParam("手动指定构建版本参数", required = false)
        @QueryParam("buildNo")
        buildNo: Int? = null,
        @ApiParam("触发审核人列表", required = false)
        @QueryParam("triggerReviewers")
        triggerReviewers: List<String>? = null
    ): Result<BuildId>

    @ApiOperation("重试流水线-重试或者跳过失败插件")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/retry")
    @Path("/{projectId}/{pipelineId}/{buildId}/retry")
    fun retry(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
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
        skipFailedTask: Boolean? = false
    ): Result<BuildId>

    @ApiOperation("手动停止流水线")
    @DELETE
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stop")
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun manualShutdown(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>

    @ApiOperation("人工审核")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/elements/{elementId}/review")
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/review")
    fun manualReview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("步骤Id", required = true)
        @PathParam("elementId")
        elementId: String,
        @ApiParam("审核信息", required = true)
        params: ReviewParam
    ): Result<Boolean>

    @ApiOperation("人工审核(new)")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/elements/{elementId}/toReview")
    @Path("/{projectId}/{pipelineId}/{buildId}/{elementId}/toReview")
    fun goToReview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("步骤Id", required = true)
        @PathParam("elementId")
        elementId: String
    ): Result<ReviewParam>

    @ApiOperation("获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    @Path("/{projectId}/{pipelineId}/{buildId}/detail")
    fun getBuildDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<ModelDetail>

    @ApiOperation("根据执行次数获取构建详情")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/record")
    fun getBuildRecordByExecuteCount(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<ModelRecord>

    @ApiOperation("获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/buildNo/{buildNo}/detail")
    @Path("/{projectId}/{pipelineId}/detail/{buildNo}")
    fun getBuildDetailByBuildNo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建BuildNo", required = true)
        @PathParam("buildNo")
        buildNo: Int
    ): Result<ModelDetail>

    @ApiOperation("获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/buildNo/{buildNo}/detail")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/record/{buildNum}")
    fun getBuildRecordByBuildNum(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建序号（buildNum）", required = true)
        @PathParam("buildNum")
        buildNum: Int
    ): Result<ModelRecord>

    @ApiOperation("获取已完成的最新构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/latestFinished")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/latestFinished")
    fun goToLatestFinishedBuild(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Response

    @ApiOperation("获取流水线构建历史")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/history")
    @Path("/{projectId}/{pipelineId}/history")
    fun getHistoryBuild(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("是否检测权限", required = false, defaultValue = "true")
        @QueryParam("checkPermission")
        checkPermission: Boolean?
    ): Result<BuildHistoryPage<BuildHistory>>

    @ApiOperation("获取流水线构建历史-new")
    @GET
    // @Path("/{projectId}/{pipelineId}/history/new")
    @Path("/{projectId}/{pipelineId}/history/new")
    fun getHistoryBuildNew(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("代码库别名", required = false)
        @QueryParam("materialAlias")
        materialAlias: List<String>?,
        @ApiParam("代码库URL", required = false)
        @QueryParam("materialUrl")
        materialUrl: String?,
        @ApiParam("分支", required = false)
        @QueryParam("materialBranch")
        materialBranch: List<String>?,
        @ApiParam("commitId", required = false)
        @QueryParam("materialCommitId")
        materialCommitId: String?,
        @ApiParam("commitMessage", required = false)
        @QueryParam("materialCommitMessage")
        materialCommitMessage: String?,
        @ApiParam("状态", required = false)
        @QueryParam("status")
        status: List<BuildStatus>?,
        @ApiParam("触发方式", required = false)
        @QueryParam("trigger")
        trigger: List<StartType>?,
        @ApiParam("排队于-开始时间(时间戳形式)", required = false)
        @QueryParam("queueTimeStartTime")
        queueTimeStartTime: Long?,
        @ApiParam("排队于-结束时间(时间戳形式)", required = false)
        @QueryParam("queueTimeEndTime")
        queueTimeEndTime: Long?,
        @ApiParam("开始于-开始时间(时间戳形式)", required = false)
        @QueryParam("startTimeStartTime")
        startTimeStartTime: Long?,
        @ApiParam("开始于-结束时间(时间戳形式)", required = false)
        @QueryParam("startTimeEndTime")
        startTimeEndTime: Long?,
        @ApiParam("结束于-开始时间(时间戳形式)", required = false)
        @QueryParam("endTimeStartTime")
        endTimeStartTime: Long?,
        @ApiParam("结束于-结束时间(时间戳形式)", required = false)
        @QueryParam("endTimeEndTime")
        endTimeEndTime: Long?,
        @ApiParam("耗时最小值", required = false)
        @QueryParam("totalTimeMin")
        totalTimeMin: Long?,
        @ApiParam("耗时最大值", required = false)
        @QueryParam("totalTimeMax")
        totalTimeMax: Long?,
        @ApiParam("备注", required = false)
        @QueryParam("remark")
        remark: String?,
        @ApiParam("构件号起始", required = false)
        @QueryParam("buildNoStart")
        buildNoStart: Int?,
        @ApiParam("构件号结束", required = false)
        @QueryParam("buildNoEnd")
        buildNoEnd: Int?,
        @ApiParam("构建信息", required = false)
        @QueryParam("buildMsg")
        buildMsg: String?
    ): Result<BuildHistoryPage<BuildHistory>>

    @ApiOperation("修改流水线备注")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/updateRemark")
    @Path("/{projectId}/{pipelineId}/{buildId}/updateRemark")
    fun updateBuildHistoryRemark(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("备注信息", required = true)
        remark: BuildHistoryRemark?
    ): Result<Boolean>

    @ApiOperation("获取流水线构建中的查询条件-状态")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionStatus")
    @Path("/{projectId}/{pipelineId}/historyCondition/status")
    fun getHistoryConditionStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<IdValue>>

    @ApiOperation("获取流水线构建中的查询条件-触发方式")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionTrigger")
    @Path("/{projectId}/{pipelineId}/historyCondition/trigger")
    fun getHistoryConditionTrigger(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<IdValue>>

    @ApiOperation("获取流水线构建中的查询条件-代码库")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionRepo")
    @Path("/{projectId}/{pipelineId}/historyCondition/repo")
    fun getHistoryConditionRepo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<String>>

    @ApiOperation("获取流水线构建中的查询条件-分支")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/historyConditionBranchName")
    @Path("/{projectId}/{pipelineId}/historyCondition/branchName")
    fun getHistoryConditionBranch(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("仓库", required = false)
        @QueryParam("alias")
        alias: List<String>?
    ): Result<List<String>>

    @ApiOperation("触发审核")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/review")
    fun buildTriggerReview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否通过审核", required = true)
        @QueryParam("approve")
        approve: Boolean
    ): Result<Boolean>

    @ApiOperation("手动触发启动阶段")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stages/{stageId}/manualStart")
    fun manualStartStage(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
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
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/taskIds/{taskId}/execution/pause")
    fun executionPauseAtom(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("任务ID", required = true)
        @PathParam("taskId")
        taskId: String,
        @ApiParam("待执行插件元素", required = false)
        element: Element?,
        @ApiParam("执行类型, true 继续, false 停止", required = true)
        @QueryParam("isContinue")
        isContinue: Boolean,
        @ApiParam("stageId", required = true)
        @QueryParam("stageId")
        stageId: String,
        @ApiParam("containerId", required = true)
        @QueryParam("containerId")
        containerId: String
    ): Result<Boolean>

    @ApiOperation("尝试将异常导致流水线中断的继续运转下去（结果可能是：失败结束 or 继续运行）")
    @PUT
    @Path("/{projectId}/{pipelineId}/try_fix_stuck_builds")
    fun tryFinishStuckBuilds(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(minLength = 1, maxLength = 128, required = true)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        @BkField(minLength = 1, maxLength = 64, required = true)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        @BkField(minLength = 32, maxLength = 34, required = true)
        pipelineId: String,
        @ApiParam("要操作的构建ID列表[最大50个]", required = true)
        @BkField(required = true)
        buildIds: Set<String>
    ): Result<Boolean>
}
