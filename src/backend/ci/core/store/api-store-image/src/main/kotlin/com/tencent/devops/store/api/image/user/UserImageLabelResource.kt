package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Label
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE_LABEL"], description = "镜像-镜像标签")
@Path("/user/market/image/label")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageLabelResource {

    @ApiOperation("获取所有镜像标签信息")
    @GET
    @Path("/labels")
    fun getAllImageLabels(): Result<List<Label>?>

    @ApiOperation("根据镜像ID获取镜像标签信息")
    @GET
    @Path("/imageIds/{imageId}/labels")
    fun getImageLabelsByImageId(
        @ApiParam("镜像ID", required = true)
        @PathParam("imageId")
        imageId: String
    ): Result<List<Label>?>
}