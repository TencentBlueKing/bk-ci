package com.tencent.devops.dispatch.kubernetes.utils

import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.pipeline.type.kubernetes.KubernetesDispatchType
import org.junit.Assert
import org.junit.Test

class DispatchUtilsTest {
    private val dispatchUtils = DispatchUtils(
        client = mock(),
        defaultImageConfig = mock()
    )

    @Test
    fun getPool() {
        val dispatchMessage = DispatchMessage(
            id = "1",
            secretKey = "",
            gateway = "",
            projectId = "1",
            pipelineId = "2",
            buildId = "3",
            dispatchMessage = "",
            userId = "",
            vmSeqId = "",
            channelCode = "",
            vmNames = "",
            atoms = mapOf(),
            zone = null,
            containerHashId = "1",
            executeCount = 1,
            containerId = "1",
            containerType = "1",
            stageId = "1",
            dispatchType = KubernetesDispatchType(
                kubernetesBuildVersion = "1.0"
            ),
            customBuildEnv = mapOf()
        )

        Assert.assertNotNull(dispatchUtils.getPool(dispatchMessage))
    }
}
