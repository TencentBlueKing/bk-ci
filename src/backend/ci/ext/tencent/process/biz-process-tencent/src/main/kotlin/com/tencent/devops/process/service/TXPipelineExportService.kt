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
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
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
import com.tencent.devops.process.pojo.JobPipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.MarketBuildAtomElementWithLocation
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.pojo.PipelineExportV2YamlData
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.YAME_META_DATA_JSON_FILTER
import com.tencent.devops.process.yaml.v2.models.export.ExportPreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.models.job.Strategy
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.process.yaml.v2.stageCheck.PreFlow
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageCheck
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageReviews
import com.tencent.devops.process.yaml.v2.stageCheck.ReviewVariable
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.atom.GetRelyAtom
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
    private val scmProxyService: ScmProxyService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXPipelineExportService::class.java)
        private val yamlObjectMapper = ObjectMapper(YAMLFactory().enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
            .apply {
                registerKotlinModule().setFilterProvider(
                    SimpleFilterProvider().addFilter(
                        YAME_META_DATA_JSON_FILTER,
                        SimpleBeanPropertyFilter.serializeAllExcept(YAME_META_DATA_JSON_FILTER)
                    )
                )
            }
    }

    private val checkoutAtomCodeSet = listOf(
        "gitCodeRepo",
        "gitCodeRepoCommon",
        "checkout"
    )

    // 导出工蜂CI-2.0的yml
    fun exportV2Yaml(userId: String, projectId: String, pipelineId: String, isGitCI: Boolean = false): Response {
        val pair = generateV2Yaml(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            isGitCI = isGitCI,
            exportFile = true
        )
        return exportToFile(pair.first, pair.second.name)
    }

    fun exportV2YamlStr(
        userId: String,
        projectId: String,
        pipelineId: String,
        isGitCI: Boolean = false
    ): PipelineExportV2YamlData {
        val pair = generateV2Yaml(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            isGitCI = isGitCI
        )
        return PipelineExportV2YamlData(pair.first, pair.third)
    }

    fun generateV2Yaml(
        userId: String,
        projectId: String,
        pipelineId: String,
        isGitCI: Boolean = false,
        exportFile: Boolean = false
    ): Triple<String, Model, Map<String, List<List<PipelineExportV2YamlConflictMapItem>>>> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = "用户($userId)无权限在工程($projectId)下导出流水线"
        )
        pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在",
                params = arrayOf(pipelineId)
            )

        val baseModel = pipelineRepositoryService.getModel(projectId, pipelineId) ?: throw ErrorCodeException(
            statusCode = Response.Status.BAD_REQUEST.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            defaultMessage = "流水线已不存在，请检查"
        )

        // 将所有插件ID按编排顺序刷新
        var stepCount = 1
        baseModel.stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    e.id = "step_$stepCount"
                    stepCount++
                }
            }
        }

        // 过滤出enable == false 的stage/job/step
        val filterStage = baseModel.stages.filter { it.stageControlOption?.enable != false }
        val enableStages: MutableList<Stage> = mutableListOf()
        filterStage.forEach { stageIt ->
            val filterContainer = stageIt.containers.filter { fit ->
                when (fit) {
                    is NormalContainer -> {
                        fit.jobControlOption?.enable != false
                    }
                    is VMBuildContainer -> {
                        fit.jobControlOption?.enable != false
                    }
                    is TriggerContainer -> true
                    else -> true
                }
            }
            filterContainer.forEach { elementIt ->
                elementIt.elements = elementIt.elements.filter { fit ->
                    fit.additionalOptions?.enable != false
                }
            }
            enableStages.add(stageIt.copy(containers = filterContainer))
        }
        val model = baseModel.copy(stages = enableStages)
        val yamlSb = getYamlStringBuilder(
            projectId = projectId,
            pipelineId = pipelineId,
            model = model,
            isGitCI = isGitCI
        )

        val pipelineGroupsMap = mutableMapOf<String, String>()
        pipelineGroupService.getGroups(userId, projectId).forEach {
            it.labels.forEach { label ->
                pipelineGroupsMap[label.id] = label.name
            }
        }

        // 获取流水线labels
        val groups = pipelineGroupService.getGroups(userId = userId, projectId = projectId, pipelineId = pipelineId)
        val labels = mutableListOf<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        model.labels = labels

        val stageTagsMap = stageTagService.getAllStageTag().data?.map {
            it.id to it.stageTagName
        }?.toMap() ?: emptyMap()

        val output2Elements = mutableMapOf</*outputName*/String, MutableList<MarketBuildAtomElementWithLocation>>()
        val outputConflictMap =
            mutableMapOf</*字段*/String, MutableList<List</*定位信息*/PipelineExportV2YamlConflictMapItem>>>()
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
                    variables = variables,
                    outputConflictMap = outputConflictMap,
                    exportFile = exportFile
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
                    variables = variables,
                    outputConflictMap = outputConflictMap,
                    exportFile = exportFile
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
        return Triple(yamlSb.toString(), model, outputConflictMap)
    }

    private fun getV2StageFromModel(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        comment: StringBuilder,
        stageTagsMap: Map<String, String>,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        exportFile: Boolean
    ): List<PreStage> {
        val stages = mutableListOf<PreStage>()
        model.stages.drop(1).forEach { stage ->
            if (stage.finally) {
                return@forEach
            }
            val pipelineExportV2YamlConflictMapItem =
                PipelineExportV2YamlConflictMapItem(
                    stage = PipelineExportV2YamlConflictMapBaseItem(
                        id = stage.id,
                        name = stage.name
                    )
                )
            val jobs = getV2JobFromStage(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                stage = stage,
                comment = comment,
                output2Elements = output2Elements,
                variables = variables,
                outputConflictMap = outputConflictMap,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                exportFile = exportFile
            ) ?: return@forEach
            val tags = mutableListOf<String>()
            stage.tag?.forEach {
                val tagName = stageTagsMap[it]
                if (!tagName.isNullOrBlank()) tags.add(tagName)
            }
            stages.add(
                PreStage(
                    name = stage.name,
                    label = tags,
                    ifField = when (stage.stageControlOption?.runCondition) {
                        StageRunCondition.CUSTOM_CONDITION_MATCH -> stage.stageControlOption?.customCondition
                        StageRunCondition.CUSTOM_VARIABLE_MATCH -> {
                            val ifString =
                                parseNameAndValueWithAnd(
                                    nameAndValueList = stage.stageControlOption?.customVariables,
                                    variables = variables,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                    output2Elements = output2Elements
                                )
                            if (stage.stageControlOption?.customVariables?.isEmpty() == true) null
                            else ifString
                        }
                        StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                            val ifString = parseNameAndValueWithOr(
                                nameAndValueList = stage.stageControlOption?.customVariables,
                                variables = variables,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                output2Elements = output2Elements
                            )
                            if (stage.stageControlOption?.customVariables?.isEmpty() == true) null
                            else ifString
                        }
                        else -> null
                    },
                    fastKill = if (stage.fastKill == true) true else null,
                    jobs = jobs,
                    checkIn = getCheckInForStage(stage),
                    // TODO 暂时不支持准出和gates的导出
                    checkOut = null
                )
            )
        }
        return stages
    }

    private fun getCheckInForStage(stage: Stage): PreStageCheck? {
        val reviews = PreStageReviews(
            flows = stage.checkIn?.reviewGroups?.map { PreFlow(it.name, it.reviewers) },
            variables = stage.checkIn?.reviewParams?.associate {
                it.key to ReviewVariable(
                    label = it.chineseName ?: it.key,
                    type = when (it.valueType) {
                        ManualReviewParamType.TEXTAREA -> "TEXTAREA"
                        ManualReviewParamType.ENUM -> "SELECTOR"
                        ManualReviewParamType.MULTIPLE -> "SELECTOR-MULTIPLE"
                        ManualReviewParamType.BOOLEAN -> "BOOL"
                        else -> "INPUT"
                    },
                    default = it.value,
                    values = it.options?.map { mit -> mit.key },
                    description = it.desc
                )
            },
            description = stage.checkIn?.reviewDesc
        )
        if (reviews.flows.isNullOrEmpty()) {
            return null
        }
        return PreStageCheck(
            reviews = reviews,
            gates = null,
            timeoutHours = stage.checkIn?.timeout
        )
    }

    private fun getV2FinalFromStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        stage: com.tencent.devops.common.pipeline.container.Stage,
        comment: StringBuilder,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        exportFile: Boolean
    ): Map<String, PreJob>? {
        if (stage.finally) {
            val pipelineExportV2YamlConflictMapItem =
                PipelineExportV2YamlConflictMapItem(
                    stage = PipelineExportV2YamlConflictMapBaseItem(
                        id = stage.id,
                        name = stage.name
                    )
                )
            return getV2JobFromStage(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                stage = stage,
                comment = comment,
                output2Elements = output2Elements,
                variables = variables,
                outputConflictMap = outputConflictMap,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                exportFile = exportFile
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
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
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
            pipelineExportV2YamlConflictMapItem.job =
                JobPipelineExportV2YamlConflictMapBaseItem(
                    id = it.id,
                    name = it.name,
                    jobId = it.jobId
                )
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
                        ifField = when (job.jobControlOption?.runCondition) {
                            JobRunCondition.CUSTOM_CONDITION_MATCH -> job.jobControlOption?.customCondition
                            JobRunCondition.CUSTOM_VARIABLE_MATCH -> {
                                val ifString =
                                    parseNameAndValueWithAnd(
                                        nameAndValueList = job.jobControlOption?.customVariables,
                                        variables = variables,
                                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                        output2Elements = output2Elements
                                    )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                                val ifString = parseNameAndValueWithOr(
                                    nameAndValueList = job.jobControlOption?.customVariables,
                                    variables = variables,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                    output2Elements = output2Elements
                                )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            else -> null
                        },
                        steps = getV2StepFromJob(
                            projectId = projectId,
                            job = job,
                            comment = comment,
                            output2Elements = output2Elements,
                            variables = variables,
                            outputConflictMap = outputConflictMap,
                            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                            exportFile = exportFile
                        ),
                        timeoutMinutes = if (timeoutMinutes < 480) timeoutMinutes else null,
                        env = null,
                        continueOnError = if (job.jobControlOption?.continueWhenFailed == true) true else null,
                        strategy = if (job.matrixGroupFlag == true) {
                            getMatrixFromJob(job.matrixControlOption)
                        } else null,
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
                                agentSelector = listOf(job.baseOS.name.toLowerCase()),
                                needs = job.buildEnv
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
                                container = Container2(
                                    image = containerImage,
                                    credentials = credentials
                                ),
                                agentSelector = null,
                                needs = job.buildEnv
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
                                container = Container2(
                                    image = containerImage,
                                    credentials = credentials
                                ),
                                agentSelector = null,
                                needs = job.buildEnv
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
                        ifField = when (job.jobControlOption?.runCondition) {
                            JobRunCondition.CUSTOM_CONDITION_MATCH -> job.jobControlOption?.customCondition
                            JobRunCondition.CUSTOM_VARIABLE_MATCH -> {
                                val ifString =
                                    parseNameAndValueWithAnd(
                                        nameAndValueList = job.jobControlOption?.customVariables,
                                        variables = variables,
                                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                        output2Elements = output2Elements
                                    )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                                val ifString = parseNameAndValueWithOr(
                                    nameAndValueList = job.jobControlOption?.customVariables,
                                    variables = variables,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                    output2Elements = output2Elements
                                )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            else -> null
                        },
                        steps = getV2StepFromJob(
                            projectId = projectId,
                            job = job,
                            comment = comment,
                            output2Elements = output2Elements,
                            variables = variables,
                            outputConflictMap = outputConflictMap,
                            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                            exportFile = exportFile
                        ),
                        timeoutMinutes = if (timeoutMinutes < 480) timeoutMinutes else null,
                        env = null,
                        continueOnError = if (job.jobControlOption?.continueWhenFailed == true) true else null,
                        strategy = if (job.matrixGroupFlag == true) {
                            getMatrixFromJob(job.matrixControlOption)
                        } else null,
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

    private fun getMatrixFromJob(
        matrixControlOption: MatrixControlOption?
    ): Strategy? {
        if (matrixControlOption == null)
            return null
        return Strategy(
            matrix = matrixControlOption.convertMatrixToYamlConfig() ?: return null,
            fastKill = matrixControlOption.fastKill,
            maxParallel = matrixControlOption.maxConcurrency
        )
    }

    private fun getV2StepFromJob(
        projectId: String,
        job: Container,
        comment: StringBuilder,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
    ): List<PreStep> {
        val stepList = mutableListOf<PreStep>()

        // 根据job里的elements统一查询数据库的store里的ATOM表prob字段
        val thirdPartyElementList = mutableListOf<ElementThirdPartySearchParam>()
        job.elements.forEach { element ->
            when (element.getClassType()) {
                MarketBuildAtomElement.classType -> {
                    val step = element as MarketBuildAtomElement
                    thirdPartyElementList.add(
                        ElementThirdPartySearchParam(
                            atomCode = step.getAtomCode(),
                            version = step.version
                        )
                    )
                }
                MarketBuildLessAtomElement.classType -> {
                    val step = element as MarketBuildLessAtomElement
                    thirdPartyElementList.add(
                        ElementThirdPartySearchParam(
                            atomCode = step.getAtomCode(),
                            version = step.version
                        )
                    )
                }
                else -> {
                }
            }
        }
        val relyList = try {
            client.get(ServiceMarketAtomResource::class).getAtomRely(GetRelyAtom(thirdPartyElementList))
        } catch (e: Exception) {
            logger.error("get Atom Rely error.", e)
            null
        }
        logger.info("[$projectId] getV2StepFromJob export relyList: ${relyList?.data} ")
        job.elements.forEach { element ->
            val originRetryTimes = element.additionalOptions?.retryCount ?: 0
            val originTimeout = element.additionalOptions?.timeout?.toInt() ?: 480
            val retryTimes = if (originRetryTimes > 1) originRetryTimes else null
            val timeoutMinutes = if (originTimeout < 480) originTimeout else null
            val continueOnError = if (element.additionalOptions?.continueWhenFailed == true) true else null
            pipelineExportV2YamlConflictMapItem.step =
                PipelineExportV2YamlConflictMapBaseItem(
                    id = element.id,
                    name = element.name
                )
            when (element.getClassType()) {
                // Bash脚本插件直接转为run
                LinuxScriptElement.classType -> {
                    val step = element as LinuxScriptElement
                    stepList.add(
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // bat插件上的
                            ifFiled = parseStepIfFiled(
                                step = step,
                                variables = variables,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                output2Elements = output2Elements
                            ),
                            uses = null,
                            with = null,
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = formatScriptOutput(
                                script = step.script,
                                output2Elements = output2Elements,
                                variables = variables,
                                outputConflictMap = outputConflictMap,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                exportFile = exportFile
                            ),
                            checkout = null,
                            shell = null
                        )
                    )
                }
                WindowsScriptElement.classType -> {
                    val step = element as WindowsScriptElement
                    stepList.add(
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // bat插件上的
                            ifFiled = parseStepIfFiled(
                                step = step,
                                variables = variables,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                output2Elements = output2Elements
                            ),
                            uses = null,
                            with = null,
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = formatScriptOutput(
                                script = step.script,
                                output2Elements = output2Elements,
                                variables = variables,
                                outputConflictMap = outputConflictMap,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                exportFile = exportFile
                            ),
                            checkout = null,
                            shell = null
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
                    logger.info(
                        "[$projectId] getV2StepFromJob export MarketBuildAtom " +
                            "atomCode(${step.getAtomCode()}), inputMap=$inputMap, step=$step"
                    )
                    if (output != null && !(output as MutableMap<String, Any>).isNullOrEmpty()) {
                        output.keys.forEach { key ->
                            val outputWithNamespace = if (namespace.isNullOrBlank()) key else "${namespace}_$key"
                            val conflictElements = output2Elements[outputWithNamespace]
                            val item = MarketBuildAtomElementWithLocation(
                                stageLocation = pipelineExportV2YamlConflictMapItem.stage?.copy(),
                                jobLocation = pipelineExportV2YamlConflictMapItem.job?.copy(),
                                stepAtom = step
                            )
                            if (!conflictElements.isNullOrEmpty()) {
                                conflictElements.add(item)
                            } else {
                                output2Elements[outputWithNamespace] = mutableListOf(item)
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
                        retryTimes = retryTimes,
                        relyMap = relyList?.data?.get(step.getAtomCode()),
                        outputConflictMap = outputConflictMap,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                        exportFile = exportFile
                    )
                    if (!checkoutAtom) stepList.add(
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // 插件上的
                            ifFiled = parseStepIfFiled(
                                step = step,
                                variables = variables,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                output2Elements = output2Elements
                            ),
                            uses = "${step.getAtomCode()}@${step.version}",
                            with = replaceMapWithDoubleCurlyBraces(
                                inputMap = inputMap,
                                output2Elements = output2Elements,
                                variables = variables,
                                relyMap = relyList?.data?.get(step.getAtomCode()),
                                outputConflictMap = outputConflictMap,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                exportFile = exportFile
                            ),
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
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
                        PreStep(
                            name = step.name,
                            id = step.stepId,
                            // 插件上的
                            ifFiled = parseStepIfFiled(
                                step = step,
                                variables = variables,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                output2Elements = output2Elements
                            ),
                            uses = "${step.getAtomCode()}@${step.version}",
                            with = replaceMapWithDoubleCurlyBraces(
                                inputMap = inputMap,
                                output2Elements = output2Elements,
                                variables = variables,
                                relyMap = relyList?.data?.get(step.getAtomCode()),
                                outputConflictMap = outputConflictMap,
                                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                exportFile = exportFile
                            ),
                            timeoutMinutes = timeoutMinutes,
                            continueOnError = continueOnError,
                            retryTimes = retryTimes,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
                        )
                    )
                }
                ManualReviewUserTaskElement.classType -> {
                    val step = element as ManualReviewUserTaskElement
                    stepList.add(
                        PreStep(
                            name = null,
                            id = step.stepId,
                            ifFiled = null,
                            uses = "### [${step.name}] 人工审核插件请改用Stage审核 ###",
                            with = null,
                            timeoutMinutes = null,
                            continueOnError = null,
                            retryTimes = null,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
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
                        PreStep(
                            name = null,
                            id = element.stepId,
                            ifFiled = null,
                            uses = "### [${element.name}] 内置老插件不支持导出，请使用市场插件 ###",
                            with = null,
                            timeoutMinutes = null,
                            continueOnError = null,
                            retryTimes = null,
                            env = null,
                            run = null,
                            checkout = null,
                            shell = null
                        )
                    )
                }
            }
        }
        return stepList
    }

    fun replaceMapWithDoubleCurlyBraces(
        inputMap: MutableMap<String, Any>?,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        relyMap: Map<String, Any>? = null,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
    ): Map<String, Any?>? {
        if (inputMap.isNullOrEmpty()) {
            return null
        }
        val result = mutableMapOf<String, Any>()
        inputMap.forEach lit@{ (key, value) ->
            val rely = relyMap?.get(key) as Map<String, Any>?
            if (rely.isNullOrEmpty()) {
                result[key] = replaceValueWithDoubleCurlyBraces(
                    value = value,
                    output2Elements = output2Elements,
                    variables = variables,
                    outputConflictMap = outputConflictMap,
                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                    exportFile = exportFile
                )
                return@lit
            }

            if (rely["expression"] == null) {
                return@lit
            }

            try {
                val expression = rely["expression"] as List<Map<String, Any>>
                when (rely["operation"]) {
                    "AND" -> {
                        expression.forEach {
                            if (checkRely(inputMap[it["key"]], it["value"], it["regex"])) {
                                result[key] = replaceValueWithDoubleCurlyBraces(
                                    value = value,
                                    output2Elements = output2Elements,
                                    variables = variables,
                                    outputConflictMap = outputConflictMap,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                    exportFile = exportFile
                                )
                                return@lit
                            }
                        }
                    }
                    "OR" -> {
                        expression.forEach {
                            if (checkRely(inputMap[it["key"]], it["value"], it["regex"])) {
                                result[key] = replaceValueWithDoubleCurlyBraces(
                                    value = value,
                                    output2Elements = output2Elements,
                                    variables = variables,
                                    outputConflictMap = outputConflictMap,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                                    exportFile = exportFile
                                )
                                return@lit
                            }
                        }
                        return@lit
                    }
                }
            } catch (e: Exception) {
                logger.warn("load atom input[rely] with error: ${e.message} ,rely=$rely")
            }
        }
        return result
    }

    private fun checkRely(key: Any?, value: Any?, regex: Any?): Boolean {
        if (value != null) return key == value
        if (regex != null) return key.toString().contains(Regex(regex.toString()))
        return false
    }

    private fun replaceValueWithDoubleCurlyBraces(
        value: Any,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
    ): Any {
        if (value is String) {
            return replaceStringWithDoubleCurlyBraces(
                value = value,
                output2Elements = output2Elements,
                variables = variables,
                outputConflictMap = outputConflictMap,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                exportFile = exportFile
            )
        }
        if (value is List<*>) {
            val result = mutableListOf<Any?>()
            value.forEach {
                if (it is String) {
                    result.add(
                        replaceStringWithDoubleCurlyBraces(
                            value = it,
                            output2Elements = output2Elements,
                            variables = variables,
                            outputConflictMap = outputConflictMap,
                            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                            exportFile = exportFile
                        )
                    )
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
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
    ): String {
        val pattern = Pattern.compile("\\\$\\{\\{?([^{}]+?)}?}")
        val matcher = pattern.matcher(value)
        var newValue = value as String
        while (matcher.find()) {
            val originKey = matcher.group(1).trim()
            // 假设匹配到了前序插件的output则优先引用，否则引用全局变量
            val existingOutputElements = output2Elements[originKey]
            var lastExistingOutputElements = MarketBuildAtomElementWithLocation()
            run outside@{
                existingOutputElements?.reversed()?.forEach {
                    if (it.jobLocation?.id == pipelineExportV2YamlConflictMapItem.job?.id ||
                        it.stageLocation?.id != pipelineExportV2YamlConflictMapItem.stage?.id
                    ) {
                        lastExistingOutputElements = it
                        return@outside
                    }
                }
            }
            val ciName = PipelineVarUtil.fetchReverseVarName(originKey)
            val namespace = lastExistingOutputElements.stepAtom?.data?.get("namespace") as String?
            val originKeyWithNamespace = if (!namespace.isNullOrBlank()) {
                originKey.replace("${namespace}_", "")
            } else originKey

            val realValue = when {
                lastExistingOutputElements.stepAtom != null &&
                    lastExistingOutputElements.jobLocation?.jobId != null -> {
                    checkConflictOutput(
                        key = originKey,
                        existingOutputElements = existingOutputElements!!,
                        outputConflictMap = outputConflictMap,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                        exportFile = exportFile
                    )
                    "\${{ jobs.${lastExistingOutputElements.jobLocation?.jobId}.steps." +
                        "${namespace?.ifBlank { null } ?: lastExistingOutputElements.stepAtom?.id}" +
                        ".outputs.$originKeyWithNamespace }}"
                }
                !variables?.get(originKey).isNullOrBlank() -> "\${{ variables.$originKeyWithNamespace }}"
                !ciName.isNullOrBlank() -> "\${{ $ciName }}"
                else -> "\${{ $originKeyWithNamespace }}"

            }
            newValue = newValue.replace(matcher.group(), realValue)
        }
        return removeExcessIndentation(newValue)
    }

    private fun removeExcessIndentation(value: String): String {
        val regex = Regex("\\n(\\t+|\\s+)\\n")
        return value.replace(regex, "\n\n")
    }

    private fun getYamlStringBuilder(
        projectId: String,
        pipelineId: String,
        model: Model,
        isGitCI: Boolean
    ): StringBuilder {

        val yamlSb = StringBuilder()
        yamlSb.append(
            "############################################################################" +
                "#########################################\n"
        )
        yamlSb.append("# 项目ID: $projectId \n")
        yamlSb.append("# 流水线ID: $pipelineId \n")
        yamlSb.append("# 流水线名称: ${model.name} \n")
        yamlSb.append("# 导出时间: ${DateTimeUtil.toDateTime(LocalDateTime.now())} \n")
        yamlSb.append("# \n")
        yamlSb.append("# 注意：不支持系统凭证(用户名、密码)的导出，请在stream项目设置下重新添加凭据：https://iwiki.woa.com/p/800638064 ！ \n")
        yamlSb.append("# 注意：[插件]输入参数可能存在敏感信息，请仔细检查，谨慎分享！！！ \n")
        if (isGitCI) {
            yamlSb.append("# 注意：[插件]Stream不支持蓝盾老版本的插件，请在研发商店搜索新插件替换 \n")
        }
        yamlSb.append(
            "########################################################" +
                "#############################################################\n\n"
        )
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
                ThirdPartyAgentEnvDispatchType(
                    envName = envId,
                    envProjectId = null,
                    workspace = workspace,
                    agentType = AgentType.ID
                )
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
    ): Pair<String, String?> {
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
                    return if (imageRepoInfo.publicFlag) {
                        Pair(completeImageName, null)
                    } else Pair(
                        completeImageName, imageRepoInfo.ticketId
                    )
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
                    } else Pair(
                        dispatchType.value, dispatchType.credentialId
                    )
                }
            }
        } catch (e: Exception) {
            return Pair("###请直接填入镜像(TLinux2.2公共镜像)的URL地址，若存在鉴权请增加 credentials 字段###", null)
        }
    }

    fun formatScriptOutput(
        script: String,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
    ): String {
        val regex = Regex("setEnv\\s+(\"?.*\"?)\\s*\\n?")
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
        return replaceStringWithDoubleCurlyBraces(
            value = formatScript,
            output2Elements = output2Elements,
            variables = variables,
            outputConflictMap = outputConflictMap,
            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
            exportFile = exportFile
        )
    }

    private fun addCheckoutAtom(
        projectId: String,
        stepList: MutableList<PreStep>,
        atomCode: String,
        step: MarketBuildAtomElement,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        variables: Map<String, String>?,
        inputMap: MutableMap<String, Any>?,
        timeoutMinutes: Int?,
        continueOnError: Boolean?,
        retryTimes: Int?,
        relyMap: Map<String, Any>? = null,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
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

            // branchName应该转换成refName
            val branchName = inputMap.remove("branchName")
            if (branchName != null) {
                inputMap["refName"] = branchName
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
            inputMap.remove("authType")

            stepList.add(
                PreStep(
                    name = step.name,
                    id = step.stepId,
                    // 插件上的
                    ifFiled = parseStepIfFiled(
                        step = step,
                        variables = variables,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                        output2Elements = output2Elements
                    ),
                    uses = null,
                    with = replaceMapWithDoubleCurlyBraces(
                        inputMap = inputMap,
                        output2Elements = output2Elements,
                        variables = variables,
                        relyMap = relyMap,
                        outputConflictMap = outputConflictMap,
                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                        exportFile = exportFile
                    ),
                    timeoutMinutes = timeoutMinutes,
                    continueOnError = continueOnError,
                    retryTimes = retryTimes,
                    env = null,
                    run = null,
                    checkout = url,
                    shell = null
                )
            )
            return true
        } catch (e: Exception) {
            logger.error("[$projectId] addCheckoutAtom failed to convert atom[$atomCode]: ", e)
        }
        return false
    }

    private fun parseStepIfFiled(
        step: Element,
        variables: Map<String, String>?,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>
    ): String? {
        return when (step.additionalOptions?.runCondition) {
            RunCondition.CUSTOM_CONDITION_MATCH -> step.additionalOptions?.customCondition
            RunCondition.CUSTOM_VARIABLE_MATCH -> {
                val ifString = parseNameAndValueWithAnd(
                    nameAndValueList = step.additionalOptions?.customVariables,
                    variables = variables,
                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                    output2Elements = output2Elements
                )
                if (step.additionalOptions?.customVariables?.isEmpty() == true) null
                else ifString
            }
            RunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                val ifString = parseNameAndValueWithOr(
                    nameAndValueList = step.additionalOptions?.customVariables,
                    variables = variables,
                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                    output2Elements = output2Elements
                )
                if (step.additionalOptions?.customVariables?.isEmpty() == true) null
                else ifString
            }
            RunCondition.PRE_TASK_FAILED_BUT_CANCEL ->
                IfType.ALWAYS_UNLESS_CANCELLED.name
            RunCondition.PRE_TASK_FAILED_EVEN_CANCEL ->
                IfType.ALWAYS.name
            RunCondition.PRE_TASK_FAILED_ONLY ->
                IfType.FAILURE.name
            else -> null
        }
    }

    private fun checkConflictOutput(
        key: String,
        existingOutputElements: MutableList<MarketBuildAtomElementWithLocation>,
        outputConflictMap: MutableMap<String, MutableList<List<PipelineExportV2YamlConflictMapItem>>>,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem,
        exportFile: Boolean
    ) {
        val distinctMap = HashMap<String?, MarketBuildAtomElementWithLocation>()
        existingOutputElements.forEach {
            distinctMap[it.jobLocation?.id] = it
        }
        val realExistingOutputElements =
            distinctMap.values.groupBy { it.stageLocation?.id }
        realExistingOutputElements.keys.reversed().forEach {
            if (it == pipelineExportV2YamlConflictMapItem.stage?.id ||
                realExistingOutputElements[it]?.size!! < 2
            ) return
            val names = realExistingOutputElements[it]?.map { _it -> _it.stepAtom?.name }
            val conflictElements = outputConflictMap[key]
            val itemElements = realExistingOutputElements[it]?.map { _it ->
                PipelineExportV2YamlConflictMapItem(
                    stage = _it.stageLocation?.copy(),
                    job = _it.jobLocation?.copy(),
                    step = PipelineExportV2YamlConflictMapBaseItem(
                        id = _it.stepAtom?.id,
                        name = _it.stepAtom?.name
                    )
                )
            } ?: return@forEach
            val item = mutableListOf(
                PipelineExportV2YamlConflictMapItem(
                    stage = pipelineExportV2YamlConflictMapItem.stage?.copy(),
                    job = pipelineExportV2YamlConflictMapItem.job?.copy(),
                    step = pipelineExportV2YamlConflictMapItem.step?.copy()
                )
            )
            item.addAll(itemElements)
            if (!conflictElements.isNullOrEmpty()) {
                conflictElements.add(item.toList())
            } else {
                outputConflictMap[key] = mutableListOf(item.toList())
            }
            if (exportFile) {
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = ProcessMessageCode.ERROR_EXPORT_OUTPUT_CONFLICT,
                    defaultMessage = "变量名[$key]来源不唯一，请修改变量名称或增加插件输出命名空间：$names",
                    params = arrayOf(key, names.toString())
                )
            }
            return
        }
    }

    private fun parseNameAndValueWithAnd(
        nameAndValueList: List<NameAndValue>? = emptyList(),
        variables: Map<String, String>?,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>
    ): String {

        var ifString = ""
        nameAndValueList?.forEachIndexed { index, nameAndValue ->
            val preStr = parseNameAndValueWithPreStr(
                output2Elements = output2Elements,
                nameAndValue = nameAndValue,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                variables = variables
            )
            ifString += if (index == nameAndValueList.size - 1) {
                "$preStr == ${nameAndValue.value}"
            } else {
                "$preStr == ${nameAndValue.value} && "
            }
        }
        return ifString
    }

    private fun parseNameAndValueWithOr(
        nameAndValueList: List<NameAndValue>? = emptyList(),
        variables: Map<String, String>?,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?,
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>
    ): String {
        var ifString = ""
        nameAndValueList?.forEachIndexed { index, nameAndValue ->
            val preStr = parseNameAndValueWithPreStr(
                output2Elements = output2Elements,
                nameAndValue = nameAndValue,
                pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem,
                variables = variables
            )
            ifString += if (index == nameAndValueList.size - 1) {
                "$preStr != ${nameAndValue.value}"
            } else {
                "$preStr != ${nameAndValue.value} || "
            }
        }
        return ifString
    }

    private fun parseNameAndValueWithPreStr(
        output2Elements: MutableMap<String, MutableList<MarketBuildAtomElementWithLocation>>,
        nameAndValue: NameAndValue,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem?,
        variables: Map<String, String>?
    ): String {
        val stepElement = output2Elements[nameAndValue.key]
        val ciName = PipelineVarUtil.fetchReverseVarName("${nameAndValue.key}")
        return if (stepElement != null) {
            var lastExistingOutputElements = MarketBuildAtomElementWithLocation()
            val keyStr = nameAndValue.key ?: ""
            run outside@{
                stepElement.reversed().forEach lit@{
                    if (it.jobLocation?.id == pipelineExportV2YamlConflictMapItem?.job?.id ||
                        it.stageLocation?.id != pipelineExportV2YamlConflictMapItem?.stage?.id
                    ) {
                        if (it.stepAtom?.id == pipelineExportV2YamlConflictMapItem?.step?.id)
                            return@lit
                        lastExistingOutputElements = it
                        return@outside
                    }
                }
            }
            val namespace = lastExistingOutputElements.stepAtom?.data?.get("namespace") as String?
            val originKeyWithNamespace = if (!namespace.isNullOrBlank()) {
                keyStr.replace("${namespace}_", "")
            } else keyStr

            when {
                lastExistingOutputElements.jobLocation?.jobId == null -> originKeyWithNamespace

                !namespace.isNullOrBlank() -> "jobs.${lastExistingOutputElements.jobLocation?.jobId}.steps." +
                    "$namespace.outputs.$originKeyWithNamespace"

                lastExistingOutputElements.stepAtom?.id.isNullOrBlank() -> originKeyWithNamespace

                else -> "jobs.${lastExistingOutputElements.jobLocation?.jobId}.steps." +
                    "${lastExistingOutputElements.stepAtom?.id}.outputs.$originKeyWithNamespace"
            }
        } else if (!ciName.isNullOrBlank()) {
            ciName
        } else if (!variables?.get(nameAndValue.key).isNullOrBlank()) {
            "variables.${nameAndValue.key}"
        } else "${nameAndValue.key}"
    }
}
