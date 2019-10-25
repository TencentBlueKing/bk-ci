package com.tencent.devops.store.api.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomStatistic
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MARKET_ATOM"], description = "原子市场-原子")
@Path("/service/market/atom/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXServiceMarketAtomResource {

    @ApiOperation("设置原子构建结果状态")
    @PUT
    @Path("/atomCodes/{atomCode}/versions/{version}")
    fun setAtomBuildStatusByAtomCode(
        @ApiParam("原子代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("版本号", required = true)
        @PathParam("version")
        version: String,
        @ApiParam("用户Id", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("原子状态", required = true)
        @QueryParam("atomStatus")
        atomStatus: AtomStatusEnum,
        @ApiParam("状态描述", required = false)
        @QueryParam("msg")
        msg: String?
    ): Result<Boolean>

    @ApiOperation("获取所有流水线原子信息")
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
}