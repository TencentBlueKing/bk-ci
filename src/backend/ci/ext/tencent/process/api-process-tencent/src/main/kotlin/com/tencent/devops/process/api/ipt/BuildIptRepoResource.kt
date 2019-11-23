package com.tencent.devops.process.api.ipt

import com.tencent.devops.process.pojo.ipt.IptBuildArtifactoryInfo
import com.tencent.devops.process.pojo.ipt.IptBuildCommitInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_IPT_REPO_RESOURCE"], description = "IPT插件构建资源")
@Path("/build/ipt/repo/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildIptRepoResource {

    @ApiOperation("")
    @GET
    @Path("/pipeline/{pipelineId}/commit/{commitId}/buildCommitInfo")
    fun getCommitBuildCommitInfo(
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("commitId")
        commitId: String
    ): IptBuildCommitInfo

    @ApiOperation("")
    @GET
    @Path("/pipeline/{pipelineId}/commit/{commitId}/artifactorytInfo")
    fun getCommitBuildArtifactorytInfo(
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("commitId")
        commitId: String
    ): IptBuildArtifactoryInfo
}