package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.commit.CommitData
import com.tencent.devops.repository.pojo.commit.CommitResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_COMMIT"], description = "git提交记录")
@Path("/service/commit/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCommitResource {

    @ApiOperation("获取流水线的最新一次commit")
    @GET
    @Path("/getLatestCommit")
    fun getLatestCommit(
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("原子ID", required = true)
        @QueryParam("elementId")
        elementId: String,
        @ApiParam("仓库ID或者仓库名", required = true)
        @QueryParam("repoId")
        repositoryId: String,
        @ApiParam("代码库请求类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?
    ): Result<CommitData?>

    @ApiOperation("根据构建ID获取提交记录")
    @GET
    @Path("/getCommitsByBuildId")
    fun getCommitsByBuildId(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_AGENT_ID)
        agentId: String
    ): Result<List<CommitResponse>>
}