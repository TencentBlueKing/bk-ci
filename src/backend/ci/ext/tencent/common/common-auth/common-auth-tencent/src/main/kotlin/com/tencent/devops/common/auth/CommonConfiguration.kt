package com.tencent.devops.common.auth

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.common.auth.code.BSArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.BSBcsAuthServiceCode
import com.tencent.devops.common.auth.code.BSCodeAuthServiceCode
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import com.tencent.devops.common.auth.code.BSEnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.auth.code.BSQualityAuthServiceCode
import com.tencent.devops.common.auth.code.BSRepoAuthServiceCode
import com.tencent.devops.common.auth.code.BSTicketAuthServiceCode
import com.tencent.devops.common.auth.code.BSVSAuthServiceCode
import com.tencent.devops.common.auth.code.BSWetestAuthServiceCode
import com.tencent.devops.common.auth.jmx.JmxAuthApi
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jmx.export.MBeanExporter

@Configuration
class CommonConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    fun jmxAuthApi(mBeanExporter: MBeanExporter) = JmxAuthApi(mBeanExporter)

    @Bean
    @Primary
    fun bcsAuthServiceCode() = BSBcsAuthServiceCode()

    @Bean
    @Primary
    fun bsPipelineAuthServiceCode() = BSPipelineAuthServiceCode()

    @Bean
    @Primary
    fun codeAuthServiceCode() = BSCodeAuthServiceCode()

    @Bean
    @Primary
    fun vsAuthServiceCode() = BSVSAuthServiceCode()

    @Bean
    @Primary
    fun environmentAuthServiceCode() = BSEnvironmentAuthServiceCode()

    @Bean
    @Primary
    fun repoAuthServiceCode() = BSRepoAuthServiceCode()

    @Bean
    @Primary
    fun ticketAuthServiceCode() = BSTicketAuthServiceCode()

    @Bean
    @Primary
    fun qualityAuthServiceCode() = BSQualityAuthServiceCode()

    @Bean
    @Primary
    fun wetestAuthServiceCode() = BSWetestAuthServiceCode()

    @Bean
    @Primary
    fun experienceAuthServiceCode() = BSExperienceAuthServiceCode()

    @Bean
    @Primary
    fun projectAuthSeriviceCode() = BSProjectServiceCodec()

    @Bean
    @Primary
    fun artifactoryAuthServiceCode() = BSArtifactoryAuthServiceCode()

    @Bean
    @Primary
    fun commonAuthServiceCode() = BSCommonAuthServiceCode()

    @Value("\${auth.url:}")
    val iamBaseUrl = ""

    @Value("\${auth.iamSystem:}")
    val systemId = ""

    @Value("\${auth.appCode:}")
    val appCode = ""

    @Value("\${auth.appSecret:}")
    val appSecret = ""

    @Value("\${auth.apigwUrl:#{null}}")
    val iamApigw = ""

    @Bean
    @ConditionalOnMissingBean
    fun iamConfiguration() = IamConfiguration(systemId, appCode, appSecret, iamBaseUrl, iamApigw)
}
