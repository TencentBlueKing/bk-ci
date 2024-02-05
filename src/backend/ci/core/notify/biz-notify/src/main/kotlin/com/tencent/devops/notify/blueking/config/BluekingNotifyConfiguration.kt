package com.tencent.devops.notify.blueking.config

import com.tencent.devops.notify.blueking.sdk.CMSApi
import com.tencent.devops.notify.blueking.sdk.pojo.NotifyProperties
import com.tencent.devops.notify.blueking.service.inner.BlueKingWeworkServiceImpl
import com.tencent.devops.notify.blueking.service.inner.EmailServiceImpl
import com.tencent.devops.notify.blueking.service.inner.RtxServiceImpl
import com.tencent.devops.notify.blueking.service.inner.SmsServiceImpl
import com.tencent.devops.notify.blueking.service.inner.VoiceServiceImpl
import com.tencent.devops.notify.blueking.service.inner.WechatServiceImpl
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.dao.RtxNotifyDao
import com.tencent.devops.notify.dao.SmsNotifyDao
import com.tencent.devops.notify.dao.VoiceNotifyDao
import com.tencent.devops.notify.dao.WechatNotifyDao
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.RtxService
import com.tencent.devops.notify.service.SmsService
import com.tencent.devops.notify.service.VoiceService
import com.tencent.devops.notify.service.WechatService
import com.tencent.devops.notify.service.WeworkService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.stream.function.StreamBridge
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
    fun cms(notifyProperties: NotifyProperties): CMSApi = CMSApi(notifyProperties)

    @Bean
    fun configuration() = com.tencent.devops.common.notify.utils.Configuration()

    @Bean
    fun notifyService(@Autowired cmsApi: CMSApi) = NotifyService(cmsApi)

    @Bean
    @ConditionalOnMissingBean(EmailService::class)
    fun emailService(
        @Autowired notifyService: NotifyService,
        @Autowired emailNotifyDao: EmailNotifyDao,
        @Autowired streamBridge: StreamBridge,
        @Autowired configuration: NotifyConfig
    ) = EmailServiceImpl(notifyService, emailNotifyDao, streamBridge, configuration)

    @Bean
    @ConditionalOnMissingBean(WeworkService::class)
    fun weworkService() = BlueKingWeworkServiceImpl()

    @Bean
    @ConditionalOnMissingBean(RtxService::class)
    fun rtxService(
        @Autowired notifyService: NotifyService,
        @Autowired rtxNotifyDao: RtxNotifyDao,
        @Autowired streamBridge: StreamBridge,
        @Autowired configuration: NotifyConfig
    ) = RtxServiceImpl(notifyService, rtxNotifyDao, streamBridge, configuration)

    @Bean
    @ConditionalOnMissingBean(SmsService::class)
    fun smsService(
        @Autowired notifyService: NotifyService,
        @Autowired smsNotifyDao: SmsNotifyDao,
        @Autowired streamBridge: StreamBridge,
        @Autowired configuration: NotifyConfig
    ) = SmsServiceImpl(notifyService, smsNotifyDao, streamBridge, configuration)

    @Bean
    @ConditionalOnMissingBean(WechatService::class)
    fun wechatService(
        @Autowired notifyService: NotifyService,
        @Autowired wechatNotifyDao: WechatNotifyDao,
        @Autowired streamBridge: StreamBridge,
        @Autowired configuration: NotifyConfig
    ) = WechatServiceImpl(notifyService, wechatNotifyDao, streamBridge, configuration)

    @Bean
    @ConditionalOnMissingBean(VoiceService::class)
    fun voiceService(
        @Autowired notifyService: NotifyService,
        @Autowired voiceNotifyDao: VoiceNotifyDao,
        @Autowired streamBridge: StreamBridge,
        @Autowired configuration: NotifyConfig,
        @Autowired dslContext: DSLContext
    ) = VoiceServiceImpl(notifyService, voiceNotifyDao, streamBridge, configuration, dslContext)
}
