package com.tencent.devops.monitoring.service.impl

import com.tencent.devops.monitoring.pojo.DispatchStatus
import com.tencent.devops.monitoring.service.DispatchReportService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class DispatchReportServiceImpl : DispatchReportService {

    override fun reportDispatchStatus(dispatchStatus: DispatchStatus): Boolean {
        return true
    }
}

@Configuration
class DispatchReportConfiguration {
    @Bean
    @ConditionalOnMissingBean(DispatchReportService::class)
    fun dispatchReportService(): DispatchReportService = DispatchReportServiceImpl()
}
