package com.tencent.devops.common.service.utils

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils

object KubernetesUtils {
    private val namespace = System.getenv("NAMESPACE")
    private val releaseName = System.getenv("RELEASE_NAME")
    private val chartName = System.getenv("CHART_NAME")
    private val multiCluster = BooleanUtils.toBoolean(System.getenv("MULTI_CLUSTER"))
    private val defaultNamespace = System.getenv("DEFAULT_NAMESPACE")
    private val enablePublicDocker =
        BooleanUtils.toBoolean(StringUtils.defaultIfBlank(System.getenv("ENABLE_PUBLIC_DOCKER"), "true"))
    private val enableK8sBuild =
        BooleanUtils.toBoolean(StringUtils.defaultIfBlank(System.getenv("ENABLE_K8S_BUILD"), "false"))

    /**
     * 是否开启docker公共构建机
     */
    fun enablePublicDocker() = enablePublicDocker

    /**
     * 是否开启k8s构建机
     */
    fun enableK8sBuild() = enableK8sBuild

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
    fun getSvrName(serviceName: String, namespace: String): String {
        return if (multiCluster) {
            "$serviceName-$chartName-$serviceName.$namespace"
        } else {
            "$releaseName-$chartName-$serviceName.$namespace"
        }
    }

    /**
     * 是否开启多集群模式
     */
    fun isMultiCluster() = multiCluster

    /**
     * 获取兜底ns
     */
    fun getDefaultNamespace(): String = defaultNamespace

    /**
     * 获取namespace
     */
    fun getNamespace(): String {
        return namespace ?: "default"
    }
}
