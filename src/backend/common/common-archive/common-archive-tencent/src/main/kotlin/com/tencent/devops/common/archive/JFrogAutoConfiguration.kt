package com.tencent.devops.common.archive

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.archive.api.JFrogConfigProperties
import com.tencent.devops.common.archive.api.JFrogExecutionApi
import com.tencent.devops.common.archive.api.JFrogPropertiesApi
import com.tencent.devops.common.archive.api.JFrogStorageApi
import com.tencent.devops.common.archive.client.JfrogService
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered

@Configuration
@PropertySource("classpath:/common-jfrog.properties")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class JFrogAutoConfiguration {

    @Bean
    @Primary
    fun jFrogAllConfigProperties() = JFrogAllConfigProperties()

    @Bean
    @Profile("prod", "nobuild_prod", "nobuild_prod_gray")
    fun jFrogConfigPropertiesProd(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
            jFrogAllConfigProperties.prodUrl,
            jFrogAllConfigProperties.prodUsername,
            jFrogAllConfigProperties.prodPassword
    )

    @Bean
    @Profile("test", "nobuild_test", "nobuild_test_gray")
    fun jFrogConfigPropertiesTest(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
            jFrogAllConfigProperties.testUrl,
            jFrogAllConfigProperties.testUsername,
            jFrogAllConfigProperties.testPassword
    )

    @Bean
    @Profile("exp")
    fun jFrogConfigPropertiesExp(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
            jFrogAllConfigProperties.devUrl,
            jFrogAllConfigProperties.devUsername,
            jFrogAllConfigProperties.devPassword
    )

    @Bean
    @Profile("dev", "default", "nobuild_dev")
    fun jFrogConfigPropertiesDev(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
            jFrogAllConfigProperties.devUrl,
            jFrogAllConfigProperties.devUsername,
            jFrogAllConfigProperties.devPassword
    )

    @Bean
    @Profile("qcloud")
    fun jFrogConfigPropertiesQcloud(jFrogAllConfigProperties: JFrogAllConfigProperties) = JFrogConfigProperties(
            jFrogAllConfigProperties.devUrl,
            jFrogAllConfigProperties.devUsername,
            jFrogAllConfigProperties.devPassword
    )

    @Bean
    @Primary
    fun jFrogStorageApi(jFrogConfigProperties: JFrogConfigProperties, objectMapper: ObjectMapper): JFrogStorageApi =
            JFrogStorageApi(jFrogConfigProperties, objectMapper)

    @Bean
    @Primary
    fun jFrogExecutionApi(jFrogConfigProperties: JFrogConfigProperties, objectMapper: ObjectMapper): JFrogExecutionApi =
            JFrogExecutionApi(jFrogConfigProperties, objectMapper)

    @Bean
    @Primary
    fun jFrogPropertiesApi(jFrogConfigProperties: JFrogConfigProperties, objectMapper: ObjectMapper): JFrogPropertiesApi =
            JFrogPropertiesApi(jFrogConfigProperties, objectMapper)

    @Bean
    @Primary
    fun jFrogService() = JfrogService()
}
