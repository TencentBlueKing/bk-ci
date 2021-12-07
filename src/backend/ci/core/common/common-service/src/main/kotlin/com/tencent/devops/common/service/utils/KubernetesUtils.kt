package com.tencent.devops.common.service.utils

import org.apache.commons.lang3.StringUtils

object KubernetesUtils {
    private val namespace = System.getenv("NAMESPACE")
    private val serviceSuffix = System.getenv("SERVICE_PREFIX")

    /**
     * 服务是否在容器中
     */
    fun inContainer() = StringUtils.isNotBlank(namespace)

    /**
     * 服务是否不在容器中
     */
    fun notInContainer() = !inContainer()

    /**
     * 获取服务发现的名称
     */
    fun getSvrName(serviceName: String) = "$serviceSuffix-$serviceName"
}
