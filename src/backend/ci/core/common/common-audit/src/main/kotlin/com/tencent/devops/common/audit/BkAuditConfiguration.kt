package com.tencent.devops.common.audit

import com.tencent.bk.audit.AuditRequestProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class BkAuditConfiguration {
    @Bean
    @Primary
    fun bkAuditRequestProvider(): AuditRequestProvider {
        return BkAuditRequestProvider()
    }
}
