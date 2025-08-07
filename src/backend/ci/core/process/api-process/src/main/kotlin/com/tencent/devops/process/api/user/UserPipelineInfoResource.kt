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

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineDetailInfo
import com.tencent.devops.process.pojo.PipelineIdAndName
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_INFO", description = "用户-流水线信息接口")
@Path("/user/pipelineInfos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList", "TooManyFunctions")
interface UserPipelineInfoResource {
    @Operation(summary = "列出所有流水线")
    @GET
    // @Path("/projects/{projectId}/")
    @Path("/{projectId}/")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "筛选流水线逗号分隔", required = false)
        @QueryParam("pipelineIdList")
        pipelineIdListString: String?
    ): Result<List<Pipeline>>

    @Operation(summary = "分页获取流水线名称与Id")
    @GET
    @Path("/get/names")
    fun paginationGetIdAndName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "渠道号", required = true)
        @QueryParam("channelCodes")
        @BkField(required = true)
        channelCodes: String,
        @Parameter(description = "keyword", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "页码", required = true, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页大小", required = true, example = "10")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<PipelineIdAndName>>

    @Operation(summary = "根据流水线名称搜索")
    @GET
    @Path("{projectId}/searchByName")
    fun searchByName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "搜索名称")
        @QueryParam("pipelineName")
        pipelineName: String?,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<List<PipelineIdAndName>>

    @Operation(summary = "根据流水线ID搜索")
    @GET
    @Path("{projectId}/searchByPipelineId")
    fun searchByPipelineName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "搜索名称")
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<PipelineIdAndName?>

    @Operation(summary = "获取流水线详情")
    @GET
    @Path("{projectId}/{pipelineId}/detail")
    fun getPipelineInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线Id")
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<PipelineDetailInfo?>
}
