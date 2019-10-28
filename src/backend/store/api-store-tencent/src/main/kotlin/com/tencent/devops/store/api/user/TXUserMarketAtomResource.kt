package com.tencent.devops.store.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.AtomDevLanguage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_ATOM"], description = "插件市场-插件")
@Path("/user/market/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXUserMarketAtomResource {

    @ApiOperation("获取插件支持的语言列表")
    @GET
    @Path("/desk/atom/language")
    fun listLanguage(): Result<List<AtomDevLanguage?>>

    @ApiOperation("删除工作台插件")
    @DELETE
    @Path("/desk/atoms/{atomCode}")
    fun deleteAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<Boolean>
}