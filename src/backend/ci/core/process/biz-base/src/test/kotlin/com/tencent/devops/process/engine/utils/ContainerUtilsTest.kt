package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun isNormalContainerEnable() {
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer()))
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = false, jobControlOption = null)))
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = false, jobControlOption = option(true))))
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = null, jobControlOption = option(true))))

        assertFalse(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = null, jobControlOption = option(false))))
        assertFalse(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = true, jobControlOption = option(false))))
        assertFalse(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = true, jobControlOption = null)))
    }

    @Test
    fun isVMBuildContainerEnable() {

        assertTrue(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS)))

        assertTrue(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS, jobControlOption = null)))
        assertTrue(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS, jobControlOption = option(true))))

        assertFalse(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS, jobControlOption = option(false))))
    }

    private fun option(enable: Boolean) = JobControlOption(enable = enable, timeout = 600, runCondition = JobRunCondition.STAGE_RUNNING)
}