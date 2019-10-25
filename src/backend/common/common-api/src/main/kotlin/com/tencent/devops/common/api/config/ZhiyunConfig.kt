package com.tencent.devops.common.api.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * 客户端通用配置
 */

@Configuration
class ZhiyunConfig {

    @Value("\${zhiyun.url}")
    val url: String = ""

    @Value("\${zhiyun.caller}")
    val caller: String = ""

    @Value("\${zhiyun.password}")
    val password: String = ""

    @Value("\${zhiyun.apiKey}")
    val apiKey: String = ""

    @Value("\${zhiyun.esbUrl}")
    val esbUrl: String = ""

}