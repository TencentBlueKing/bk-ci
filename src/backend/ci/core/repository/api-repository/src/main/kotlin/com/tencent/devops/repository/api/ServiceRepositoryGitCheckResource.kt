package com.tencent.devops.repository.api

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_REPOSITORY_GIT_CHECK"], description = "服务=GitCheck相关")
@Path("/service/repository/gitcheck")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRepositoryGitCheckResource {

    @ApiOperation("获取gitcheck信息")
    @POST
    @Path("/get")
    fun getGitCheck(
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("commitId", required = true)
        @QueryParam("commitId")
        commitId: String,
        @ApiParam("context", required = true)
        @QueryParam("context")
        context: String,
        @ApiParam("repositoryConfig", required = true)
        repositoryConfig: RepositoryConfig
    ): Result<RepositoryGitCheck?>

    @ApiOperation("记录gitcheck信息")
    @POST
    @Path("/create")
    fun createGitCheck(
        @ApiParam("gitCheck", required = true)
        gitCheck: RepositoryGitCheck
    )

    @ApiOperation("更新gitcheck信息")
    @PUT
    @Path("/update")
    fun updateGitCheck(
        @ApiParam("gitCheckId", required = true)
        @QueryParam("gitCheckId")
        gitCheckId: Long,
        @ApiParam("buildNumber", required = true)
        @QueryParam("buildNumber")
        buildNumber: Int
    )
}
