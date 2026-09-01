package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.AiPromptInfo
import com.tencent.devops.ai.pojo.UserLlmConfigInfo
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AI_USER_AUDIT", description = "运营-用户自助配置审计")
@Path("/op/ai/user-audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiUserAuditResource {

    @Operation(summary = "用户提示词列表")
    @GET
    @Path("/prompts")
    fun listPrompts(): Result<List<AiPromptInfo>>

    @Operation(summary = "删除用户提示词")
    @DELETE
    @Path("/prompts/{promptId}")
    fun deletePrompt(
        @Parameter(description = "提示词ID", required = true)
        @PathParam("promptId")
        promptId: String
    ): Result<Boolean>

    @Operation(summary = "用户 LLM 配置列表（密钥已脱敏）")
    @GET
    @Path("/llm-configs")
    fun listLlmConfigs(): Result<List<UserLlmConfigInfo>>

    @Operation(summary = "删除用户 LLM 配置")
    @DELETE
    @Path("/llm-configs/{userId}")
    fun deleteLlmConfig(
        @Parameter(description = "用户ID", required = true)
        @PathParam("userId")
        userId: String
    ): Result<Boolean>
}
