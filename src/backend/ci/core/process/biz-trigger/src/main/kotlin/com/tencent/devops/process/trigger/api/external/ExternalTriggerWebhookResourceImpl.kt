package com.tencent.devops.process.trigger.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.trigger.artifact.ArtifactWebhookRequestService
import com.tencent.devops.process.trigger.tapd.TapdWebhookRequestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * artifact 路径与 process 的 ExternalWebhookResourceImpl 相同，同进程加载会导致 Jersey 资源冲突，
 * 只能在 trigger 独立部署（trigger.standalone.enabled=true）时加载
 */
@RestResource
@ConditionalOnProperty(prefix = "trigger.standalone", name = ["enabled"], havingValue = "true")
class ExternalTriggerWebhookResourceImpl @Autowired constructor(
    private val artifactWebhookRequestService: ArtifactWebhookRequestService,
    private val tapdWebhookRequestService: TapdWebhookRequestService
) : ExternalTriggerWebhookResource {

    override fun artifactWebhook(
        eventType: String?,
        secret: String?,
        body: String
    ): Result<Boolean> {
        return Result(
            artifactWebhookRequestService.receive(eventType = eventType, secret = secret, body = body)
        )
    }

    override fun tapdWebhook(body: Map<String, Any>): Result<Boolean> {
        return Result(tapdWebhookRequestService.receive(body = body))
    }
}
