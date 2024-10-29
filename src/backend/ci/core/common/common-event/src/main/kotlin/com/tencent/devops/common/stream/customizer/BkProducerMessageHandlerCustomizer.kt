package com.tencent.devops.common.stream.customizer

import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint

class BkProducerMessageHandlerCustomizer : ProducerMessageHandlerCustomizer<AmqpOutboundEndpoint> {
    override fun configure(handler: AmqpOutboundEndpoint, destinationName: String) {
        logger.info("handler customizer , destinationName: $destinationName , handler: ${handler.beanName}")
        handler.rabbitTemplate?.isUsePublisherConnection = false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkProducerMessageHandlerCustomizer::class.java)
    }
}