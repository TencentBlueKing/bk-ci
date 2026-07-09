package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.constant.TapdWebhookConstant
import com.tencent.devops.process.trigger.tapd.TapdWebhookRequestService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ExternalTapdEventResourceImpl @Autowired constructor(
    private val tapdWebhookRequestService: TapdWebhookRequestService
) : ExternalTapdEventResource {

    @Value("\${tapd.webhook.secret:}")
    private val tapdWebhookSecret: String = ""

    override fun webhook(
        body: Map<String, Any>
    ): Result<Boolean> {
        if (tapdWebhookSecret.isNotBlank()) {
            val secret = body[TapdWebhookConstant.TAPD_KEY_SECRET]?.toString()
            if (secret != tapdWebhookSecret) {
                logger.warn("the secret of tapd webhook is illegal")
                throw InvalidParamException(
                    message = "secret illegal",
                    params = arrayOf("secret")
                )
            }
        }
        return tapdWebhookRequestService.dispatch(body = body)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalTapdEventResourceImpl::class.java)
    }
}
