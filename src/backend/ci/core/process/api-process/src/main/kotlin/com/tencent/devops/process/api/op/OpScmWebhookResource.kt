package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_WEBHOOK", description = "用户-webhook")
@Path("/op/webhook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpScmWebhookResource {

    @Operation(summary = "更新所有的webhook项目名")
    @PUT
    @Path("/updateProjectNameAndTaskId")
    fun updateProjectNameAndTaskId(): Result<Boolean>

    @Operation(summary = "更新webhook secret")
    @PUT
    @Path("/updateWebhookSecret")
    fun updateWebhookSecret(
        @Parameter(description = "代码库请求类型", required = false)
        @QueryParam("scmType")
        scmType: String
    ): Result<Boolean>

    @Operation(summary = "更新webhook 事件信息")
    @PUT
    @Path("/updateWebhookEventInfo")
    fun updateWebhookEventInfo(
        @Parameter(description = "待更新的项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Boolean>

    @Operation(summary = "更新webhook projectName")
    @PUT
    @Path("/updateWebhookProjectName")
    fun updateWebhookProjectName(
        @Parameter(description = "待更新的项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?
    ): Result<Boolean>
}
