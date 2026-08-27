package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.AiKbCatalogVO
import com.tencent.devops.ai.pojo.AiKbEntryUpsertRequest
import com.tencent.devops.ai.pojo.AiKbEntryVO
import com.tencent.devops.ai.pojo.AiKbSourceUpsertRequest
import com.tencent.devops.ai.pojo.AiKbSourceVO
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

@Tag(name = "OP_AI_KB_CATALOG", description = "运营-知识库目录（含项目自有知识库）")
@Path("/op/ai/kb-catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiKbCatalogResource {

    @Operation(summary = "知识库与目录条目")
    @GET
    @Path("/")
    fun list(): Result<AiKbCatalogVO>

    @Operation(summary = "新增知识库。projectId 为空=平台级，有值=项目自有")
    @POST
    @Path("/sources")
    fun createSource(request: AiKbSourceUpsertRequest): Result<AiKbSourceVO>

    @Operation(summary = "更新知识库")
    @PUT
    @Path("/sources/{sourceId}")
    fun updateSource(
        @Parameter(description = "知识库ID", required = true)
        @PathParam("sourceId")
        sourceId: String,
        request: AiKbSourceUpsertRequest
    ): Result<Boolean>

    @Operation(summary = "删除知识库及其条目")
    @DELETE
    @Path("/sources/{sourceId}")
    fun deleteSource(
        @Parameter(description = "知识库ID", required = true)
        @PathParam("sourceId")
        sourceId: String
    ): Result<Boolean>

    @Operation(summary = "新增目录条目")
    @POST
    @Path("/entries")
    fun createEntry(request: AiKbEntryUpsertRequest): Result<AiKbEntryVO>

    @Operation(summary = "更新目录条目")
    @PUT
    @Path("/entries/{entryId}")
    fun updateEntry(
        @Parameter(description = "条目ID", required = true)
        @PathParam("entryId")
        entryId: String,
        request: AiKbEntryUpsertRequest
    ): Result<Boolean>

    @Operation(summary = "删除目录条目")
    @DELETE
    @Path("/entries/{entryId}")
    fun deleteEntry(
        @Parameter(description = "条目ID", required = true)
        @PathParam("entryId")
        entryId: String
    ): Result<Boolean>
}
