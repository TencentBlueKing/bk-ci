package com.tencent.devops.process.trigger.api.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.webhook.pojo.code.github.GithubWebhook
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

/**
 * 与 process 的 [com.tencent.devops.process.api.service.ServiceScmWebhookResource] 路径一致，仅在 trigger 独立部署时加载
 */
@Tag(name = "SERVICE_TRIGGER_SCM", description = "服务-Trigger-SCM")
@Path("/service/scm")
@ServiceInterface("trigger")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTriggerScmWebhookResource {

    @Operation(summary = "Github仓库提交")
    @POST
    @Path("/github/commit")
    fun webHookCodeGithubCommit(
        webhook: GithubWebhook
    ): Result<Boolean>
}
