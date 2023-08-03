package com.tencent.devops.notify.config

import com.tencent.devops.common.notify.utils.TOF4Service
import com.tencent.devops.common.notify.utils.TOFConfiguration
import com.tencent.devops.common.notify.utils.TOFService
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.inner.EmailServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["emailChannel"], havingValue = "blueking")
class PrimaryEmailConfiguration {

    private val logger = LoggerFactory.getLogger(PrimaryEmailConfiguration::class.java)

    @Bean
    @Primary
    fun emailService(
        tofService: TOFService,
        emailNotifyDao: EmailNotifyDao,
        rabbitTemplate: RabbitTemplate,
        configuration: TOFConfiguration,
        tof4Service: TOF4Service
    ): EmailService {
        logger.info("init bk email tof service")
        return EmailServiceImpl(tofService, emailNotifyDao, rabbitTemplate, configuration, tof4Service)
    }
}
