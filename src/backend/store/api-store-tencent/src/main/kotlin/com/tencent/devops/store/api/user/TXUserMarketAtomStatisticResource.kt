package com.tencent.devops.store.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.AtomPipelineExecInfo
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

@Api(tags = ["USER_MARKET_ATOM_STATISTIC"], description = "原子市场-原子-统计")
@Path("/user/market/atom/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXUserMarketAtomStatisticResource {

    @ApiOperation("根据插件代码获取对应的流水线信息")
    @GET
    @Path("/projectCodes/{projectCode}/atomCodes/{atomCode}/pipelines")
    fun getAtomPipelines(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("原子代码", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<AtomPipelineExecInfo>>
}