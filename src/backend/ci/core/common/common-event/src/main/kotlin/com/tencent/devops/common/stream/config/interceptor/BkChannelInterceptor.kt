package com.tencent.devops.common.stream.config.interceptor

import com.tencent.devops.common.service.trace.TraceTag
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.support.MessageHeaderAccessor

@SuppressWarnings("NestedBlockDepth", "TooGenericExceptionCaught")
class BkChannelInterceptor : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        try {
            if (channel is DirectWithAttributesChannel) {
                if (channel.beanName != null && channel.beanName.contains("-in-")) {
                    message.headers[TraceTag.X_DEVOPS_RID]?.let {
                        MDC.put(TraceTag.BIZID, it.toString())
                    }
                } else {
                    MDC.get(TraceTag.BIZID)?.ifBlank { TraceTag.buildBiz() }?.let {
                        val messageHeaderAccessor = MessageHeaderAccessor(message)
                        messageHeaderAccessor.setHeader(TraceTag.X_DEVOPS_RID, it)
                        return MessageBuilder.createMessage(message.payload, messageHeaderAccessor.messageHeaders)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("set header failed", e)
        }
        return message
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkChannelInterceptor::class.java)
    }
}
