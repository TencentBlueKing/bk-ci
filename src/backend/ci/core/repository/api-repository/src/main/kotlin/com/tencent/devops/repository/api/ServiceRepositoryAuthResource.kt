package com.tencent.devops.repository.api

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

@Api(tags = ["AUTH_CALLBACK_REPOSITORY"], description = "iam回调repository接口")
@Path("/open/repository/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRepositoryAuthResource {
    @POST
    @Path("/")
    @ApiOperation("iam代码库回调接口")
    fun repositoryInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO
    ): CallbackBaseResponseDTO?
}
