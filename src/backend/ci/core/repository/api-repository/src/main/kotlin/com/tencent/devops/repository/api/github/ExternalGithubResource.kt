package com.tencent.devops.repository.api.github

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

@Api(tags = ["EXTERNAL_GITHUB"], description = "github外部接口")
@Path("/external/github")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalGithubResource {

    @ApiOperation("Github Oauth回调")
    @GET
    @Path("/callback")
    fun callback(
        @ApiParam(value = "code")
        @QueryParam("code")
        code: String,
        @ApiParam(value = "state")
        @QueryParam("state")
        state: String
    ): Response
}
