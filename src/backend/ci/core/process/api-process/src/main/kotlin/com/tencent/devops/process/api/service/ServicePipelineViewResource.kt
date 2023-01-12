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

@Api(tags = ["SERVICE_PIPELINE_VIEW"], description = "服务-流水线视图")
@Path("/service/pipelineView/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineViewResource {
    @ApiOperation("用户获取视图流水线编排列表")
    @GET
    @Path("/listViewPipelines")
    fun listViewPipelines(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @ApiParam("按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String?,
        @ApiParam("按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String?,
        @ApiParam("按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String?,
        @ApiParam("按视图过滤", required = false)
        @QueryParam("filterByViewIds")
        filterByViewIds: String? = null,
        @ApiParam("用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @ApiParam("用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @ApiParam("维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?,
        @ApiParam("排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation?,
        @ApiParam("是否展示已删除流水线", required = false)
        @QueryParam("showDelete")
        showDelete: Boolean? = false
    ): Result<PipelineViewPipelinePage<Pipeline>>

    @ApiOperation("获取视图列表")
    @GET
    @Path("/list")
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
    @Path("")
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
    @Path("")
    fun getView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @ApiParam("用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @ApiParam("维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?
    ): Result<PipelineNewView>

    @ApiOperation("删除视图")
    @DELETE
    @Path("")
    fun deleteView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @ApiParam("用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @ApiParam("维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?
    ): Result<Boolean>

    @ApiOperation("更改视图")
    @PUT
    @Path("")
    fun updateView(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @ApiParam("用户视图ID,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewId")
        viewId: String?,
        @ApiParam("用户视图名称,表示用户当前所在视图 viewId和viewName 选其一填入", required = false)
        @QueryParam("viewName")
        viewName: String?,
        @ApiParam("维度是否为项目,和viewName搭配使用", required = false)
        @QueryParam("isProject")
        isProject: Boolean?,
        pipelineView: PipelineViewForm
    ): Result<Boolean>
}
