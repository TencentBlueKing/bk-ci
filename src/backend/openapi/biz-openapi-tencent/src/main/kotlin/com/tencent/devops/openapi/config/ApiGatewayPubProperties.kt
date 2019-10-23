package com.tencent.devops.dispatch.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "api.gateway.pub")
data class ApiGatewayPubProperties(
    val pubFile: String? = null
)