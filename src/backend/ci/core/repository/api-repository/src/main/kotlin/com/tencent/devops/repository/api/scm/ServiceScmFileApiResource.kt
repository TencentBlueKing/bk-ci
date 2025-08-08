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

package com.tencent.devops.repository.api.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.pojo.hub.ScmFilePushReq
import com.tencent.devops.repository.pojo.hub.ScmFilePushResult
import com.tencent.devops.scm.api.pojo.Content
import com.tencent.devops.scm.api.pojo.Tree
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_SCM_FILE_API", description = "服务-代码源-文件API")
@Path("/service/scm/file/api/{projectId}/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceScmFileApiResource {

    @Operation(summary = "获取指定文件路径目录")
    @POST
    @Path("listFileTree")
    fun listFileTree(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("path")
        path: String,
        @Parameter(description = "ref可以是SHA, branch name, tag name", required = true)
        @QueryParam("ref")
        ref: String,
        @Parameter(description = "是否递归获取", required = false)
        @QueryParam("recursive")
        recursive: Boolean = false,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository
    ): Result<List<Tree>>

    @Operation(summary = "获取文件内容")
    @POST
    @Path("getFileContent")
    fun getFileContent(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "文件路径", required = true)
        @QueryParam("path")
        path: String,
        @Parameter(description = "ref可以是SHA, branch name, tag name", required = true)
        @QueryParam("ref")
        ref: String,
        @Parameter(description = "代码库授权信息", required = true)
        authRepository: AuthRepository
    ): Result<Content?>

    @Operation(summary = "推送文件")
    @POST
    @Path("/pushFile")
    fun pushFile(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        filePushReq: ScmFilePushReq
    ): Result<ScmFilePushResult>
}
