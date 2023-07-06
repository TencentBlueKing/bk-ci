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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ContainerMutexStatus
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Suppress("ALL", "UNUSED")
class MutexControlTest {

    private val buildLogPrinter: BuildLogPrinter = BuildLogPrinter(mockk())
    private val redisOperation: RedisOperation = RedisOperation(mockk())
    private val variables: Map<String, String> = mapOf(Pair("var1", "Test"))
    private val buildId: String = "b-12345678901234567890123456789012"
    private val containerId: String = "1"
    private val projectId: String = "demo"
    private val pipelineId: String = "p-12345678901234567890123456789012"
    private val containerHashId: String = "c-12345678901234567890123456789012"
    private val stageId: String = "stage-1"
    private val mutexGroup: MutexGroup = MutexGroup(
        enable = true,
        mutexGroupName = "mutexGroupName\${var1}",
        queueEnable = true,
        timeout = 100800,
        queue = 15
    )
    private val container: PipelineBuildContainer = PipelineBuildContainer(
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        stageId = stageId,
        containerId = containerId,
        containerHashId = containerHashId,
        containerType = "vmBuild",
        seq = containerId.toInt(),
        status = BuildStatus.RUNNING,
        controlOption = PipelineBuildContainerControlOption(jobControlOption = JobControlOption()),
        matrixGroupId = null,
        matrixGroupFlag = false
    )
    private val mutexControl: MutexControl = MutexControl(
        buildLogPrinter = buildLogPrinter,
        redisOperation = redisOperation,
        containerBuildRecordService = mockk(),
        pipelineUrlBean = mockk(),
        pipelineContainerService = mockk()
    )

    @Test
    // 测试MutexControl的初始化功能
    fun initMutexGroup() {
        val initMutexGroup = mutexControl.decorateMutexGroup(
            mutexGroup = mutexGroup,
            variables = variables
        )
        Assertions.assertNotNull(initMutexGroup)
        Assertions.assertEquals("mutexGroupName\${var1}", initMutexGroup!!.mutexGroupName)
        Assertions.assertEquals("mutexGroupNameTest", initMutexGroup.runtimeMutexGroup)
        Assertions.assertEquals(10080, initMutexGroup.timeout)
        Assertions.assertEquals(10, initMutexGroup.queue)
    }

    @Disabled
    // 测试MutexControl的锁功能
    fun checkContainerMutex() {
        val initMutexGroup = mutexControl.decorateMutexGroup(
            mutexGroup = mutexGroup,
            variables = variables
        )
        Assertions.assertEquals(ContainerMutexStatus.READY, mutexControl.acquireMutex(initMutexGroup, container))
    }

    @Disabled
    // 测试MutexControl的解锁功能
    fun releaseContainerMutex() {
        val initMutexGroup = mutexControl.decorateMutexGroup(
            mutexGroup = mutexGroup,
            variables = variables
        )
        mutexControl.releaseContainerMutex(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            mutexGroup = initMutexGroup,
            executeCount = container.executeCount
        )
    }
}
