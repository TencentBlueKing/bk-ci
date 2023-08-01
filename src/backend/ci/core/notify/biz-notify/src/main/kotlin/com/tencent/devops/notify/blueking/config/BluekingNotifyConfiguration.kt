package com.tencent.devops.notify.blueking.config

import com.tencent.devops.notify.blueking.sdk.CMSApi
import com.tencent.devops.notify.blueking.sdk.pojo.NotifyProperties
import com.tencent.devops.notify.blueking.sdk.utils.NotifyUtils
import com.tencent.devops.notify.blueking.service.inner.BlueKingWeworkServiceImpl
import com.tencent.devops.notify.blueking.service.inner.EmailServiceImpl
import com.tencent.devops.notify.blueking.service.inner.OrgServiceImpl
import com.tencent.devops.notify.blueking.service.inner.RtxServiceImpl
import com.tencent.devops.notify.blueking.service.inner.SmsServiceImpl
import com.tencent.devops.notify.blueking.service.inner.WechatServiceImpl
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.dao.RtxNotifyDao
import com.tencent.devops.notify.dao.SmsNotifyDao
import com.tencent.devops.notify.dao.WechatNotifyDao
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.tencent.devops.common.notify.utils.Configuration as NotifyConfig

@Suppress("TooManyFunctions")
@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["emailChannel"], havingValue = "blueking")
class BluekingNotifyConfiguration {

    @Bean
    fun notifyProperties(
        @Value("\${esb.code:#{null}}")
        appCode: String? = null,
        @Value("\${esb.secret:#{null}}")
        appSecret: String? = null,
        @Value("\${bk.paas.host:#{null}}")
        bkHost: String? = null
    ): NotifyProperties =
        NotifyProperties(appCode = appCode, appSecret = appSecret, bkHost = bkHost)

    @Bean
    fun notifyUtils(notifyProperties: NotifyProperties): NotifyUtils =
        NotifyUtils(notifyProperties)

    @Bean
    fun cms(notifyUtils: NotifyUtils): CMSApi = CMSApi(notifyUtils)

    @Bean
    fun configuration() = com.tencent.devops.common.notify.utils.Configuration()

    @Bean
    fun notifyService(@Autowired cmsApi: CMSApi) = NotifyService(cmsApi)

    @Bean
    fun emailService(
        @Autowired notifyService: NotifyService,
        @Autowired emailNotifyDao: EmailNotifyDao,
        @Autowired rabbitTemplate: RabbitTemplate,
        @Autowired configuration: NotifyConfig
    ) = EmailServiceImpl(notifyService, emailNotifyDao, rabbitTemplate, configuration)

    @Bean
    fun weworkService() = BlueKingWeworkServiceImpl()

    @Bean
    fun orgService() = OrgServiceImpl()

    @Bean
    fun rtxService(
        @Autowired notifyService: NotifyService,
        @Autowired rtxNotifyDao: RtxNotifyDao,
        @Autowired rabbitTemplate: RabbitTemplate,
        @Autowired configuration: NotifyConfig
    ) = RtxServiceImpl(notifyService, rtxNotifyDao, rabbitTemplate, configuration)

    @Bean
    fun smsService(
        @Autowired notifyService: NotifyService,
        @Autowired smsNotifyDao: SmsNotifyDao,
        @Autowired rabbitTemplate: RabbitTemplate,
        @Autowired configuration: NotifyConfig
    ) = SmsServiceImpl(notifyService, smsNotifyDao, rabbitTemplate, configuration)

    @Bean
    fun wechatService(
        @Autowired notifyService: NotifyService,
        @Autowired wechatNotifyDao: WechatNotifyDao,
        @Autowired rabbitTemplate: RabbitTemplate,
        @Autowired configuration: NotifyConfig
    ) = WechatServiceImpl(notifyService, wechatNotifyDao, rabbitTemplate, configuration)
}
