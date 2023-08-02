package com.tencent.devops.notify.tencentcloud.config

import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.tencentcloud.service.inner.TencentCloudEmailServiceImpl
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["emailChannel"], havingValue = "tencentCloud")
class TencentCloudEmailConfiguration {

    @Bean
    fun tencentCloudConfiguration(
        @Value("\${notify.tencentCloud.secretId:}")
        secretId: String = "",
        @Value("\${notify.tencentCloud.secretKey:}")
        secretKey: String = "",
        @Value("\${notify.tencentCloud.emailRegion:ap-hongkong}")
        emailRegion: String = "ap-hongkong",
        @Value("\${notify.tencentCloud.emailSender:tencent-ci@mail.ci.tencent.com}")
        emailSender: String = "tencent-ci@mail.ci.tencent.com"
    ) =
        TencentCloudConfiguration(secretId, secretKey, emailRegion, emailSender)

    @Bean
    fun emailService(
        @Autowired emailNotifyDao: EmailNotifyDao,
        @Autowired rabbitTemplate: RabbitTemplate,
        @Autowired configuration: TencentCloudConfiguration
    ) = TencentCloudEmailServiceImpl(emailNotifyDao, rabbitTemplate, configuration)
}
