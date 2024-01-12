package com.tencent.devops.artifactory.api.builds

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "BUILD_ARTIFACTORY_CONF", description = "仓库-配置管理")
@Path("/build/artifactories/conf")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildArtifactoryConfigResource {

    @Operation(summary = "获取配置项artifactory.realm")
    @GET
    @Path("/realm")
    fun getRealm(): Result<String>
}
