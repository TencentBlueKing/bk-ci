package com.tencent.devops.process.config

import com.tencent.devops.common.web.mq.EXTEND_RABBIT_TEMPLATE_NAME
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.Resource

@Configuration
class MeasureConfig {

    @Bean
    @ConditionalOnMissingBean(MeasureEventDispatcher::class)
    fun measureEventDispatcher(
        @Autowired extendRabbitTemplate: RabbitTemplate
    ): MeasureEventDispatcher =
        MeasureEventDispatcher(extendRabbitTemplate)
}
