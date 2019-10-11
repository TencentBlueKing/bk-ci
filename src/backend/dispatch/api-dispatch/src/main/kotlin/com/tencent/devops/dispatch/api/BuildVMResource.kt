package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.VM
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Created by rdeng on 2017/9/4.
 */
@Api(tags = ["BUILD_VM"], description = "VM 管理")
@Path("/build/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildVMResource {

    @ApiOperation("根据虚拟机ID获取虚拟机详情")
    @GET
    @Path("/getVmByPipeLine")
    fun getVmByPipeLine(
        @ApiParam(value = "构建 ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "虚拟机Seq ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String
    ): Result<VM>
}
