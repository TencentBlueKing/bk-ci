package com.tencent.devops.ai.api.user

import com.tencent.devops.ai.pojo.UserLlmConfigInfo
import com.tencent.devops.ai.pojo.UserLlmConfigUpsertRequest
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_AI_LLM_CONFIG", description = "用户-AI自定义大模型配置")
@Path("/user/ai/llm/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAiLlmConfigResource {

    @Operation(summary = "获取用户自定义大模型配置")
    @GET
    @Path("/")
    fun get(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<UserLlmConfigInfo?>

    @Operation(summary = "保存自定义大模型配置")
    @PUT
    @Path("/")
    fun upsert(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "配置请求体", required = true)
        request: UserLlmConfigUpsertRequest
    ): Result<UserLlmConfigInfo>

    @Operation(summary = "删除自定义大模型配置")
    @DELETE
    @Path("/")
    fun delete(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>
}
