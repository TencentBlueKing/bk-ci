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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryRemark
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.BuildTaskPauseInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_BUILD_V4", description = "OPENAPI-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwBuildResourceV4 {

    @Operation(summary = "启动构建", tags = ["v4_app_build_start", "v4_user_build_start"])
    @POST
    @Path("/build_start")
    fun start(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(
            description = "启动参数：map<变量名(string),变量值(string)>", required = false,
            examples = [
                ExampleObject(
                    description = "当需要指定启动时流水线变量 var1 为 foobar 时",
                    value = "{\"var1\": \"foobar\"}"
                ),
                ExampleObject(description = "若流水线没有设置输入变量，则填空", value = "{}"),
                ExampleObject(
                    description = "如需指定自定义触发材料时, 需传入特定参数, " +
                            "详情请查看: https://github.com/TencentBlueKing/bk-ci/issues/10302",
                    value = "{" +
                                "\"BK_CI_MATERIAL_ID\": \"触发材料ID\"," +
                                "\"BK_CI_MATERIAL_NAME\": \"触发材料名称\"," +
                                "\"BK_CI_MATERIAL_URL\": \"触发材料链接\"" +
                            "}"
                )
            ]
        )
        values: Map<String, String>?,
        @Parameter(description = "手动指定构建版本参数", required = false)
        @QueryParam("buildNo")
        buildNo: Int? = null
    ): Result<BuildId>

    @Operation(summary = "停止构建", tags = ["v4_app_build_stop", "v4_user_build_stop"])
    @POST
    @Path("/build_stop")
    fun stop(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String
    ): Result<Boolean>

    @Operation(summary = "重试构建-重试或者跳过失败插件", tags = ["v4_app_build_retry", "v4_user_build_retry"])
    @POST
    @Path("/build_retry")
    fun retry(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID(构建ID和构建号，二选其一填入)", required = false)
        @QueryParam("buildId")
        buildId: String?,
        @Parameter(description = "构建号(构建ID和构建号，二选其一填入)", required = false)
        @QueryParam("buildNumber")
        buildNumber: Int?,
        @Parameter(description = "要重试或跳过的插件ID，或者StageId, 或stepId", required = false)
        @QueryParam("taskId")
        taskId: String? = null,
        @Parameter(description = "仅重试所有失败Job", required = false)
        @QueryParam("failedContainer")
        failedContainer: Boolean? = false,
        @Parameter(
            description = "跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）",
            required = false
        )
        @QueryParam("skip")
        skipFailedTask: Boolean? = false
    ): Result<BuildId>

    @Operation(
        summary = "查看构建状态信息,#4295增加stageStatus等",
        tags = ["v4_app_build_status", "v4_user_build_status"]
    )
    @GET
    @Path("/build_status")
    fun getStatus(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String
    ): Result<BuildHistoryWithVars>

    @Operation(
        summary = "批量获取构建详情",
        tags = ["v4_app_batch_get_build_status", "v4_user_batch_get_build_status"]
    )
    @POST
    @Path("/batch_get_build_status")
    fun batchGetBuildStatus(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String,
        @QueryParam("构建开始时间")
        startBeginTime: String? = null,
        @QueryParam("构建结束时间")
        endBeginTime: String? = null,
        @Parameter(description = "构建ID列表,最大不能超过100个", required = true)
        buildIdSet: Set<String>,
    ): Result<List<BuildHistory>>

    @Operation(summary = "获取流水线构建历史", tags = ["v4_user_build_list", "v4_app_build_list"])
    @GET
    @Path("/build_histories")
    fun getHistoryBuild(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(
            description = "利用updateTime进行排序，True为降序，False为升序，null时以Build number 降序",
            required = false, example = "null"
        )
        @QueryParam("updateTimeDesc")
        updateTimeDesc: Boolean? = null,
        @Parameter(description = "源材料代码库别名", required = false)
        @QueryParam("materialAlias")
        materialAlias: List<String>?,
        @Parameter(description = "代码库URL", required = false)
        @QueryParam("materialUrl")
        materialUrl: String?,
        @Parameter(description = "源材料分支", required = false)
        @QueryParam("materialBranch")
        materialBranch: List<String>?,
        @Parameter(description = "源材料commitId", required = false)
        @QueryParam("materialCommitId")
        materialCommitId: String?,
        @Parameter(description = "源材料commitMessage", required = false)
        @QueryParam("materialCommitMessage")
        materialCommitMessage: String?,
        @Parameter(description = "状态", required = false)
        @QueryParam("status")
        status: List<BuildStatus>?,
        @Parameter(description = "触发方式", required = false)
        @QueryParam("trigger")
        trigger: List<StartType>?,
        @Parameter(description = "排队于-开始时间(时间戳毫秒级别，13位数字)", required = false)
        @QueryParam("queueTimeStartTime")
        queueTimeStartTime: Long?,
        @Parameter(description = "排队于-结束时间(时间戳毫秒级别，13位数字)", required = false)
        @QueryParam("queueTimeEndTime")
        queueTimeEndTime: Long?,
        @Parameter(description = "开始于-流水线的执行开始时间(时间戳毫秒级别，13位数字)", required = false)
        @QueryParam("startTimeStartTime")
        startTimeStartTime: Long?,
        @Parameter(description = "开始于-流水线的执行结束时间(时间戳毫秒级别，13位数字)", required = false)
        @QueryParam("startTimeEndTime")
        startTimeEndTime: Long?,
        @Parameter(description = "结束于-流水线的执行开始时间(时间戳毫秒级别，13位数字)", required = false)
        @QueryParam("endTimeStartTime")
        endTimeStartTime: Long?,
        @Parameter(description = "结束于-流水线的执行结束时间(时间戳毫秒级别，13位数字)", required = false)
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
        @Parameter(description = "执行人", required = false)
        @QueryParam("startUser")
        startUser: List<String>?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false,
        @Parameter(description = "触发代码库", required = false)
        @QueryParam("triggerAlias")
        triggerAlias: List<String>? = null,
        @Parameter(description = "触发分支", required = false)
        @QueryParam("triggerBranch")
        triggerBranch: List<String>? = null
    ): Result<BuildHistoryPage<BuildHistory>>

    @Operation(summary = "获取流水线手动启动参数", tags = ["v4_app_build_startInfo", "v4_user_build_startInfo"])
    @GET
    @Path("/build_manual_startup_info")
    fun manualStartupInfo(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "指定草稿版本（为调试构建）", required = false)
        @QueryParam("version")
        debugVersion: Int?
    ): Result<BuildManualStartupInfo>

    @Operation(summary = "构建详情", tags = ["v4_app_build_detail", "v4_user_build_detail"])
    @GET
    @Path("/build_detail")
    fun detail(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<ModelRecord>

    @Operation(summary = "手动审核启动阶段", tags = ["v4_app_build_stage_start", "v4_user_build_stage_start"])
    @POST
    @Path("/manual_start_build_stage")
    fun manualStartStage(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "阶段ID", required = true)
        @QueryParam("stageId")
        stageId: String,
        @Parameter(description = "取消执行", required = false)
        @QueryParam("cancel")
        cancel: Boolean?,
        @Parameter(description = "审核请求体", required = false)
        reviewRequest: StageReviewRequest? = null
    ): Result<Boolean>

    @Operation(
        summary = "获取构建中的变量值(注意：变量具有时效性，只能获取最近一个月的任务数据)",
        tags = ["v4_app_build_variables_value", "v4_user_build_variables_value"]
    )
    @POST
    @Path("/build_variables")
    fun getVariableValue(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(
            description = "变量名列表", required = true,
            examples = [ExampleObject(
                description = "以数组形式把需要获取的变量key传进来，比如获取variable1变量",
                value = """
                            ["variable1"]
                                """
            )]
        )
        variableNames: List<String>
    ): Result<Map<String, String>>

    @Operation(summary = "操作暂停插件", tags = ["v4_app_pause_build_execute", "v4_user_pause_build_execute"])
    @POST
    @Path("/build_execute_pause")
    fun executionPauseAtom(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        taskPauseExecute: BuildTaskPauseInfo
    ): Result<Boolean>

    @Operation(summary = "取消并发起新构建", tags = ["v4_app_build_restart", "v4_user_build_restart"])
    @POST
    @Path("/build_restart")
    fun buildRestart(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @BkField(required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @BkField(required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        @BkField(required = true)
        buildId: String
    ): Result<String>

    @Operation(summary = "修改某次构建的备注", tags = ["v4_app_update_remark", "v4_user_update_remark"])
    @POST
    @Path("update_remark")
    fun updateRemark(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        @BkField(required = true)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @BkField(required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        @BkField(required = false)
        pipelineId: String?,
        @Parameter(description = "构建ID", required = true)
        @QueryParam("buildId")
        @BkField(required = true)
        buildId: String,
        @Parameter(description = "备注信息", required = true)
        remark: BuildHistoryRemark?
    ): Result<Boolean>

    @Operation(summary = "人工审核插件进行审核", tags = ["v4_app_manual_review", "v4_user_manual_review"])
    @POST
    @Path("/manual_review")
    fun manualReview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID（p-开头）", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "构建ID（b-开头）", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "步骤Id（e-开头）", required = false)
        @QueryParam("elementId")
        elementId: String?,
        @Parameter(description = "审核信息", required = true)
        params: ReviewParam,
        @Parameter(description = "对应stepId", required = false)
        @QueryParam("stepId")
        stepId: String?
    ): Result<Boolean>

    @Operation(
        summary = "获取流水线手动启动分页的参数",
        tags = ["v4_app_build_startOptions", "v4_user_build_startOptions"]
    )
    @POST
    @Path("/build_manual_startup_options")
    fun manualStartupOptions(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigwType", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "搜索参数", required = false)
        @QueryParam("search")
        search: String? = null,
        @Parameter(description = "请求参数", required = true)
        property: BuildFormProperty
    ): Result<List<BuildFormValue>>

    @Operation(
        summary = "尝试将异常导致流水线中断的继续运转下去（结果可能是：失败结束 or 继续运行）",
        tags = ["v4_app_try_fix_stuck_builds", "v4_user_try_fix_stuck_builds"]
    )
    @POST
    @Path("/try_fix_stuck_builds")
    fun tryFinishStuckBuilds(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "要操作的构建ID列表[最大50个]", required = true)
        buildIds: Set<String>
    ): Result<Boolean>
}
