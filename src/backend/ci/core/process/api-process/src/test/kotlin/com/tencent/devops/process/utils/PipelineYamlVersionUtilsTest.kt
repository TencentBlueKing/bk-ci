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

package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PipelineYamlVersionUtilsTest {

    @Test
    fun getVersions() {
        val model = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val sameModel = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel1 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer()
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel2 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer(),
                        NormalContainer()
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffModel3 = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t1"
                                )
                            )
                        )
                    )
                ),
                Stage(
                    id = "stage-2",
                    containers = listOf(
                        NormalContainer(),
                        NormalContainer(
                            elements = listOf(
                                LinuxScriptElement(
                                    script = "echo 1",
                                    continueNoneZero = true,
                                    scriptType = BuildScriptType.SHELL
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val diffTrigger = Model(
            name = "name1",
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = "t2"
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = "userId"
        )
        val version = 1
        assertEquals(PipelineVersionUtils.getPipelineVersion(version, model, sameModel), version)
        assertEquals(PipelineVersionUtils.getTriggerVersion(version, model, sameModel), version)
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model, diffModel1))
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, model, diffModel2))
        assertEquals(version + 1, PipelineVersionUtils.getTriggerVersion(version, model, diffTrigger))
        assertEquals(version + 1, PipelineVersionUtils.getPipelineVersion(version, diffModel2, diffModel3))
    }
}
