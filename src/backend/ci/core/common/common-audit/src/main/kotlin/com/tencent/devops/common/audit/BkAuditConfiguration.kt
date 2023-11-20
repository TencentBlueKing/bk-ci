package com.tencent.devops.common.audit

import com.tencent.bk.audit.AuditRequestProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(name = ["audit.enabled"], havingValue = "true", matchIfMissing = true)
class BkAuditConfiguration {
    @Bean
    @Primary
    fun bkAuditRequestProvider(): AuditRequestProvider {
        return BkAuditRequestProvider()
    }

    @Bean
    fun bkAuditPostFilter(): BkAuditPostFilter {
        return BkAuditPostFilter()
    }
}
