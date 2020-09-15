package com.tencent.bk.codecc.apiquery.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "api.gateway.pub")
data class ApiGatewayPubProperties(
    val pubFile: String? = null
)