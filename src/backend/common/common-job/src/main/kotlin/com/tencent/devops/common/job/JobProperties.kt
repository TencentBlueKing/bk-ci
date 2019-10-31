package com.tencent.devops.common.job

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JobProperties {
    @Value("\${job.nginx.devUrl:#{null}}")
    val devUrl: String? = null
    @Value("\${job.nginx.testUrl:#{null}}")
    val testUrl: String? = null
    @Value("\${job.nginx.prodUrl:#{null}}")
    val prodUrl: String? = null

    @Value("\${job.link.devUrl:#{null}}")
    val devLinkUrl: String? = null
    @Value("\${job.link.testUrl:#{null}}")
    val testLinkUrl: String? = null
    @Value("\${job.link.prodUrl:#{null}}")
    val prodLinkUrl: String? = null
}
