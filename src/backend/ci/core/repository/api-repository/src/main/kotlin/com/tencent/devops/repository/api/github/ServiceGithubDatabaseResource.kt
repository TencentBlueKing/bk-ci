package com.tencent.devops.repository.api.github

import com.tencent.devops.common.sdk.github.request.GetTreeRequest
import com.tencent.devops.common.sdk.github.response.GithubTreeResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_DATABASE_GITHUB"], description = "服务-github-database")
@Path("/service/github/database")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubDatabaseResource {
    
    @ApiOperation("获取tree")
    @POST
    @Path("/getTree")
    fun getTree(
        request: GetTreeRequest,
        userId: String
    ): GithubTreeResponse
}
