package com.tencent.devops.repository.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["EXTERNAL_REPO"], description = "外部-仓库资源")
@Path("/external/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalRepoResource {

    @ApiOperation("根据用户ID列举Git上面的工程")
    @GET
    @Path("/git/callback")
    fun gitCallback(
        @ApiParam(value = "code")
        @QueryParam("code")
        code: String,
        @ApiParam(value = "state")
        @QueryParam("state")
        state: String
    ): Response
}