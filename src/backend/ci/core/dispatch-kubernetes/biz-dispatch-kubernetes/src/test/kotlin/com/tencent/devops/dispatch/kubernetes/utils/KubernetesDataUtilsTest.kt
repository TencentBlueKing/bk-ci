package com.tencent.devops.dispatch.kubernetes.utils

import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test

class KubernetesDataUtilsTest {
    private val kubernetesDataUtils: KubernetesDataUtils = mock()

    @Test
    fun getPodVolume() {
        Assert.assertNotNull(kubernetesDataUtils.getPodVolume())
    }

    @Test
    fun getPodVolumeMount() {
        Assert.assertNotNull(kubernetesDataUtils.getPodVolumeMount())
    }
}
