package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.VMType
import com.tencent.devops.dispatch.macos.pojo.VMTypeCreate
import com.tencent.devops.dispatch.macos.pojo.VMTypeUpdate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_VMTYPE"], description = "OP接口-虚拟机类型资源")
@Path("op/macos/vmTypes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpVMTypeResource {

    @GET
    @Path("/{vmTypeId}")
    @ApiOperation("获取虚拟机类型")
    fun get(
        @PathParam("vmTypeId")
        vmTypeId: Int
    ): Result<VMType?>

    @GET
    @Path("/")
    @ApiOperation("获取虚拟机类型列表")
    fun list(): Result<List<VMType>?>

    @POST
    @Path("/")
    @ApiOperation("创建虚拟机类型")
    fun create(
        @ApiParam("vmType", required = true)
        vmType: VMTypeCreate
    ): Result<Boolean>

    @DELETE
    @Path("/{vmTypeId}")
    @ApiOperation("删除虚拟机类型")
    fun delete(
        @PathParam("vmTypeId")
        vmTypeId: Int
    ): Result<Boolean>

    @PUT
    @Path("/{vmTypeId}")
    @ApiOperation("更新虚拟机类型")
    fun update(
        @ApiParam("vmType", required = true)
        vmType: VMTypeUpdate
    ): Result<Boolean>
}
