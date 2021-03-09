package com.tencent.devops.process.api.auth

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

@Api(tags = ["AUTH_CALLBACK_PROCESS"], description = "iam回调process接口")
@Path("/open/pipeline/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProcessAuthResource {
    @POST
    @Path("/")
    @ApiOperation("iam流水线回调接口")
    fun pipelineInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO
    ): CallbackBaseResponseDTO?
}
