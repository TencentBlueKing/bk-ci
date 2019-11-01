package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.image.ImageBaseInfoUpdateRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_MARKET_IMAGE"], description = "镜像市场-镜像")
@Path("/build/market/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildImageResource {

    @ApiOperation("更新镜像信息")
    @PUT
    @Path("/projectCodes/{projectCode}/imageCodes/{imageCode}/versions/{version}")
    fun updateImageBaseInfo(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("镜像标识", required = true)
        @PathParam("imageCode")
        imageCode: String,
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String,
        @ApiParam("镜像基本信息修改请求报文体", required = true)
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean>
}