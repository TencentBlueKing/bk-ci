package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_PRROCESS"], description = "获取流水线数据")
@Path("/build/process/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildProcessResource {
    @ApiOperation("获取修改流水线的用户")
    @POST
    @Path("/getLastUpdateUser/{pipelineId}/")
    fun getUpdateUser(
        @ApiParam("渠道号，默认为DS", required = false)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<String>
}