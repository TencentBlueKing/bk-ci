/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContainerUtilsTest {

    @Test
    fun getContainerStartupKey() {

        val pipelineId = "p-12345678901234567890123456789012"
        val buildId = "b-12345678901234567890123456789012"
        val containerId = "1"
        assertEquals(
            "container:startup:$pipelineId:$buildId:$containerId",
            ContainerUtils.getContainerStartupKey(pipelineId = pipelineId,
                buildId = buildId,
                containerId = containerId)
        )
    }

    @Test
    fun isNormalContainerEnable() {
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer()))
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = false,
            jobControlOption = null)))
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = false,
            jobControlOption = option(true))))
        assertTrue(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = null,
            jobControlOption = option(true))))

        assertFalse(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = null,
            jobControlOption = option(false))))
        assertFalse(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = true,
            jobControlOption = option(false))))
        assertFalse(ContainerUtils.isNormalContainerEnable(NormalContainer(enableSkip = true,
            jobControlOption = null)))
    }

    @Test
    fun isVMBuildContainerEnable() {

        assertTrue(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS)))

        assertTrue(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS,
            jobControlOption = null)))
        assertTrue(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS,
            jobControlOption = option(true))))

        assertFalse(ContainerUtils.isVMBuildContainerEnable(VMBuildContainer(baseOS = VMBaseOS.MACOS,
            jobControlOption = option(false))))
    }

    private fun option(enable: Boolean) =
        JobControlOption(enable = enable, runCondition = JobRunCondition.STAGE_RUNNING)
}
