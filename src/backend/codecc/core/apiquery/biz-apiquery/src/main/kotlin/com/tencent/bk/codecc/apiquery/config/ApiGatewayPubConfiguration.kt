package com.tencent.bk.codecc.apiquery.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ApiGatewayPubProperties::class)
class ApiGatewayPubConfiguration