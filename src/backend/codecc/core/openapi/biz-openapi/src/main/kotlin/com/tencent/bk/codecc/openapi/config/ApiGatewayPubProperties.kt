package com.tencent.bk.codecc.openapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "api.gateway.pub")
data class ApiGatewayPubProperties(
    val pubFile: String? = null
)