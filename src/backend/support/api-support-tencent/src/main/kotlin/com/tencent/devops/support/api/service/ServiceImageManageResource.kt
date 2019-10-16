package com.tencent.devops.support.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.image.UploadImageRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_IMAGE_MANAGE"], description = "图片管理")
@Path("/service/image/manage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceImageManageResource {

    @ApiOperation("压缩图片")
    @POST
    @Path("/compress")
    fun compressImage(
        @ApiParam("网络图片路径", required = true)
        @QueryParam("imageUrl")
        imageUrl: String,
        @ApiParam("压缩宽度", required = true)
        @QueryParam("compressWidth")
        compressWidth: Int,
        @ApiParam("压缩高度", required = true)
        @QueryParam("compressHeight")
        compressHeight: Int
    ): Result<String>

    @ApiOperation("上传图片")
    @POST
    @Path("/upload")
    fun uploadImage(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("上传图片请求报文体", required = true)
        uploadImageRequest: UploadImageRequest
    ): Result<String?>
}