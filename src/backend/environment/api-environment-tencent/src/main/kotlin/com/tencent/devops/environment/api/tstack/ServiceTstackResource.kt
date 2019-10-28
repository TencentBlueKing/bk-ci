package com.tencent.devops.environment.api.tstack

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.tstack.TstackNode
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_TSTACK"], description = "服务-Stack")
@Path("/service/tstack")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTstackResource {
    @ApiOperation("更新Tstack节点可用状态")
    @POST
    @Path("/node/{hashId}/updateNodeAvailable")
    fun updateNodeAvailable(
        @ApiParam("Hash ID", required = true)
        @PathParam("hashId")
        hashId: String,
        @ApiParam("是否可用", required = true)
        available: Boolean
    ): Result<Boolean>

    @ApiOperation("获取构建节点信息")
    @GET
    @Path("/projects/{projectId}/node/{hashId}")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("Hash ID", required = true)
        @PathParam("hashId")
        hashId: String
    ): Result<TstackNode?>

    @ApiOperation("获取可用Tstack节点列表")
    @GET
    @Path("/projects/{projectId}/listAvailableNodes")
    fun listAvailableTstackNodes(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<TstackNode>>
}