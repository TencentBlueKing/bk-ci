package com.tencent.devops.common.notify.blueking

import com.tencent.devops.common.notify.blueking.sdk.CMSApi
import com.tencent.devops.common.notify.blueking.sdk.Properties
import com.tencent.devops.common.notify.blueking.sdk.pojo.NotifyProperties
import com.tencent.devops.common.notify.blueking.sdk.utils.NotifyUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:/common-notify.properties")
class CMSAutoConfiguration {

    @Bean
    @Primary
    fun properties(): Properties = Properties()

    @Bean
    @Primary
    fun notifyProperties(properties: Properties): NotifyProperties =
            NotifyProperties(properties.appCode, properties.appSecret, properties.bkHost)

    @Bean
    @Primary
    fun notifyUtils(notifyProperties: NotifyProperties): NotifyUtils =
            NotifyUtils(notifyProperties)

    @Bean
    @Primary
    fun cms(notifyUtils: NotifyUtils): CMSApi = CMSApi(notifyUtils)

    @Bean
    fun toFService(cmsApi: CMSApi): NotifyService = NotifyService(cmsApi)

    @Bean
    fun tofConfiguration() = com.tencent.devops.common.notify.blueking.Configuration()
}