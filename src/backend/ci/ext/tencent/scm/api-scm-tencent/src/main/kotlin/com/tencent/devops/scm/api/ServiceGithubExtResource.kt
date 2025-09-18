package com.tencent.devops.scm.api

import com.tencent.devops.scm.pojo.GithubWebhookSyncReq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_GITHUB_EXT", description = "github扩展接口")
@Path("/service/github/ext")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceGithubExtResource {

    @Operation(summary = "Github仓库提交")
    @POST
    @Path("/webhook/commit")
    fun webhookCommit(
        @Parameter(description = "事件类型", required = true)
        @HeaderParam("X-GitHub-Event")
        event: String,
        @Parameter(description = "事件ID", required = true)
        @HeaderParam("X-Github-Delivery")
        guid: String,
        @Parameter(description = "secretKey签名(sha1)", required = true)
        @HeaderParam("X-Hub-Signature")
        signature: String,
        // 这里直接用body: String, 会出现body被转义,变成"{}",而不是{},导致同步后签名失败,索引直接用对象接收
        webhookSyncReq: GithubWebhookSyncReq
    )
}
