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
    @Path("/projects/{projectId}/repositories/{repositoryId}/latestRevision")
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
    @Path("/projects/{projectId}/repositories/{repositoryId}/branches")
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
    @Path("/projects/{projectId}/repositories/{repositoryId}/tags")
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