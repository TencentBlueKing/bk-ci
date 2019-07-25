package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.AgentBuildInfo
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

@Api(tags = ["SERVICE_AGENT"], description = "服务-Agent")
@Path("/service/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAgentResource {

    @ApiOperation("获取agent构建信息")
    @GET
    @Path("/{agentId}/listBuilds")
    fun listAgentBuild(
        @ApiParam("agent Hash ID", required = true)
        @PathParam("agentId")
        agentId: String,
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Page<AgentBuildInfo>
}