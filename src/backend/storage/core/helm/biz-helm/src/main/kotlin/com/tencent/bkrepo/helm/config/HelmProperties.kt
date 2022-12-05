package com.tencent.bkrepo.helm.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "helm")
data class HelmProperties(
    /**
     * generic服务domain地址，用于生成临时url
     */
    var domain: String = "localhost"
)
