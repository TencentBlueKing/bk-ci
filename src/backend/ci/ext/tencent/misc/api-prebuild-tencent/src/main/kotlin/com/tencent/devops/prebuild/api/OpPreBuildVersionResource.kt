package com.tencent.devops.prebuild.api

import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.project.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PREBUILD", description = "OP-PREBUILD版本资源")
@Path("/op/prebuild/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPreBuildVersionResource {

    @Operation(summary = "新增IDE插件的版本信息")
    @POST
    @Path("/ide")
    fun creatIdeVersion(
        @Parameter(description = "版本信息", required = true)
        prePluginVersion: PrePluginVersion
    ): Result<Boolean>

    @Operation(summary = "修改IDE插件的版本信息")
    @PUT
    @Path("/ide")
    fun updateIdeVersion(
        @Parameter(description = "版本信息", required = true)
        prePluginVersion: PrePluginVersion
    ): Result<Boolean>

    @Operation(summary = "删除IDE插件的版本信息")
    @DELETE
    @Path("/ide")
    fun deleteIdeVersion(
        @Parameter(description = "/版本号", required = true)
        @QueryParam("version")
        version: String
    ): Result<Boolean>

    @Operation(summary = "查询IDE插件的版本信息")
    @GET
    @Path("/ide")
    fun getIdeVersion(): Result<List<PrePluginVersion>>
}
