package com.tencent.devops.process.config

import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MeasureConfig {

    @Bean
    fun measureEventDispatcher(
        @Autowired extendRabbitTemplate: RabbitTemplate
    ): MeasureEventDispatcher =
        MeasureEventDispatcher(extendRabbitTemplate)
}
