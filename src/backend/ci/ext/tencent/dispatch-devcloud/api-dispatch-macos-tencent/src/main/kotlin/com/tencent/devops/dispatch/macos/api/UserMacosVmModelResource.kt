package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.devcloud.DMAllVmModelData
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelData
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelRequest
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * User接口 - macOS VM Model资源
 */
@Tag(name = "USER_MACOS_VM_MODEL", description = "USER接口-macOS VM Model资源")
@Path("user/macos/vmModel")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMacosVmModelResource {

    @POST
    @Path("/")
    @Operation(summary = "获取macOS VM Model列表")
    fun getVmModelList(
        @Parameter(description = "请求参数", required = true)
        request: DevCloudMacosVmModelRequest
    ): Result<List<DevCloudMacosVmModelData>>

    @GET
    @Path("/all")
    @Operation(summary = "获取所有macOS VM Model类型")
    fun getAllVmModels(): Result<DMAllVmModelData>
}
