package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.StreamTriggerBuildReq
import com.tencent.devops.gitci.pojo.V2TriggerBuildReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_STREAM"], description = "OPEN-API-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/stream")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwStreamResourceV3 {

    @ApiOperation("人工TriggerBuild启动构建")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/startup")
    fun triggerStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("TriggerBuild请求", required = true)
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<Boolean>
}
