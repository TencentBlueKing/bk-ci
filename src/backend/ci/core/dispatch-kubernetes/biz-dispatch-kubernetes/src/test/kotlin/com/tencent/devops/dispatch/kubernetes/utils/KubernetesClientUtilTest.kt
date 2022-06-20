package com.tencent.devops.dispatch.kubernetes.utils

import io.kubernetes.client.openapi.ApiResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KubernetesClientUtilTest {
    @Test
    fun getClientFailInfo() {
        val clientFailInfo = KubernetesClientUtil.getClientFailInfo("test")
        Assertions.assertEquals("Dispatch-kubernetes 异常信息 - test", clientFailInfo)
    }

    @Test
    fun getKubernetesWorkloadOnlyLabelValue() {
        val kubernetesWorkloadOnlyLabelValue = KubernetesClientUtil.getKubernetesWorkloadOnlyLabelValue("test")
        Assertions.assertNotNull(kubernetesWorkloadOnlyLabelValue)
    }

    @Test
    fun apiHandler() {
        val apiHandle = KubernetesClientUtil.apiHandle {
            ApiResponse<String>(1, mapOf())
        }
        Assertions.assertNotNull(apiHandle)
    }
}
