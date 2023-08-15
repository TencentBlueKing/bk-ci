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

package com.tencent.devops.process.yaml.modelTransfer

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.process.yaml.modelCreate.ModelCommon
import com.tencent.devops.process.yaml.modelCreate.ModelCreateException
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferModelCreator
import com.tencent.devops.process.yaml.modelTransfer.pojo.CheckoutAtomParam
import com.tencent.devops.process.yaml.modelTransfer.pojo.RunAtomParam
import com.tencent.devops.process.yaml.modelTransfer.pojo.WebHookTriggerElementChanger
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.process.yaml.v2.models.step.Step
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ElementTransfer @Autowired(required = false) constructor(
    val client: Client,
    @Autowired(required = false)
    val creator: TransferModelCreator,
    val transferCache: TransferCacheService,
    val triggerTransfer: TriggerTransfer
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ElementTransfer::class.java)
    }

    fun yaml2Triggers(triggerOns: Map<ScmType, TriggerOn>, elements: MutableList<Element>) {
        triggerOns.forEach {
            when (it.key) {
                ScmType.CODE_GIT -> triggerTransfer.yaml2TriggerGit(it.value, elements)
                ScmType.CODE_TGIT -> triggerTransfer.yaml2TriggerTGit(it.value, elements)
                ScmType.GITHUB -> triggerTransfer.yaml2TriggerGithub(it.value, elements)
            }
        }
    }

    fun triggers2Yaml(elements: List<Element>): Map<ScmType, TriggerOn> {
        val res = mutableMapOf<ScmType, TriggerOn>()
        val fix = elements.groupBy { it.getClassType() }

        val gitElement = fix[CodeGitWebHookTriggerElement.classType]?.map {
            WebHookTriggerElementChanger(it as CodeGitWebHookTriggerElement)
        }
        if (!gitElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(gitElement)
            res.putAll(gitTrigger.associateBy { ScmType.CODE_GIT })
        }

        val tGitElement = fix[CodeTGitWebHookTriggerElement.classType]?.map {
            WebHookTriggerElementChanger(it as CodeTGitWebHookTriggerElement)
        }
        if (!tGitElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(tGitElement)
            res.putAll(gitTrigger.associateBy { ScmType.CODE_TGIT })
        }

        val githubElement = fix[CodeGithubWebHookTriggerElement.classType]?.map {
            WebHookTriggerElementChanger(it as CodeGithubWebHookTriggerElement)
        }
        if (!githubElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(githubElement)
            res.putAll(gitTrigger.associateBy { ScmType.GITHUB })
        }
        return res
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun yaml2Elements(
        job: Job,
        yamlInput: YamlTransferInput
    ): MutableList<Element> {
        // 解析service
        val elementList = makeServiceElementList(job)
        // 解析job steps
        job.steps!!.forEach { step ->
            val element: Element = yaml2element(step, job.runsOn.agentSelector?.first())
            elementList.add(element)
        }

        return elementList
    }

    fun yaml2element(
        step: Step,
        agentSelector: String?
    ): Element {
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

        // bash
        val element: Element = when {
            step.run != null -> {
                makeRunElement(step, agentSelector, additionalOptions)
            }

            step.checkout != null -> {
                creator.transferCheckoutElement(step, additionalOptions)
            }

            else -> {
                creator.transferMarketBuildAtomElement(step, additionalOptions)
            }
        }
        return element
    }

    private fun setupTimeout(step: Step) = step.timeoutMinutes?.toLong() ?: 480

    private fun makeRunElement(
        step: Step,
        agentSelector: String?,
        additionalOptions: ElementAdditionalOptions
    ): Element {
        val type = step.runAdditionalOptions?.get(RunAtomParam::shell.name)
            ?: when (agentSelector) {
                "windows" ->
                    RunAtomParam.ShellType.CMD.shellName
                null -> null
                else -> RunAtomParam.ShellType.BASH.shellName
            }
        return when (type) {
            RunAtomParam.ShellType.BASH.shellName -> LinuxScriptElement(
                id = step.taskId,
                name = step.name ?: "run",
                stepId = step.id,
                scriptType = BuildScriptType.SHELL,
                script = step.run ?: "",
                continueNoneZero = false,
                additionalOptions = additionalOptions
            )
            RunAtomParam.ShellType.CMD.shellName -> WindowsScriptElement(
                id = step.taskId,
                name = step.name ?: "run",
                stepId = step.id,
                scriptType = BuildScriptType.BAT,
                script = step.run ?: ""
            )
            else -> {
                val data = mutableMapOf<String, Any>()
                data["input"] = mapOf(
                    RunAtomParam::script.name to step.run,
                    RunAtomParam::shell.name to type
                )
                MarketBuildAtomElement(
                    id = step.taskId,
                    name = step.name ?: "run",
                    stepId = step.id,
                    atomCode = creator.runPlugInAtomCode ?: throw ModelCreateException("runPlugInAtomCode must exist"),
                    version = creator.runPlugInVersion ?: throw ModelCreateException("runPlugInVersion must exist"),
                    data = data,
                    additionalOptions = additionalOptions
                )
            }
        }
    }

    fun model2YamlSteps(
        job: Container
    ): List<PreStep> {
        val stepList = mutableListOf<PreStep>()
        job.elements.forEach { element ->
            val step = element2YamlStep(element)
            if (step != null) {
                stepList.add(step)
            }
        }
        return stepList
    }

    @Suppress("ComplexMethod")
    fun element2YamlStep(element: Element): PreStep? {
        val retryTimes = element.additionalOptions?.retryCount ?: 0
        val timeoutMinutes = element.additionalOptions?.timeout?.toInt() ?: 480
        val continueOnError = element.additionalOptions?.continueWhenFailed
        val uses = "${element.getAtomCode()}@${element.version}"
        return when {
            element is LinuxScriptElement -> {
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    // bat插件上的
                    ifFiled = parseStepIfFiled(element),
                    uses = null,
                    with = null,
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = element.script,
                    checkout = null,
                    shell = RunAtomParam.ShellType.BASH.shellName
                )
            }
            element is WindowsScriptElement -> {
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    // bat插件上的
                    ifFiled = parseStepIfFiled(element),
                    uses = null,
                    with = null,
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = element.script,
                    checkout = null,
                    shell = RunAtomParam.ShellType.CMD.shellName
                )
            }
            element.getAtomCode() == "checkout" && element is MarketBuildAtomElement -> {
                val input = element.data["input"] as Map<String, Any>? ?: emptyMap()
                val url = input[CheckoutAtomParam::repositoryUrl.name].toString().ifBlank { null }
                // todo 等待checkout插件新增self参数
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    // 插件上的
                    ifFiled = parseStepIfFiled(element),
                    uses = null,
                    with = simplifyParams(uses, input),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = null,
                    checkout = url,
                    shell = null
                )
            }
            element.getAtomCode() == "run" && element is MarketBuildAtomElement -> {
                val input = element.data["input"] as Map<String, Any>? ?: emptyMap()
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    // 插件上的
                    ifFiled = parseStepIfFiled(element),
                    uses = null,
                    with = simplifyParams(
                        uses,
                        input.filterNot {
                            it.key == RunAtomParam::shell.name || it.key == RunAtomParam::script.name
                        }
                    ),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = input[RunAtomParam::script.name]?.toString(),
                    checkout = null,
                    shell = input[RunAtomParam::shell.name]?.toString()
                )
            }
            element is MarketBuildLessAtomElement -> {
                val input = element.data["input"] as Map<String, Any>? ?: emptyMap()
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    // 插件上的
                    ifFiled = parseStepIfFiled(element),
                    uses = uses,
                    with = simplifyParams(uses, input),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = null,
                    checkout = null,
                    shell = null
                )
            }
            element is MarketBuildAtomElement -> {
                val input = element.data["input"] as Map<String, Any>? ?: emptyMap()
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    // 插件上的
                    ifFiled = parseStepIfFiled(element),
                    uses = uses,
                    with = simplifyParams(uses, input),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = null,
                    checkout = null,
                    shell = null
                )
            }
            else -> null
        }
    }

    private fun parseStepIfFiled(
        step: Element
    ): String? {
        return when (step.additionalOptions?.runCondition) {
            RunCondition.CUSTOM_CONDITION_MATCH -> step.additionalOptions?.customCondition
            RunCondition.CUSTOM_VARIABLE_MATCH -> ModelCommon.customVariableMatch(
                step.additionalOptions?.customVariables
            )
            RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> ModelCommon.customVariableMatchNotRun(
                step.additionalOptions?.customVariables
            )
            RunCondition.PRE_TASK_FAILED_BUT_CANCEL ->
                IfType.ALWAYS_UNLESS_CANCELLED.name
            RunCondition.PRE_TASK_FAILED_EVEN_CANCEL ->
                IfType.ALWAYS.name
            RunCondition.PRE_TASK_FAILED_ONLY ->
                IfType.FAILURE.name
            else -> null
        }
    }

    private fun simplifyParams(uses: String, input: Map<String, Any>): Map<String, Any> {
        val out = input.toMutableMap()
        val defaultValue = transferCache.getAtomDefaultValue(uses)
        defaultValue.forEach {
            val value = out[it.key]
            if (value is String && it.value == value) {
                out.remove(it.key)
            }
            // 单独针对list的情况
            if (value is List<*> && it.value == value.joinToString(separator = ",")) {
                out.remove(it.key)
            }
        }
        return out
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
