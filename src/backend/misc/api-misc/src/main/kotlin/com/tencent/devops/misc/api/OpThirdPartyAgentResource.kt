package com.tencent.devops.misc.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_ENVIRONMENT_THIRD_PARTY_AGENT"], description = "第三方构建机资源")
@Path("/op/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpThirdPartyAgentResource {

    @ApiOperation("设置agent最大并发升级数量")
    @POST
    @Path("/agents/setMaxParallelUpgradeCount")
    fun setMaxParallelUpgradeCount(
        @ApiParam("maxParallelUpgradeCount", required = true)
        maxParallelUpgradeCount: Int
    ): Result<Boolean>

    @ApiOperation("获取agent最大并发升级数量")
    @POST
    @Path("/agents/getMaxParallelUpgradeCount")
    fun getMaxParallelUpgradeCount(): Result<Int?>
}