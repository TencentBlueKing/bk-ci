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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.code.p4.api.P4FileSpec
import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_P4"], description = "服务-p4相关")
@Path("/service/p4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceP4Resource {

    @ApiOperation("获取p4文件变更列表")
    @GET
    @Path("/{projectId}/{repositoryId}/getChangelistFiles")
    fun getChangelistFiles(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @ApiParam("p4 版本号", required = true)
        @QueryParam("change")
        change: Int
    ): Result<List<P4FileSpec>>

    @ApiOperation("获取p4 shelve文件变更列表")
    @GET
    @Path("/{projectId}/{repositoryId}/getShelvedFiles")
    fun getShelvedFiles(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @ApiParam("p4 版本号", required = true)
        @QueryParam("change")
        change: Int
    ): Result<List<P4FileSpec>>

    @ApiOperation("获取p4文件内容")
    @GET
    @Path("getFileContent")
    fun getFileContent(
        @ApiParam(value = "p4Port")
        @QueryParam("p4Port")
        p4Port: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "版本号")
        @QueryParam("reversion")
        reversion: Int,
        @ApiParam(value = "username")
        @HeaderParam("username")
        username: String,
        @ApiParam(value = "password")
        @HeaderParam("password")
        password: String
    ): Result<String>

    @ApiOperation("获取p4服务端信息")
    @GET
    @Path("/{projectId}/{repositoryId}/serverInfo")
    fun getServerInfo(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("代码库哈希ID或代代码库名称", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<P4ServerInfo>
}
