package com.tencent.devops.turbo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "tbs")
data class TBSProperties(
    var rootPath: String? = null,
    var urlTemplate: String? = null
)
