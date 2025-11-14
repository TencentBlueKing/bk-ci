package com.tencent.devops.stream.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class StreamLoginConfig {
    @Value("\${github.redirectUrl:}")
    val githubRedirectUrl: String = ""
}
