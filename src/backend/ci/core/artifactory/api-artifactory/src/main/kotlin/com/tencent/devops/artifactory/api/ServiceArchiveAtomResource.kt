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

package com.tencent.devops.artifactory.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_ARTIFACTORY", description = "仓库-插件")
@Path("/service/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArchiveAtomResource {

    @Operation(summary = "获取插件包文件内容")
    @GET
    @Path("/atom/file/content")
    fun getAtomFileContent(
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String
    ): Result<String>

    @Operation(summary = "下载插件包文件")
    @GET
    @Path("/atom/file/download")
    fun downloadAtomFile(
        @Parameter(description = "文件路径", required = true)
        @QueryParam("filePath")
        filePath: String,
        @Context
        response: HttpServletResponse
    )

    @Operation(summary = "删除插件包文件")
    @DELETE
    @Path("/atom/file/delete")
    fun deleteAtomFile(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目编码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件代码", required = true)
        @QueryParam("atomCode")
        atomCode: String
    ): Result<Boolean>

    @Operation(summary = "更新插件包文件内容")
    @PUT
    @Path("/projectCodes/{projectCode}/atoms/{atomCode}/file/content")
    fun updateArchiveFile(
        @Parameter(description = "项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "插件编码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "插件版本号", required = true)
        @QueryParam("version")
        version: String,
        @Parameter(description = "文件名", required = true)
        @QueryParam("fileName")
        fileName: String,
        @Parameter(description = "文件内容", required = true)
        content: String
    ): Result<Boolean>
}
