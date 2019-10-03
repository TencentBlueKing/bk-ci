package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.VM
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_VM"], description = "服务-虚拟机")
@Path("/service/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceVMResource {

    @ApiOperation("根据VM hashID 获取虚拟机信息")
    @GET
    @Path("/{vmHashId}")
    fun get(
        @ApiParam("VM哈希ID", required = true)
        @PathParam("vmHashId")
        vmHashId: String
    ): Result<VM>
}