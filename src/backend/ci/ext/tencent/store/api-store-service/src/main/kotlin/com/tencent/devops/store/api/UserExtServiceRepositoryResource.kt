package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_SERVICE_REPOSITORY"], description = "研发商店-扩展服务-代码库")
@Path("/user/market/service/repositorys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceRepositoryResource {
    @ApiOperation("扩展服务-更改扩展代码库的用户信息")
    @PUT
    @Path("/{serviceCode}")
    fun changeServiceRepositoryUserInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("扩展代码", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<Boolean>

    @ApiOperation("自动获取Readme.md信息")
    @GET
    @Path("/serviceCodes/{serviceCode}/readme/")
    fun getReadme(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务Code ", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<String?>
}