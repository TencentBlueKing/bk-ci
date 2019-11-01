package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMemberItem
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IMAGE_MEMBER"], description = "镜像市场-镜像-用户")
@Path("/user/market/desk/image/member/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMarketImageMemberResource {

    @ApiOperation("查看镜像成员信息")
    @GET
    @Path("/view")
    fun view(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像代码", required = true)
        @QueryParam("imageCode")
        imageCode: String
    ): Result<StoreMemberItem?>
}