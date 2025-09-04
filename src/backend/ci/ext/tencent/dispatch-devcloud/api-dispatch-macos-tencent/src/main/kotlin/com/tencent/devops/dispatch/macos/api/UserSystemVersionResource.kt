package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.MacOsVersionVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_SYSTEM_VERSION", description = "USER接口-系统版本资源")
@Path("user/macos/systemVersions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSystemVersionResource {

    @GET
    @Path("/")
    @Operation(summary = "获取macos操作系统版本列表")
    fun list(): Result<List<String>>

    @GET
    @Path("/v2")
    @Operation(summary = "获取macos操作系统版本列表")
    fun listV2(): Result<MacOsVersionVO>
}
