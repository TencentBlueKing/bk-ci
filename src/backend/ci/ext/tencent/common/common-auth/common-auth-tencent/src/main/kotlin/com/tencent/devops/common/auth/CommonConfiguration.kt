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
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.jmx.export.MBeanExporter
import java.util.Arrays

@Configuration
@AutoConfigureBefore(name = ["com.tencent.devops.common.auth.MockAuthAutoConfiguration"])
class CommonConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun managerService(client: Client) = ManagerService(client)

    @Bean
    fun jmxAuthApi(mBeanExporter: MBeanExporter) = JmxAuthApi(mBeanExporter)

    @Bean
    fun bcsAuthServiceCode() = BSBcsAuthServiceCode()

    @Bean
    fun bsPipelineAuthServiceCode() = BSPipelineAuthServiceCode()

    @Bean
    fun codeAuthServiceCode() = BSCodeAuthServiceCode()

    @Bean
    fun vsAuthServiceCode() = BSVSAuthServiceCode()

    @Bean
    fun environmentAuthServiceCode() = BSEnvironmentAuthServiceCode()

    @Bean
    fun repoAuthServiceCode() = BSRepoAuthServiceCode()

    @Bean
    fun ticketAuthServiceCode() = BSTicketAuthServiceCode()

    @Bean
    fun qualityAuthServiceCode() = BSQualityAuthServiceCode()

    @Bean
    fun wetestAuthServiceCode() = BSWetestAuthServiceCode()

    @Bean
    fun experienceAuthServiceCode() = BSExperienceAuthServiceCode()

    @Bean
    fun projectAuthSeriviceCode() = BSProjectServiceCodec()

    @Bean
    fun artifactoryAuthServiceCode() = BSArtifactoryAuthServiceCode()

    @Bean
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
