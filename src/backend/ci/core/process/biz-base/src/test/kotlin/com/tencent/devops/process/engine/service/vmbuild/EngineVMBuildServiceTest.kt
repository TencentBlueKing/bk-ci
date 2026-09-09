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

package com.tencent.devops.process.engine.service.vmbuild

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.BuildProcessRestartAction
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * 容器重启后第二次上报环境就绪时，用于判断Job下是否已经有构建步骤真正执行过。
 * 判断错误会导致误杀可安全接管的构建，或放行状态已不一致的构建
 */
class EngineVMBuildServiceTest {

    @Test
    fun containsExecutedTaskIgnoresVmTasks() {
        val tasks = listOf(
            buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
            buildTask(taskId = "${VMUtils.getStopVmLabel()}$VM_SEQ_ID", status = BuildStatus.QUEUE),
            buildTask(taskId = "${VMUtils.getEndLabel()}$VM_SEQ_ID", status = BuildStatus.QUEUE)
        )
        Assertions.assertFalse(EngineVMBuildService.containsExecutedTask(tasks, EXECUTE_COUNT))
    }

    @Test
    fun containsExecutedTaskFalseWhenTaskNotClaimed() {
        // 构建机领取任务前，引擎最多把任务推进到QUEUE_CACHE，此时startTime为空
        listOf(BuildStatus.QUEUE, BuildStatus.QUEUE_CACHE).forEach { status ->
            val tasks = listOf(
                buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
                buildTask(taskId = BUSINESS_TASK_ID, status = status)
            )
            Assertions.assertFalse(
                EngineVMBuildService.containsExecutedTask(tasks, EXECUTE_COUNT), "status=$status"
            )
        }
    }

    @Test
    fun containsExecutedTaskFalseWhenTaskSkipped() {
        // 条件不满足被跳过的步骤是最终态但从未真正执行，不能算执行过
        val tasks = listOf(
            buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
            buildTask(taskId = BUSINESS_TASK_ID, status = BuildStatus.SKIP)
        )
        Assertions.assertFalse(EngineVMBuildService.containsExecutedTask(tasks, EXECUTE_COUNT))
    }

    @Test
    fun containsExecutedTaskFalseWhenTaskFromPreviousRound() {
        // Job内局部重试：上一轮已成功的插件不会被重置，仍保留旧执行次数和开始时间，不能算本轮执行过
        val tasks = listOf(
            buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
            buildTask(
                taskId = BUSINESS_TASK_ID,
                status = BuildStatus.SUCCEED,
                started = true,
                executeCount = PREVIOUS_EXECUTE_COUNT
            ),
            buildTask(taskId = "e-2", status = BuildStatus.QUEUE_CACHE)
        )
        Assertions.assertFalse(EngineVMBuildService.containsExecutedTask(tasks, EXECUTE_COUNT))
    }

    @Test
    fun containsExecutedTaskTrueWhenTaskRunning() {
        // 自动重试场景下startTime可能不被刷新，因此运行中本身就要算执行过
        val tasks = listOf(
            buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
            buildTask(taskId = BUSINESS_TASK_ID, status = BuildStatus.RUNNING)
        )
        Assertions.assertTrue(EngineVMBuildService.containsExecutedTask(tasks, EXECUTE_COUNT))
    }

    @Test
    fun containsExecutedTaskTrueWhenTaskFinishedWithStartTime() {
        val tasks = listOf(
            buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
            buildTask(taskId = BUSINESS_TASK_ID, status = BuildStatus.SUCCEED, started = true),
            buildTask(taskId = "e-2", status = BuildStatus.QUEUE_CACHE)
        )
        Assertions.assertTrue(EngineVMBuildService.containsExecutedTask(tasks, EXECUTE_COUNT))
    }

    @Test
    fun containsExecutedTaskFalseWhenNoTask() {
        Assertions.assertFalse(EngineVMBuildService.containsExecutedTask(emptyList(), EXECUTE_COUNT))
    }

    @Test
    fun decideRestartActionRejectsWhenSwitchOff() {
        // 开关关闭时保持原有行为，不管有没有执行过步骤都只拒绝，不终止
        listOf(BuildStatus.QUEUE_CACHE, BuildStatus.RUNNING).forEach { status ->
            val action = EngineVMBuildService.decideRestartAction(
                terminateEnabled = false,
                tasks = listOf(buildTask(taskId = BUSINESS_TASK_ID, status = status)),
                executeCount = EXECUTE_COUNT
            )
            Assertions.assertEquals(BuildProcessRestartAction.REJECT, action, "status=$status")
        }
    }

    @Test
    fun decideRestartActionTerminatesWhenTaskExecuted() {
        val action = EngineVMBuildService.decideRestartAction(
            terminateEnabled = true,
            tasks = listOf(buildTask(taskId = BUSINESS_TASK_ID, status = BuildStatus.RUNNING)),
            executeCount = EXECUTE_COUNT
        )
        Assertions.assertEquals(BuildProcessRestartAction.TERMINATE, action)
    }

    @Test
    fun decideRestartActionResumesWhenNoTaskExecuted() {
        val action = EngineVMBuildService.decideRestartAction(
            terminateEnabled = true,
            tasks = listOf(
                buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
                buildTask(taskId = BUSINESS_TASK_ID, status = BuildStatus.QUEUE_CACHE)
            ),
            executeCount = EXECUTE_COUNT
        )
        Assertions.assertEquals(BuildProcessRestartAction.RESUME, action)
    }

    @Test
    fun decideRestartActionResumesWhenOnlyPreviousRoundExecuted() {
        // Job内局部重试后容器重启：本轮还没执行任何步骤，历史轮次的成功插件不应把它误判成终止
        val action = EngineVMBuildService.decideRestartAction(
            terminateEnabled = true,
            tasks = listOf(
                buildTask(taskId = VMUtils.genStartVMTaskId(VM_SEQ_ID), status = BuildStatus.SUCCEED, started = true),
                buildTask(
                    taskId = BUSINESS_TASK_ID,
                    status = BuildStatus.SUCCEED,
                    started = true,
                    executeCount = PREVIOUS_EXECUTE_COUNT
                ),
                buildTask(taskId = "e-2", status = BuildStatus.QUEUE_CACHE)
            ),
            executeCount = EXECUTE_COUNT
        )
        Assertions.assertEquals(BuildProcessRestartAction.RESUME, action)
    }

    private fun buildTask(
        taskId: String,
        status: BuildStatus,
        started: Boolean = false,
        executeCount: Int = EXECUTE_COUNT
    ) = PipelineBuildTask(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        stageId = "stage-1",
        containerId = VM_SEQ_ID,
        containerHashId = "c-1",
        containerType = "vmBuild",
        taskSeq = 1,
        taskId = taskId,
        taskName = taskId,
        taskType = "linuxScript",
        taskAtom = "",
        status = status,
        taskParams = mutableMapOf(),
        additionalOptions = null,
        executeCount = executeCount,
        starter = "admin",
        approver = null,
        subProjectId = null,
        subBuildId = null,
        startTime = if (started) LocalDateTime.now() else null
    )

    companion object {
        private const val PROJECT_ID = "demo"
        private const val PIPELINE_ID = "p-1"
        private const val BUILD_ID = "b-1"
        private const val VM_SEQ_ID = "1"
        private const val BUSINESS_TASK_ID = "e-1"

        // 当前这一轮的执行次数，取大于1的值以便和局部重试遗留的历史任务区分
        private const val EXECUTE_COUNT = 2
        private const val PREVIOUS_EXECUTE_COUNT = 1
    }
}
