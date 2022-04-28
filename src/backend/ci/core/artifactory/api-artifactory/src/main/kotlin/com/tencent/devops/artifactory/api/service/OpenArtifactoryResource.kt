package com.tencent.devops.artifactory.api.service

import com.tencent.bkrepo.webhook.pojo.payload.node.NodeCreatedEventPayload
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TOKEN
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_ARTIFACTORY"], description = "open_artifactory")
@Path("/open/artifactories/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpenArtifactoryResource {

    @POST
    @Path("/artifactList")
    @ApiOperation("更新流水线构件列表")
    fun updateArtifactList(
        @ApiParam("认证token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TOKEN)
        token: String,
        @ApiParam("新上传构件回调数据", required = true)
        nodeCreatedEventPayload: NodeCreatedEventPayload
    )
}
