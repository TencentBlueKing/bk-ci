package com.tencent.devops.process.config

import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SampleMeasureConfig {

    @Bean
    @ConditionalOnMissingBean(MeasureEventDispatcher::class)
    fun measureEventDispatcher(rabbitTemplate: RabbitTemplate) = MeasureEventDispatcher(rabbitTemplate)
}
