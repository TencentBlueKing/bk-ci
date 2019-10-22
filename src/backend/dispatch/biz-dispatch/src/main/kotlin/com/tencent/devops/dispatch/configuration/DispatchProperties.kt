package com.tencent.devops.dispatch.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "dispatch")
data class DispatchProperties(
    val workerFile: String? = null,
    val dockerFile: String? = null,
    val scripts: String? = null
)