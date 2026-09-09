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

package com.tencent.devops.process.util

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TaskUtilsTest : TestBase() {

    private val taskList = mutableListOf<PipelineBuildTask>()

    private var vmBuildContainer: PipelineBuildContainer =
        genVmBuildContainer(id = firstContainerIdInt, status = BuildStatus.RUNNING)

    @BeforeEach
    override fun setUp() {
        super.setUp()
        vmBuildContainer = genVmBuildContainer(id = firstContainerIdInt, status = BuildStatus.RUNNING)
        taskList.add(genTask(taskId = "e-11", vmContainer = vmBuildContainer)
            .copy(taskSeq = 1, status = BuildStatus.SUCCEED)
        )
        taskList.add(genTask(taskId = "e-12", vmContainer = vmBuildContainer)
            .copy(taskSeq = 2, status = BuildStatus.SUCCEED)
        )
        taskList.add(genTask(taskId = "e-13", vmContainer = vmBuildContainer)
            .copy(taskSeq = 4, status = BuildStatus.QUEUE)
        )
    }

    @Test
    fun getPostTaskAndExecuteFlag() {
        val task = genTask(taskId = "e-12345678901234567890123456789012", vmContainer = vmBuildContainer,
            elementAdditionalOptions = elementAdditionalOptions().copy(elementPostInfo = nullObject))
        Assertions.assertFalse(
            TaskUtils.getPostExecuteFlag(
                task = task, taskList = taskList, isContainerFailed = true, hasFailedTaskInInSuccessContainer = true
            )
        )
    }

    @Test
    fun isStartVMTask() {
        var taskId = "mockId"
        Assertions.assertFalse(
            TaskUtils.isStartVMTask(
                genTask(taskId = taskId, vmContainer = genVmBuildContainer(id = firstContainerIdInt))
            )
        )
        // startVM-xxxx
        taskId = VMUtils.genStartVMTaskId(firstContainerId)
        Assertions.assertTrue(
            TaskUtils.isStartVMTask(
                genTask(taskId = taskId, vmContainer = genVmBuildContainer(id = firstContainerIdInt))
            )
        )
    }

    @Test
    fun getCancelTaskIdRedisKey() {
        Assertions.assertEquals(
            "CANCEL_TASK_IDS_b1_c1",
            TaskUtils.getCancelTaskIdRedisKey("b1", "c1", true)
        )
        Assertions.assertEquals(
            "CANCEL_TASK_IDS_b1_c1_set",
            TaskUtils.getCancelTaskIdRedisKey("b1", "c1", false)
        )
    }

    /**
     * #13581 Job 取消标记需要落到 Job 级 Redis 集合，且与具体插件 ID 解耦。
     */
    @Test
    fun markAndCheckJobCancelFlag() {
        val redisOperation = mockk<RedisOperation>()
        val key = TaskUtils.getCancelTaskIdRedisKey("b1", "c1", false)
        every { redisOperation.addSetValue(key, TaskUtils.JOB_CANCEL_FLAG) } returns true
        every { redisOperation.expire(key, any(), any()) } just runs
        every { redisOperation.hasKey(key) } returns true

        TaskUtils.markJobCancelFlag(redisOperation, "b1", "c1")
        Assertions.assertTrue(TaskUtils.isJobCancelFlag(redisOperation, "b1", "c1"))

        verify { redisOperation.addSetValue(key, TaskUtils.JOB_CANCEL_FLAG) }
        verify { redisOperation.expire(key, any(), any()) }
    }

    @Test
    fun isJobCancelFlagWhenKeyMissing() {
        val redisOperation = mockk<RedisOperation>()
        every { redisOperation.hasKey(any()) } returns false
        Assertions.assertFalse(TaskUtils.isJobCancelFlag(redisOperation, "b1", "c1"))
    }
}
