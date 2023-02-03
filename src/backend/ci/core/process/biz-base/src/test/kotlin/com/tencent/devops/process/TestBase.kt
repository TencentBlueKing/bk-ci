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

package com.tencent.devops.process

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime

open class TestBase : BkCiAbstractTest() {

    var variables: MutableMap<String, String> = mutableMapOf()

    companion object {
        val nullObject = null
        val inputTypeConfigMap =
            mapOf("vuex-input" to 1024, "vuex-textarea" to 4096, "atom-ace-editor" to 16384, "default" to 1024)
        const val projectId = "devops1"
        const val buildId = "b-12345678901234567890123456789012"
        const val pipelineId = "p-12345678901234567890123456789012"
        const val containerHashId = "c-12345678901234567890123456789012"
        const val stageId = "stage-1"
        const val firstContainerId = "1"
        const val firstContainerIdInt = 1
        const val atomCode = "atomCode"
        const val userId = "user0"
    }

    @BeforeEach
    open fun setUp() {
        variables.clear()
    }

    fun genModel(stageSize: Int, jobSize: Int, elementSize: Int, needFinally: Boolean = false): Model {
        val stages = genStages(stageSize, jobSize, elementSize, needFinally)
        return Model(name = "DefaultModelCheckPluginTest", desc = "unit test", stages = stages)
    }

    fun genStages(stageSize: Int, jobSize: Int, elementSize: Int, needFinally: Boolean = false): List<Stage> {
        val stags = mutableListOf<Stage>()
        stags.add(
            Stage(
                containers = genContainers(
                    seq = 0,
                    jobSize = jobSize,
                    elementSize = elementSize
                ),
                id = "stage-trigger"
            )
        )
        for (seq in 1..stageSize) {
            val stageId = "stage-$seq"
            stags.add(
                Stage(
                    containers = genContainers(
                        seq = seq,
                        jobSize = jobSize,
                        elementSize = elementSize
                    ),
                    id = stageId
                )
            )
        }
        if (needFinally) {
            stags.add(
                Stage(
                    containers = genContainers(
                        seq = Int.MAX_VALUE,
                        jobSize = jobSize,
                        elementSize = elementSize
                    ),
                    id = "stage-finally"
                )
            )
        }
        return stags
    }

    fun genContainers(seq: Int, jobSize: Int, elementSize: Int): List<Container> {
        val jobs = mutableListOf<Container>()
        when (seq) {
            0 -> {
                val elements = mutableListOf<Element>(
                    ManualTriggerElement(canElementSkip = true, useLatestParameters = true)
                )
                jobs.add(TriggerContainer(id = "1", name = "trigger", elements = elements))
            }

            Int.MAX_VALUE -> { // finally
                for (i in 1..jobSize) {
                    if (i % 2 == 0) {
                        jobs.add(genNormal(elementSize))
                    } else {
                        jobs.add(genVm(elementSize, baseOS = VMBaseOS.MACOS))
                    }
                }
            }

            else -> {
                for (i in 1..jobSize) {
                    when {
                        i % 3 == 0 -> {
                            jobs.add(genVm(elementSize, baseOS = VMBaseOS.LINUX))
                        }

                        i % 3 == 2 -> {
                            jobs.add(genVm(elementSize, baseOS = VMBaseOS.WINDOWS))
                        }

                        else -> {
                            jobs.add(genVm(elementSize, baseOS = VMBaseOS.MACOS))
                        }
                    }
                }
            }
        }
        return jobs
    }

    fun genNormal(elementSize: Int): Container {
        val normalContainer = NormalContainer()
        if (elementSize > 0) {
            normalContainer.elements = genRandomElements(buildLess = true, elementSize = elementSize)
        }
        return normalContainer
    }

    fun genVm(elementSize: Int, baseOS: VMBaseOS): Container {
        val vmBuildContainer =
            if (baseOS == VMBaseOS.WINDOWS) {
                VMBuildContainer(baseOS = baseOS, thirdPartyAgentEnvId = "12")
            } else {
                VMBuildContainer(baseOS = baseOS)
            }
        if (elementSize > 0) {
            vmBuildContainer.elements = genRandomElements(buildLess = false, elementSize = elementSize)
        }
        return vmBuildContainer
    }

    fun genRandomElements(buildLess: Boolean = false, elementSize: Int): List<Element> {
        val list = mutableListOf<Element>()
        for (seq in 1..elementSize) {
            if (buildLess) {
                list.add(
                    MarketBuildLessAtomElement(
                        name = "${Math.random()} name",
                        id = "e-${UUIDUtil.generate()}",
                        atomCode = atomCode,
                        data = mapOf("input" to mapOf(), "output" to mapOf<String, String>())
                    )
                )
            } else {
                list.add(
                    MarketBuildAtomElement(
                        name = "${Math.random()} name",
                        id = "e-${UUIDUtil.generate()}",
                        atomCode = atomCode,
                        data = mapOf("input" to mapOf(), "output" to mapOf<String, String>())
                    )
                )
            }
        }
        return list
    }

    fun genVmBuildContainer(
        jobControlOption: JobControlOption? = null,
        id: Int? = firstContainerIdInt,
        status: BuildStatus = BuildStatus.RUNNING
    ): PipelineBuildContainer {
        val vmContainerType = "vmBuild"
        val startTime = LocalDateTime.now().minusMinutes(10)
        val containerCost = 5
        return PipelineBuildContainer(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            containerId = id?.toString() ?: firstContainerId,
            containerHashId = containerHashId,
            seq = id ?: firstContainerIdInt,
            containerType = vmContainerType,
            status = status,
            startTime = startTime,
            endTime = null,
            controlOption = PipelineBuildContainerControlOption(
                jobControlOption = jobControlOption ?: JobControlOption(
                    enable = true, timeout = 600, runCondition = JobRunCondition.STAGE_RUNNING
                ),
                mutexGroup = null
            ),
            cost = containerCost,
            matrixGroupId = null,
            matrixGroupFlag = null
        )
    }

    fun genTask(
        taskId: String,
        vmContainer: PipelineBuildContainer,
        elementAdditionalOptions: ElementAdditionalOptions? = null
    ): PipelineBuildTask {
        with(vmContainer) {
            return PipelineBuildTask(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerType = vmContainer.containerType, containerId = containerId,
                startTime = startTime?.plusSeconds(vmContainer.cost.toLong()), status = status, stageId = stageId,
                taskId = taskId, taskAtom = "", taskName = "Demo", taskParams = mutableMapOf(), taskSeq = 1,
                taskType = vmContainer.containerType, starter = "user1", stepId = null,
                containerHashId = containerId, approver = null, subProjectId = null, subBuildId = null,
                additionalOptions = elementAdditionalOptions ?: elementAdditionalOptions()
            )
        }
    }

    fun elementAdditionalOptions(
        enable: Boolean = true,
        runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS,
        customVariables: MutableList<NameAndValue>? = null,
        elementPostInfo: ElementPostInfo? = null,
        retryCount: Int = 0
    ): ElementAdditionalOptions {
        return ElementAdditionalOptions(
            enable = enable,
            runCondition = runCondition,
            customVariables = customVariables,
            retryCount = retryCount,
            elementPostInfo = elementPostInfo,
            otherTask = nullObject,
            customCondition = nullObject,
            subscriptionPauseUser = null
        )
    }
}
