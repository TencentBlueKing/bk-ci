package com.tencent.devops.experience.api.service

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_IAM_TOKEN
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_CALLBACK_EXPERIENCE"], description = "experience")
@Path("/open/experience/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceExperienceAuthResource {
    @POST
    @Path("/task")
    @ApiOperation("iam版本体验回调接口")
    fun experienceTaskInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @ApiParam("token")
        token: String
    ): CallbackBaseResponseDTO?

    @POST
    @Path("/group")
    @ApiOperation("iam版本体验组回调接口")
    fun experienceGroup(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @ApiParam("token")
        token: String
    ): CallbackBaseResponseDTO?
}
