package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.AiOverviewVO
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AI_OVERVIEW", description = "运营-智能体管理概览")
@Path("/op/ai/overview")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiOverviewResource {

    @Operation(summary = "智能体管理资源计数")
    @GET
    @Path("/")
    fun get(): Result<AiOverviewVO>
}
