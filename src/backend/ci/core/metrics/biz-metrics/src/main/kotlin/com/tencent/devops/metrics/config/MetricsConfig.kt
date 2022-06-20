package com.tencent.devops.metrics.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MetricsConfig {
    @Value("\${queryParam.queryCountMax:10000}")
    val queryCountMax: Int = 10000
}
