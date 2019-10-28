package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomReq
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_MARKET_IDE_ATOM"], description = "IDE中调用的接口")
@Path("/external/market/ideAtom")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalMarketIdeAtomResource {

    @ApiOperation("插件列表")
    @GET
    @Path("/list/")
    fun list(
        @ApiParam("IDE类型", required = true)
        @QueryParam("categoryCode")
        categoryCode: String,
        @ApiParam("插件名称", required = false)
        @QueryParam("atomName")
        atomName: String?,
        @ApiParam("插件分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("功能标签(多个用逗号分隔)", required = false)
        @QueryParam("labelCodes")
        labelCodes: String?,
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
    ): Result<ExternalIdeAtomResp>

    @ApiOperation("根据插件代码获取IDE插件信息")
    @GET
    @Path("/atomCodes/{atomCode}")
    fun getIdeAtomsByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String
    ): Result<IdeAtom?>

    @ApiOperation("安装IDE插件")
    @POST
    @Path("/install")
    fun installIdeAtom(
        @ApiParam("安装IDE插件请求报文体", required = true)
        installIdeAtomReq: InstallIdeAtomReq
    ): Result<InstallIdeAtomResp?>
}