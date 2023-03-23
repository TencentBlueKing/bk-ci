package com.tencent.devops.notify.tencentcloud.config

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.tencentcloud.service.inner.TencentCloudEmailServiceImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TencentClouldEmailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "notify", name = ["emailChannel"], havingValue = "tencentCloud")
    fun emailService(
        emailNotifyDao: EmailNotifyDao,
        eventDispatcher: SampleEventDispatcher,
        configuration: TencentCloudConfiguration
    ) = TencentCloudEmailServiceImpl(emailNotifyDao, eventDispatcher, configuration)
}
