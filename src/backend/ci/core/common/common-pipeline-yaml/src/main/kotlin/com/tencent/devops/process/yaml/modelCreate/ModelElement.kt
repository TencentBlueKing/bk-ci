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

package com.tencent.devops.process.yaml.modelCreate

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.process.yaml.modelCreate.inner.InnerModelCreator
import com.tencent.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.utils.PathMatchUtils
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.step.Step
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ModelElement @Autowired(required = false) constructor(
    val client: Client,
    @Autowired(required = false)
    val inner: InnerModelCreator?
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ModelElement::class.java)
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun makeElementList(
        job: Job,
        changeSet: Set<String>? = null,
        jobEnable: Boolean = true,
        event: ModelCreateEvent
    ): MutableList<Element> {
        // 解析service
        val elementList = makeServiceElementList(job)
        // 解析job steps
        job.steps!!.forEach { step ->
            val timeout = setupTimeout(step)
            val additionalOptions = ElementAdditionalOptions(
                continueWhenFailed = step.continueOnError ?: false,
                timeout = timeout,
                timeoutVar = timeout.toString(),
                retryWhenFailed = step.retryTimes != null,
                retryCount = step.retryTimes ?: 0,
                enableCustomEnv = step.env != null,
                customEnv = getElementEnv(step.env),
                runCondition = when {
                    step.ifFiled.isNullOrBlank() -> RunCondition.PRE_TASK_SUCCESS
                    IfType.ALWAYS_UNLESS_CANCELLED.name == (step.ifFiled) ->
                        RunCondition.PRE_TASK_FAILED_BUT_CANCEL

                    IfType.ALWAYS.name == (step.ifFiled) ->
                        RunCondition.PRE_TASK_FAILED_EVEN_CANCEL

                    IfType.FAILURE.name == (step.ifFiled) ->
                        RunCondition.PRE_TASK_FAILED_ONLY

                    else -> RunCondition.CUSTOM_CONDITION_MATCH
                },
                customCondition = if (step.ifFiled.isNullOrBlank()) {
                    step.ifFiled
                } else {
                    ModelCreateUtil.removeIfBrackets(step.ifFiled)
                },
                manualRetry = false
            )

            additionalOptions.enable = jobEnable && PathMatchUtils.isIncludePathMatch(
                step.ifModify, changeSet, event.checkIfModify, event
            )
            // bash
            val element: Element? = when {
                step.run != null -> {
                    makeRunElement(step, job, additionalOptions)
                }

                step.checkout != null -> {
                    inner!!.makeCheckoutElement(step, event, additionalOptions)
                }

                else -> {
                    inner!!.makeMarketBuildAtomElement(job, step, event, additionalOptions)
                }
            }

            if (element != null) {
                // 统一禁用插件的retry属性
                element.canRetry = false
                elementList.add(element)

                if (element is MarketBuildAtomElement) {
                    ModelCommon.installMarketAtom(
                        client = client,
                        projectCode = event.projectCode,
                        userId = event.elementInstallUserId,
                        atomCode = element.getAtomCode()
                    )
                }
            }
        }

        return elementList
    }

    private fun setupTimeout(step: Step) = step.timeoutMinutes?.toLong() ?: 480

    private fun makeRunElement(
        step: Step,
        job: Job,
        additionalOptions: ElementAdditionalOptions
    ): Element {
        return if (inner!!.marketRunTask) {
            val data = mutableMapOf<String, Any>()
            data["input"] = mapOf(
                "script" to step.run,
                "shell" to (step.runAdditionalOptions?.get("shell") ?: "")
            )
            MarketBuildAtomElement(
                id = step.taskId,
                name = step.name ?: "run",
                stepId = step.id,
                atomCode = inner!!.runPlugInAtomCode ?: throw ModelCreateException("runPlugInAtomCode must exist"),
                version = inner!!.runPlugInVersion ?: throw ModelCreateException("runPlugInVersion must exist"),
                data = data,
                additionalOptions = additionalOptions
            )
        } else {
            val linux = LinuxScriptElement(
                id = step.taskId,
                name = step.name ?: "run",
                stepId = step.id,
                scriptType = BuildScriptType.SHELL,
                script = step.run!!,
                continueNoneZero = false,
                additionalOptions = additionalOptions
            )
            if (job.runsOn.agentSelector.isNullOrEmpty()) {
                linux
            } else {
                when (job.runsOn.agentSelector.first()) {
                    "linux" -> linux
                    "macos" -> linux
                    "windows" -> WindowsScriptElement(
                        id = step.taskId,
                        name = step.name ?: "run",
                        stepId = step.id,
                        scriptType = BuildScriptType.BAT,
                        script = step.run
                    )
                    else -> linux
                }
            }
        }
    }

    protected fun makeServiceElementList(job: Job): MutableList<Element> {
        return mutableListOf()
    }

    private fun getElementEnv(env: Map<String, Any?>?): List<NameAndValue>? {
        if (env == null) {
            return null
        }

        val nameAndValueList = mutableListOf<NameAndValue>()
        env.forEach {
            nameAndValueList.add(
                NameAndValue(
                    key = it.key,
                    value = it.value.toString()
                )
            )
        }

        return nameAndValueList
    }
}
