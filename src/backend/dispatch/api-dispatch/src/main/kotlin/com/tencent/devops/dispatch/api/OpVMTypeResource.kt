package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.VMType
import com.tencent.devops.dispatch.pojo.VMTypeCreate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = arrayOf("OP_VM_TYPE"), description = "虚拟机类型")
@Path("/op/vmtypes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpVMTypeResource {

    @ApiOperation("列举所有虚拟机类型")
    @GET
    @Path("/")
    fun list(): Result<List<VMType>>

    @ApiOperation("添加虚拟机类型")
    @POST
    @Path("/")
    fun create(
        @ApiParam(value = "类型名称", required = true)
        typeName: VMTypeCreate
    ): Result<Boolean>

    @ApiOperation("更改虚拟机类型名称")
    @PUT
    @Path("/")
    fun update(
        @ApiParam(value = "虚拟机类型", required = true)
        vmType: VMType
    ): Result<Boolean>

    @ApiOperation("删除虚拟机类型")
    @DELETE
    @Path("/")
    fun delete(
        @ApiParam(value = "虚拟机类型ID", required = true)
        @QueryParam("id")
        typeId: Int
    ): Result<Boolean>
}