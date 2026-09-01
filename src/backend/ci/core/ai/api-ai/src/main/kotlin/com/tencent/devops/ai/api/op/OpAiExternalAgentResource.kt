package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.ExternalAgentCreate
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import com.tencent.devops.ai.pojo.ExternalAgentUpdate
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AI_EXTERNAL_AGENT", description = "运营-外部智能体管理")
@Path("/op/ai/external/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiExternalAgentResource {

    @Operation(summary = "外部智能体列表")
    @GET
    @Path("/")
    fun list(): Result<List<ExternalAgentInfo>>

    @Operation(summary = "新增外部智能体")
    @POST
    @Path("/")
    fun create(request: ExternalAgentCreate): Result<ExternalAgentInfo>

    @Operation(summary = "更新外部智能体")
    @PUT
    @Path("/{configId}")
    fun update(
        @Parameter(description = "配置ID", required = true)
        @PathParam("configId")
        configId: String,
        request: ExternalAgentUpdate
    ): Result<Boolean>

    @Operation(summary = "删除外部智能体")
    @DELETE
    @Path("/{configId}")
    fun delete(
        @Parameter(description = "配置ID", required = true)
        @PathParam("configId")
        configId: String
    ): Result<Boolean>
}
