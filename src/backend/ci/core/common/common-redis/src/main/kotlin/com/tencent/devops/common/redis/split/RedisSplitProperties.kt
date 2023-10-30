package com.tencent.devops.common.redis.split

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("spring.redis.split")
@ConstructorBinding
data class RedisSplitProperties(
    val mode: Mode = Mode.WRITE_READ_MASTER,
    val database: Int? = null,
    val host: String? = null,
    val password: String? = null,
    val port: Int? = null
) {
    enum class Mode {
        WRITE_READ_MASTER,
        WRITE_ALL_READ_MASTER
    }
}
