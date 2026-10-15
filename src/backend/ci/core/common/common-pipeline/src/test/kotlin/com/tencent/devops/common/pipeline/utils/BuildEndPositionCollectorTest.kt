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
 * documentation files (the Software), to deal in the Software without restriction, including without limitation the
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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildEndType
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.EndPosition
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BuildEndPositionCollectorTest {

    @Test
    fun `collect failed plugin and review abort skip control tasks`() {
        val model = Model(
            name = "p",
            desc = null,
            stages = listOf(
                Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
                Stage(
                    id = "stage-2",
                    name = "stage-1",
                    containers = listOf(
                        NormalContainer(
                            id = "1",
                            containerId = "1",
                            name = "构建环境-Linux",
                            status = BuildStatus.FAILED.name,
                            elements = listOf(
                                LinuxScriptElement(
                                    id = "e-bash",
                                    name = "Bash",
                                    status = BuildStatus.SUCCEED.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false
                                ),
                                LinuxScriptElement(
                                    id = "e-0806",
                                    name = "0806插件",
                                    status = BuildStatus.FAILED.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false
                                ),
                                LinuxScriptElement(
                                    id = "stopVM-1",
                                    name = "完结源环境",
                                    status = BuildStatus.SUCCEED.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false
                                ),
                                ManualReviewUserTaskElement(
                                    id = "e-review",
                                    name = "人工审核",
                                    status = BuildStatus.REVIEW_ABORT.name
                                )
                            )
                        )
                    )
                )
            )
        )

        val positions = BuildEndPositionCollector.collectFailPositions(model)

        Assertions.assertEquals(2, positions.size)
        Assertions.assertEquals(BuildStatus.FAILED.name, positions[0].statusAtEnd)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, positions[0].endType)
        Assertions.assertEquals("1-1-2", positions[0].position)
        Assertions.assertEquals(BuildStatus.REVIEW_ABORT.name, positions[1].statusAtEnd)
        Assertions.assertEquals(BuildEndType.FAIL_REVIEW, positions[1].endType)
        Assertions.assertEquals(BuildEndType.FAIL_MULTIPLE, BuildEndPositionCollector.aggregateFailEndType(positions))
        Assertions.assertEquals(BuildEndPositionCollector.REASON_PLUGIN_FAIL, positions[0].reasonCode)
    }

    @Test
    fun `collect pause terminated canceled plugin with reason`() {
        val model = Model(
            name = "p",
            desc = null,
            stages = listOf(
                Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
                Stage(
                    id = "stage-2",
                    name = "stage-1",
                    containers = listOf(
                        NormalContainer(
                            id = "1",
                            containerId = "1",
                            name = "构建环境-Linux",
                            status = BuildStatus.FAILED.name,
                            elements = listOf(
                                LinuxScriptElement(
                                    id = "e-pause",
                                    name = "0806插件",
                                    status = BuildStatus.CANCELED.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false,
                                    additionalOptions = ElementAdditionalOptions(pauseBeforeExec = true)
                                )
                            )
                        )
                    )
                )
            )
        )

        val positions = BuildEndPositionCollector.collectFailPositions(model)

        Assertions.assertEquals(1, positions.size)
        Assertions.assertEquals(BuildStatus.CANCELED.name, positions[0].statusAtEnd)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, positions[0].endType)
        Assertions.assertEquals(BuildEndPositionCollector.REASON_PAUSE_TERMINATED, positions[0].reasonCode)
    }

    @Test
    fun `collect mutex canceled job as fail position`() {
        val model = Model(
            name = "p",
            desc = null,
            stages = listOf(
                Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
                Stage(
                    id = "stage-2",
                    name = "stage-1",
                    containers = listOf(
                        NormalContainer(
                            id = "1",
                            containerId = "1",
                            name = "1231342342",
                            status = BuildStatus.CANCELED.name,
                            mutexGroup = MutexGroup(
                                enable = true,
                                mutexGroupName = "123",
                                queueEnable = false
                            ),
                            elements = emptyList()
                        )
                    )
                )
            )
        )

        val positions = BuildEndPositionCollector.collectFailPositions(model)

        Assertions.assertEquals(1, positions.size)
        Assertions.assertEquals("1-1", positions[0].position)
        Assertions.assertEquals(BuildEndType.FAIL_EXEC, positions[0].endType)
        Assertions.assertEquals(BuildEndPositionCollector.REASON_MUTEX_QUEUE_DISABLED, positions[0].reasonCode)
        Assertions.assertEquals(listOf("123"), positions[0].reasonParams)
    }

    @Test
    fun `collect cancel positions include pause plugin`() {
        val model = Model(
            name = "p",
            desc = null,
            stages = listOf(
                Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
                Stage(
                    id = "stage-2",
                    name = "stage-1",
                    containers = listOf(
                        NormalContainer(
                            id = "1",
                            containerId = "1",
                            name = "构建环境-Linux",
                            status = BuildStatus.CANCELED.name,
                            jobControlOption = JobControlOption(timeout = 1),
                            elements = listOf(
                                LinuxScriptElement(
                                    id = "e-pause",
                                    name = "0806插件",
                                    status = BuildStatus.CANCELED.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false,
                                    additionalOptions = ElementAdditionalOptions(pauseBeforeExec = true)
                                ),
                                LinuxScriptElement(
                                    id = "e-unexec",
                                    name = "Bash",
                                    status = BuildStatus.UNEXEC.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false
                                )
                            )
                        )
                    )
                )
            )
        )

        val positions = BuildEndPositionCollector.collectCancelPositions(model)

        Assertions.assertEquals(1, positions.size)
        Assertions.assertEquals("e-pause", positions[0].taskId)
        Assertions.assertEquals(BuildEndPositionCollector.REASON_PAUSE_TERMINATED, positions[0].reasonCode)
    }

    @Test
    fun `collect cancel positions from two timeout jobs`() {
        val model = Model(
            name = "p",
            desc = null,
            stages = listOf(
                Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
                Stage(
                    id = "stage-2",
                    name = "stage-1",
                    containers = listOf(
                        pauseJob("1", "e-0806-a"),
                        pauseJob("2", "e-0806-b")
                    )
                )
            )
        )

        val positions = BuildEndPositionCollector.collectCancelPositions(model)

        Assertions.assertEquals(2, positions.size)
        Assertions.assertEquals("e-0806-a", positions[0].taskId)
        Assertions.assertEquals("e-0806-b", positions[1].taskId)
        Assertions.assertEquals("1-1-1", positions[0].position)
        Assertions.assertEquals("1-2-1", positions[1].position)
    }

    @Test
    fun `collect cancel positions include job timeout plugin status`() {
        val model = Model(
            name = "p",
            desc = null,
            stages = listOf(
                Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
                Stage(
                    id = "stage-2",
                    name = "stage-1",
                    containers = listOf(
                        NormalContainer(
                            id = "1",
                            containerId = "1",
                            name = "构建环境-Linux",
                            status = BuildStatus.CANCELED.name,
                            elements = listOf(
                                LinuxScriptElement(
                                    id = "e-timeout",
                                    name = "0806插件",
                                    status = BuildStatus.EXEC_TIMEOUT.name,
                                    scriptType = BuildScriptType.SHELL,
                                    script = "echo",
                                    continueNoneZero = false
                                )
                            )
                        )
                    )
                )
            )
        )

        val positions = BuildEndPositionCollector.collectCancelPositions(model)

        Assertions.assertEquals(1, positions.size)
        Assertions.assertEquals("e-timeout", positions[0].taskId)
        Assertions.assertEquals(BuildStatus.EXEC_TIMEOUT.name, positions[0].statusAtEnd)
    }

    @Test
    fun `keep the first fifty fail positions and drop the rest`() {
        val jobs = (1..51).map { index ->
            failedPluginJob(containerId = index.toString(), taskIds = listOf("e-$index"))
        }

        val positions = BuildEndPositionCollector.collectFailPositions(stageModel(jobs))

        Assertions.assertEquals(BuildEndPositionCollector.POSITION_MAX_SIZE, positions.size)
        Assertions.assertEquals("e-1", positions.first().taskId)
        Assertions.assertEquals("e-50", positions.last().taskId)
        Assertions.assertTrue(positions.none { it.taskId == "e-51" })
    }

    @Test
    fun `fiftieth position keeps the job row of the boundary container`() {
        val jobs = (1..49).map { index ->
            failedPluginJob(containerId = index.toString(), taskIds = listOf("e-$index"))
        } + listOf(
            mutexCanceledJob("boundary"),
            failedPluginJob(containerId = "after", taskIds = listOf("e-after"))
        )

        val positions = BuildEndPositionCollector.collectFailPositions(stageModel(jobs))

        Assertions.assertEquals(BuildEndPositionCollector.POSITION_MAX_SIZE, positions.size)
        Assertions.assertEquals("e-49", positions[48].taskId)
        Assertions.assertEquals("boundary", positions[49].containerId)
        Assertions.assertNull(positions[49].taskId)
        Assertions.assertEquals(
            BuildEndPositionCollector.REASON_MUTEX_QUEUE_DISABLED,
            positions[49].reasonCode
        )
        Assertions.assertTrue(positions.none { it.taskId == "e-after" })
    }

    @Test
    fun `overflow inside the boundary container keeps the earliest plugins`() {
        val jobs = (1..49).map { index ->
            failedPluginJob(containerId = index.toString(), taskIds = listOf("e-$index"))
        } + failedPluginJob(containerId = "fat", taskIds = listOf("b1", "b2", "b3"))

        val positions = BuildEndPositionCollector.collectFailPositions(stageModel(jobs))

        Assertions.assertEquals(BuildEndPositionCollector.POSITION_MAX_SIZE, positions.size)
        Assertions.assertEquals("b1", positions.last().taskId)
        Assertions.assertTrue(positions.none { it.taskId == "b2" || it.taskId == "b3" })
    }

    @Test
    fun `display reason collapses whitespace and truncates`() {
        val raw = "Script command execution failed with exit code(127)\n\n" +
            "Error message:tracking-tmp/devops_script_user_${"x".repeat(300)}"
        val display = BuildEndPositionCollector.toDisplayReason(raw)

        Assertions.assertNotNull(display)
        Assertions.assertTrue(display!!.endsWith("..."))
        Assertions.assertTrue(display.length <= BuildEndPositionCollector.REASON_DISPLAY_MAX + 3)
        Assertions.assertFalse(display.contains("\n"))
    }

    @Test
    fun `fail aggregate downgrades timeout and keeps fast kill as multiple`() {
        val stepTimeout = endPosition(BuildEndType.TIMEOUT_STEP)
        val jobTimeout = endPosition(BuildEndType.TIMEOUT_JOB)
        val quality = endPosition(BuildEndType.FAIL_QUALITY)
        val fastKill = endPosition(BuildEndType.FAIL_FAST_KILL)

        Assertions.assertEquals(
            BuildEndType.FAIL_EXEC,
            BuildEndPositionCollector.aggregateFailEndType(listOf(stepTimeout))
        )
        Assertions.assertEquals(
            BuildEndType.FAIL_EXEC,
            BuildEndPositionCollector.aggregateFailEndType(listOf(stepTimeout, jobTimeout))
        )
        Assertions.assertEquals(
            BuildEndType.FAIL_MULTIPLE,
            BuildEndPositionCollector.aggregateFailEndType(listOf(stepTimeout, fastKill))
        )
        Assertions.assertEquals(
            BuildEndType.FAIL_EXEC,
            BuildEndPositionCollector.aggregateFailEndType(listOf(fastKill))
        )
        Assertions.assertEquals(
            BuildEndType.FAIL_MULTIPLE,
            BuildEndPositionCollector.aggregateFailEndType(listOf(stepTimeout, quality))
        )
        Assertions.assertEquals(BuildEndType.TIMEOUT_STEP, stepTimeout.endType)
    }

    private fun endPosition(endType: BuildEndType) = EndPosition(
        position = "1-1-1",
        componentPath = "stage/job/task",
        statusAtEnd = BuildStatus.FAILED.name,
        endType = endType,
        stageId = "stage-2",
        containerId = "1",
        taskId = "e-1"
    )

    private fun stageModel(containers: List<NormalContainer>) = Model(
        name = "p",
        desc = null,
        stages = listOf(
            Stage(containers = emptyList(), id = "stage-0", name = "trigger"),
            Stage(id = "stage-2", name = "stage-1", containers = containers)
        )
    )

    private fun failedPluginJob(containerId: String, taskIds: List<String>) = NormalContainer(
        id = containerId,
        containerId = containerId,
        name = "job-$containerId",
        status = BuildStatus.FAILED.name,
        elements = taskIds.map { taskId ->
            LinuxScriptElement(
                id = taskId,
                name = taskId,
                status = BuildStatus.FAILED.name,
                scriptType = BuildScriptType.SHELL,
                script = "echo",
                continueNoneZero = false
            )
        }
    )

    private fun mutexCanceledJob(containerId: String) = NormalContainer(
        id = containerId,
        containerId = containerId,
        name = "job-$containerId",
        status = BuildStatus.CANCELED.name,
        mutexGroup = MutexGroup(
            enable = true,
            mutexGroupName = "g",
            queueEnable = false
        ),
        elements = emptyList()
    )

    private fun pauseJob(containerId: String, taskId: String) = NormalContainer(
        id = containerId,
        containerId = containerId,
        name = "构建环境-Linux",
        status = BuildStatus.CANCELED.name,
        jobControlOption = JobControlOption(timeout = 1),
        elements = listOf(
            LinuxScriptElement(
                id = taskId,
                name = "0806插件",
                status = BuildStatus.PAUSE.name,
                scriptType = BuildScriptType.SHELL,
                script = "echo",
                continueNoneZero = false,
                additionalOptions = ElementAdditionalOptions(pauseBeforeExec = true)
            ),
            LinuxScriptElement(
                id = "e-unexec-$containerId",
                name = "Bash",
                status = BuildStatus.UNEXEC.name,
                scriptType = BuildScriptType.SHELL,
                script = "echo",
                continueNoneZero = false
            )
        )
    )
}
