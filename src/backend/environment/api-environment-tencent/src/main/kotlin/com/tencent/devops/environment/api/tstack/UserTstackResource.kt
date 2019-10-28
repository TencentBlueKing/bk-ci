package com.tencent.devops.environment.api.tstack

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.tstack.TstackNode
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_TSTACK"], description = "用户-Stack")
@Path("/user/tstack")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTstackResource {
    @ApiOperation("获取可用Tstack节点列表")
    @GET
    @Path("/{projectId}/listAvailableNodes")
    fun listAvailableTstackNodes(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<TstackNode>>

    @ApiOperation("获取 Web Console Token")
    @GET
    @Path("/{projectId}/nodes/{nodeHashId}/getVncToken")
    fun getVncToken(
        @ApiParam("项目ID", required = true)
@PathParam("projectId")
projectId: String,
        @ApiParam("node Hash ID", required = true)
        @PathParam("nodeHashId")
        nodeHashId: String
    ): Result<String>
}