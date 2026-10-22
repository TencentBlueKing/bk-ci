package com.tencent.devops.environment.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_DOCKER_CONFIG", description = "服务-容器任务配置")
@Path("/service/environment/dockerConfig/agents")
@Produces(MediaType.APPLICATION_JSON)
interface ServiceDockerConfigResource {
    @GET
    @Path("/{agentId}")
    @Operation(summary = "实际分配/启动容器任务时读取节点配置，不用于Agent轮询")
    fun get(@PathParam("agentId") agentId: String): Result<AgentDockerConfig>
}
