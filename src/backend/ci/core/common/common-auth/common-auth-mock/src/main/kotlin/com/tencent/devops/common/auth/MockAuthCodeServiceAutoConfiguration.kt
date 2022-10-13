package com.tencent.devops.common.auth

import com.tencent.devops.common.auth.code.MockArtifactoryAuthServiceCode
import com.tencent.devops.common.auth.code.MockBcsAuthServiceCode
import com.tencent.devops.common.auth.code.MockCodeAuthServiceCode
import com.tencent.devops.common.auth.code.MockEnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.MockPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.MockProjectAuthServiceCode
import com.tencent.devops.common.auth.code.MockQualityAuthServiceCode
import com.tencent.devops.common.auth.code.MockRepoAuthServiceCode
import com.tencent.devops.common.auth.code.MockTicketAuthServiceCode
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@ConditionalOnMissingClass(value = ["com.tencent.devops.common.auth.CommonConfiguration"])
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class MockAuthCodeServiceAutoConfiguration {
    @Bean
    fun bcsAuthServiceCode() = MockBcsAuthServiceCode()

    @Bean
    fun pipelineAuthServiceCode() = MockPipelineAuthServiceCode()

    @Bean
    fun codeAuthServiceCode() = MockCodeAuthServiceCode()

    @Bean
    fun projectAuthServiceCode() = MockProjectAuthServiceCode()

    @Bean
    fun environmentAuthServiceCode() = MockEnvironmentAuthServiceCode()

    @Bean
    fun repoAuthServiceCode() = MockRepoAuthServiceCode()

    @Bean
    fun ticketAuthServiceCode() = MockTicketAuthServiceCode()

    @Bean
    fun qualityAuthServiceCode() = MockQualityAuthServiceCode()

    @Bean
    fun artifactoryAuthServiceCode() = MockArtifactoryAuthServiceCode()
}
