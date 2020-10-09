package com.tencent.devops.environment.api.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_THIRD_PARTY_AGENT"], description = "第三方构建机资源")
@Path("/build/agent/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildThirdPartyAgentResource {
    @ApiOperation("根据agentId获取系统")
    @GET
    @Path("/project/{projectId}/agentId/{agentId}/getOs")
    fun getOs(
        @ApiParam(value = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("agentId", required = true)
        @PathParam("agentId")
        agentId: String
    ): Result<String>
}