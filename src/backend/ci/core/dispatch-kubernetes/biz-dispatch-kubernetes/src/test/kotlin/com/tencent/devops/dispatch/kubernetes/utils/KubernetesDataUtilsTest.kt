package com.tencent.devops.dispatch.kubernetes.utils

import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KubernetesDataUtilsTest {
    private val kubernetesDataUtils: KubernetesDataUtils = mock()

    @Test
    fun getPodVolume() {
        Assertions.assertNotNull(kubernetesDataUtils.getPodVolume())
    }

    @Test
    fun getPodVolumeMount() {
        Assertions.assertNotNull(kubernetesDataUtils.getPodVolumeMount())
    }
}
