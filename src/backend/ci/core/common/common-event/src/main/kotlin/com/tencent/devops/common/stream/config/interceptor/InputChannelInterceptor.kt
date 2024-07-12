package com.tencent.devops.common.stream.config.interceptor

import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.MDC
import org.springframework.integration.config.GlobalChannelInterceptor
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Service

@Service
@GlobalChannelInterceptor(patterns = ["*-in*"])
class InputChannelInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        message.headers[TraceTag.X_DEVOPS_RID]?.let {
            MDC.put(TraceTag.BIZID, it.toString())
        }
        return message
    }
}
