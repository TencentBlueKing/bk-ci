package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantRevokeRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertResult
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_CREATIVE_FLOW_SHARE", description = "服务-创作流分享授权")
@Path("/service/creative/flows/share_grants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCreativeFlowShareResource {

    @Operation(summary = "批量写入创作流分享授权")
    @POST
    @Path("/batch_upsert")
    fun upsertGrants(
        @Parameter(description = "授权人（分身发布者）", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "批量写入请求", required = true)
        request: CreativeFlowShareGrantUpsertRequest
    ): Result<CreativeFlowShareGrantUpsertResult>

    @Operation(summary = "查询创作流分享授权")
    @GET
    @Path("/")
    fun listGrants(
        @Parameter(description = "查询人", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "分享ID")
        @QueryParam("shareId")
        shareId: String?,
        @Parameter(description = "创作流条目ID")
        @QueryParam("flowId")
        flowId: String?,
        @Parameter(description = "分身编码")
        @QueryParam("talentCode")
        talentCode: String?,
        @Parameter(description = "源项目ID")
        @QueryParam("sourceProjectId")
        sourceProjectId: String?,
        @Parameter(description = "源流水线ID")
        @QueryParam("sourcePipelineId")
        sourcePipelineId: String?,
        @Parameter(description = "是否包含已撤销授权，默认 false")
        @QueryParam("includeRevoked")
        @DefaultValue("false")
        includeRevoked: Boolean?
    ): Result<List<CreativeFlowShareGrantVo>>

    @Operation(summary = "撤销创作流分享授权")
    @DELETE
    @Path("/")
    fun revokeGrants(
        @Parameter(description = "操作人", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "撤销请求：shareId+flowIds 或 talentCode 二选一", required = true)
        request: CreativeFlowShareGrantRevokeRequest
    ): Result<Int>
}
