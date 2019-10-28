package com.tencent.devops.common.wechatwork

import com.tencent.ops.common.wechatwork.WechatWorkConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered

@Configuration
@PropertySource("classpath:/common-wechatwork.properties")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class WechatWorkAutoConfiguration {
    @Bean
    @Primary
    fun wechatWorkProperties() = WechatWorkProperties()

    @Bean
    @Profile("dev", "default", "test", "qcloud", "exp")
    fun wechatWorkConfigurationDev(wechatWorkProperties: WechatWorkProperties) = WechatWorkConfiguration(
            wechatWorkProperties.devCorpId,
            wechatWorkProperties.devServiceId,
            wechatWorkProperties.devSecret,
            wechatWorkProperties.devToken,
            wechatWorkProperties.devAesKey,
            wechatWorkProperties.devUrl

    )

    @Bean
    @Profile("prod")
    fun wechatWorkConfigurationProd(wechatWorkProperties: WechatWorkProperties) = WechatWorkConfiguration(
            wechatWorkProperties.prodCorpId,
            wechatWorkProperties.prodServiceId,
            wechatWorkProperties.prodSecret,
            wechatWorkProperties.prodToken,
            wechatWorkProperties.prodAesKey,
            wechatWorkProperties.prodUrl
    )

    @Bean
    @Primary
    fun wechatWorkService(wechatWorkConfiguration: WechatWorkConfiguration) = WechatWorkService(wechatWorkConfiguration)
}