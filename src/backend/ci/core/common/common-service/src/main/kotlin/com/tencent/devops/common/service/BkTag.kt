package com.tencent.devops.common.service

import com.tencent.devops.common.service.utils.KubernetesUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class BkTag @Autowired constructor(
    private val applicationContext: ApplicationContext
) {
    @Value("\${spring.cloud.consul.discovery.tags:prod}")
    private val tag: String = "prod"

    fun getTag(): String {
        if (KubernetesUtils.inContainer()) {
            return KubernetesUtils.getNamespace()
        }
        return tag
    }
}
