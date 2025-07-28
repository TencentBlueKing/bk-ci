package com.tencent.devops.artifactory.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Component

@RefreshScope
@Component
class ReportArchiveTaskConfig {

    @Value("\${report.compress.enabled:false}")
    var enabledCompress: Boolean = false

    @Value("#{'\${report.compress.enabledPipelines}'.split(',')}")
    var enableCompressPipelines: MutableList<String> = mutableListOf()

    @Value("\${report.compress.threshold:9223372036854775807}")
    var compressThreshold: Long = Long.MAX_VALUE
}