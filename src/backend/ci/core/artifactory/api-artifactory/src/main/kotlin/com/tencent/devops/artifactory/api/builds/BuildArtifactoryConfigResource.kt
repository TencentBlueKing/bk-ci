package com.tencent.devops.artifactory.api.builds

import com.tencent.devops.artifactory.pojo.ReportPluginConfig
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_ARTIFACTORY_CONF", description = "仓库-配置管理")
@Path("/build/artifactories/conf")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildArtifactoryConfigResource {

    @Operation(summary = "获取配置项artifactory.realm")
    @GET
    @Path("/realm")
    fun getRealm(): Result<String>

    @Operation(summary = "获取归档报告插件配置")
    @GET
    @Path("/report")
    fun getReportConfig(): Result<ReportPluginConfig>
}
