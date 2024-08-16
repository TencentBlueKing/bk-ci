package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.VirtualMachineInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_VM", description = "SERVICE接口-虚拟机资源")
@Path("service/macos/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceVMResource {
    @GET
    @Path("/")
    @Operation(summary = "获取vm列表")
    fun list(): Result<List<VirtualMachineInfo>>
}
