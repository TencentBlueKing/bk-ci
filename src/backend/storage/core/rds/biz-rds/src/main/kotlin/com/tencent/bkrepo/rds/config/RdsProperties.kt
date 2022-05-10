package com.tencent.bkrepo.rds.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rds")
data class RdsProperties(
    /**
     * rds服务domain地址，用于生成临时url
     */
    var domain: String = "localhost"
)
