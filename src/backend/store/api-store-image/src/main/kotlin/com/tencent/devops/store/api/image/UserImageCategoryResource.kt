package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Category
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE_CATEGORY"], description = "镜像-镜像范畴")
@Path("/user/market/image/categorys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageCategoryResource {

    @ApiOperation("获取所有镜像范畴信息")
    @GET
    @Path("/")
    fun getAllImageCategorys(): Result<List<Category>?>
}