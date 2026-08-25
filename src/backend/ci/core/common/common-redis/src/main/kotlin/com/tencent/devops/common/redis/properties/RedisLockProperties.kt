package com.tencent.devops.common.redis.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.data.redis.lock")
data class RedisLockProperties(
    val database: Int? = null,
    val host: String? = null,
    val password: String? = null,
    val port: Int? = null
)
