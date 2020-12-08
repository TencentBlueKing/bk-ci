package com.tencent.devops.auth.api

import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.HeaderParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_RESOURCE_CALLBACK"], description = "权限-资源-回调接口")
@Path("/service/auth/resource")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AuthResourceCallBackResource {

    @POST
    @Path("/projects")
    @ApiOperation("项目列表")
    fun projectInfo(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam("Authorization")
        @ApiParam("token")
        token: String
    ): CallbackBaseResponseDTO?

    @POST
    @Path("/instances/list")
    @ApiOperation("特定资源列表")
    fun resourceList(
        @ApiParam(value = "回调信息")
        callBackInfo: CallbackRequestDTO,
        @HeaderParam("Authorization")
        @ApiParam("token")
        token: String
    ): CallbackBaseResponseDTO?
}