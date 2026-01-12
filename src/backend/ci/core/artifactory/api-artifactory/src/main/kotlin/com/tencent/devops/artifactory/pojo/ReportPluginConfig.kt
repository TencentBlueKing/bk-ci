package com.tencent.devops.artifactory.pojo

data class ReportPluginConfig(
    val enableCompress: Boolean,
    val enableCompressPipelines: List<String>,
    val compressThreshold: Long,
    val compressSizeLimit: Long,
)
