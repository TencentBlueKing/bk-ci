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
import com.tencent.devops.process.TestBase
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
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
}
