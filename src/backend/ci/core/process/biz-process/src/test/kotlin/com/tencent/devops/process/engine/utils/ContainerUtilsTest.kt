package com.tencent.devops.process.engine.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ContainerUtilsTest {

    @Test
    fun getContainerStartupKey() {

        val pipelineId = "p-12345678901234567890123456789012"
        val buildId = "b-12345678901234567890123456789012"
        val containerId = "1"
        assertEquals(
            "container:startup:$pipelineId:$buildId:$containerId",
            ContainerUtils.getContainerStartupKey(pipelineId = pipelineId, buildId = buildId, containerId = containerId)
        )
    }
}