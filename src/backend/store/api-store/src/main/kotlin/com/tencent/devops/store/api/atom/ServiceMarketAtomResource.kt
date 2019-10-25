package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.atom.*
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MARKET_ATOM"], description = "原子市场-原子")
@Path("/service/market/atom/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMarketAtomResource {

    @ApiOperation("获取指定项目下所有流水线原子的名称信息")
    @GET
    @Path("/project/{projectCode}/atomNames")
    fun getProjectAtomNames(
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String/* atomCode */, String/* cnName */>>

    @ApiOperation("根据插件版本ID获取插件版本进度")
    @GET
    @Path("/desk/atom/release/process/atomId/{atomId}")
    fun getProcessInfo(
            @ApiParam("atomId", required = true)
            @PathParam("atomId")
            atomId: String
    ): Result<AtomProcessInfo>

    @ApiOperation("获取所有流水线插件信息")
    @GET
    @Path("/project/{projectCode}/projectElement")
    fun getProjectElements(
        @ApiParam("项目编码", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String/* atomCode */, String/* cnName */>>

    @ApiOperation("根据插件代码获取插件详细信息")
    @GET
    @Path("/{atomCode}")
    fun getAtomByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("用户名", required = true)
        @QueryParam("username")
        username: String
    ): Result<AtomVersion?>

    @ApiOperation("根据插件代码获取插件统计信息")
    @GET
    @Path("/{atomCode}/statistic")
    fun getAtomStatisticByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("用户名", required = true)
        @QueryParam("username")
        username: String
    ): Result<AtomStatistic>

    @ApiOperation("根据插件代码获取使用的流水线详情")
    @GET
    @Path("/{atomCode}/pipelines")
    fun getAtomPipelinesByCode(
        @ApiParam("插件代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("用户名", required = true)
        @QueryParam("username")
        username: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AtomPipeline>>

    @ApiOperation("安装插件到项目")
    @POST
    @Path("/atom/install")
    fun installAtom(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("渠道类型", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS,
        @ApiParam("安装插件到项目请求报文体", required = true)
        installAtomReq: InstallAtomReq
    ): Result<Boolean>
}