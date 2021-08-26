package com.tencent.devops.common.client.pojo

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AllProperties {
    @Value("\${devopsGateway.host:#{null}}")
    val devopsDevUrl: String? = null
}
