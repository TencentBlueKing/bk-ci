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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineAtomRel
import com.tencent.devops.store.pojo.atom.AtomProp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_ATOM", description = "用户-流水线-插件")
@Path("/user/pipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineAtomResource {

    @Operation(summary = "获取插件流水线相关信息列表")
    @GET
    @Path("/atoms/{atomCode}/rel/list")
    fun getPipelineAtomRelList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "查询开始时间，格式yyyy-MM-dd HH:mm:ss", required = true)
        @QueryParam("startUpdateTime")
        startUpdateTime: String,
        @Parameter(description = "查询结束时间，格式yyyy-MM-dd HH:mm:ss", required = true)
        @QueryParam("endUpdateTime")
        endUpdateTime: String,
        @Parameter(description = "第几页", required = true, example = "1")
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页多少条", required = true, example = "10")
        @QueryParam("pageSize")
        pageSize: Int = 10
    ): Result<Page<PipelineAtomRel>?>

    @Operation(summary = "导出插件流水线相关信息csv文件")
    @POST
    @Path("/atoms/{atomCode}/rel/csv/export")
    fun exportPipelineAtomRelCsv(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本号", required = false)
        @QueryParam("version")
        version: String?,
        @Parameter(description = "查询开始时间，格式yyyy-MM-dd HH:mm:ss", required = true)
        @QueryParam("startUpdateTime")
        startUpdateTime: String,
        @Parameter(description = "查询结束时间，格式yyyy-MM-dd HH:mm:ss", required = true)
        @QueryParam("endUpdateTime")
        endUpdateTime: String,
        @Context
        response: HttpServletResponse
    )

    @Operation(summary = "获取流水线下插件属性列表")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/atom/prop/list")
    fun getPipelineAtomPropList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "指定流水线版本", required = false)
        @QueryParam("version")
        version: Int?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<Map<String, AtomProp>?>
}
