package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_VARIABLE"], description = "服务-构建参数")
@Path("/service/variable")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceVarResource {
    @ApiOperation("获取指定构建或指定流水线下的构建变量")
    @Path("/getBuildVariable")
    @GET
    fun getBuildVar(
        @ApiParam(value = "构建ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam(value = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<Map<String, String>>
}