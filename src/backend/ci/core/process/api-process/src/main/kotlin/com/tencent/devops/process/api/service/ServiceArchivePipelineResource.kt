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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
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

@Tag(name = "SERVICE_PIPELINE_ARCHIVE", description = "服务-流水线资源")
@Path("/service/archive/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArchivePipelineResource {

    @Operation(summary = "迁移归档流水线数据")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/data/migrate")
    fun migrateArchivePipelineData(
        @Parameter(description = "用户id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @Parameter(description = "取消正在运行构建标识", required = true)
        @QueryParam("cancelFlag")
        cancelFlag: Boolean = false
    ): Result<Boolean>

    @Operation(summary = "获取已归档流水线列表")
    @GET
    @Path("/projects/{projectId}/archived/pipelines/list")
    @Suppress("LongParameterList")
    fun getArchivedPipelineList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String? = null,
        @Parameter(description = "按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String? = null,
        @Parameter(description = "按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String? = null,
        @Parameter(description = "流水线排序", required = false, example = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @Parameter(description = "排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation? = null
    ): Result<Page<PipelineInfo>>
}
