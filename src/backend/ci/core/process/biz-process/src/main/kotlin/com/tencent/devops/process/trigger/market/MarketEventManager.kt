package com.tencent.devops.process.trigger.market

import com.tencent.devops.process.trigger.event.GenericWebhookRequestEvent
import com.tencent.devops.process.trigger.event.RemoteDevWebhookRequestEvent
import org.springframework.stereotype.Service

@Service
class MarketEventManager {

    fun handleRemoteDevWebhookRequestEvent(event: RemoteDevWebhookRequestEvent) {

    }

    fun handleGenericWebhookRequestEvent(event: GenericWebhookRequestEvent) {

    }
}
