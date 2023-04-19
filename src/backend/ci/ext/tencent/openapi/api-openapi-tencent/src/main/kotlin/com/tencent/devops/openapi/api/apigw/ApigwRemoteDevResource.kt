package com.tencent.devops.openapi.api.apigw

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_REMOTEDEV"], description = "OPEN-API-REMOTEDEV服务")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwRemoteDevResource {
    @ApiOperation("更新client对应环境的稳定版本")
    @PUT
    @Path("/updateClientVersion")
    fun updateClientVersion(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("env", required = true)
        @QueryParam("env")
        env: String,
        @ApiParam("version", required = true)
        @QueryParam("version")
        version: String
    ): Result<Boolean>
}
