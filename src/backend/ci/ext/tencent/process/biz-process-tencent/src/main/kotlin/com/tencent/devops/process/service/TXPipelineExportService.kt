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

package com.tencent.devops.process.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.ci.v2.Credentials
import com.tencent.devops.common.ci.v2.ExportPreScriptBuildYaml
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.PreJob
import com.tencent.devops.common.ci.v2.PreStage
import com.tencent.devops.common.ci.v2.RunsOn
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.store.StoreImageHelper
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.common.ci.v2.Step as V2Step
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime
import java.util.regex.Pattern
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Suppress("ALL")
@Service("TXPipelineExportService")
class TXPipelineExportService @Autowired constructor(
    private val stageTagService: StageTagService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val storeImageHelper: StoreImageHelper,
    private val scmProxyService: ScmProxyService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXPipelineExportService::class.java)
        private val yamlObjectMapper = ObjectMapper(YAMLFactory().enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
            .apply {
            registerModule(KotlinModule())
        }
    }

    private val checkoutAtomCodeSet = listOf(
        "gitCodeRepo",
        "gitCodeRepoCommon",
        "checkout"
    )

    // 导出工蜂CI-2.0的yml
    fun exportV2Yaml(userId: String, projectId: String, pipelineId: String, isGitCI: Boolean = false): Response {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = "用户($userId)无权限在工程($projectId)下导出流水线"
        )
        val model = pipelineRepositoryService.getModel(pipelineId) ?: throw ErrorCodeException(
            statusCode = Response.Status.BAD_REQUEST.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            defaultMessage = "流水线已不存在，请检查"
        )
        val yamlSb = getYamlStringBuilder(
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            isGitCI = isGitCI
        )

        // 将所有插件ID按编排顺序刷新
        var stepCount = 1
        model.stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    e.id = "step_$stepCount"
                    stepCount++
                }
            }
        }

        val pipelineGroupsMap = mutableMapOf<String, String>()
        pipelineGroupService.getGroups(userId, projectId).forEach {
            it.labels.forEach { label ->
                pipelineGroupsMap[label.id] = label.name
            }
        }
        val stageTagsMap = stageTagService.getAllStageTag().data?.map {
            it.id to it.stageTagName
        }?.toMap() ?: emptyMap()

        val output2Elements = mutableMapOf</*outputName*/String, MutableList<MarketBuildAtomElement>>()
        val variables = getVariableFromModel(model)
        val yamlObj = try {
            ExportPreScriptBuildYaml(
                version = "v2.0",
                name = model.name,
                label = model.labels.map { pipelineGroupsMap[it] ?: "" },
                triggerOn = null,
                variables = variables,
                stages = getV2StageFromModel(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    model = model,
                    comment = yamlSb,
                    stageTagsMap = stageTagsMap,
                    output2Elements = output2Elements,
                    variables = variables
                ),
                extends = null,
                resources = null,
                notices = null,
                finally = getV2FinalFromStage(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    stage = model.stages.last(),
                    comment = yamlSb,
                    output2Elements = output2Elements,
                    variables = variables
                )
            )
        } catch (t: Throwable) {
            logger.error("Export v2 yaml with error, return blank yml", t)
            if (t is ErrorCodeException) throw t
            ExportPreScriptBuildYaml(
                version = "v2.0",
                name = model.name,
                label = if (model.labels.isNullOrEmpty()) null else model.labels,
                triggerOn = null,
                variables = null,
                stages = null,
                extends = null,
                resources = null,
                notices = null,
                finally = null
            )
        }
        val modelYaml = toYamlStr(yamlObj)
        yamlSb.append(modelYaml)
        return exportToFile(yamlSb.toString(), model.name)
    }

    private fun getV2StageFromModel(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        comment: StringBuilder,
        stageTagsMap: Map<String, String>,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): List<PreStage> {
        val stages = mutableListOf<PreStage>()
        model.stages.drop(1).forEach { stage ->
            if (stage.finally) {
                return@forEach
            }
            val jobs = getV2JobFromStage(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                stage = stage,
                comment = comment,
                output2Elements = output2Elements,
                variables = variables
            )
            val tags = mutableListOf<String>()
            stage.tag?.forEach {
                val tagName = stageTagsMap[it]
                if (!tagName.isNullOrBlank()) tags.add(tagName)
            }
            stages.add(
                PreStage(
                    name = stage.name,
                    id = null,
                    label = tags,
                    ifField = if (stage.stageControlOption?.runCondition == StageRunCondition.CUSTOM_CONDITION_MATCH) {
                        stage.stageControlOption?.customCondition
                    } else {
                        null
                    },
                    fastKill = if (stage.fastKill == true) true else null,
                    jobs = jobs
                )
            )
        }
        return stages
    }

    private fun getV2FinalFromStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        stage: com.tencent.devops.common.pipeline.container.Stage,
        comment: StringBuilder,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): Map<String, PreJob>? {
        if (stage.finally) {
            return getV2JobFromStage(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                stage = stage,
                comment = comment,
                output2Elements = output2Elements,
                variables = variables
            )
        }
        return null
    }

    private fun getV2JobFromStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        stage: com.tencent.devops.common.pipeline.container.Stage,
        comment: StringBuilder,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): Map<String, PreJob>? {
        val jobs = mutableMapOf<String, PreJob>()
        stage.containers.forEach {
            val jobKey = if (!it.jobId.isNullOrBlank()) {
                it.jobId!!
            } else if (!it.id.isNullOrBlank()) {
                "job_${it.id!!}"
            } else {
                "unknown_job"
            }
            when (it.getClassType()) {
                NormalContainer.classType -> {
                    val job = it as NormalContainer
                    val timeoutMinutes = job.jobControlOption?.timeout ?: 480
                    jobs[jobKey] = PreJob(
                        name = job.name,
                        runsOn = RunsOn(
                            selfHosted = null,
                            poolName = JobRunsOnType.AGENT_LESS.type,
                            container = null
                        ),
                        container = null,
                        services = null,
                        ifField = if (job.jobControlOption?.runCondition ==
                            JobRunCondition.CUSTOM_CONDITION_MATCH) {
                            job.jobControlOption?.customCondition
                        } else {
                            null
                        },
                        steps = getV2StepFromJob(
                            projectId = projectId,
                            job = job,
                            comment = comment,
                            output2Elements = output2Elements,
                            variables = variables
                        ),
                        timeoutMinutes = if (timeoutMinutes < 480) timeoutMinutes else null,
                        env = null,
                        continueOnError = if (job.jobControlOption?.continueWhenFailed == true) true else null,
                        strategy = null,
                        // 蓝盾这边是自定义Job ID
                        dependOn = if (!job.jobControlOption?.dependOnId.isNullOrEmpty()) {
                            job.jobControlOption?.dependOnId
                        } else null
                    )
                }
                VMBuildContainer.classType -> {
                    val job = it as VMBuildContainer
                    val timeoutMinutes = job.jobControlOption?.timeout ?: 480

                    // 编译环境的相关映射处理
                    val runsOn = when (val dispatchType = getDispatchType(job)) {
                        is ThirdPartyAgentEnvDispatchType -> {
                            RunsOn(
                                selfHosted = true,
                                poolName = "### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###",
                                container = null,
                                agentSelector = listOf(job.baseOS.name.toLowerCase())
                            )
                        }
                        is DockerDispatchType -> {
                            val (containerImage, credentials) = getImageNameAndCredentials(
                                userId = userId,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                dispatchType = dispatchType
                            )
                            RunsOn(
                                selfHosted = null,
                                poolName = JobRunsOnType.DOCKER.type,
                                container = com.tencent.devops.common.ci.v2.Container(
                                    image = containerImage,
                                    credentials = credentials
                                ),
                                agentSelector = null
                            )
                        }
                        is PublicDevCloudDispathcType -> {
                            val (containerImage, credentials) = getImageNameAndCredentials(
                                userId = userId,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                dispatchType = dispatchType
                            )
                            RunsOn(
                                selfHosted = null,
                                poolName = JobRunsOnType.DOCKER.type,
                                container = com.tencent.devops.common.ci.v2.Container(
                                    image = containerImage,
                                    credentials = credentials
                                ),
                                agentSelector = null
                            )
                        }
                        is MacOSDispatchType -> {
                            RunsOn(
                                selfHosted = null,
                                poolName = "### 可以通过 runs-on: macos-10.15 使用macOS公共构建集群。" +
                                    "注意默认的Xcode版本为12.2，若需自定义，请在JOB下自行执行 xcode-select 命令切换 ###",
                                container = null,
                                agentSelector = null
                            )
                        }
                        else -> {
                            RunsOn(
                                selfHosted = null,
                                poolName = "### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###",
                                container = null,
                                agentSelector = null
                            )
                        }
                    }

                    jobs[jobKey] = PreJob(
                        name = job.name,
                        runsOn = runsOn,
                        container = null,
                        services = null,
                        ifField = if (job.jobControlOption?.runCondition ==
                            JobRunCondition.CUSTOM_CONDITION_MATCH) {
                            job.jobControlOption?.customCondition
                        } else {
                            null
                        },
                        steps = getV2StepFromJob(
                            projectId = projectId,
                            job = job,
                            comment = comment,
                            output2Elements = output2Elements,
                            variables = variables
                        ),
                        timeoutMinutes = if (timeoutMinutes < 480) timeoutMinutes else null,
                        env = null,
                        continueOnError = if (job.jobControlOption?.continueWhenFailed == true) true else null,
                        strategy = null,
                        dependOn = if (!job.jobControlOption?.dependOnId.isNullOrEmpty()) {
                            job.jobControlOption?.dependOnId
                        } else null
                    )
                }
                else -> {
                    logger.error("get jobs from stage failed, unknown classType:(${it.getClassType()})")
                }
            }
        }
        return if (jobs.isEmpty()) null else jobs
    }

    private fun getV2StepFromJob(
        projectId: String,
        job: Container,
        comment: StringBuilder,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): List<V2Step> {
        val stepList = mutableListOf<V2Step>()
        job.elements.forEach { element ->
            val originRetryTimes = element.additionalOptions?.retryCount ?: 0
            val originTimeout = element.additionalOptions?.timeout?.toInt() ?: 480
            val retryTimes = if (originRetryTimes > 1) originRetryTimes else null
            val timeoutMinutes = if (originTimeout < 480) originTimeout else null
            val continueOnError = if (element.additionalOptions?.continueWhenFailed == true) true else null
            when (element.getClassType()) {
                // Bash脚本插件直接转为run
                LinuxScriptElement.classType -> {
                    val step = element as LinuxScriptElement
                    stepList.add(
                        V2Step(
                            name = step.name,
                            id = step.id,
                            ifFiled = if (step.additionalOptions?.runCondition ==
                                RunCondition.CUSTOM_CONDITION_MATCH) {
                                step.additionalOptions?.customCondition
                            } else {
                                null
                            },
                            uses = null,
                            with = null,
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = formatScriptOutput(step.script, output2Elements, variables),
                            checkout = null
                        )
                    )
                }
                WindowsScriptElement.classType -> {
                    val step = element as WindowsScriptElement
                    stepList.add(
                        V2Step(
                            name = step.name,
                            id = step.id,
                            ifFiled = if (step.additionalOptions?.runCondition ==
                                RunCondition.CUSTOM_CONDITION_MATCH) {
                                step.additionalOptions?.customCondition
                            } else {
                                null
                            },
                            uses = null,
                            with = null,
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = formatScriptOutput(step.script, output2Elements, variables),
                            checkout = null
                        )
                    )
                }
                MarketBuildAtomElement.classType -> {
                    val step = element as MarketBuildAtomElement
                    val input = element.data["input"]
                    val output = element.data["output"]
                    val namespace = element.data["namespace"] as String?
                    val inputMap = if (input != null && !(input as MutableMap<String, Any>).isNullOrEmpty()) {
                        input
                    } else null
                    logger.info("[$projectId] getV2StepFromJob export MarketBuildAtom " +
                        "atomCode(${step.getAtomCode()}), inputMap=$inputMap, step=$step")
                    if (output != null && !(output as MutableMap<String, Any>).isNullOrEmpty()) {
                        output.keys.forEach { key ->
                            val outputWithNamespace = if (namespace.isNullOrBlank()) key else "${namespace}_$key"
                            val conflictElements = output2Elements[outputWithNamespace]
                            if (!conflictElements.isNullOrEmpty()) {
                                conflictElements.add(step)
                            } else {
                                output2Elements[outputWithNamespace] = mutableListOf(step)
                            }
                        }
                    }
                    val checkoutAtom = addCheckoutAtom(
                        projectId = projectId,
                        stepList = stepList,
                        atomCode = step.getAtomCode(),
                        step = step,
                        output2Elements = output2Elements,
                        variables = variables,
                        inputMap = inputMap,
                        timeoutMinutes = timeoutMinutes,
                        continueOnError = continueOnError,
                        retryTimes = retryTimes
                    )
                    if (!checkoutAtom) stepList.add(
                        V2Step(
                            name = step.name,
                            id = step.id,
                            ifFiled = if (step.additionalOptions?.runCondition ==
                                RunCondition.CUSTOM_CONDITION_MATCH) {
                                step.additionalOptions?.customCondition
                            } else {
                                null
                            },
                            uses = "${step.getAtomCode()}@${step.version}",
                            with = replaceMapWithDoubleCurlyBraces(inputMap, output2Elements, variables),
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = null,
                            checkout = null
                        )
                    )
                }
                MarketBuildLessAtomElement.classType -> {
                    val step = element as MarketBuildLessAtomElement
                    val input = element.data["input"]
                    val inputMap = if (input != null && !(input as MutableMap<String, Any>).isNullOrEmpty()) {
                        input
                    } else null
                    stepList.add(
                        V2Step(
                            name = step.name,
                            id = step.id,
                            ifFiled = if (step.additionalOptions?.runCondition ==
                                RunCondition.CUSTOM_CONDITION_MATCH) {
                                step.additionalOptions?.customCondition
                            } else {
                                null
                            },
                            uses = "${step.getAtomCode()}@${step.version}",
                            with = replaceMapWithDoubleCurlyBraces(inputMap, output2Elements, variables),
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = null,
                            checkout = null
                        )
                    )
                }
                ManualReviewUserTaskElement.classType -> {
                    val step = element as ManualReviewUserTaskElement
                    stepList.add(
                        V2Step(
                            name = null,
                            id = step.id,
                            ifFiled = null,
                            uses = "### [${step.name}] 人工审核插件请改用Stage审核 ###",
                            with = null,
                            timeoutMinutes = null,
                            continueOnError = null,
                            retryTimes = null,
                            env = null,
                            run = null,
                            checkout = null
                        )
                    )
                }
                else -> {
                    logger.info("Not support plugin:${element.getClassType()}, skip...")
                    comment.append(
                        "# 注意：不支持插件【${element.name}(${element.getClassType()})】的导出，" +
                            "请在蓝盾研发商店查找推荐的替换插件！\n"
                    )
                    stepList.add(
                        V2Step(
                            name = null,
                            id = element.id,
                            ifFiled = null,
                            uses = "### [${element.name}] 内置老插件不支持导出，请使用市场插件 ###",
                            with = null,
                            timeoutMinutes = null,
                            continueOnError = null,
                            retryTimes = null,
                            env = null,
                            run = null,
                            checkout = null
                        )
                    )
                }
            }
        }
        return stepList
    }

    fun replaceMapWithDoubleCurlyBraces(
        inputMap: MutableMap<String, Any>?,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): Map<String, Any?>? {
        if (inputMap.isNullOrEmpty()) {
            return null
        }
        val result = mutableMapOf<String, Any>()
        inputMap.forEach { (key, value) ->
            result[key] = replaceValueWithDoubleCurlyBraces(value, output2Elements, variables)
        }
        return result
    }

    private fun replaceValueWithDoubleCurlyBraces(
        value: Any,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): Any {
        if (value is String) {
            return replaceStringWithDoubleCurlyBraces(value, output2Elements, variables)
        }
        if (value is List<*>) {
            val result = mutableListOf<Any?>()
            value.forEach {
                if (it is String) {
                    result.add(replaceStringWithDoubleCurlyBraces(it, output2Elements, variables))
                } else {
                    result.add(it)
                }
            }
            return result
        }

        return value
    }

    private fun replaceStringWithDoubleCurlyBraces(
        value: String,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): String {
        val pattern = Pattern.compile("\\\$\\{([^{}]+?)}")
        val matcher = pattern.matcher(value)
        var newValue = value as String
        while (matcher.find()) {
            val originKey = matcher.group(1).trim()
            // 假设匹配到了前序插件的output则优先引用，否则引用全局变量
            val existingOutputElements = output2Elements[originKey]
            val realValue = if (!existingOutputElements.isNullOrEmpty()) {
                checkConflictOutput(originKey, existingOutputElements)
                "\${{ steps.${existingOutputElements.first().id}.outputs.$originKey }}"
            } else if (!variables?.get(originKey).isNullOrBlank()) {
                "\${{ variables.$originKey }}"
            } else {
                "\${{ $originKey }}"
            }
            newValue = newValue.replace(matcher.group(), realValue)
        }
        return newValue
    }

    private fun getYamlStringBuilder(
        projectId: String,
        pipelineId: String,
        model: Model,
        isGitCI: Boolean
    ): StringBuilder {

        val yamlSb = StringBuilder()
        yamlSb.append("############################################################################" +
            "#########################################\n")
        yamlSb.append("# 项目ID: $projectId \n")
        yamlSb.append("# 流水线ID: $pipelineId \n")
        yamlSb.append("# 流水线名称: ${model.name} \n")
        yamlSb.append("# 导出时间: ${DateTimeUtil.toDateTime(LocalDateTime.now())} \n")
        yamlSb.append("# \n")
        yamlSb.append("# 注意：不支持系统凭证(用户名、密码)的导出，请检查系统凭证的完整性！ \n")
        yamlSb.append("# 注意：[插件]输入参数可能存在敏感信息，请仔细检查，谨慎分享！！！ \n")
        if (isGitCI) {
            yamlSb.append("# 注意：[插件]Stream不支持蓝盾老版本的插件，请在研发商店搜索新插件替换 \n")
        }
        yamlSb.append("########################################################" +
            "#############################################################\n\n")
        return yamlSb
    }

    private fun exportToFile(yaml: String, pipelineName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
            val sb = StringBuilder()
            sb.append(yaml)
            output.write(sb.toString().toByteArray())
            output.flush()
        }
        val fileName = URLEncoder.encode("$pipelineName.yml", "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $fileName")
            .header("Cache-Control", "no-cache")
            .build()
    }

    private fun getVariableFromModel(model: Model): Map<String, String>? {
        val result = mutableMapOf<String, String>()
        (model.stages[0].containers[0] as TriggerContainer).params.forEach {
            result[it.id] = it.defaultValue.toString()
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    private fun toYamlStr(bean: Any?): String {
        return bean?.let {
            yamlObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(it)!!
        } ?: ""
    }

    /**
     * 新版的构建环境直接传入指定的构建机方式
     */
    private fun getDispatchType(param: VMBuildContainer): DispatchType {
        if (param.dispatchType != null) {
            return param.dispatchType!!
        } else {
            // 第三方构建机ID
            val agentId = param.thirdPartyAgentId ?: ""
            // 构建环境ID
            val envId = param.thirdPartyAgentEnvId ?: ""
            val workspace = param.thirdPartyWorkspace ?: ""
            return if (agentId.isNotBlank()) {
                ThirdPartyAgentIDDispatchType(displayName = agentId, workspace = workspace, agentType = AgentType.ID)
            } else if (envId.isNotBlank()) {
                ThirdPartyAgentEnvDispatchType(envName = envId, workspace = workspace, agentType = AgentType.ID)
            } // docker建机指定版本(旧)
            else if (!param.dockerBuildVersion.isNullOrBlank()) {
                DockerDispatchType(param.dockerBuildVersion!!)
            } else {
                ESXiDispatchType()
            }
        }
    }

    private fun getImageNameAndCredentials(
        userId: String,
        projectId: String,
        pipelineId: String,
        dispatchType: StoreDispatchType
    ): Pair<String, Credentials?> {
        try {
            when (dispatchType.imageType) {
                ImageType.BKSTORE -> {
                    val imageRepoInfo = storeImageHelper.getImageRepoInfo(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = "",
                        imageCode = dispatchType.imageCode,
                        imageVersion = dispatchType.imageVersion,
                        defaultPrefix = null
                    )
                    val completeImageName = if (ImageType.BKDEVOPS == imageRepoInfo.sourceType) {
                        // 蓝盾项目源镜像
                        "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                    } else {
                        // 第三方源镜像
                        // dockerhub镜像名称不带斜杠前缀
                        if (imageRepoInfo.repoUrl.isBlank()) {
                            imageRepoInfo.repoName
                        } else {
                            "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                        }
                    } + ":" + imageRepoInfo.repoTag
                    return Pair(completeImageName, Credentials(
                        "### 重新配置凭据(${imageRepoInfo.ticketId})后填入 ###",
                        "### 重新配置凭据(${imageRepoInfo.ticketId})后填入 ###"
                    ))
                }
                ImageType.BKDEVOPS -> {
                    // 针对非商店的旧数据处理
                    return if (dispatchType.value != DockerVersion.TLINUX1_2.value && dispatchType.value != DockerVersion.TLINUX2_2.value) {
                        dispatchType.dockerBuildVersion = "bkdevops/" + dispatchType.value
                        Pair("bkdevops/" + dispatchType.value, null)
                    } else {
                        Pair("### 该镜像暂不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###", null)
                    }
                }
                else -> {
                    return if (dispatchType.credentialId.isNullOrBlank()) {
                        Pair(dispatchType.value, null)
                    } else Pair(dispatchType.value, Credentials(
                        "### 重新配置凭据(${dispatchType.credentialId})后填入 ###",
                        "### 重新配置凭据(${dispatchType.credentialId})后填入 ###"
                    ))
                }
            }
        } catch (e: Exception) {
            return Pair("###请直接填入镜像(TLinux2.2公共镜像)的URL地址，若存在鉴权请增加 credentials 字段###", null)
        }
    }

    private fun formatScriptOutput(
        script: String,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?
    ): String {
        val regex = Regex("setEnv\\s+(.*[\\s]+.*)[\\s\\n]")
        val foundMatches = regex.findAll(script)
        var formatScript: String = script
        foundMatches.forEach { result ->
            val keyValueStr = if (result.groupValues.size >= 2) result.groupValues[1] else return@forEach
            val keyAndValue = keyValueStr.split(Regex("\\s+"))
            if (keyAndValue.size < 2) return@forEach
            val key = keyAndValue[0].removeSurrounding("\"")
            val value = keyAndValue[1].removeSurrounding("\"")
            formatScript =
                formatScript.replace(result.value, "echo \"::set-output name=$key::$value\"\n")
        }
        return replaceStringWithDoubleCurlyBraces(formatScript, output2Elements, variables)
    }

    private fun addCheckoutAtom(
        projectId: String,
        stepList: MutableList<V2Step>,
        atomCode: String,
        step: MarketBuildAtomElement,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElement>>,
        variables: Map<String, String>?,
        inputMap: MutableMap<String, Any>?,
        timeoutMinutes: Int?,
        continueOnError: Boolean?,
        retryTimes: Int?
    ): Boolean {
        if (inputMap == null || atomCode.isBlank() || !checkoutAtomCodeSet.contains(atomCode)) return false
        logger.info("[$projectId] addCheckoutAtom export with atomCode($atomCode), inputMap=$inputMap, step=$step")
        try {
            val repositoryUrl = inputMap["repositoryUrl"] as String?
            val url = if (repositoryUrl.isNullOrBlank()) {
                val repositoryHashId = inputMap["repositoryHashId"] as String?
                val repositoryName = inputMap["repositoryName"] as String?
                val repositoryType = inputMap["repositoryType"] as String?
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = repositoryHashId,
                    repositoryName = repositoryName,
                    repositoryType = RepositoryType.parseType(repositoryType)
                )
                val repo = scmProxyService.getRepo(projectId, repositoryConfig)
                repo.url
            } else {
                repositoryUrl
            }

            // 去掉所有插件上的凭证配置
            inputMap.remove("credentialId")
            inputMap.remove("ticketId")
            inputMap.remove("username")
            inputMap.remove("password")
            inputMap.remove("username")
            inputMap.remove("accessToken")
            inputMap.remove("personalAccessToken")

            // 去掉原来的仓库指定参数
            inputMap.remove("repositoryType")
            inputMap.remove("repositoryHashId")
            inputMap.remove("repositoryName")
            inputMap.remove("repositoryUrl")

            // 将鉴权类型统一刷成token方式
            inputMap["authType"] = "ACCESS_TOKEN"

            stepList.add(
                V2Step(
                    name = step.name,
                    id = step.id,
                    ifFiled = if (step.additionalOptions?.runCondition ==
                        RunCondition.CUSTOM_CONDITION_MATCH) {
                        step.additionalOptions?.customCondition
                    } else {
                        null
                    },
                    uses = null,
                    with = replaceMapWithDoubleCurlyBraces(inputMap, output2Elements, variables),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = null,
                    checkout = url
                )
            )
            return true
        } catch (e: Exception) {
            logger.error("[$projectId] addCheckoutAtom failed to convert atom[$atomCode]: ", e)
        }
        return false
    }

    private fun checkConflictOutput(
        key: String,
        existingOutputElements: MutableList<MarketBuildAtomElement>
    ) {
        if (existingOutputElements.size > 1) {
            val names = existingOutputElements.map { it.name }
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_EXPORT_OUTPUT_CONFLICT,
                defaultMessage = "变量名[$key]来源不唯一，请修改变量名称或增加插件输出命名空间：$names",
                params = arrayOf(key, names.toString())
            )
        }
    }
}
