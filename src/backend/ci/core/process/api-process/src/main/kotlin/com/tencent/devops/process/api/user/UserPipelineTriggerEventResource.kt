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
 *
 */

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonStatistics
import com.tencent.devops.process.pojo.trigger.RepoTriggerEventVo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_TRIGGER_EVENT", description = "用户-流水线触发事件")
@Path("/user/trigger/event")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineTriggerEventResource {

    @Operation(summary = "获取触发类型")
    @GET
    @Path("listTriggerType")
    fun listTriggerType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库类型,为空则返回所有事件类型", required = false)
        @QueryParam("scmType")
        scmType: ScmType?
    ): Result<List<IdValue>>

    @Operation(summary = "获取事件类型")
    @GET
    @Path("listEventType")
    fun listEventType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码库类型,为空则返回所有事件类型", required = false)
        @QueryParam("scmType")
        scmType: ScmType?
    ): Result<List<IdValue>>

    @Operation(summary = "获取流水线触发事件列表")
    @GET
    @Path("/{projectId}/{pipelineId}/listPipelineTriggerEvent")
    fun listPipelineTriggerEvent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "事件类型", required = false)
        @QueryParam("eventType")
        eventType: String?,
        @Parameter(description = "触发类型", required = false)
        @QueryParam("triggerType")
        triggerType: String?,
        @Parameter(description = "触发用户", required = false)
        @QueryParam("triggerUser")
        triggerUser: String?,
        @Parameter(description = "事件ID", required = false)
        @QueryParam("eventId")
        eventId: Long?,
        @Parameter(description = "触发状态", required = false)
        @QueryParam("reason")
        reason: PipelineTriggerReason?,
        @Parameter(description = "开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @Parameter(description = "结束", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SQLPage<PipelineTriggerEventVo>>

    @Operation(summary = "获取代码库webhook事件列表")
    @GET
    @Path("/{projectId}/{repoHashId}/listRepoTriggerEvent")
    fun listRepoTriggerEvent(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库hashId", required = true)
        @PathParam("repoHashId")
        repoHashId: String,
        @Parameter(description = "事件类型", required = false)
        @QueryParam("eventType")
        eventType: String?,
        @Parameter(description = "触发类型", required = false)
        @QueryParam("triggerType")
        triggerType: String?,
        @Parameter(description = "触发用户", required = false)
        @QueryParam("triggerUser")
        triggerUser: String?,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "事件ID", required = false)
        @QueryParam("eventId")
        eventId: Long?,
        @Parameter(description = "流水线名称", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?,
        @Parameter(description = "触发状态", required = false)
        @QueryParam("reason")
        reason: PipelineTriggerReason?,
        @Parameter(description = "开始时间", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @Parameter(description = "结束", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SQLPage<RepoTriggerEventVo>>

    @Operation(summary = "获取触发事件详情")
    @GET
    @Path("/{projectId}/{eventId}/listEventDetail")
    fun listEventDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "事件ID", required = true)
        @PathParam("eventId")
        eventId: Long,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "流水线名", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?,
        @QueryParam("reason")
        reason: PipelineTriggerReason?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SQLPage<PipelineTriggerEventVo>>

    @Operation(summary = "获取触发事件详情")
    @GET
    @Path("/{projectId}/{eventId}/triggerReasonStatistics")
    fun triggerReasonStatistics(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "事件ID", required = true)
        @PathParam("eventId")
        eventId: Long,
        @Parameter(description = "流水线ID", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "流水线名", required = false)
        @QueryParam("pipelineName")
        pipelineName: String?
    ): Result<PipelineTriggerReasonStatistics>

    @Operation(summary = "重新触发")
    @POST
    @Path("/{projectId}/{detailId}/replay")
    fun replay(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "触发详情ID", required = true)
        @PathParam("detailId")
        detailId: Long
    ): Result<Boolean>

    @Operation(summary = "一键重新触发")
    @POST
    @Path("/{projectId}/{eventId}/replayAll")
    fun replayAll(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "事件ID", required = true)
        @PathParam("eventId")
        eventId: Long
    ): Result<Boolean>
}
