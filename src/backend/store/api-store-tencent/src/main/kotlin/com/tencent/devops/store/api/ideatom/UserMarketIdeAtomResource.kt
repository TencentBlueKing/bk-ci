package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ideatom.IdeAtomDetail
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomMainItem
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_IDE_ATOM"], description = "IDE插件")
@Path("/user/market/ideAtom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMarketIdeAtomResource {

    @ApiOperation("获取IDE插件市场首页的数据")
    @Path("/list/main")
    @GET
    fun mainPageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketIdeAtomMainItem>>

    @ApiOperation("IDE插件市场搜索插件")
    @GET
    @Path("/atom/list/")
    fun queryIdeAtomList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @ApiParam("插件范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @ApiParam("插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("功能标签(多个用逗号分隔)", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("研发来源(蓝盾 第三方)", required = false)
        @QueryParam("rdType")
        rdType: IdeAtomTypeEnum?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: MarketIdeAtomSortTypeEnum? = MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketIdeAtomResp>

    @ApiOperation("根据IDE插件标识获取插件正式版本详情")
    @GET
    @Path("/atomCodes/{atomCode}")
    fun getIdeAtomByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("atomCode", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<IdeAtomDetail?>
}