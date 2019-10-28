package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.third.wetest.WetestResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_WETEST"], description = "process相关wetest接口")
@Path("/build/wetest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildWetestResource {

    @ApiOperation("保存wetest响应结果")
    @POST
    @Path("/save")
    fun save(
        @ApiParam(value = "security响应结果", required = true)
        response: WetestResponse,
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        buildId: String
    ): Result<Boolean>
}