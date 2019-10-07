package com.tencent.devops.store.api.atom

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomProcessInfo
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
}