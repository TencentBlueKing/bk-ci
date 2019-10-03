package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.VMWithPrivateProject
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PRIVATE_VM"], description = "VM 专机管理")
@Path("/op/privateVMs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPrivateVMResource {

    @ApiOperation("获取所有的专机信息")
    @GET
    @Path("/")
    fun list(): Result<List<VMWithPrivateProject>>

    @ApiOperation("绑定专机")
    @POST
    @Path("/{vmId}/projects/{projectId}")
    fun bind(
        @ApiParam(value = "虚拟机ID", required = true)
        @PathParam("vmId")
        vmId: Int,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("解绑专机")
    @DELETE
    @Path("/{vmId}/projects/{projectId}")
    fun unbind(
        @ApiParam(value = "虚拟机ID", required = true)
        @PathParam("vmId")
        vmId: Int,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
