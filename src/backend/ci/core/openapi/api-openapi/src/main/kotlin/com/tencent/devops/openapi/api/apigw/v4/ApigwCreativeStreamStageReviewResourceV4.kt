package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.openapi.IgnoreProjectId
import com.tencent.devops.process.pojo.creative.CreativeStreamStageReviewContent
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_CREATIVE_STREAM_STAGE_REVIEW_V4", description = "OPENAPI-创作流 Stage 审核卡片")
@Path("/{apigwType:apigw-app|apigw}/v4/creative_stream/stage_review_content")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v4")
interface ApigwCreativeStreamStageReviewResourceV4 {

    @Operation(summary = "CDS 预定义接口：拉取创作流 Stage 审核卡片", tags = ["v4_app_creative_stream_stage_review_content"])
    @GET
    @Path("/")
    @IgnoreProjectId(ignore = true)
    fun getContent(
        @Parameter(description = "appCode", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID（CDS 透传，可空）", required = false)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        @Parameter(description = "审批 taskId", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<CreativeStreamStageReviewContent>
}
