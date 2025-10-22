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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.pojo.BuildTask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TaskUtilTest {

    @Test
    fun isContinueWhenFailed() {
        val elementAdditionalOptions = elementAdditionalOptions()
        val task = genTask(elementAdditionalOptions = elementAdditionalOptions)
        Assertions.assertFalse(TaskUtil.isContinueWhenFailed(task))

        val manualSkipTask = genTask(elementAdditionalOptions.copy(manualSkip = true, continueWhenFailed = true))
        Assertions.assertFalse(TaskUtil.isContinueWhenFailed(manualSkipTask))

        val autoSkipTask = genTask(elementAdditionalOptions.copy(manualSkip = false, continueWhenFailed = true))
        Assertions.assertTrue(TaskUtil.isContinueWhenFailed(autoSkipTask))

        val defaultOldTask = genTask(elementAdditionalOptions.copy(manualSkip = null, continueWhenFailed = true))
        Assertions.assertTrue(TaskUtil.isContinueWhenFailed(defaultOldTask))
    }

    @Test
    fun isTimeOut() {
        var expect = 1L
        val elementAdditionalOptions = elementAdditionalOptions(timeout = expect)
        val task = genTask(elementAdditionalOptions = elementAdditionalOptions)
        Assertions.assertEquals(expect, TaskUtil.getTimeOut(task))

        expect = TimeUnit.DAYS.toMinutes(Timeout.MAX_JOB_RUN_DAYS)
        val nullElementAdditionalOptionsTask = genTask(elementAdditionalOptions = null)
        Assertions.assertEquals(expect, TaskUtil.getTimeOut(nullElementAdditionalOptionsTask))
    }

    @Test
    fun funTest() {
        val taskId = "t-123"
        TaskUtil.setTaskId(taskId)
        Assertions.assertEquals(taskId, TaskUtil.getTaskId())
        TaskUtil.removeTaskId()
        Assertions.assertEquals("", TaskUtil.getTaskId())

        Assertions.assertFalse(TaskUtil.isVmBuildEnv(NormalContainer.classType))
        Assertions.assertTrue(TaskUtil.isVmBuildEnv(VMBuildContainer.classType))
    }

    private fun elementAdditionalOptions(timeout: Long = 0): ElementAdditionalOptions {
        return ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = false,
            retryWhenFailed = false,
            runCondition = RunCondition.PRE_TASK_SUCCESS, customVariables = mutableListOf(),
            retryCount = 0,
            timeout = timeout,
            otherTask = null,
            customCondition = null,
            pauseBeforeExec = null,
            subscriptionPauseUser = null
        )
    }

    private fun genTask(elementAdditionalOptions: ElementAdditionalOptions?): BuildTask {
        return BuildTask(
            buildId = "buildId",
            vmSeqId = "1",
            status = BuildTaskStatus.DO,
            taskId = "task.taskId",
            elementId = "task.taskId",
            stepId = "task.stepId",
            elementName = "task.taskName",
            type = "taskType",
            params = if (elementAdditionalOptions != null) {
                mapOf("additionalOptions" to JsonUtil.toJson(elementAdditionalOptions))
            } else {
                mapOf()
            },
            buildVariable = mapOf(),
            containerType = "normal",
            executeCount = 1
        )
    }
}
