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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
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
import io.swagger.v3.oas.annotations.media.ExampleObject
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

@Tag(name = "OPENAPI_PIPELINE_VIEW_GROUP_V4", description = "OPENAPI-流水线视图")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/pipelineView")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
interface ApigwPipelineViewResourceV4 {

    @Operation(
        summary = "用户获取视图(流水线组)流水线编排列表",
        tags = ["v4_user_pipeline_view_pipelines", "v4_app_pipeline_view_pipelines"]
    )
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
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
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

    @Operation(summary = "获取视图(流水线组)列表", tags = ["v4_user_pipeline_view_list", "v4_app_pipeline_view_list"])
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

    @Operation(summary = "添加视图(流水线组)", tags = ["v4_user_pipeline_view_create", "v4_app_pipeline_view_create"])
    @POST
    @Path("")
    fun addView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(
            description = "流水线视图创建模型",
            examples = [
                ExampleObject(
                    description = "当使用静态分组(手动指定组内所包含的流水线)时," +
                            "仅需指定名称(name)、是否项目级别(projected)和分组类型(viewType:2)",
                    value = """
                            {
                                "name": "我是分组名称",
                                "projected": true,
                                "viewType": 2
                            }"""
                ),
                ExampleObject(
                    description = "当使用静态分组时，同时传入几条流水线",
                    value = """
                            {
                                "name": "我是分组名称",
                                "projected": true,
                                "viewType": 2,
                                "pipelineIds": [
                                    "p-xxx",
                                    "p-xxx"
                                ]
                            }"""
                ),
                ExampleObject(
                    description = "当使用动态分组(组内所包含的流水线根据设置的流水线名称、标签等属性去动态匹配)，" +
                            "需额外指定逻辑符(logic)、过滤规则(filters)",
                    value = """
                            {
                                "name": "我是分组名称",
                                "projected": true,
                                "viewType": 1,
                                "logic": "AND",
                                "filters": [
                                  {
                                    "@type": "filterByName",
                                    "condition": "LIKE",
                                    "pipelineName": "xxx"
                                  },
                                  {
                                    "@type": "filterByCreator",
                                    "condition": "INCLUDE",
                                    "userIds": [
                                      "xxx"
                                    ]
                                  }
                                ]
                            }"""
                )
            ]

        )
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId>

    @Operation(summary = "获取视图(流水线组)", tags = ["v4_user_pipeline_view_get", "v4_app_pipeline_view_get"])
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

    @Operation(summary = "删除视图(流水线组)", tags = ["v4_user_pipeline_view_delete", "v4_app_pipeline_view_delete"])
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

    @Operation(summary = "更改视图(流水线组)", tags = ["v4_user_pipeline_view_update", "v4_app_pipeline_view_update"])
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
        @Parameter(
            description = "流水线视图更新模型",
            examples = [
                ExampleObject(
                    description = "当给分组改名时," +
                            "仅需指定名称(name)、是否项目级别(projected)",
                    value = """
                            {
                                "name": "我是分组名称",
                                "projected": true
                            }"""
                ),
                ExampleObject(
                    description = "当使用静态分组时，新增流水线到分组，需要将原来的流水线加上，否则会被移除。",
                    value = """
                            {
                                "name": "我是分组名称",
                                "projected": true,
                                "viewType": 2,
                                "pipelineIds": [
                                    "p-old1",
                                    "p-old2",
                                    "p-new"
                                ]
                            }"""
                ),
                ExampleObject(
                    description = "当使用动态分组(组内所包含的流水线根据设置的流水线名称、标签等属性去动态匹配)，" +
                            "需指定逻辑符(logic)、过滤规则(filters)",
                    value = """
                            {
                                "name": "我是分组名称",
                                "projected": true,
                                "viewType": 1,
                                "logic": "AND",
                                "filters": [
                                  {
                                    "@type": "filterByName",
                                    "condition": "LIKE",
                                    "pipelineName": "xxx"
                                  },
                                  {
                                    "@type": "filterByCreator",
                                    "condition": "INCLUDE",
                                    "userIds": [
                                      "xxx"
                                    ]
                                  }
                                ]
                            }"""
                )
            ]
        )
        pipelineView: PipelineViewForm
    ): Result<Boolean>
}
