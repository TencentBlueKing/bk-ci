package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.VMType
import com.tencent.devops.dispatch.macos.pojo.VMTypeCreate
import com.tencent.devops.dispatch.macos.pojo.VMTypeUpdate
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_VMTYPE", description = "OP接口-虚拟机类型资源")
@Path("op/macos/vmTypes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpVMTypeResource {

    @GET
    @Path("/{vmTypeId}")
    @Operation(summary = "获取虚拟机类型")
    fun get(
        @PathParam("vmTypeId")
        vmTypeId: Int
    ): Result<VMType?>

    @GET
    @Path("/")
    @Operation(summary = "获取虚拟机类型列表")
    fun list(): Result<List<VMType>?>

    @POST
    @Path("/")
    @Operation(summary = "创建虚拟机类型")
    fun create(
        @Parameter(description = "vmType", required = true)
        vmType: VMTypeCreate
    ): Result<Boolean>

    @DELETE
    @Path("/{vmTypeId}")
    @Operation(summary = "删除虚拟机类型")
    fun delete(
        @PathParam("vmTypeId")
        vmTypeId: Int
    ): Result<Boolean>

    @PUT
    @Path("/{vmTypeId}")
    @Operation(summary = "更新虚拟机类型")
    fun update(
        @Parameter(description = "vmType", required = true)
        vmType: VMTypeUpdate
    ): Result<Boolean>
}
