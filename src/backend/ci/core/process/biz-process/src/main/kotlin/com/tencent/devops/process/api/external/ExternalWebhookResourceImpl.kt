package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.trigger.event.ArtifactWebhookRequestEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ExternalWebhookResourceImpl @Autowired constructor(
    private val sampleEventDispatcher: SampleEventDispatcher
) : ExternalWebhookResource {

    @Value("\${external.webhook.artifact.secret:#{null}}")
    private val artifactWebhookSecret: String? = null

    override fun artifactWebhook(
        eventType: String?,
        secret: String?,
        body: String
    ): Result<Boolean> {
        // body 为原始 JSON 字符串，可能带缩进/换行；打印前压成紧凑单行，解析失败则回退原文
        val compactBody = runCatching {
            JsonUtil.getObjectMapper(formatted = false).readTree(body).toString()
        }.getOrDefault(body)
        logger.info("artifact webhook request|$eventType|$compactBody")
        if (!artifactWebhookSecret.isNullOrBlank() && secret != artifactWebhookSecret) {
            logger.warn("the secret of artifact webhook is illegal")
            throw InvalidParamException(
                message = "secret illegal",
                params = arrayOf("secret")
            )
        }
        if (eventType.isNullOrBlank()) {
            logger.warn("the eventType of artifact webhook is missing")
            throw InvalidParamException(
                message = "eventType illegal",
                params = arrayOf("eventType")
            )
        }
        sampleEventDispatcher.dispatch(
            ArtifactWebhookRequestEvent(
                eventType = eventType,
                request = WebhookRequest(
                    headers = emptyMap(),
                    body = compactBody
                )
            )
        )
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalWebhookResourceImpl::class.java)
    }
}
