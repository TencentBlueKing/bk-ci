package com.tencent.devops.process.config

import com.tencent.devops.common.web.mq.EXTEND_RABBIT_TEMPLATE_NAME
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MeasureEventDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MeasureConfig {

    @Bean
    fun measureEventDispatcher(
        @Qualifier(EXTEND_RABBIT_TEMPLATE_NAME) extendRabbitTemplate: RabbitTemplate
    ): MeasureEventDispatcher =
        MeasureEventDispatcher(extendRabbitTemplate)
}
