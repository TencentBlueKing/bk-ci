package com.tencent.devops.quality.api.v2

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

@Api(tags = ["AUTH_CALLBACK_QUALITY"], description = "iam回调quality接口")
@Path("/open/quality/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceQualityAuthResource {
    @POST
    @Path("/rule")
    @ApiOperation("iam质量红线规则回调接口")
    fun qualityRuleInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @ApiParam("token")
        token: String
    ): CallbackBaseResponseDTO?

    @POST
    @Path("/group")
    @ApiOperation("iam质量红线用户组回调接口")
    fun qualityGroupInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam(AUTH_HEADER_IAM_TOKEN)
        @ApiParam("token")
        token: String
    ): CallbackBaseResponseDTO?
}
