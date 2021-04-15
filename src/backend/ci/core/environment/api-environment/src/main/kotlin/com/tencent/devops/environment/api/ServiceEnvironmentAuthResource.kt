package com.tencent.devops.environment.api

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_CALLBACK_ENVIRONMENT"], description = "iam回调environment接口")
@Path("/open/environment/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceEnvironmentAuthResource {
    @POST
    @Path("/env")
    @ApiOperation("iam环境管理回调接口")
    fun environmentInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO
    ): CallbackBaseResponseDTO?

    @POST
    @Path("/node")
    @ApiOperation("iam节点回调接口")
    fun nodeInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO
    ): CallbackBaseResponseDTO?
}
