package com.tencent.devops.experience.api.service

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_IAM_TOKEN
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "AUTH_CALLBACK_EXPERIENCE", description = "experience")
@Path("/open/experience/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceExperienceAuthResource {
    @POST
    @Path("/task")
    @Operation(summary = "iam版本体验回调接口")
    fun experienceTaskInfo(
        @Parameter(description = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @Parameter(description = "token")
        token: String
    ): CallbackBaseResponseDTO?

    @POST
    @Path("/group")
    @Operation(summary = "iam版本体验组回调接口")
    fun experienceGroup(
        @Parameter(description = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @Parameter(description = "token")
        token: String
    ): CallbackBaseResponseDTO?
}
