package com.tencent.devops.environment.api.tstack

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_TSTACK"], description = "Stack构建机")
@Path("/op/tstack")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpTstackResource {
    @ApiOperation("创建TStack构建机")
    @POST
    @Path("/createTstackNode")
    fun createTstackNode(): Result<String>

    @ApiOperation("销毁TStack构建机")
    @POST
    @Path("/destroyTstackNode")
    fun destroyTstackNode(
        @ApiParam("tstackNodeId", required = true)
        @QueryParam("tstackNodeId")
        stackNodeId: String
    ): Result<Boolean>

    @ApiOperation("分配虚拟机到项目")
    @POST
    @Path("/assignTstackNode")
    fun assignTstackNode(
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("tstackNodeId", required = true)
        @QueryParam("tstackNodeId")
        stackNodeId: String,
        @ApiParam("节点所有者", required = true)
        @QueryParam("user")
        user: String
    ): Result<String>

    @ApiOperation("从项目回收虚拟机")
    @POST
    @Path("/unassignTstackNode")
    fun unassignTstackNode(
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("tstackNodeId", required = true)
        @QueryParam("tstackNodeId")
        stackNodeId: String
    ): Result<Boolean>
}