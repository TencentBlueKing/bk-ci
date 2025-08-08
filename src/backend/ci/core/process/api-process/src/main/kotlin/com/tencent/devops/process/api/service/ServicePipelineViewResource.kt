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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineNewView
import com.tencent.devops.process.pojo.classify.PipelineNewViewSummary
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.PipelineViewId
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "SERVICE_PIPELINE_VIEW", description = "服务-流水线视图")
@Path("/service/pipelineView/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineViewResource {
    @Operation(summary = "用户获取视图流水线编排列表")
    @GET
    @Path("/listViewPipelines")
    fun listViewPipelines(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "流水线排序", required = false, example = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @Parameter(description = "按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?,
        @Parameter(description = "按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String?,
        @Parameter(description = "按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String?,
        @Parameter(description = "按视图过滤", required = false)
        @QueryParam("filterByViewIds")
        filterByViewIds: String? = null,
        @Parameter(description = "用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @Parameter(description = "用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @Parameter(description = "维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?,
        @Parameter(description = "排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation?,
        @Parameter(description = "是否展示已删除流水线", required = false)
        @QueryParam("showDelete")
        showDelete: Boolean? = false
    ): Result<PipelineViewPipelinePage<Pipeline>>

    @Operation(summary = "获取视图列表")
    @GET
    @Path("/list")
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
    @Path("")
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
    @Path("")
    fun getView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @Parameter(description = "用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @Parameter(description = "维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?
    ): Result<PipelineNewView>

    @Operation(summary = "删除视图")
    @DELETE
    @Path("")
    fun deleteView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @Parameter(description = "用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @Parameter(description = "维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?
    ): Result<Boolean>

    @Operation(summary = "更改视图")
    @PUT
    @Path("")
    fun updateView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @Parameter(description = "用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @Parameter(description = "维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?,
        pipelineView: PipelineViewForm
    ): Result<Boolean>

    @Operation(summary = "根据流水线ID获取视图ID(流水线组ID)")
    @GET
    @Path("/pipelines/{pipelineId}/listViewIds")
    fun listViewIdsByPipelineId(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Set<Long>>

    @Operation(summary = "根据视图ID获取流水线ID列表")
    @POST
    @Path("/pipelines/listPipelineIdByViewIds")
    fun listPipelineIdByViewIds(
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "按视图过滤", required = false)
        viewIdsEncode: List<String>
    ): Result<List<String>>
}
