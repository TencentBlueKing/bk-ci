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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.code.p4.api.P4ChangeList
import com.tencent.devops.scm.code.p4.api.P4FileSpec
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
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

@Tag(name = "SERVICE_P4", description = "服务-p4相关")
@Path("/service/p4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceP4Resource {

    @Operation(summary = "获取p4文件变更列表")
    @GET
    @Path("/{projectId}/{repositoryId}/getChangelistFiles")
    fun getChangelistFiles(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "p4 版本号", required = true)
        @QueryParam("change")
        change: Int
    ): Result<List<P4FileSpec>>

    @Operation(summary = "获取p4 shelve文件变更列表")
    @GET
    @Path("/{projectId}/{repositoryId}/getShelvedFiles")
    fun getShelvedFiles(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "p4 版本号", required = true)
        @QueryParam("change")
        change: Int
    ): Result<List<P4FileSpec>>

    @Operation(summary = "获取p4文件内容")
    @GET
    @Path("getFileContent")
    fun getFileContent(
        @Parameter(description = "p4Port")
        @QueryParam("p4Port")
        p4Port: String,
        @Parameter(description = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @Parameter(description = "版本号")
        @QueryParam("reversion")
        reversion: Int,
        @Parameter(description = "username")
        @HeaderParam("username")
        username: String,
        @Parameter(description = "password")
        @HeaderParam("password")
        password: String
    ): Result<String>

    @Operation(summary = "获取p4服务端信息")
    @GET
    @Path("/{projectId}/{repositoryId}/serverInfo")
    fun getServerInfo(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<P4ServerInfo>

    @Operation(summary = "获取p4文件变更列表(含提交信息)")
    @GET
    @Path("/{projectId}/{repositoryId}/getChangelist")
    fun getChangelist(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "p4 版本号", required = true)
        @QueryParam("change")
        change: Int
    ): Result<P4ChangeList>

    @Operation(summary = "获取p4 shelve文件变更列表(含提交信息)")
    @GET
    @Path("/{projectId}/{repositoryId}/getShelvedChangeList")
    fun getShelvedChangeList(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "p4 版本号", required = true)
        @QueryParam("change")
        change: Int
    ): Result<P4ChangeList>
}
