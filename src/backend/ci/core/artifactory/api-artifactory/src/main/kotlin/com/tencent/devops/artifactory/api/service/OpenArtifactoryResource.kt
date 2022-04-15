package com.tencent.devops.artifactory.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_ARTIFACTORY"], description = "open_artifactory")
@Path("/open/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenArtifactoryResource {

    @PUT
    @Path("/projects/{projectId}/pipeline/{pipelineId}/buildId/{buildId}/artifactList")
    @ApiOperation("更新流水线构件列表")
    fun updateArtifactList(
        @ApiParam("认证token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        token: String,
        @ApiParam("用户id", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建id", required = true)
        @PathParam("buildId")
        buildId: String
    )
}