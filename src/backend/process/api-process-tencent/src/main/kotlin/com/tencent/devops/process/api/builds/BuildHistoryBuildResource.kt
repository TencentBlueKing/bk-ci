package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildHistory
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_HISTORY_BUILD"], description = "构建-历史构建资源")
@Path("/build/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildHistoryBuildResource {

    @ApiOperation("获取流水线构建单条历史")
    @GET
    @Path("/listByBuildId")
    fun getSingleHistoryByBuildId(
        @ApiParam("流水线buildId", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String
    ): Result<BuildHistory?>
}