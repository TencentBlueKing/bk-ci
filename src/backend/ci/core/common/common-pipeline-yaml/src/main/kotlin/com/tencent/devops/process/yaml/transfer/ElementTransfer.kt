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

package com.tencent.devops.process.yaml.transfer

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.CharsetType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.transfer.IfType
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.RunAtomParam
import com.tencent.devops.common.pipeline.utils.TransferUtil
import com.tencent.devops.process.yaml.creator.ModelCreateException
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.transfer.pojo.CheckoutAtomParam
import com.tencent.devops.process.yaml.transfer.pojo.WebHookTriggerElementChanger
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.v3.models.IfField
import com.tencent.devops.process.yaml.v3.models.IfField.Mode
import com.tencent.devops.process.yaml.v3.models.TriggerType
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v3.models.on.EnableType
import com.tencent.devops.process.yaml.v3.models.on.ManualRule
import com.tencent.devops.process.yaml.v3.models.on.RemoteRule
import com.tencent.devops.process.yaml.v3.models.on.SchedulesRule
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.step.PreCheckoutStep
import com.tencent.devops.process.yaml.v3.models.step.PreManualReviewUserTaskElement
import com.tencent.devops.process.yaml.v3.models.step.Step
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexMethod")
class ElementTransfer @Autowired(required = false) constructor(
    val client: Client,
    @Autowired(required = false)
    val creator: TransferCreator,
    val transferCache: TransferCacheService,
    val triggerTransfer: TriggerTransfer
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ElementTransfer::class.java)
    }

    fun yaml2Triggers(yamlInput: YamlTransferInput, elements: MutableList<Element>) {
        yamlInput.yaml.formatTriggerOn(yamlInput.defaultScmType).forEach {
            yamlInput.aspectWrapper.setYamlTriggerOn(it.second, PipelineTransferAspectWrapper.AspectType.BEFORE)
            when (it.first) {
                TriggerType.BASE -> triggerTransfer.yaml2TriggerBase(yamlInput, it.second, elements)
                TriggerType.CODE_GIT -> triggerTransfer.yaml2TriggerGit(it.second, elements)
                TriggerType.CODE_TGIT -> triggerTransfer.yaml2TriggerTGit(it.second, elements)
                TriggerType.GITHUB -> triggerTransfer.yaml2TriggerGithub(it.second, elements)
                TriggerType.CODE_SVN -> triggerTransfer.yaml2TriggerSvn(it.second, elements)
                TriggerType.CODE_P4 -> triggerTransfer.yaml2TriggerP4(it.second, elements)
                TriggerType.CODE_GITLAB -> triggerTransfer.yaml2TriggerGitlab(it.second, elements)
            }
            yamlInput.aspectWrapper.setModelElement4Model(
                elements.last(),
                PipelineTransferAspectWrapper.AspectType.AFTER
            )
        }
    }

    fun baseTriggers2yaml(elements: List<Element>, aspectWrapper: PipelineTransferAspectWrapper): TriggerOn? {
        val triggerOn = lazy { TriggerOn() }
        val schedules = mutableListOf<SchedulesRule>()
        triggerOn.value.manual = ManualRule(
            enable = false
        )
        elements.forEach { element ->
            aspectWrapper.setModelElement4Model(
                element,
                PipelineTransferAspectWrapper.AspectType.BEFORE
            )
            if (element is ManualTriggerElement) {
                triggerOn.value.manual = ManualRule(
                    name = element.name,
                    enable = element.elementEnabled().nullIfDefault(true),
                    canElementSkip = element.canElementSkip.nullIfDefault(false),
                    useLatestParameters = element.useLatestParameters.nullIfDefault(false)
                )
                return@forEach
            }
            if (element is TimerTriggerElement) {
                val timePoints = element.newExpression?.map {
                    val (_, m, h) = it.split(" ")
                    "${h.padStart(2, '0')}:${m.padStart(2, '0')}"
                }
                val week: List<String>? = element.newExpression
                    ?.firstOrNull()
                    ?.split(" ")
                    ?.get(5)
                    ?.split(",")
                    ?.map m@{ w ->
                        when (w) {
                            "1" -> "Sun"
                            "2" -> "Mon"
                            "3" -> "Tue"
                            "4" -> "Wed"
                            "5" -> "Thu"
                            "6" -> "Fri"
                            "7" -> "Sat"
                            else -> return@m ""
                        }
                    }
                // ui->code,repositoryType为null时,repoType才需要在code模式下展示
                val (repoType, repoHashId, repoName) = when {
                    element.repositoryType == TriggerRepositoryType.ID && !element.repoHashId.isNullOrBlank() ->
                        Triple(null, element.repoHashId, null)

                    element.repositoryType == TriggerRepositoryType.NAME && !element.repoName.isNullOrBlank() ->
                        Triple(null, null, element.repoName)

                    element.repositoryType == TriggerRepositoryType.SELF ->
                        Triple(null, null, null)

                    else -> Triple(TriggerRepositoryType.NONE.name, null, null)
                }
                schedules.add(
                    SchedulesRule(
                        name = element.name,
                        interval = week?.let { SchedulesRule.Interval(week, timePoints) },
                        cron = if (element.advanceExpression?.size == 1) {
                            element.advanceExpression?.first()
                        } else {
                            element.advanceExpression
                        },
                        repoType = repoType,
                        repoId = repoHashId,
                        repoName = repoName,
                        branches = element.branches,
                        always = (element.noScm != true).nullIfDefault(false),
                        enable = element.elementEnabled().nullIfDefault(true),
                        startParams = element.convertStartParams()
                    )
                )
                return@forEach
            }
            if (element is RemoteTriggerElement) {
                triggerOn.value.remote = if (element.elementEnabled()) {
                    RemoteRule(element.name, EnableType.TRUE.value)
                } else {
                    RemoteRule(element.name, EnableType.FALSE.value)
                }
            }
        }
        if (schedules.isNotEmpty()) {
            triggerOn.value.schedules = schedules
        }
        if (triggerOn.isInitialized()) {
            aspectWrapper.setYamlTriggerOn(
                triggerOn.value,
                PipelineTransferAspectWrapper.AspectType.AFTER
            )
            return triggerOn.value
        }
        return null
    }

    fun scmTriggers2Yaml(
        elements: List<Element>,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper
    ): Map<ScmType, List<TriggerOn>> {
        val res = mutableMapOf<ScmType, List<TriggerOn>>()
        val fix = elements.groupBy { it.getClassType() }

        val gitElement = fix[CodeGitWebHookTriggerElement.classType]?.map {
            aspectWrapper.setModelElement4Model(it, PipelineTransferAspectWrapper.AspectType.BEFORE)
            WebHookTriggerElementChanger(it as CodeGitWebHookTriggerElement)
        }
        if (!gitElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(
                elements = gitElement,
                projectId = projectId,
                aspectWrapper = aspectWrapper,
                defaultName = "Git"
            )
            res.putAll(gitTrigger.groupBy { ScmType.CODE_GIT })
        }

        val tGitElement = fix[CodeTGitWebHookTriggerElement.classType]?.map {
            aspectWrapper.setModelElement4Model(it, PipelineTransferAspectWrapper.AspectType.BEFORE)
            WebHookTriggerElementChanger(it as CodeTGitWebHookTriggerElement)
        }
        if (!tGitElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(
                elements = tGitElement,
                projectId = projectId,
                aspectWrapper = aspectWrapper,
                defaultName = "TGit"
            )
            res.putAll(gitTrigger.groupBy { ScmType.CODE_TGIT })
        }

        val githubElement = fix[CodeGithubWebHookTriggerElement.classType]?.map {
            aspectWrapper.setModelElement4Model(it, PipelineTransferAspectWrapper.AspectType.BEFORE)
            WebHookTriggerElementChanger(it as CodeGithubWebHookTriggerElement)
        }
        if (!githubElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(
                elements = githubElement,
                projectId = projectId,
                aspectWrapper = aspectWrapper,
                defaultName = "GitHub"
            )
            res.putAll(gitTrigger.groupBy { ScmType.GITHUB })
        }

        val svnElement = fix[CodeSVNWebHookTriggerElement.classType]?.map {
            aspectWrapper.setModelElement4Model(it, PipelineTransferAspectWrapper.AspectType.BEFORE)
            WebHookTriggerElementChanger(it as CodeSVNWebHookTriggerElement)
        }
        if (!svnElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(
                elements = svnElement,
                projectId = projectId,
                aspectWrapper = aspectWrapper,
                defaultName = "SVN"
            )
            res.putAll(gitTrigger.groupBy { ScmType.CODE_SVN })
        }

        val p4Element = fix[CodeP4WebHookTriggerElement.classType]?.map {
            aspectWrapper.setModelElement4Model(it, PipelineTransferAspectWrapper.AspectType.BEFORE)
            WebHookTriggerElementChanger(it as CodeP4WebHookTriggerElement)
        }
        if (!p4Element.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(
                elements = p4Element,
                projectId = projectId,
                aspectWrapper = aspectWrapper,
                defaultName = "P4"
            )
            res.putAll(gitTrigger.groupBy { ScmType.CODE_P4 })
        }

        val gitlabElement = fix[CodeGitlabWebHookTriggerElement.classType]?.map {
            aspectWrapper.setModelElement4Model(it, PipelineTransferAspectWrapper.AspectType.BEFORE)
            WebHookTriggerElementChanger(it as CodeGitlabWebHookTriggerElement)
        }
        if (!gitlabElement.isNullOrEmpty()) {
            val gitTrigger = triggerTransfer.git2YamlTriggerOn(
                elements = gitlabElement,
                projectId = projectId,
                aspectWrapper = aspectWrapper,
                defaultName = "Gitlab"
            )
            res.putAll(gitTrigger.groupBy { ScmType.CODE_GITLAB })
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
            yamlInput.aspectWrapper.setYamlStep4Yaml(
                yamlStep = step,
                aspectType = PipelineTransferAspectWrapper.AspectType.BEFORE
            )
            val element: Element = yaml2element(
                userId = yamlInput.userId,
                step = step,
                agentSelector = job.runsOn.agentSelector?.first(),
                jobRunsOnType = JobRunsOnType.parse(job.runsOn.poolName)
            )
            yamlInput.aspectWrapper.setModelElement4Model(element, PipelineTransferAspectWrapper.AspectType.AFTER)
            elementList.add(element)
        }

        return elementList
    }

    fun yaml2element(
        userId: String,
        step: Step,
        agentSelector: String?,
        jobRunsOnType: JobRunsOnType? = null
    ): Element {
        val runCondition = when {
            step.ifField == null -> RunCondition.PRE_TASK_SUCCESS
            IfType.ALWAYS_UNLESS_CANCELLED.name == (step.ifField.expression) ->
                RunCondition.PRE_TASK_FAILED_BUT_CANCEL

            IfType.ALWAYS.name == (step.ifField.expression) ->
                RunCondition.PRE_TASK_FAILED_EVEN_CANCEL

            IfType.FAILURE.name == (step.ifField.expression) ->
                RunCondition.PRE_TASK_FAILED_ONLY

            !step.ifField.expression.isNullOrBlank() -> RunCondition.CUSTOM_CONDITION_MATCH

            step.ifField.mode == Mode.RUN_WHEN_ALL_PARAMS_MATCH -> RunCondition.CUSTOM_VARIABLE_MATCH
            step.ifField.mode == Mode.NOT_RUN_WHEN_ALL_PARAMS_MATCH -> RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN

            else -> RunCondition.PRE_TASK_SUCCESS
        }
        val continueOnError = Step.ContinueOnErrorType.parse(step.continueOnError)
        val additionalOptions = ElementAdditionalOptions(
            enable = step.enable ?: true,
            continueWhenFailed = continueOnError != null,
            manualSkip = continueOnError == Step.ContinueOnErrorType.MANUAL_SKIP,
            timeout = step.timeoutMinutes?.toLongOrNull() ?: VariableDefault.DEFAULT_TASK_TIME_OUT,
            timeoutVar = step.timeoutMinutes ?: VariableDefault.DEFAULT_TASK_TIME_OUT.toString(),
            retryWhenFailed = step.retryTimes != null && step.retryTimes > 0,
            retryCount = step.retryTimes ?: VariableDefault.DEFAULT_RETRY_COUNT,
            runCondition = runCondition,
            customCondition = if (runCondition == RunCondition.CUSTOM_CONDITION_MATCH) {
                step.ifField?.expression
            } else {
                null
            },
            customVariables = if (runCondition == RunCondition.CUSTOM_VARIABLE_MATCH ||
                runCondition == RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
            ) {
                step.ifField?.params?.map { NameAndValue(it.key, it.value) }
            } else {
                null
            },
            manualRetry = step.manualRetry ?: false,
            subscriptionPauseUser = userId
        )

        // bash
        val element: Element = when {
            step.run != null -> {
                makeRunElement(step, agentSelector)
            }

            step.uses?.contains("${LinuxScriptElement.classType}@") == true -> {
                LinuxScriptElement(
                    id = step.taskId,
                    name = step.name ?: "linuxScript",
                    stepId = step.id,
                    scriptType = BuildScriptType.SHELL,
                    script = step.with?.get(LinuxScriptElement::script.name) as String,
                    continueNoneZero = (step.with?.get(LinuxScriptElement::continueNoneZero.name) as Boolean?) ?: false,
                    enableArchiveFile = (step.with?.get(LinuxScriptElement::enableArchiveFile.name) as Boolean?)
                        ?: false,
                    archiveFile = step.with?.get(LinuxScriptElement::archiveFile.name) as String?
                )
            }

            step.uses?.contains("${WindowsScriptElement.classType}@") == true -> {
                WindowsScriptElement(
                    id = step.taskId,
                    name = step.name ?: "windowsScript",
                    stepId = step.id,
                    scriptType = BuildScriptType.BAT,
                    script = step.with?.get(WindowsScriptElement::script.name) as String,
                    charsetType = (step.with?.get(WindowsScriptElement::charsetType.name) as String?)?.let {
                        CharsetType.valueOf(it)
                    }
                )
            }

            step.uses?.contains("${ManualReviewUserTaskElement.classType}@") == true -> {
                val pre = step.with?.let {
                    JsonUtil.anyTo(it, object : TypeReference<PreManualReviewUserTaskElement>() {})
                }
                ManualReviewUserTaskElement(
                    id = step.taskId,
                    name = step.name ?: "manualReviewUserTask",
                    reviewUsers = pre?.reviewUsers ?: mutableListOf(),
                    desc = pre?.desc,
                    suggest = pre?.suggest,
                    params = pre?.params ?: mutableListOf(),
                    namespace = pre?.namespace,
                    notifyType = pre?.notifyType,
                    notifyTitle = pre?.notifyTitle,
                    markdownContent = pre?.markdownContent,
                    notifyGroup = pre?.notifyGroup,
                    reminderTime = pre?.reminderTime
                )
            }

            step.checkout != null -> {
                creator.transferCheckoutElement(step)
            }

            jobRunsOnType == JobRunsOnType.AGENT_LESS -> {
                creator.transferMarketBuildLessAtomElement(step)
            }

            else -> {
                creator.transferMarketBuildAtomElement(step)
            }
        }.apply {
            this.customEnv = ModelCreateUtil.getCustomEnv(step.env)
            this.additionalOptions = additionalOptions
        }
        return element
    }

    private fun makeRunElement(
        step: Step,
        agentSelector: String?
    ): Element {
        val type = step.runAdditionalOptions?.get(RunAtomParam::shell.name)
            ?: when (agentSelector) {
                "windows" ->
                    RunAtomParam.ShellType.CMD.shellName

                null -> null
                else -> RunAtomParam.ShellType.BASH.shellName
            }
        return when (type) {
//            RunAtomParam.ShellType.BASH.shellName -> LinuxScriptElement(
//                id = step.taskId,
//                name = step.name ?: "run",
//                stepId = step.id,
//                scriptType = BuildScriptType.SHELL,
//                script = step.run ?: "",
//                continueNoneZero = false
//            )
//
//            RunAtomParam.ShellType.CMD.shellName -> WindowsScriptElement(
//                id = step.taskId,
//                name = step.name ?: "run",
//                stepId = step.id,
//                scriptType = BuildScriptType.BAT,
//                script = step.run ?: "",
//                charsetType = step.with?.get(RunAtomParam::charsetType.name)?.toString()
//                    ?.let { CharsetType.valueOf(it) }
//            )

            else -> {
                val data = mutableMapOf<String, Any>()
                data["input"] = mapOf(
                    RunAtomParam::script.name to step.run,
                    RunAtomParam::shell.name to type,
                    RunAtomParam::charsetType.name to RunAtomParam.CharsetType.parse(
                        step.with?.get(RunAtomParam::charsetType.name)?.toString()
                    )
                )
                MarketBuildAtomElement(
                    id = step.taskId,
                    name = step.name ?: "run",
                    stepId = step.id,
                    atomCode = creator.runPlugInAtomCode ?: throw ModelCreateException("runPlugInAtomCode must exist"),
                    version = creator.runPlugInVersion ?: throw ModelCreateException("runPlugInVersion must exist"),
                    data = data
                )
            }
        }
    }

    fun model2YamlSteps(
        job: Container,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper
    ): List<PreStep> {
        val stepList = mutableListOf<PreStep>()
        job.elements.forEach { element ->
            aspectWrapper.setModelElement4Model(element, PipelineTransferAspectWrapper.AspectType.BEFORE)
            val step = element2YamlStep(element, projectId)
            aspectWrapper.setYamlStep4Yaml(
                yamlPreStep = step,
                aspectType = PipelineTransferAspectWrapper.AspectType.AFTER
            )
            if (step != null) {
                stepList.add(step)
            }
        }
        return stepList
    }

    @Suppress("ComplexMethod")
    fun element2YamlStep(element: Element, projectId: String): PreStep? {
        val uses = "${element.getAtomCode()}@${element.version}"
        return when {
            element.getAtomCode() == "checkout" && element is MarketBuildAtomElement -> {
                val input = element.data["input"] as Map<String, Any>? ?: emptyMap()
                val repositoryType = input[CheckoutAtomParam::repositoryType.name].toString().ifBlank { null }?.let {
                    CheckoutAtomParam.CheckoutRepositoryType.valueOf(it)
                }
                val repositoryHashId = input[CheckoutAtomParam::repositoryHashId.name].toString().ifBlank { null }
                val repositoryName = input[CheckoutAtomParam::repositoryName.name].toString().ifBlank { null }
                val repositoryUrl = input[CheckoutAtomParam::repositoryUrl.name].toString().ifBlank { null }
                val checkout = when (repositoryType) {
                    CheckoutAtomParam.CheckoutRepositoryType.ID -> PreCheckoutStep(repoId = repositoryHashId)
                    CheckoutAtomParam.CheckoutRepositoryType.NAME -> PreCheckoutStep(repoName = repositoryName)
                    CheckoutAtomParam.CheckoutRepositoryType.URL -> repositoryUrl
                    CheckoutAtomParam.CheckoutRepositoryType.SELF -> "self"
                    else -> null
                } ?: "self"
                // todo 等待checkout插件新增self参数
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    uses = null,
                    with = TransferUtil.simplifyParams(transferCache.getAtomDefaultValue(uses), input).apply {
                        this.remove(CheckoutAtomParam::repositoryType.name)
                        this.remove(CheckoutAtomParam::repositoryHashId.name)
                        this.remove(CheckoutAtomParam::repositoryName.name)
                        this.remove(CheckoutAtomParam::repositoryUrl.name)
                    }.ifEmpty { null },
                    checkout = checkout
                )
            }

            element.getAtomCode() == creator.runPlugInAtomCode && element is MarketBuildAtomElement -> {
                val input = element.data["input"] as Map<String, Any>? ?: emptyMap()
                PreStep(
                    name = element.name,
                    id = element.stepId,
                    uses = null,
                    with = TransferUtil.simplifyParams(
                        transferCache.getAtomDefaultValue(uses),
                        input.filterNot {
                            it.key == RunAtomParam::shell.name || it.key == RunAtomParam::script.name
                        }
                    ).ifEmpty { null },
                    run = input[RunAtomParam::script.name]?.toString(),
                    shell = input[RunAtomParam::shell.name]?.toString()
                )
            }

            else -> element.transferYaml(transferCache.getAtomDefaultValue(uses))
        }?.apply {
            this.ifField = parseStepIfFiled(element)
            this.enable = element.elementEnabled().nullIfDefault(true)
            this.timeoutMinutes =
                (element.additionalOptions?.timeoutVar ?: element.additionalOptions?.timeout?.toString()).nullIfDefault(
                    VariableDefault.DEFAULT_TASK_TIME_OUT.toString()
                )

            this.continueOnError = when {
                element.additionalOptions?.manualSkip == true -> Step.ContinueOnErrorType.MANUAL_SKIP.alis
                element.additionalOptions?.continueWhenFailed == true -> true
                else -> null
            }
            this.retryTimes = if (element.additionalOptions?.retryWhenFailed == true) {
                element.additionalOptions?.retryCount
            } else null
            this.manualRetry = element.additionalOptions?.manualRetry?.nullIfDefault(false)
            this.env = element.customEnv?.associateBy({ it.key ?: "" }) {
                it.value
            }?.ifEmpty { null }
        }
    }

    private fun parseStepIfFiled(
        step: Element
    ): Any? {
        return when (step.additionalOptions?.runCondition) {
            RunCondition.CUSTOM_CONDITION_MATCH -> step.additionalOptions?.customCondition
            RunCondition.CUSTOM_VARIABLE_MATCH -> IfField(
                mode = IfField.Mode.RUN_WHEN_ALL_PARAMS_MATCH,
                params = step.additionalOptions?.customVariables?.associateBy({ it.key ?: "" }, { it.value ?: "" })
            )

            RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> IfField(
                mode = IfField.Mode.NOT_RUN_WHEN_ALL_PARAMS_MATCH,
                params = step.additionalOptions?.customVariables?.associateBy({ it.key ?: "" }, { it.value ?: "" })
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

    protected fun makeServiceElementList(job: Job): MutableList<Element> {
        return mutableListOf()
    }
}
