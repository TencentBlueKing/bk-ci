package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.openapi.IgnoreProjectId
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantRevokeRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertResult
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_CREATIVE_FLOW_SHARE_V4", description = "OPENAPI-创作流分享授权")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/creative_flows/share_grants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
interface ApigwCreativeFlowShareResourceV4 {

    @Operation(summary = "批量写入创作流分享授权", tags = ["v4_app_creative_flow_share_grant_upsert"])
    @POST
    @Path("/batch_upsert")
    @IgnoreProjectId(ignore = true)
    fun upsertGrants(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "批量写入请求", required = true)
        request: CreativeFlowShareGrantUpsertRequest
    ): Result<CreativeFlowShareGrantUpsertResult>

    @Operation(
        summary = "查询创作流分享授权",
        tags = ["v4_app_creative_flow_share_grant_list", "v4_user_creative_flow_share_grant_list"]
    )
    @GET
    @Path("/")
    @IgnoreProjectId(ignore = true)
    fun listGrants(
        @Parameter(description = "appCode", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "分享命名空间ID")
        @QueryParam("shareId")
        shareId: String?,
        @Parameter(description = "分享条目ID")
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
        sourcePipelineId: String?
    ): Result<List<CreativeFlowShareGrantVo>>

    @Operation(summary = "撤销创作流分享授权", tags = ["v4_app_creative_flow_share_grant_revoke"])
    @DELETE
    @Path("/")
    @IgnoreProjectId(ignore = true)
    fun revokeGrants(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "撤销请求", required = true)
        request: CreativeFlowShareGrantRevokeRequest
    ): Result<Int>
}
