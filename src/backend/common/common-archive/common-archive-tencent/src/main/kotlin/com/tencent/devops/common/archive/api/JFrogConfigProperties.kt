package com.tencent.devops.common.archive.api

import org.springframework.stereotype.Component

@Component
data class JFrogConfigProperties(
    val url: String? = null,
    val username: String? = null,
    val password: String? = null
)