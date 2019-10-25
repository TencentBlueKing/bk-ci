package com.tencent.devops.common.client.pojo

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AllProperties {
    @Value("\${gateway.dev.url:#{null}}")
    val gatewayDevUrl: String? = null
    @Value("\${gateway.test.url:#{null}}")
    val gatewayTestUrl: String? = null
    @Value("\${gateway.prod.url:#{null}}")
    val gatewayProdUrl: String? = null
}