package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.trigger.tapd.TapdWebhookRequestService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalTapdEventResourceImpl @Autowired constructor(
    private val tapdWebhookRequestService: TapdWebhookRequestService
) : ExternalTapdEventResource {

    override fun webhook(
        body: Map<String, Any>
    ): Result<Boolean> = tapdWebhookRequestService.dispatch(
        body = body
    )
}
