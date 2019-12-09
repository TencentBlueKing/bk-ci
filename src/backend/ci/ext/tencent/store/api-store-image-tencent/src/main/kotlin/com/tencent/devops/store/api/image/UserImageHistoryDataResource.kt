package com.tencent.devops.store.api.image

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.response.SimpleImageInfo
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

@Api(tags = ["USER_MARKET_IMAGE_HISTORY"], description = "研发商店-镜像-历史数据")
@Path("/user/market/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageHistoryDataResource {
    @ApiOperation("历史数据转换")
    @GET
    @Path("/transfer")
    fun tranferHistoryImage(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("机器类型", required = true)
        @QueryParam("agentType")
        agentType: ImageAgentTypeEnum,
        @ApiParam("镜像特殊代号/路径", required = false)
        @QueryParam("value")
        value: String?
    ): Result<SimpleImageInfo>
}