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
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildHistoryVariables
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildTaskPauseInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
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

@Api(tags = ["SERVICE_BUILD"], description = "服务-构建资源")
@Path("/service/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ServiceBuildResource {
    @ApiOperation("通过buildId获取流水线pipelineId")
    @GET
    @Path("/{projectId}/get_pipeline_id_from_build_id")
    fun getPipelineIdFromBuildId(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @QueryParam("buildId")
        buildId: String
    ): Result<String>

    @ApiOperation("通过buildNumber 和 pipelineId 获取流水线buildId")
    @GET
    @Path("/{projectId}/get_build_id_from_build_number")
    fun getBuildIdFromBuildNumber(
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建号", required = true)
        @QueryParam("buildNumber")
        buildNumber: Int
    ): Result<String>

    @ApiOperation("Notify process that the vm startup for the build")
    @PUT
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/vmStatus")
    @Path("/{projectId}/{pipelineId}/{buildId}/vmStatus")
    fun setVMStatus(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("VM SEQ ID", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("status", required = true)
        @QueryParam("status")
        status: BuildStatus,
        @ApiParam("错误类型", required = false)
        @QueryParam("errorType")
        errorType: ErrorType? = null,
        @ApiParam("错误码", required = false)
        @QueryParam("errorCode")
        errorCode: Int? = null,
        @ApiParam("错误信息", required = false)
        @QueryParam("errorMsg")
        errorMsg: String? = null
    ): Result<Boolean>

    @ApiOperation("根据构建ID获取项目ID以及流水线ID")
    @GET
    // @Path("/builds/{buildId}/basic")
    @Path("/{buildId}/basic")
    fun serviceBasic(
        @ApiParam(value = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<BuildBasicInfo>

    @ApiOperation("根据构建ID获取项目ID以及流水线ID")
    @POST
    // @Path("/batchBasic")
    @Path("/batchBasic")
    fun batchServiceBasic(
        @ApiParam("构建ID", required = true)
        buildIds: Set<String>
    ): Result<Map<String, BuildBasicInfo>>

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
        pipelineId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<BuildManualStartupInfo>

    @ApiOperation("搜索流水线参数")
    @POST
    @Path("/{projectId}/{pipelineId}/manualSearchOptions")
    fun manualSearchOptions(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("搜索参数", required = false)
        @QueryParam("search")
        search: String? = null,
        @ApiParam("流水线参数", required = false)
        buildFormProperty: BuildFormProperty
    ): Result<List<BuildFormValue>>

    @Deprecated(message = "do not use", replaceWith = ReplaceWith("@see ServiceBuildResource.manualStartupNew"))
    @ApiOperation("手动启动流水线")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/start")
    @Path("/{projectId}/{pipelineId}/")
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
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("手动指定构建版本参数", required = false)
        @QueryParam("buildNo")
        buildNo: Int? = null
    ): Result<BuildId>

    @ApiOperation("重试流水线-重试或者跳过失败插件")
    @POST
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
        skipFailedTask: Boolean? = false,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("是否忽略人工触发", required = false)
        @QueryParam("checkManualStartup")
        checkManualStartup: Boolean? = false
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
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

    @ApiOperation("系统异常导致停止流水线")
    @DELETE
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/shutdown")
    @Path("/shutdown/{projectId}/{pipelineId}/{buildId}/")
    fun serviceShutdown(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
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
        params: ReviewParam,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

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
        approve: Boolean,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<Boolean>

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
        buildId: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
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
        executeCount: Int?,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<ModelRecord>

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
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam(
            value = "利用updateTime进行排序，True为降序，False为升序，null时以Build number 降序",
            required = false, defaultValue = "20"
        )
        @QueryParam("updateTimeDesc")
        updateTimeDesc: Boolean? = null,
        @ApiParam("代码库别名", required = false)
        @QueryParam("materialAlias")
        materialAlias: List<String>? = null,
        @ApiParam("代码库URL", required = false)
        @QueryParam("materialUrl")
        materialUrl: String? = null,
        @ApiParam("分支", required = false)
        @QueryParam("materialBranch")
        materialBranch: List<String>? = null,
        @ApiParam("commitId", required = false)
        @QueryParam("materialCommitId")
        materialCommitId: String? = null,
        @ApiParam("commitMessage", required = false)
        @QueryParam("materialCommitMessage")
        materialCommitMessage: String? = null,
        @ApiParam("状态", required = false)
        @QueryParam("status")
        status: List<BuildStatus>? = null,
        @ApiParam("触发方式", required = false)
        @QueryParam("trigger")
        trigger: List<StartType>? = null,
        @ApiParam("排队于-开始时间(时间戳形式)", required = false)
        @QueryParam("queueTimeStartTime")
        queueTimeStartTime: Long? = null,
        @ApiParam("排队于-结束时间(时间戳形式)", required = false)
        @QueryParam("queueTimeEndTime")
        queueTimeEndTime: Long? = null,
        @ApiParam("开始于-开始时间(时间戳形式)", required = false)
        @QueryParam("startTimeStartTime")
        startTimeStartTime: Long? = null,
        @ApiParam("开始于-结束时间(时间戳形式)", required = false)
        @QueryParam("startTimeEndTime")
        startTimeEndTime: Long? = null,
        @ApiParam("结束于-开始时间(时间戳形式)", required = false)
        @QueryParam("endTimeStartTime")
        endTimeStartTime: Long? = null,
        @ApiParam("结束于-结束时间(时间戳形式)", required = false)
        @QueryParam("endTimeEndTime")
        endTimeEndTime: Long? = null,
        @ApiParam("耗时最小值", required = false)
        @QueryParam("totalTimeMin")
        totalTimeMin: Long? = null,
        @ApiParam("耗时最大值", required = false)
        @QueryParam("totalTimeMax")
        totalTimeMax: Long? = null,
        @ApiParam("备注", required = false)
        @QueryParam("remark")
        remark: String? = null,
        @ApiParam("构件号起始", required = false)
        @QueryParam("buildNoStart")
        buildNoStart: Int? = null,
        @ApiParam("构件号结束", required = false)
        @QueryParam("buildNoEnd")
        buildNoEnd: Int? = null,
        @ApiParam("构建信息", required = false)
        @QueryParam("buildMsg")
        buildMsg: String? = null,
        @ApiParam("触发人", required = false)
        @QueryParam("startUser")
        startUser: List<String>? = null
    ): Result<BuildHistoryPage<BuildHistory>>

    @ApiOperation("获取构建详情")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/status")
    @Path("/{projectId}/{pipelineId}/{buildId}/status")
    fun getBuildStatus(
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
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<BuildHistoryWithVars>

    @ApiOperation("获取构建详情（平台调用，不鉴权）")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/nopermission/status")
    fun getBuildStatusWithoutPermission(
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
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<BuildHistoryWithVars>

    @ApiOperation("获取构建详情的状态（平台调用，不鉴权）")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/nopermission/detail_status")
    fun getBuildDetailStatusWithoutPermission(
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
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<String>

    @ApiOperation("获取构建全部变量")
    @GET
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/vars")
    @Path("/{projectId}/{pipelineId}/{buildId}/vars")
    fun getBuildVars(
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
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode = ChannelCode.BS
    ): Result<BuildHistoryVariables>

    @ApiOperation("获取构建中的变量值")
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/variables")
    fun getBuildVariableValue(
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
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode = ChannelCode.BS,
        @ApiParam("变量名列表", required = true)
        variableNames: List<String>
    ): Result<Map<String, String>>

    @ApiOperation("批量获取构建详情")
    @POST
    // @Path("/projects/{projectId}/batchStatus")
    @Path("/{projectId}/batchStatus")
    fun getBatchBuildStatus(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("构建ID", required = true)
        buildId: Set<String>,
        @ApiParam("渠道号，默认为BS", required = true)
        @QueryParam("channelCode")
        channelCode: ChannelCode = ChannelCode.BS,
        @QueryParam("startBeginTime")
        startBeginTime: String? = null,
        @QueryParam("endBeginTime")
        endBeginTime: String? = null
    ): Result<List<BuildHistory>>

    @ApiOperation("获取流水线构建历史, 返回buildid")
    @GET
    @Path("/{projectId}/batch_get_builds")
    fun getBuilds(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("状态id", required = false)
        @QueryParam("buildStatus")
        buildStatus: Set<BuildStatus>? = null,
        @QueryParam("channelCode")
        channelCode: ChannelCode = ChannelCode.BS
    ): Result<List<String>>

    @ApiOperation("根据流水线id获取最新执行信息")
    @POST
    // @Path("/projects/{projectId}/getPipelineLatestBuild")
    @Path("/{projectId}/getPipelineLatestBuild")
    fun getPipelineLatestBuildByIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id列表", required = true)
        pipelineIds: List<String>
    ): Result<Map<String, PipelineLatestBuild>>

    @ApiOperation("第三方构建机Agent构建结束")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/seqs/{vmSeqId}/workerBuildFinish")
    @Path("/{projectId}/{pipelineId}/{buildId}/{vmSeqId}/workerBuildFinish")
    fun workerBuildFinish(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("Container序列号", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("构建机节点ID（不是AgentID)", required = true)
        @QueryParam("nodeHashId")
        nodeHashId: String? = null,
        @ApiParam("结果状态", required = true)
        simpleResult: SimpleResult
    ): Result<Boolean>

    @ApiOperation("保存构建详情")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/seqs/{vmSeqId}/saveBuildVmInfo")
    @Path("/{projectId}/{pipelineId}/{buildId}/{vmSeqId}/saveBuildVmInfo")
    fun saveBuildVmInfo(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("构建 VM ID", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("参数", required = true)
        vmInfo: VmInfo
    ): Result<Boolean>

    @ApiOperation("获取流水线构建单条历史")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildNum}/history")
    fun getSingleHistoryBuild(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线buildNum", required = true)
        @PathParam("buildNum")
        buildNum: String,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<BuildHistory?>

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

    @ApiOperation("质量红线忽略触发启动阶段")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stages/{stageId}/qualityTrigger")
    fun qualityTriggerStage(
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
        @ApiParam("审核请求体", required = true)
        qualityRequest: StageQualityRequest
    ): Result<Boolean>

    @ApiOperation("操作暂停插件")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/execution/pause")
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
        taskPauseExecute: BuildTaskPauseInfo
    ): Result<Boolean>

    @ApiOperation("手动启动流水线")
    @POST
    @Path("/{pipelineId}/")
    fun manualStartupNew(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("启动参数", required = true)
        values: Map<String, String>,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode,
        @ApiParam("手动指定构建版本参数", required = false)
        @QueryParam("buildNo")
        buildNo: Int? = null,
        @ApiParam("启动类型", required = false)
        @QueryParam("startType")
        startType: StartType
    ): Result<BuildId>

    @ApiOperation("取消并发起新构建")
    @POST
    @Path("projects/{projectId}/pipelines/{pipelineId}/buildIds/{buildId}/build/restart")
    fun buildRestart(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @ApiParam("项目ID", required = true)
        @BkField(required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        @BkField(required = true)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        @BkField(required = true)
        buildId: String
    ): Result<String>

    @ApiOperation("修改某次构建的备注")
    @POST
    @Path("projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/updateRemark")
    fun updateRemark(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @ApiParam("项目ID", required = true)
        @BkField(required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        @BkField(required = true)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        @BkField(required = true)
        buildId: String,
        @ApiParam("备注信息", required = true)
        remark: BuildHistoryRemark?
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
