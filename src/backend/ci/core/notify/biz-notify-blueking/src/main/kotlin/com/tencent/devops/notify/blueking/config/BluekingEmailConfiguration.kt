package com.tencent.devops.notify.blueking.config

import com.tencent.devops.notify.blueking.service.inner.EmailServiceImpl
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.dao.EmailNotifyDao
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.tencent.devops.common.notify.utils.Configuration as NotifyConfig

@Configuration
class BluekingEmailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "notify", name = ["emailChannel"], havingValue = "blueking")
    fun emailService(
        notifyService: NotifyService,
        emailNotifyDao: EmailNotifyDao,
        streamBridge: StreamBridge,
        configuration: NotifyConfig
    ) = EmailServiceImpl(notifyService, emailNotifyDao, streamBridge, configuration)
}
