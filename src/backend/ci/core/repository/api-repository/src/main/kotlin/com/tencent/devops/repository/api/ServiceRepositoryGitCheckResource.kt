package com.tencent.devops.repository.api

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_REPOSITORY_GIT_CHECK", description = "服务=GitCheck相关")
@Path("/service/repository/gitcheck")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRepositoryGitCheckResource {

    @Operation(summary = "获取gitcheck信息")
    @POST
    @Path("/get")
    fun getGitCheck(
        @Parameter(description = "pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "commitId", required = true)
        @QueryParam("commitId")
        commitId: String,
        @Parameter(description = "context", required = true)
        @QueryParam("context")
        context: String,
        @Parameter(description = "targetBranch", required = false)
        @QueryParam("targetBranch")
        targetBranch: String?,
        @Parameter(description = "repositoryConfig", required = true)
        repositoryConfig: RepositoryConfig
    ): Result<RepositoryGitCheck?>

    @Operation(summary = "记录gitcheck信息")
    @POST
    @Path("/create")
    fun createGitCheck(
        @Parameter(description = "gitCheck", required = true)
        gitCheck: RepositoryGitCheck
    )

    @Operation(summary = "更新gitcheck信息")
    @PUT
    @Path("/update")
    fun updateGitCheck(
        @Parameter(description = "gitCheckId", required = true)
        @QueryParam("gitCheckId")
        gitCheckId: Long,
        @Parameter(description = "buildNumber", required = true)
        @QueryParam("buildNumber")
        buildNumber: Int,
        @Parameter(description = "checkRunId", required = false)
        @QueryParam("checkRunId")
        checkRunId: Long?
    )
}
