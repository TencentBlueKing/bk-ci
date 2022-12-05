package com.tencent.bkrepo.pypi.artifact

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pypi")
data class PypiProperties(
    var domain: String = "localhost"
)
