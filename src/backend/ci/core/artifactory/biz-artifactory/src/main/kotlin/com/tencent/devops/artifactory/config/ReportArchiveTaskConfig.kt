package com.tencent.devops.artifactory.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize

@Component
class ReportArchiveTaskConfig {

    @Value("\${report.compress.enabled:false}")
    var enabledCompress: Boolean = false

    @Value("#{'\${report.compress.enabledPipelines:}'.split(',')}")
    var enableCompressPipelines: MutableList<String> = mutableListOf()

    @Value("\${report.compress.threshold:9223372036854775807}")
    var compressThreshold: Long = Long.MAX_VALUE

    @Value("\${report.compress.size.limit:524288000}")
    var compressSizeLimit: Long = DataSize.ofMegabytes(500).toBytes()
}