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

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Path("/op/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPRepositoryResource {
    @Operation(summary = "用于对数据库表填充哈希值")
    @POST
    @Path("/addhashid")
    fun addHashId()

    @Operation(summary = "修改工蜂老域名")
    @POST
    @Path("/updateGitDomain")
    fun updateGitDomain(
        @Parameter(description = "git老域名", required = true)
        @QueryParam("oldGitDomain")
        oldGitDomain: String,
        @Parameter(description = "git新域名", required = true)
        @QueryParam("newGitDomain")
        newGitDomain: String,
        @Parameter(description = "灰度项目列表,多个用,分割", required = true)
        @QueryParam("grayProject")
        grayProject: String?,
        @Parameter(description = "灰度权重", required = true)
        @QueryParam("grayWeight")
        grayWeight: Int?,
        @Parameter(description = "灰度白名单,多个用,分割", required = true)
        @QueryParam("grayWhiteProject")
        grayWhiteProject: String?
    ): Result<Boolean>

    @Operation(summary = "更新git项目ID")
    @POST
    @Path("/updateGitProjectId")
    fun updateGitProjectId()

    @Operation(summary = "更新github项目ID")
    @POST
    @Path("/updateGithubProjectId")
    fun updateGithubProjectId()

    @Operation(summary = "设置工蜂webhook路由到灰度")
    @PUT
    @Path("/{projectId}/{repositoryId}/setGrayGitHookUrl")
    fun setGrayGitHookUrl(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库ID", required = true)
        @PathParam("repositoryId")
        repositoryId: Long
    ): Result<Boolean>

    @Operation(summary = "移除工蜂webhook路由到灰度")
    @PUT
    @Path("/{projectId}/{repositoryId}/removeGrayGitHookUrl")
    fun removeGrayGitHookUrl(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库ID", required = true)
        @PathParam("repositoryId")
        repositoryId: Long
    ): Result<Boolean>

    @Operation(summary = "批量移除代码库与流水线关联关系")
    @DELETE
    @Path("/{projectId}/{repoHashId}/removeRepositoryPipelineRef")
    fun removeRepositoryPipelineRef(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库ID", required = true)
        @PathParam("repoHashId")
        repoHashId: String
    ): Result<Boolean>

    @Operation(summary = "")
    @PUT
    @Path("updateRepoCredentialType")
    fun updateRepoCredentialType(
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "代码库ID", required = false)
        @QueryParam("repoHashId")
        repoHashId: String?
    ): Result<Boolean>

    @Operation(summary = "")
    @PUT
    @Path("updateRepoScmCode")
    fun updateRepoScmCode(
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "代码库ID", required = false)
        @QueryParam("repoHashId")
        repoHashId: String?
    ): Result<Boolean>
}
