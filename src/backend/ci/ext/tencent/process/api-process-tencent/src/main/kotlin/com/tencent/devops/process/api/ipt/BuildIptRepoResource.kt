package com.tencent.devops.process.api.ipt

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_IPT_REPO_RESOURCE"], description = "IPT插件构建资源")
@Path("/build/ipt/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildIptRepoResource {

    @ApiOperation("")
    @GET
    @Path("/pipeline/{pipelineId}/commit/{commitId}/artifactorytInfo")
    fun getCommitBuildArtifactorytInfo(
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("userId")
        userId: String,
        @PathParam("commitId")
        commitId: String
    ): Result<IptBuildArtifactoryInfo>
}