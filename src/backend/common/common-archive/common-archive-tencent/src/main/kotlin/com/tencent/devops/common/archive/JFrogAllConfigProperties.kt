package com.tencent.devops.common.archive

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JFrogAllConfigProperties {
    @Value("\${jfrog.dev.url:#{null}}")
    val devUrl: String? = null
    @Value("\${jfrog.dev.username:#{null}}")
    val devUsername: String? = null
    @Value("\${jfrog.dev.password:#{null}}")
    val devPassword: String? = null

    @Value("\${jfrog.test.url:#{null}}")
    val testUrl: String? = null
    @Value("\${jfrog.test.username:#{null}}")
    val testUsername: String? = null
    @Value("\${jfrog.test.password:#{null}}")
    val testPassword: String? = null

    @Value("\${jfrog.prod.url:#{null}}")
    val prodUrl: String? = null
    @Value("\${jfrog.prod.username:#{null}}")
    val prodUsername: String? = null
    @Value("\${jfrog.prod.password:#{null}}")
    val prodPassword: String? = null
}