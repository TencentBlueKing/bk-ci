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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun refreshCanRetry() {
        val containers = mutableListOf<Container>()
        val stages = mutableListOf<Stage>()
        val model = Model(name = "test", desc = "description", stages = stages)
        stages.add(Stage(containers = containers, id = "1"))
        val noRetryElement = ManualReviewUserTaskElement()
        containers.add(NormalContainer(elements = listOf(noRetryElement)))
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertFalse(noRetryElement.canRetry!!)

        noRetryElement.additionalOptions = elementAdditionalOptions(enable = true)
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertFalse(noRetryElement.canRetry!!)

        // 状态是成功的 则不允许 重试
        noRetryElement.additionalOptions = elementAdditionalOptions(enable = true)
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.SUCCEED)
        assertFalse(noRetryElement.canRetry!!)

        val retryElement = LinuxScriptElement(script = "pwd",
            scriptType = BuildScriptType.SHELL,
            continueNoneZero = false)
        val elements = mutableListOf(retryElement)
        containers.add(VMBuildContainer(baseOS = VMBaseOS.MACOS, elements = elements))
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertFalse(noRetryElement.canRetry!!)

        retryElement.canRetry = true
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertTrue(retryElement.canRetry!!)

        // 默认允许重试
        retryElement.additionalOptions = elementAdditionalOptions(enable = true)
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertTrue(retryElement.canRetry!!)

        val preTaskFailedRun = LinuxScriptElement(script = "cd ..",
            scriptType = BuildScriptType.SHELL,
            continueNoneZero = false)
        preTaskFailedRun.additionalOptions = elementAdditionalOptions(enable = true,
            runCondition = RunCondition.PRE_TASK_FAILED_BUT_CANCEL,
            continueWhenFailed = false)
        elements.add(preTaskFailedRun)
        // 通过前面插件即使失败也运行，让前置失败的插件不能重试
        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertFalse(retryElement.canRetry!!)
        assertFalse(preTaskFailedRun.canRetry!!)

        // 通过设置失败继续，让该插件不能重试
        preTaskFailedRun.additionalOptions = elementAdditionalOptions(enable = true,
            runCondition = RunCondition.PRE_TASK_FAILED_BUT_CANCEL,
            continueWhenFailed = true)

        ModelUtils.refreshCanRetry(model = model, canRetry = true, status = BuildStatus.FAILED)
        assertFalse(retryElement.canRetry!!)
        assertFalse(preTaskFailedRun.canRetry!!)
    }

    private fun elementAdditionalOptions(
        enable: Boolean = true,
        runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS,
        continueWhenFailed: Boolean = false
    ): ElementAdditionalOptions {

        return ElementAdditionalOptions(
            enable = enable,
            continueWhenFailed = continueWhenFailed,
            retryWhenFailed = false,
            runCondition = runCondition,
            customVariables = null,
            retryCount = 0,
            timeout = 100,
            otherTask = null,
            customCondition = null,
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
        )
    }
}
