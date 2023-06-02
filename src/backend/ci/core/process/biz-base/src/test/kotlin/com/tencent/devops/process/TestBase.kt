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
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.ElementPostInfo
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.engine.common.Timeout
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
        const val illegalTimeoutVar = "illegalTimeoutVar"
        const val timeoutVar = "timeoutVar"
        const val biggerTimeoutVar = "biggerTimeoutVar"
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
                name = "trigger_stage",
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
                    name = stageId,
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
                    name = "stage-finally",
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

    private fun genContainers(seq: Int, jobSize: Int, elementSize: Int): List<Container> {
        val jobs = mutableListOf<Container>()
        when (seq) {
            0 -> {
                val elements = mutableListOf<Element>(
                    ManualTriggerElement(canElementSkip = true, useLatestParameters = true)
                )
                val list = mutableListOf<BuildFormProperty>()
                list.add(
                    BuildFormProperty(
                        id = timeoutVar,
                        required = true,
                        type = BuildFormPropertyType.STRING,
                        defaultValue = "100",
                        options = null,
                        desc = "job或task的超时分钟数",
                        repoHashId = null,
                        relativePath = null,
                        scmType = null,
                        containerType = null,
                        glob = null,
                        properties = null
                    )
                )
                list.add(
                    BuildFormProperty(
                        id = biggerTimeoutVar,
                        required = true,
                        type = BuildFormPropertyType.STRING,
                        defaultValue = (Timeout.MAX_MINUTES + 100L).toString(),
                        options = null,
                        desc = "job或task的超时分钟数(超出最大值100）",
                        repoHashId = null,
                        relativePath = null,
                        scmType = null,
                        containerType = null,
                        glob = null,
                        properties = null
                    )
                )

                list.add(
                    BuildFormProperty(
                        id = illegalTimeoutVar,
                        required = true,
                        type = BuildFormPropertyType.STRING,
                        defaultValue = "xyz",
                        options = null,
                        desc = "job或task的超时分钟数(非数字）",
                        repoHashId = null,
                        relativePath = null,
                        scmType = null,
                        containerType = null,
                        glob = null,
                        properties = null
                    )
                )
                jobs.add(TriggerContainer(id = "1", name = "trigger", elements = elements, params = list))
            }

            Int.MAX_VALUE -> { // finally
                for (i in 1..jobSize) {
                    if (i % 2 == 0) {
                        jobs.add(genNormal(seq, i, elementSize))
                    } else {
                        jobs.add(genVm(seq, i, elementSize, baseOS = VMBaseOS.MACOS))
                    }
                }
            }

            else -> {
                for (i in 1..jobSize) {
                    when {
                        i % 3 == 0 -> {
                            jobs.add(genVm(seq, i, elementSize, baseOS = VMBaseOS.LINUX))
                        }

                        i % 3 == 2 -> {
                            jobs.add(genVm(seq, i, elementSize, baseOS = VMBaseOS.WINDOWS))
                        }

                        else -> {
                            jobs.add(genVm(seq, i, elementSize, baseOS = VMBaseOS.MACOS))
                        }
                    }
                }
            }
        }
        return jobs
    }

    fun genNormal(
        stageSeq: Int,
        jobSeq: Int,
        elementSize: Int,
        jobControlOption: JobControlOption? = null
    ): Container {
        val name = "(BL)${stageSeq + 1}-$jobSeq"
        val normalContainer = NormalContainer(name = name)
        if (elementSize > 0) {
            normalContainer.elements = genRandomElements(name, buildLess = true, elementSize = elementSize)
        }
        normalContainer.jobControlOption = jobControlOption ?: JobControlOption()
        return normalContainer
    }

    fun genVm(
        stageSeq: Int,
        jobSeq: Int,
        elementSize: Int,
        baseOS: VMBaseOS,
        jobControlOption: JobControlOption? = null
    ): Container {
        val name = "($baseOS)${stageSeq + 1}-$jobSeq"
        val vmBuildContainer =
            if (baseOS == VMBaseOS.WINDOWS) {
                VMBuildContainer(baseOS = baseOS, thirdPartyAgentEnvId = "12", name = name)
            } else {
                VMBuildContainer(baseOS = baseOS, name = name)
            }
        if (elementSize > 0) {
            vmBuildContainer.elements = genRandomElements(name, buildLess = false, elementSize = elementSize)
        }
        vmBuildContainer.jobControlOption = jobControlOption ?: JobControlOption()
        return vmBuildContainer
    }

    fun genRandomElements(namePrefix: String, buildLess: Boolean = false, elementSize: Int): List<Element> {
        val list = mutableListOf<Element>()
        for (seq in 1..elementSize) {
            val name = "$namePrefix-$seq"
            if (buildLess) {
                list.add(
                    MarketBuildLessAtomElement(
                        name = name,
                        id = "e-${UUIDUtil.generate()}",
                        atomCode = atomCode,
                        data = mapOf("input" to mapOf(), "output" to mapOf<String, String>())
                    ).apply {
                        additionalOptions = elementAdditionalOptions()
                    }
                )
            } else {
                list.add(
                    MarketBuildAtomElement(
                        name = name,
                        id = "e-${UUIDUtil.generate()}",
                        atomCode = atomCode,
                        data = mapOf("input" to mapOf(), "output" to mapOf<String, String>())
                    ).apply {
                        additionalOptions = elementAdditionalOptions()
                    }
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
                    enable = true, runCondition = JobRunCondition.STAGE_RUNNING
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
