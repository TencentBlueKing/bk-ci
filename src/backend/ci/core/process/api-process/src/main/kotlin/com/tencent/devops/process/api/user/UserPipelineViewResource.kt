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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewBulkAdd
import com.tencent.devops.process.pojo.classify.PipelineViewBulkRemove
import com.tencent.devops.process.pojo.classify.PipelineViewDict
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewHitFilters
import com.tencent.devops.process.pojo.classify.PipelineViewId
import com.tencent.devops.process.pojo.classify.PipelineViewMatchDynamic
import com.tencent.devops.process.pojo.classify.PipelineViewPipelineCount
import com.tencent.devops.process.pojo.classify.PipelineViewPreview
import com.tencent.devops.process.pojo.classify.PipelineViewSettings
import com.tencent.devops.process.pojo.classify.PipelineViewTopForm
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

@Tag(name = "USER_PIPELINE_VIEW", description = "用户-流水线视图")
@Path("/user/pipelineViews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineViewResource {
    @Operation(summary = "获取视图设置")
    @GET
    @Path("/projects/{projectId}/settings")
    fun getViewSettings(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineViewSettings>

    @Operation(summary = "更新视图设置")
    @POST
    @Path("/projects/{projectId}/settings")
    fun updateViewSettings(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        viewIdList: List<String>
    ): Result<Boolean>

    @Operation(summary = "获取所有视图")
    @GET
    @Path("/projects/{projectId}/")
    fun getViews(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineNewViewSummary>>

    @Operation(summary = "获取视图列表")
    @GET
    @Path("/projects/{projectId}/list")
    fun listView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("projected")
        @Parameter(description = "是否为项目流水线组 , 为空时不区分", required = false)
        projected: Boolean? = null,
        @QueryParam("viewType")
        @Parameter(description = "流水线组类型 , 1--动态, 2--静态 , 为空时不区分", required = false)
        viewType: Int? = null
    ): Result<List<PipelineNewViewSummary>>

    @Operation(summary = "添加视图")
    @POST
    @Path("/projects/{projectId}/")
    fun addView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId>

    @Operation(summary = "获取视图")
    @GET
    @Path("/projects/{projectId}/views/{viewId}")
    fun getView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<PipelineNewView>

    @Operation(summary = "删除视图")
    @DELETE
    @Path("/projects/{projectId}/views/{viewId}")
    fun deleteView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<Boolean>

    @Operation(summary = "更改视图")
    @PUT
    @Path("/projects/{projectId}/views/{viewId}")
    fun updateView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签ID", required = true)
        @PathParam("viewId")
        viewId: String,
        pipelineView: PipelineViewForm
    ): Result<Boolean>

    @Operation(summary = "置顶视图")
    @POST
    @Path("/projects/{projectId}/views/{viewId}/top")
    fun topView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签ID", required = true)
        @PathParam("viewId")
        viewId: String,
        pipelineViewTopForm: PipelineViewTopForm
    ): Result<Boolean>

    @Operation(summary = "预览视图")
    @POST
    @Path("/projects/{projectId}/preview")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun preview(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewPreview>

    @Operation(summary = "获取流水线组与流水线的对应关系")
    @GET
    @Path("/projects/{projectId}/dict")
    fun dict(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineViewDict>

    @Operation(summary = "流水线组过滤条件")
    @GET
    @Path("/projects/{projectId}/getHitFilters")
    fun getHitFilters(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @QueryParam("pipelineId")
        pipelineId: String,
        @QueryParam("viewId")
        viewId: String
    ): Result<PipelineViewHitFilters>

    @Operation(summary = "命中动态组情况")
    @POST
    @Path("/projects/{projectId}/matchDynamicView")
    @BkApiPermission([BkApiHandleType.API_NO_AUTH_CHECK])
    fun matchDynamicView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        pipelineViewMatchDynamic: PipelineViewMatchDynamic
    ): Result<List<String>>

    @Operation(summary = "批量添加")
    @POST
    @Path("/projects/{projectId}/bulkAdd")
    fun bulkAdd(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        bulkAdd: PipelineViewBulkAdd
    ): Result<Boolean>

    @Operation(summary = "批量移除")
    @POST
    @Path("/projects/{projectId}/bulkRemove")
    fun bulkRemove(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        bulkRemove: PipelineViewBulkRemove
    ): Result<Boolean>

    @Operation(summary = "根据流水线ID获取视图(流水线组)")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/listViews")
    fun listViewByPipelineId(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<PipelineNewViewSummary>>

    @Operation(summary = "根据视图ID获取当前流水线的具体数目")
    @GET
    @Path("/projects/{projectId}/views/{viewId}/pipelineCount")
    fun pipelineCount(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<PipelineViewPipelineCount>

    @Operation(summary = "根据流水线ID获取视图ID(流水线组ID)")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/listViewIds")
    fun listViewIdsByPipelineId(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Set<Long>>

    @Operation(summary = "用户有权限添加的静态流水线组")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/listPermissionStaticViews")
    fun listPermissionStaticViews(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<PipelineNewViewSummary>>
}
