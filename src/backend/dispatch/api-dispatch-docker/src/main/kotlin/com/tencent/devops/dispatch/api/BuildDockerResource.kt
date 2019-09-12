package com.tencent.devops.dispatch.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["BUILD_DOCKER"], description = "构建-构建执行DOCKER资源")
@Path("/build/dockers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildDockerResource {

    @ApiOperation("下载构建执行器")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiResponses(
            ApiResponse(code = 304, message = "本地的构建执行器已是最新，无需下载")
    )
    fun download(
        @ApiParam("本地eTag标签", required = false)
        @QueryParam("eTag")
        eTag: String?
    ): Response
}
