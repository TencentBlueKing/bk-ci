package com.tencent.devops.common.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.job.api.pojo.BkJobProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered

@Configuration
@PropertySource("classpath:/common-job.properties")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class JobAutoConfiguration {

    @Bean
    @Primary
    fun jobProperties() = JobProperties()

    @Bean
    @Profile("prod")
    fun bkJobPropertiesProd(jobProperties: JobProperties) = BkJobProperties(
            jobProperties.prodUrl, jobProperties.prodLinkUrl
    )

    @Bean
    @Profile("test")
    fun bkJobPropertiesTest(jobProperties: JobProperties) = BkJobProperties(
            jobProperties.testUrl, jobProperties.testLinkUrl
    )

    @Bean
    @Profile("exp")
    fun bkJobPropertiesExp(jobProperties: JobProperties) = BkJobProperties(
            jobProperties.testUrl, jobProperties.devLinkUrl
    )

    @Bean
    @Profile("dev", "default")
    fun bkJobPropertiesDev(jobProperties: JobProperties) = BkJobProperties(
            jobProperties.devUrl, jobProperties.devLinkUrl
    )

    @Bean
    @Profile("qcloud")
    fun bkJobPropertiesQcloud(jobProperties: JobProperties) = BkJobProperties(
            jobProperties.testUrl, jobProperties.testLinkUrl
    )

    @Bean
    @ConditionalOnMissingBean(JobClient::class)
    fun jobClient(objectMapper: ObjectMapper, @Autowired bkJobProperties: BkJobProperties) = JobClient(objectMapper, bkJobProperties)
}
