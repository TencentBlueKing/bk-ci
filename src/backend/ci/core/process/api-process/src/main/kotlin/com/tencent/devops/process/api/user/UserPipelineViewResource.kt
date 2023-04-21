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
import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["USER_PIPELINE_VIEW"], description = "用户-流水线视图")
@Path("/user/pipelineViews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineViewResource {
    @ApiOperation("获取视图设置")
    @GET
    @Path("/projects/{projectId}/settings")
    fun getViewSettings(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineViewSettings>

    @ApiOperation("更新视图设置")
    @POST
    @Path("/projects/{projectId}/settings")
    fun updateViewSettings(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        viewIdList: List<String>
    ): Result<Boolean>

    @ApiOperation("获取所有视图")
    @GET
    @Path("/projects/{projectId}/")
    fun getViews(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineNewViewSummary>>

    @ApiOperation("获取视图列表")
    @GET
    @Path("/projects/{projectId}/list")
    fun listView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @QueryParam("projected")
        @ApiParam(value = "是否为项目流水线组 , 为空时不区分", required = false)
        projected: Boolean? = null,
        @QueryParam("viewType")
        @ApiParam(value = "流水线组类型 , 1--动态, 2--静态 , 为空时不区分", required = false)
        viewType: Int? = null
    ): Result<List<PipelineNewViewSummary>>

    @ApiOperation("添加视图")
    @POST
    @Path("/projects/{projectId}/")
    fun addView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId>

    @ApiOperation("获取视图")
    @GET
    @Path("/projects/{projectId}/views/{viewId}")
    fun getView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<PipelineNewView>

    @ApiOperation("删除视图")
    @DELETE
    @Path("/projects/{projectId}/views/{viewId}")
    fun deleteView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<Boolean>

    @ApiOperation("更改视图")
    @PUT
    @Path("/projects/{projectId}/views/{viewId}")
    fun updateView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String,
        pipelineView: PipelineViewForm
    ): Result<Boolean>

    @ApiOperation("置顶视图")
    @POST
    @Path("/projects/{projectId}/views/{viewId}/top")
    fun topView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String,
        pipelineViewTopForm: PipelineViewTopForm
    ): Result<Boolean>

    @ApiOperation("预览视图")
    @POST
    @Path("/projects/{projectId}/preview")
    fun preview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewPreview>

    @ApiOperation("获取流水线组与流水线的对应关系")
    @GET
    @Path("/projects/{projectId}/dict")
    fun dict(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String
    ): Result<PipelineViewDict>

    @ApiOperation("流水线组过滤条件")
    @GET
    @Path("/projects/{projectId}/getHitFilters")
    fun getHitFilters(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @QueryParam("pipelineId")
        pipelineId: String,
        @QueryParam("viewId")
        viewId: String
    ): Result<PipelineViewHitFilters>

    @ApiOperation("命中动态组情况")
    @POST
    @Path("/projects/{projectId}/matchDynamicView")
    fun matchDynamicView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        pipelineViewMatchDynamic: PipelineViewMatchDynamic
    ): Result<List<String>>

    @ApiOperation("批量添加")
    @POST
    @Path("/projects/{projectId}/bulkAdd")
    fun bulkAdd(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        bulkAdd: PipelineViewBulkAdd
    ): Result<Boolean>

    @ApiOperation("批量移除")
    @POST
    @Path("/projects/{projectId}/bulkRemove")
    fun bulkRemove(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        bulkRemove: PipelineViewBulkRemove
    ): Result<Boolean>

    @ApiOperation("根据流水线ID获取视图(流水线组)")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/listViews")
    fun listViewByPipelineId(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<PipelineNewViewSummary>>

    @ApiOperation("根据视图ID获取当前流水线的具体数目")
    @GET
    @Path("/projects/{projectId}/views/{viewId}/pipelineCount")
    fun pipelineCount(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @PathParam("viewId")
        viewId: String
    ): Result<PipelineViewPipelineCount>

    @ApiOperation("根据流水线ID获取视图ID(流水线组ID)")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/listViewIds")
    fun listViewIdsByPipelineId(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Set<Long>>
}
