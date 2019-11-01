package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.image.response.ImageDetail
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MARKET_IMAGE"], description = "SERVICE-研发商店-镜像")
@Path("/service/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceImageResource {
    @ApiOperation("查询镜像是否已安装到项目")
    @GET
    @Path("/image/projectCodes/{projectCode}/imageCodes/{imageCode}/isInstalled")
    fun isInstalled(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String
    ): Result<Boolean>

    @ApiOperation("根据code查询镜像详情")
    @GET
    @Path("/image/projectCodes/{projectCode}/imageCodes/{imageCode}/imageVersions/{imageVersion}")
    fun getImageDetailByCodeAndVersion(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目标识", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("镜像版本", required = false)
        @PathParam("imageVersion")
        imageVersion: String?
    ): Result<ImageDetail>
}