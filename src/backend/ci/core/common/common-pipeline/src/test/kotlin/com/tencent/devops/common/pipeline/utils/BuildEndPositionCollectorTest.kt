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
}
