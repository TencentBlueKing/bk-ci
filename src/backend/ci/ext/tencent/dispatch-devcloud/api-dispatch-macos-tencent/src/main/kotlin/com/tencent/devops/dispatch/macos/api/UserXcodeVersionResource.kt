package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.MacOsVersionVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_XCODE_VERSION", description = "USER接口-xcode版本资源")
@Path("user/macos/xcodeVersions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserXcodeVersionResource {

    @GET
    @Path("/")
    @Operation(summary = "获取XCODE版本列表")
    fun list(): Result<List<String>>

    @GET
    @Path("/v2")
    @Operation(summary = "获取XCODE版本列表")
    fun listV2(
        @Parameter(description = "systemVersion", required = true)
        @QueryParam("systemVersion")
        systemVersion: String?
    ): Result<MacOsVersionVO>
}
