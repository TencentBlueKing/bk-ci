package com.tencent.devops.project.api.service

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

@Api(tags = ["AUTH_CALLBACK_PROJECT"], description = "iam回调project接口")
@Path("/open/project/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectAuthResource {
    @POST
    @Path("/")
    @ApiOperation("iam项目回调接口")
    fun projectInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO
    ): CallbackBaseResponseDTO?
}
