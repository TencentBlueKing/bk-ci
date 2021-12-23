package com.tencent.devops.dispatch.kubernetes.utils

import io.kubernetes.client.openapi.ApiResponse
import org.junit.Assert
import org.junit.Test

class KubernetesClientUtilTest {
    @Test
    fun getClientFailInfo() {
        val clientFailInfo = KubernetesClientUtil.getClientFailInfo("test")
        Assert.assertEquals("Dispatch-kubernetes 异常信息 - test", clientFailInfo)
    }

    @Test
    fun getKubernetesWorkloadOnlyLabelValue() {
        val kubernetesWorkloadOnlyLabelValue = KubernetesClientUtil.getKubernetesWorkloadOnlyLabelValue("test")
        Assert.assertNotNull(kubernetesWorkloadOnlyLabelValue)
    }

    @Test
    fun apiHandler() {
        val apiHandle = KubernetesClientUtil.apiHandle {
            ApiResponse<String>(1, mapOf())
        }
        Assert.assertNotNull(apiHandle)
    }
}
