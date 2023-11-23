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

package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPENAPI_PIPELINE_ARCHIVE_V4"], description = "OPENAPI-流水线归档资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/archived/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwPipelineArchiveResourceV4 {

    @ApiOperation("迁移归档流水线数据", tags = ["v4_app_pipeline_archive", "v4_user_pipeline_archive"])
    @POST
    @Path("/{pipelineId}/data/migrate")
    fun migrateArchivePipelineData(
        @ApiParam(value = "用户id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目id", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam(value = "流水线id", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @ApiOperation("获取已归档流水线列表", tags = ["v4_app_archived_pipeline_list", "v4_user_archived_pipeline_list"])
    @GET
    @Path("/list")
    @Suppress("LongParameterList")
    fun getArchivedPipelineList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam("按流水线过滤", required = false)
        @QueryParam("filterByPipelineName")
        filterByPipelineName: String? = null,
        @ApiParam("按创建人过滤", required = false)
        @QueryParam("filterByCreator")
        filterByCreator: String? = null,
        @ApiParam("按标签过滤", required = false)
        @QueryParam("filterByLabels")
        filterByLabels: String? = null,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME,
        @ApiParam("排序规则", required = false)
        @QueryParam("collation")
        collation: PipelineCollation? = null
    ): Result<Page<PipelineInfo>>
}
