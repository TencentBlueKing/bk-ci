package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunProduct
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_ZHIYUN"], description = "用户-织云业务列表")
@Path("/user/zhiyun")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserZhiyunResource {

    @ApiOperation("查询业务信息")
    @GET
    @Path("/product")
    fun getProduct(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ZhiyunProduct>>
}