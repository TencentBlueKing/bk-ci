package com.tencent.devops.common.service

import com.tencent.devops.common.service.utils.KubernetesUtils
import org.slf4j.LoggerFactory

class BkTag constructor(
    private val consulTag: String
) {
    private val gatewayTag = ThreadLocal<String>()

    fun getFinalTag(): String {
        return getGatewayTag() ?: getLocalTag()
    }

    fun getLocalTag(): String {
        if (KubernetesUtils.inContainer()) {
            return (if (KubernetesUtils.isMultiCluster()) "kubernetes-" else "") + KubernetesUtils.getNamespace()
        }
        return consulTag
    }

    fun getGatewayTag(): String? = gatewayTag.get()

    fun setGatewayTag(consulTag: String) = gatewayTag.set(consulTag)

    fun removeGatewayTag() = gatewayTag.remove()

    fun <T> invokeByTag(tag: String?, action: () -> T): T {
        try {
            tag?.let { setGatewayTag(it) }
            return action()
        } finally {
            tag?.let { removeGatewayTag() }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkTag::class.java)
    }
}
