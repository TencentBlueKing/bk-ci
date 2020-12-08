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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.pojo.RevisionInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_SCM"], description = "用户-scm相关接口")
@Path("/user/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserScmResource {

    @ApiOperation("获取仓库最新版本")
    @GET
    // @Path("/projects/{projectId}/repositories/{repositoryId}/latestRevision")
    @Path("/{projectId}/{repositoryId}/latestRevision")
    fun getLatestRevision(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("repo hash id or repo name", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("branch name", required = false)
        @QueryParam("branchName")
        branchName: String? = null,
        @ApiParam("SVN additional path", required = false)
        @QueryParam("additionalPath")
        additionalPath: String? = null,
        @ApiParam("代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<RevisionInfo>

    @ApiOperation("列出仓库所有分支")
    @GET
    // @Path("/projects/{projectId}/repositories/{repositoryId}/branches")
    @Path("/{projectId}/{repositoryId}/branches")
    fun listBranches(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("repo hash id", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<List<String>>

    @ApiOperation("列出仓库所有分支")
    @GET
    // @Path("/projects/{projectId}/repositories/{repositoryId}/tags")
    @Path("/{projectId}/{repositoryId}/tags")
    fun listTags(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("repo hash id", required = true)
        @PathParam("repositoryId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<List<String>>
}