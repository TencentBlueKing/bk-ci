package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.creative.CreativeStreamStageReviewContent
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_CREATIVE_STREAM_STAGE_REVIEW", description = "服务-创作流 Stage 审核卡片")
@Path("/service/creative/stream/stage_review")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCreativeStreamStageReviewResource {

    @Operation(summary = "CDS 预定义接口：按 taskId 拉取 Stage 审核卡片内容")
    @GET
    @Path("/content")
    fun getContent(
        @Parameter(description = "调用方用户（CDS 透传 X-Bk-Username，可空）")
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @Parameter(
            description = "审批 taskId，格式 cs-stage~{projectId}~{buildId}~{stageId}~{executeCount}",
            required = true
        )
        @QueryParam("taskId")
        taskId: String
    ): Result<CreativeStreamStageReviewContent>
}
