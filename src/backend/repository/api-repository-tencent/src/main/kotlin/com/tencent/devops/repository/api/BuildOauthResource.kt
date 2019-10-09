package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.oauth.GitToken
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import java.awt.PageAttributes
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_REPOSITORY_OAUTH"], description = "构建-oauth相关")
@Path("/build/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildOauthResource {

    @ApiOperation("获取git代码库accessToken信息")
    @GET
    @Path("/git/{userId}")
    fun gitGet(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<GitToken?>

    @ApiOperation("获取git代码库accessToken信息")
    @GET
//    @Path("/git/{userId}")
    @Path("/git/userId/{userId}")
    fun gitGetV2(
            @ApiParam("用户ID", required = true)
            @PathParam("userId")
            userId: String
    ): Result<GitToken?>
}