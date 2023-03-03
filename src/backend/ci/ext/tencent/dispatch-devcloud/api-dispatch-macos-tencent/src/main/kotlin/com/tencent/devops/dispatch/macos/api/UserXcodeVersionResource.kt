package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.MacOsVersionVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_XCODE_VERSION"], description = "USER接口-xcode版本资源")
@Path("user/xcodeVersions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserXcodeVersionResource {

    @GET
    @Path("/")
    @ApiOperation("获取XCODE版本列表")
    fun list(): Result<List<String>>

    @GET
    @Path("/v2")
    @ApiOperation("获取XCODE版本列表")
    fun listV2(
        @ApiParam("systemVersion", required = true)
        @QueryParam("systemVersion")
        systemVersion: String?
    ): Result<MacOsVersionVO>
}
