package com.tencent.devops.stream.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class StreamLoginConfig(
    @Value("\${login.github.redirectUrl:}")
    val githubRedirectUrl: String = ""
)
