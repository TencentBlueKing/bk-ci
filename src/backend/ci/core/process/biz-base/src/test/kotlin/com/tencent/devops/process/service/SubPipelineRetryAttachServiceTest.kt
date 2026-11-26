/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("#8609 子流水线重试绑定凭证与插件级开关解析单测")
class SubPipelineRetryAttachServiceTest {

    private val redisOperation: RedisOperation = mockk(relaxed = true)
    private lateinit var service: SubPipelineRetryAttachService

    @BeforeEach
    fun setUp() {
        service = SubPipelineRetryAttachService(redisOperation)
    }

    @Test
    @DisplayName("插件级开关解析：兼容研发商店插件、内置插件以及存量未配置场景")
    fun testIsRetrySyncEnabled() {
        // 1. 存量未配置 -> 默认关闭
        assertFalse(SubPipelineRetryAttachService.isRetrySyncEnabled(mockTask(emptyMap())))
        assertFalse(
            SubPipelineRetryAttachService.isRetrySyncEnabled(
                mockTask(mapOf("data" to mapOf("input" to mapOf("runMode" to "syn"))))
            )
        )

        // 2. 商店插件 Boolean / String
        assertTrue(
            SubPipelineRetryAttachService.isRetrySyncEnabled(
                mockTask(mapOf("data" to mapOf("input" to mapOf(SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM to true))))
            )
        )
        assertTrue(
            SubPipelineRetryAttachService.isRetrySyncEnabled(
                mockTask(mapOf("data" to mapOf("input" to mapOf(SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM to "true"))))
            )
        )
        assertFalse(
            SubPipelineRetryAttachService.isRetrySyncEnabled(
                mockTask(mapOf("data" to mapOf("input" to mapOf(SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM to false))))
            )
        )

        // 3. 内置 SubPipelineCallElement 直接参数
        assertTrue(
            SubPipelineRetryAttachService.isRetrySyncEnabled(
                mockTask(mapOf(SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM to true))
            )
        )
        assertFalse(
            SubPipelineRetryAttachService.isRetrySyncEnabled(
                mockTask(mapOf(SUB_PIPELINE_ENABLE_RETRY_SYNC_PARAM to false))
            )
        )
    }

    @Test
    @DisplayName("四维强校验通过且 Lua 原子删除成功时消费凭证")
    fun testConsumeAttachInfoSuccess() {
        val info = sampleAttachInfo()
        val raw = JsonUtil.toJson(info, formatted = false)
        val key = "subpipeline:retry:attach:p1:b-parent:e-task1"
        every { redisOperation.get(key) } returns raw
        every { redisOperation.execute<Long>(any(), listOf(key), raw) } returns 1L

        val consumed = service.consumeAttachInfo(
            parentProjectId = "p1",
            parentBuildId = "b-parent",
            parentTaskId = "e-task1",
            actualExecuteCount = 2,
            callProjectId = "p-sub",
            callPipelineId = "pipe-sub",
            taskSubProjectId = "p-sub",
            taskSubBuildId = "b-sub-1"
        )

        assertNotNull(consumed)
        assertEquals("b-sub-1", consumed!!.subBuildId)
        verify(exactly = 1) { redisOperation.sremove("subpipeline:retry:attach:tasks:p1:b-parent", "e-task1") }
    }

    @Test
    @DisplayName("防轮询错对象：executeCount / 目标子流水线 / task.subBuildId 任一不匹配时拒绝消费")
    fun testConsumeAttachInfoRejectOnMismatch() {
        val info = sampleAttachInfo()
        val raw = JsonUtil.toJson(info, formatted = false)
        val key = "subpipeline:retry:attach:p1:b-parent:e-task1"
        every { redisOperation.get(key) } returns raw

        // executeCount 不匹配（例如用户手动又重跑了第 3 次）
        assertNull(
            service.consumeAttachInfo(
                parentProjectId = "p1",
                parentBuildId = "b-parent",
                parentTaskId = "e-task1",
                actualExecuteCount = 3,
                callProjectId = "p-sub",
                callPipelineId = "pipe-sub",
                taskSubProjectId = "p-sub",
                taskSubBuildId = "b-sub-1"
            )
        )

        // 目标子流水线变化
        assertNull(
            service.consumeAttachInfo(
                parentProjectId = "p1",
                parentBuildId = "b-parent",
                parentTaskId = "e-task1",
                actualExecuteCount = 2,
                callProjectId = "p-sub",
                callPipelineId = "pipe-other",
                taskSubProjectId = "p-sub",
                taskSubBuildId = "b-sub-1"
            )
        )

        // task.subBuildId 已被替换为新子构建 b-sub-2
        assertNull(
            service.consumeAttachInfo(
                parentProjectId = "p1",
                parentBuildId = "b-parent",
                parentTaskId = "e-task1",
                actualExecuteCount = 2,
                callProjectId = "p-sub",
                callPipelineId = "pipe-sub",
                taskSubProjectId = "p-sub",
                taskSubBuildId = "b-sub-2"
            )
        )

        verify(exactly = 0) { redisOperation.execute<Long>(any(), any(), any()) }
    }

    @Test
    @DisplayName("并发消费：Lua Compare-And-Delete 返回 0 时拒绝重复复用")
    fun testConsumeAttachInfoAlreadyConsumed() {
        val info = sampleAttachInfo()
        val raw = JsonUtil.toJson(info, formatted = false)
        val key = "subpipeline:retry:attach:p1:b-parent:e-task1"
        every { redisOperation.get(key) } returns raw
        every { redisOperation.execute<Long>(any(), listOf(key), raw) } returns 0L

        assertNull(
            service.consumeAttachInfo(
                parentProjectId = "p1",
                parentBuildId = "b-parent",
                parentTaskId = "e-task1",
                actualExecuteCount = 2,
                callProjectId = "p-sub",
                callPipelineId = "pipe-sub",
                taskSubProjectId = "p-sub",
                taskSubBuildId = "b-sub-1"
            )
        )
    }

    @Test
    @DisplayName("清理整条构建残留凭证：按索引精准批量删除")
    fun testClearBuildAttachInfos() {
        val indexKey = "subpipeline:retry:attach:tasks:p1:b-parent"
        every { redisOperation.getSetMembers(indexKey) } returns setOf("e-task1", "e-task2")

        service.clearBuildAttachInfos("p1", "b-parent")

        verify(exactly = 1) {
            redisOperation.delete(
                listOf(
                    "subpipeline:retry:attach:p1:b-parent:e-task1",
                    "subpipeline:retry:attach:p1:b-parent:e-task2"
                )
            )
        }
        verify(exactly = 1) { redisOperation.delete(indexKey) }
    }

    private fun sampleAttachInfo() = SubPipelineRetryAttachService.SubPipelineRetryAttachInfo(
        parentProjectId = "p1",
        parentPipelineId = "pipe-parent",
        parentBuildId = "b-parent",
        parentTaskId = "e-task1",
        expectedParentExecuteCount = 2,
        subProjectId = "p-sub",
        subPipelineId = "pipe-sub",
        subPipelineName = "sub-pipeline",
        subBuildId = "b-sub-1",
        subBuildNum = 10,
        subExecuteCount = 2,
        operator = "tester",
        triggerTime = 1700000000000L
    )

    private fun mockTask(taskParams: Map<String, Any>): PipelineBuildTask {
        val task = mockk<PipelineBuildTask>(relaxed = true)
        every { task.taskParams } returns taskParams.toMutableMap()
        every { task.status } returns BuildStatus.FAILED
        return task
    }
}
