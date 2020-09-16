package com.tencent.devops.artifactory.api.external

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_URL"], description = "链接服务")
@Path("/external/url")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalShortUrlResource {

    @ApiOperation("访问下载链接")
    @Path("/visit/{urlId}")
    @GET
    fun visitShortUrl(
        @ApiParam("urlId", required = true)
        @PathParam("urlId")
        urlId: String,
        @Context
        response: HttpServletResponse
    )
}