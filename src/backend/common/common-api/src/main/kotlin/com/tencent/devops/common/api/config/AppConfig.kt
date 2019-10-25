package com.tencent.devops.common.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * 客户端通用配置
 */

@Configuration
class AppConfig {

    @Value("\${app.appCode}")
    val appCode: String = ""

    @Value("\${app.appSecret}")
    val appSecret: String = ""

}