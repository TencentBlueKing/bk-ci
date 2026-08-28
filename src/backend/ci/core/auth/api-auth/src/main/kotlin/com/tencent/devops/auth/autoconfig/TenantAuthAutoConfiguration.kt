package com.tencent.devops.auth.autoconfig

import com.tencent.devops.auth.util.TC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TenantAuthAutoConfiguration {
    @Bean
    fun tenantConvert() = TC()
}