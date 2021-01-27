package com.tencent.devops.openapi.api.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.api.apigw.pojo.VirtualMachineInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_DISPATCH"], description = "SERVICE接口-虚拟机资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/dispatch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwDispatchResource {
    @GET
    @Path("/macos/vms")
    @ApiOperation("获取vm列表")
    fun macOSList(): Result<List<VirtualMachineInfo>>
}
