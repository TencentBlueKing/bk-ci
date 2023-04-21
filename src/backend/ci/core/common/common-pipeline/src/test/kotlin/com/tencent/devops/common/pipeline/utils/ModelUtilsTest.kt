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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.util.JsonUtil.toJson
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("ALL")
class ModelUtilsTest {

    @Test
    fun initContainerOldData() {
        NormalContainer(enableSkip = true, conditions = listOf(NameAndValue(key = "a", value = "1"))).let { container ->
            ModelUtils.initContainerOldData(container)
            val jobControlOption = container.jobControlOption
            assertNotNull(jobControlOption)
            assertTrue(jobControlOption?.enable!!)
            assertTrue(jobControlOption.runCondition == JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN)
        }

        NormalContainer(enableSkip = true, conditions = listOf()).let { container ->
            ModelUtils.initContainerOldData(container)
            val jobControlOption = container.jobControlOption
            assertNotNull(jobControlOption)
            assertTrue(jobControlOption?.enable!!)
            assertTrue(jobControlOption.runCondition == JobRunCondition.STAGE_RUNNING)
        }

        VMBuildContainer(baseOS = VMBaseOS.MACOS).let { container ->
            ModelUtils.initContainerOldData(container)
            val jobControlOption = container.jobControlOption
            assertNotNull(jobControlOption)
            assertTrue(jobControlOption?.enable!!)
            assertTrue(jobControlOption.runCondition == JobRunCondition.STAGE_RUNNING)
        }
    }

    @Test
    fun canManualStartup() {

        val elements = mutableListOf<Element>()
        val triggerContainer = TriggerContainer(
            id = "1", name = "trigger", elements = elements
        )

        // 无任何手动触发器插件时
        assertFalse(ModelUtils.canManualStartup(triggerContainer))

        val defaultTriggerElement = ManualTriggerElement()
        elements.clear()
        elements.add(defaultTriggerElement)
        // 隐式enable, 默认
        assertTrue(ModelUtils.canManualStartup(triggerContainer))

        // 显式enable
        defaultTriggerElement.additionalOptions = elementAdditionalOptions(enable = true)
        assertTrue(ModelUtils.canManualStartup(triggerContainer))

        // 显式disable
        defaultTriggerElement.additionalOptions = elementAdditionalOptions(enable = false)
        assertFalse(ModelUtils.canManualStartup(triggerContainer))
    }

    @Test
    fun canRemoteStartup() {
        val elements = mutableListOf<Element>()
        val triggerContainer = TriggerContainer(
            id = "1", name = "trigger", elements = elements
        )

        // 无任何远程触发器插件时
        assertFalse(ModelUtils.canRemoteStartup(triggerContainer))

        val defaultTriggerElement = RemoteTriggerElement()
        elements.clear()
        elements.add(defaultTriggerElement)
        // 隐式enable, 默认
        assertTrue(ModelUtils.canRemoteStartup(triggerContainer))

        // 显式enable
        defaultTriggerElement.additionalOptions = elementAdditionalOptions(enable = true)
        assertTrue(ModelUtils.canRemoteStartup(triggerContainer))

        // 显式disable
        defaultTriggerElement.additionalOptions = elementAdditionalOptions(enable = false)
        assertFalse(ModelUtils.canRemoteStartup(triggerContainer))
    }

    @Test
    fun `single element retry`() {
        val containers = mutableListOf<Container>()
        val stages = mutableListOf<Stage>()
        val model = Model(name = "test", desc = "description", stages = stages)
        stages.add(
            Stage(
                id = "1",
                name = "trigger",
                containers = listOf(NormalContainer(elements = listOf(ManualReviewUserTaskElement()))),
                status = BuildStatus.SUCCEED.name
            )
        )

        val failStatus = BuildStatus.FAILED.name
        stages.add(Stage(name = "stage-2", id = "2", status = failStatus, containers = containers))

        val retryElement = LinuxScriptElement(
            script = "pwd",
            scriptType = BuildScriptType.SHELL,
            continueNoneZero = false
        )
        val elements = mutableListOf(retryElement)
        containers.add(VMBuildContainer(baseOS = VMBaseOS.MACOS, elements = elements, status = failStatus))

        // 没有指定manualRetry, 默认允许手动重试（为了兼容旧数据使用习惯））、没有指定manualSkip则默认不允许跳过
        retryElement.additionalOptions = elementAdditionalOptions()
        loopCheckElement(stage = stages[1], e = retryElement, model = model, canRetry = true, canSkip = false)

        // 指定要手动重试
        retryElement.additionalOptions = elementAdditionalOptions(manualRetry = true)

        loopCheckElement(stage = stages[1], e = retryElement, model = model, canRetry = true, canSkip = false)

        // 指定要手动重试 + 自动重试
        retryElement.additionalOptions = elementAdditionalOptions(manualRetry = true, retryWhenFailed = true)

        loopCheckElement(stage = stages[1], e = retryElement, model = model, canRetry = true, canSkip = false)

        // 指定自动跳过，所有状态都不会允许手动重试和手动跳过
        retryElement.additionalOptions = elementAdditionalOptions(manualRetry = true, continueWhenFailed = true)

        loopCheckElement(stage = stages[1], e = retryElement, model = model, canRetry = false, canSkip = false)

        // 指定要手动跳过 ，所有状态都不会允许重试, 但失败可跳过
        retryElement.additionalOptions = elementAdditionalOptions(
            manualRetry = false,
            continueWhenFailed = true,
            manualSkip = true
        )
        loopCheckElement(stage = stages[1], e = retryElement, model = model, canRetry = false, canSkip = true)

        // 指定要手动跳过+手动重试 ，所失败可跳过或重试
        retryElement.additionalOptions = elementAdditionalOptions(
            manualRetry = true,
            continueWhenFailed = true,
            manualSkip = true
        )
        loopCheckElement(stage = stages[1], e = retryElement, model = model, canRetry = true, canSkip = true)
    }

    @Test
    fun `element skip`() {
        val containers = mutableListOf<Container>()
        val stages = mutableListOf<Stage>()
        val model = Model(name = "test", desc = "description", stages = stages)
        stages.add(
            Stage(
                id = "1",
                name = "trigger",
                containers = listOf(NormalContainer(elements = listOf(ManualReviewUserTaskElement()))),
                status = BuildStatus.SUCCEED.name
            )
        )

        val failStatus = BuildStatus.FAILED
        stages.add(Stage(name = "stage-2", id = "2", status = failStatus.name, containers = containers))

        val elements = mutableListOf<Element>()
        containers.add(VMBuildContainer(baseOS = VMBaseOS.MACOS, elements = elements, status = failStatus.name))

        // 失败自动跳过的，不会出现重试按钮
        val e1 = LinuxScriptElement(script = "pwd", scriptType = BuildScriptType.SHELL, continueNoneZero = false)
        e1.additionalOptions = elementAdditionalOptions(
            continueWhenFailed = true,
            manualRetry = true,
            manualSkip = false
        )
        // 手动跳过
        val e2 = LinuxScriptElement(script = "pwd", scriptType = BuildScriptType.SHELL, continueNoneZero = false)
        e2.additionalOptions = elementAdditionalOptions(
            continueWhenFailed = true,
            manualSkip = true,
            runCondition = RunCondition.PRE_TASK_FAILED_ONLY
        ) // 没有指定manualRetry, 默认允许手动重试（为了兼容旧数据使用习惯）
        elements.add(e1)
        elements.add(e2)

        resetElement(e1, failStatus)
        resetElement(e2, failStatus)

        ModelUtils.refreshCanRetry(model)
        assertEquals(false, e1.canRetry ?: false) // e1是 失败自动跳过, 永远不会出现 重试或跳过
        assertEquals(false, e1.canSkip ?: false) // e1是 失败自动跳过, 永远不会出现 重试或跳过
        assertEquals(true, e2.canRetry) // 没有指定manualRetry, 默认允许手动重试（为了兼容旧数据使用习惯）
        assertEquals(true, e2.canSkip)

        resetElement(e1, failStatus)
        resetElement(e2, failStatus)
        // 设置了手动重试，不允许跳过
        e1.additionalOptions = elementAdditionalOptions(manualRetry = true)
        // 设置允许重试，并且前面有失败的也运行，将不会有任何跳过或重试的按钮
        e2.additionalOptions = elementAdditionalOptions(
            runCondition = RunCondition.PRE_TASK_FAILED_BUT_CANCEL,
            manualRetry = true
        )
        ModelUtils.refreshCanRetry(model)
        assertEquals(false, e1.canRetry ?: false) // e1是 受到e2 的永远不会出现 重试或跳过
        assertEquals(false, e1.canSkip ?: false) // 第一个是 失败自动跳过, 永远不会出现 重试或跳过
        assertEquals(false, e2.canSkip ?: false)
        assertEquals(false, e2.canRetry ?: false)

        resetElement(e1, BuildStatus.SUCCEED)
        resetElement(e2, failStatus)
        // 设置允许重试
        e2.additionalOptions = elementAdditionalOptions(
            runCondition = RunCondition.PRE_TASK_SUCCESS,
            manualRetry = true
        )
        ModelUtils.refreshCanRetry(model)
        assertEquals(false, e1.canRetry ?: false)
        assertEquals(false, e1.canSkip ?: false)
        assertEquals(false, e2.canSkip ?: false)
        assertEquals(true, e2.canRetry)

        resetElement(e1, BuildStatus.SUCCEED)
        resetElement(e2, failStatus)
        // 设置允许跳过
        e2.additionalOptions = elementAdditionalOptions(
            runCondition = RunCondition.PRE_TASK_SUCCESS,
            continueWhenFailed = true, manualSkip = true
        )
        ModelUtils.refreshCanRetry(model)
        assertEquals(false, e1.canRetry ?: false)
        assertEquals(false, e1.canSkip ?: false)
        assertEquals(true, e2.canRetry) // 没有指定manualRetry, 默认允许手动重试（为了兼容旧数据使用习惯）
        assertEquals(true, e2.canSkip)

        //
        resetElement(e1, BuildStatus.SUCCEED)
        resetElement(e2, failStatus)
        // 设置允许跳过+重试
        e2.additionalOptions = elementAdditionalOptions(
            runCondition = RunCondition.PRE_TASK_SUCCESS,
            manualRetry = true,
            continueWhenFailed = true, manualSkip = true
        )
        ModelUtils.refreshCanRetry(model)
        assertEquals(false, e1.canRetry ?: false)
        assertEquals(false, e1.canSkip ?: false)
        assertEquals(true, e2.canRetry)
        assertEquals(true, e2.canSkip)
    }

    private fun resetElement(e: Element, status: BuildStatus) {
        e.canSkip = null
        e.canRetry = null
        e.status = status.name
    }

    private fun loopCheckElement(stage: Stage, e: Element, model: Model, canRetry: Boolean, canSkip: Boolean) {
        stage.canRetry = null
        stage.containers[0].canRetry = null
        BuildStatus.values().forEach { status ->
            e.canSkip = null
            e.canRetry = null
            when {
                status.isFailure() -> {
                    e.status = BuildStatusSwitcher.taskStatusMaker.finish(status).name
                    ModelUtils.refreshCanRetry(model = model)
//                    println("$status|${e.name} = ${e.status}, canRetry=${e.canRetry}, canSkip=${e.canSkip}")
                    assertEquals(canRetry, e.canRetry ?: false)
                    assertEquals(canSkip, e.canSkip ?: false)
                }
                status.isCancel() -> {
                    e.status = BuildStatusSwitcher.taskStatusMaker.cancel(status).name
//                    println("$status|${e.name} = ${e.status}, canRetry=${e.canRetry}, canSkip=${e.canSkip}")
                    ModelUtils.refreshCanRetry(model = model)
                    assertEquals(canRetry, e.canRetry ?: false)
                    assertEquals(canSkip, e.canSkip ?: false)
                }
                else -> {
                    e.status = status.name
//                    println("$status|${e.name} = ${e.status}, canRetry=${e.canRetry}, canSkip=${e.canSkip}")
                    ModelUtils.refreshCanRetry(model = model)
                    assertEquals(false, e.canRetry ?: false)
                    assertEquals(false, e.canSkip ?: false)
                }
            }
        }
        assertEquals(true, stage.canRetry)
        assertEquals(true, stage.containers[0].canRetry)
    }

    private fun elementAdditionalOptions(
        enable: Boolean = true,
        runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS,
        continueWhenFailed: Boolean = false,
        manualSkip: Boolean? = null,
        manualRetry: Boolean = true,
        retryWhenFailed: Boolean = false
    ): ElementAdditionalOptions {

        return ElementAdditionalOptions(
            enable = enable,
            continueWhenFailed = continueWhenFailed,
            retryWhenFailed = retryWhenFailed,
            runCondition = runCondition,
            customVariables = null,
            manualRetry = manualRetry,
            manualSkip = manualSkip,
            retryCount = 1,
            timeout = 100,
            otherTask = null,
            customCondition = null,
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
        )
    }

    @Test
    fun generateBuildModelDetail() {
        // 结构相同的map对象合并
        var baseModelMap = mutableMapOf<String, Any>(
            "name" to "123",
            "desc" to "456"
        )
        var modelFieldRecordMap = mapOf<String, Any>(
            "name" to "哈哈",
            "desc" to "测试"
        )
        var mergeModelMap = ModelUtils.generateBuildModelDetail(baseModelMap, modelFieldRecordMap)
        assertEquals("{\"name\":\"哈哈\",\"desc\":\"测试\"}", toJson(mergeModelMap, false))
        // 结构不相同的map对象合并
        baseModelMap = mutableMapOf(
            "name" to "123",
            "desc" to "456",
            "note" to "789"
        )
        modelFieldRecordMap = mapOf(
            "name" to "哈哈",
            "desc" to "测试"
        )
        mergeModelMap = ModelUtils.generateBuildModelDetail(baseModelMap, modelFieldRecordMap)
        assertEquals("{\"name\":\"哈哈\",\"desc\":\"测试\",\"note\":\"789\"}", toJson(mergeModelMap, false))
        baseModelMap = mutableMapOf(
            "name" to "123",
            "desc" to "456"
        )
        modelFieldRecordMap = mapOf(
            "name" to "哈哈",
            "desc" to "测试",
            "note" to "789"
        )
        mergeModelMap = ModelUtils.generateBuildModelDetail(baseModelMap, modelFieldRecordMap)
        assertEquals("{\"name\":\"哈哈\",\"desc\":\"测试\",\"note\":\"789\"}", toJson(mergeModelMap, false))
        // 包含map对象和list对象的map集合合并
        baseModelMap = mutableMapOf(
            "name" to "123",
            "desc" to "456",
            "stages" to mutableListOf(
                mutableMapOf(
                    "id" to "stage-11",
                    "name" to "stage-name-11",
                    "status" to "BUILDING",
                    "dataList" to mutableListOf(
                        mutableListOf(1, 2),
                        mutableListOf(4, 5, 6)
                    )
                )
            )
        )
        modelFieldRecordMap = mutableMapOf(
            "name" to "哈哈",
            "desc" to "测试",
            "stages" to listOf(
                mapOf(
                    "id" to "stage-1111",
                    "name" to "stage-name-1111",
                    "status" to "TEST",
                    "dataList" to listOf<List<Any>>(
                        listOf(8, 2, 5),
                        listOf(),
                        listOf(7, 8, 9)
                    )
                )
            )
        )
        mergeModelMap = ModelUtils.generateBuildModelDetail(baseModelMap, modelFieldRecordMap)
        assertEquals("{\"name\":\"哈哈\",\"desc\":\"测试\",\"stages\":[{\"id\":\"stage-1111\"," +
            "\"name\":\"stage-name-1111\",\"status\":\"TEST\",\"dataList\":[[8,2,5],[4,5,6],[7,8,9]]}]}",
            toJson(mergeModelMap, false))
    }
}
