package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.AiSkillCreate
import com.tencent.devops.ai.pojo.AiSkillInfo
import com.tencent.devops.ai.pojo.AiSkillUpdate
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

@Tag(name = "OP_AI_SKILL", description = "运营-系统技能管理")
@Path("/op/ai/skills")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiSkillResource {

    @Operation(summary = "技能列表（含 SYSTEM / USER）")
    @GET
    @Path("/")
    fun list(): Result<List<AiSkillInfo>>

    @Operation(summary = "新增系统技能")
    @POST
    @Path("/")
    fun create(request: AiSkillCreate): Result<AiSkillInfo>

    @Operation(summary = "更新技能")
    @PUT
    @Path("/{skillId}")
    fun update(
        @Parameter(description = "技能ID", required = true)
        @PathParam("skillId")
        skillId: String,
        request: AiSkillUpdate
    ): Result<Boolean>

    @Operation(summary = "删除技能")
    @DELETE
    @Path("/{skillId}")
    fun delete(
        @Parameter(description = "技能ID", required = true)
        @PathParam("skillId")
        skillId: String
    ): Result<Boolean>
}
