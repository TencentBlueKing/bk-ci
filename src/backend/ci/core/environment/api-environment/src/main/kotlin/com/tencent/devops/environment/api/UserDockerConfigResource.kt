package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.environment.pojo.BatchDockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigScope
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_DOCKER_CONFIG", description = "用户-环境和节点容器配置")
@Path("/user/environment/dockerConfig/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDockerConfigResource {
    @GET
    @Path("/{scope}/{resourceId}")
    @Operation(summary = "查询容器配置；不存在时返回Docker和不限")
    fun get(
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("scope") scope: DockerConfigScope,
        @PathParam("resourceId") resourceId: String
    ): Result<AgentDockerConfig>

    @POST
    @Path("/{scope}/{resourceId}")
    @Operation(summary = "修改容器配置；null保持不变，空字符串清空")
    fun save(
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("scope") scope: DockerConfigScope,
        @PathParam("resourceId") resourceId: String,
        request: DockerConfigRequest
    ): Result<Boolean>

    @POST
    @Path("/nodes/batch")
    @Operation(summary = "批量修改Linux构建节点容器上限，返回修改数量")
    fun batch(
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        request: BatchDockerConfigRequest
    ): Result<Int>
}
