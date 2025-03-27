package com.tencent.devops.common.stream.customizer

import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint
import org.springframework.messaging.MessageHandler

class BkProducerMessageHandlerCustomizer : ProducerMessageHandlerCustomizer<MessageHandler> {
    override fun configure(handler: MessageHandler, destinationName: String) {
        if (handler is AmqpOutboundEndpoint) {
            logger.info("handler customizer success , destinationName: $destinationName , handler: ${handler.beanName}")
            handler.rabbitTemplate?.isUsePublisherConnection = false // 生产者和消费者共享连接池, 提高性能
        }else{
            logger.info("handler customizer failed , destinationName: $destinationName , handler: ${handler.javaClass}")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkProducerMessageHandlerCustomizer::class.java)
    }
}