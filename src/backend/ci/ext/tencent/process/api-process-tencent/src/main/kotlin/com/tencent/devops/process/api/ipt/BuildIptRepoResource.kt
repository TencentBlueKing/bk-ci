package com.tencent.devops.process.api.ipt

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.process.pojo.ipt.IptBuildCommitInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_IPT_REPO_RESOURCE"], description = "IPT插件构建资源")
@Path("/build/ipt/repo/repositories")
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
    ): IptBuildArtifactoryInfo
}