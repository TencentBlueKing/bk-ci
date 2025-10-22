/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.StageReviewGroup
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamPair
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import com.tencent.devops.process.yaml.creator.ModelCommon
import com.tencent.devops.process.yaml.creator.ModelCreateException
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_CHECKIN_TIMEOUT_HOURS
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.v3.check.PreFlow
import com.tencent.devops.process.yaml.v3.check.PreStageCheck
import com.tencent.devops.process.yaml.v3.check.PreStageReviews
import com.tencent.devops.process.yaml.v3.check.ReviewVariable
import com.tencent.devops.process.yaml.v3.check.StageCheck
import com.tencent.devops.process.yaml.v3.enums.ContentFormat
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.IfField
import com.tencent.devops.process.yaml.v3.models.IfField.Mode
import com.tencent.devops.process.yaml.v3.models.RecommendedVersion
import com.tencent.devops.process.yaml.v3.models.Variable
import com.tencent.devops.process.yaml.v3.models.VariablePropType
import com.tencent.devops.process.yaml.v3.models.VariableProps
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.models.stage.StageLabel
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.tencent.devops.process.yaml.v3.models.stage.Stage as StreamV3Stage

@Component
@Suppress("ComplexMethod")
class StageTransfer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val containerTransfer: ContainerTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer,
    val transferCacheService: TransferCacheService,
    val transferCreator: TransferCreator
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StageTransfer::class.java)
    }

    fun yaml2TriggerStage(yamlInput: YamlTransferInput, stageIndex: Int): Stage {
        // 第一个stage，触发类
        val triggerElementList = mutableListOf<Element>()
        elementTransfer.yaml2Triggers(yamlInput, triggerElementList)

        val triggerContainer = TriggerContainer(
            id = "0",
            name = I18nUtil.getCodeLanMessage(CommonMessageCode.BK_BUILD_TRIGGER),
            elements = triggerElementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = variableTransfer.makeVariableFromYaml(makeVariables(yamlInput.yaml))
        )
        with(yamlInput.yaml.recommendedVersion) {
            if (this != null && this.enabled) {
                triggerContainer.buildNo = BuildNo(
                    this.buildNo.initialValue,
                    RecommendedVersion.Strategy.parse(this.buildNo.strategy).toBuildNoType(),
                    this.allowModifyAtStartup
                )
            }
        }
        yamlInput.aspectWrapper.setModelJob4Model(triggerContainer, PipelineTransferAspectWrapper.AspectType.AFTER)

        val stageId = VMUtils.genStageId(stageIndex)
        return Stage(listOf(triggerContainer), id = stageId, name = stageId)
    }

    private fun makeVariables(yaml: IPreTemplateScriptBuildYamlParser): Map<String, Variable> {
        val variable = yaml.formatVariables()
        if (yaml.recommendedVersion == null || yaml.recommendedVersion?.enabled == false) return variable
        return with(yaml.recommendedVersion) {
            variable.plus(
                mapOf(
                    MAJORVERSION to Variable(
                        value = this!!.major.toString(),
                        allowModifyAtStartup = allowModifyAtStartup,
                        props = VariableProps(
                            type = VariablePropType.VUEX_INPUT.name,
                            description = I18nUtil.getCodeLanMessage(MAJORVERSION)
                        )
                    ),
                    MINORVERSION to Variable(
                        value = this!!.minor.toString(),
                        allowModifyAtStartup = allowModifyAtStartup,
                        props = VariableProps(
                            type = VariablePropType.VUEX_INPUT.name,
                            description = I18nUtil.getCodeLanMessage(MINORVERSION)
                        )
                    ),
                    FIXVERSION to Variable(
                        value = this!!.fix.toString(),
                        allowModifyAtStartup = allowModifyAtStartup,
                        props = VariableProps(
                            type = VariablePropType.VUEX_INPUT.name,
                            description = I18nUtil.getCodeLanMessage(FIXVERSION)
                        )
                    )
                )
            )
        }
    }

    fun yaml2FinallyStage(
        stageIndex: Int,
        finallyJobs: List<Job>,
        yamlInput: YamlTransferInput
    ): Stage {
        return yaml2Stage(
            stage = StreamV3Stage(
                name = "Finally",
                label = emptyList(),
                ifField = null,
                fastKill = false,
                jobs = finallyJobs,
                checkIn = null,
                checkOut = null
            ),
            stageIndex = stageIndex,
            yamlInput = yamlInput,
            finalStage = true
        )
    }

    fun yaml2Stage(
        stage: StreamV3Stage,
        stageIndex: Int,
        yamlInput: YamlTransferInput,
        finalStage: Boolean = false
    ): Stage {
        val containerList = mutableListOf<Container>()

        stage.jobs.forEachIndexed { jobIndex, job ->
            yamlInput.aspectWrapper.setYamlJob4Yaml(
                yamlJob = job,
                aspectType = PipelineTransferAspectWrapper.AspectType.BEFORE
            )
            preCheckJob(job, yamlInput)
            val elementList = elementTransfer.yaml2Elements(
                job = job,
                yamlInput = yamlInput
            )

            val jobEnable = if (job.enable != null) job.enable!! else true
            if (job.runsOn.poolName == JobRunsOnType.AGENT_LESS.type) {
                containerTransfer.addNormalContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    jobEnable = jobEnable,
                    finalStage = finalStage
                )
            } else {
                containerTransfer.addVmBuildContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    projectCode = yamlInput.projectCode,
                    userId = yamlInput.userId,
                    finalStage = finalStage,
                    jobEnable = jobEnable,
                    resources = yamlInput.yaml.formatResources(),
                    buildTemplateAcrossInfo = yamlInput.jobTemplateAcrossInfo?.get(job.id)
                )
            }
            yamlInput.aspectWrapper.setModelJob4Model(
                containerList.last(),
                PipelineTransferAspectWrapper.AspectType.AFTER
            )
        }

        val stageEnable = if (stage.enable != null) stage.enable!! else true

        // 根据if设置stageController
        val stageControlOption = if (!finalStage) {
            val runCondition = when {
                stage.ifField == null -> StageRunCondition.AFTER_LAST_FINISHED
                !stage.ifField.expression.isNullOrBlank() -> StageRunCondition.CUSTOM_CONDITION_MATCH
                stage.ifField.mode == Mode.RUN_WHEN_ALL_PARAMS_MATCH -> StageRunCondition.CUSTOM_VARIABLE_MATCH
                stage.ifField.mode == Mode.NOT_RUN_WHEN_ALL_PARAMS_MATCH ->
                    StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN

                else -> StageRunCondition.AFTER_LAST_FINISHED
            }
            StageControlOption(
                enable = stageEnable,
                runCondition = runCondition,
                customCondition = if (runCondition == StageRunCondition.CUSTOM_CONDITION_MATCH) {
                    stage.ifField?.expression
                } else {
                    null
                },
                customVariables = if (runCondition == StageRunCondition.CUSTOM_VARIABLE_MATCH ||
                    runCondition == StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
                ) {
                    stage.ifField?.params?.map { NameAndValue(it.key, it.value) }
                } else {
                    null
                }
            )
        } else StageControlOption(
            enable = stageEnable
        )

        val stageId = VMUtils.genStageId(stageIndex)
        return Stage(
            id = stageId,
            stageIdForUser = stage.id,
            name = stage.name ?: if (finalStage) {
                "Final"
            } else {
                VMUtils.genStageId(stageIndex - 1)
            },
            tag = stage.label,
            fastKill = stage.fastKill,
            stageControlOption = stageControlOption,
            containers = containerList,
            finally = finalStage,
            checkIn = createStagePauseCheck(
                stageCheck = stage.checkIn
            ),
            checkOut = createStagePauseCheck(
                stageCheck = stage.checkOut
            )
        )
    }

    fun model2YamlStage(
        stage: Stage,
        userId: String,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper
    ): PreStage {
        val jobs = stage.containers.associateTo(LinkedHashMap()) { job ->
            aspectWrapper.setModelJob4Model(job, PipelineTransferAspectWrapper.AspectType.BEFORE)
            val steps = elementTransfer.model2YamlSteps(job, projectId, aspectWrapper)

            (job.jobId?.ifBlank { null } ?: ScriptYmlUtils.randomString("job_")) to when (job.getClassType()) {
                NormalContainer.classType -> containerTransfer.addYamlNormalContainer(job as NormalContainer, steps)
                VMBuildContainer.classType -> containerTransfer.addYamlVMBuildContainer(
                    userId = userId,
                    projectId = projectId,
                    job = job as VMBuildContainer,
                    steps = steps
                )

                else -> throw ModelCreateException("unknown classType:(${job.getClassType()})")
            }.also { preJob ->
                aspectWrapper.setYamlJob4Yaml(
                    yamlPreJob = preJob,
                    aspectType = PipelineTransferAspectWrapper.AspectType.AFTER
                )
            }
        }
        return PreStage(
            id = stage.stageIdForUser,
            enable = stage.stageEnabled().nullIfDefault(true),
            name = stage.name,
            label = maskYamlStageLabel(stage.tag).ifEmpty { null },
            ifField = when (stage.stageControlOption?.runCondition) {
                StageRunCondition.CUSTOM_CONDITION_MATCH -> stage.stageControlOption?.customCondition
                StageRunCondition.CUSTOM_VARIABLE_MATCH -> IfField(
                    mode = IfField.Mode.RUN_WHEN_ALL_PARAMS_MATCH,
                    params = stage.stageControlOption?.customVariables?.associateBy(
                        { it.key ?: "" },
                        { it.value ?: "" })
                )

                StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> IfField(
                    mode = IfField.Mode.NOT_RUN_WHEN_ALL_PARAMS_MATCH,
                    params = stage.stageControlOption?.customVariables?.associateBy(
                        { it.key ?: "" },
                        { it.value ?: "" })
                )

                else -> null
            },
            fastKill = if (stage.fastKill == true) true else null,
            jobs = jobs,
            checkIn = getCheckInForStage(stage),
            // TODO 暂时不支持准出和gates的导出
            checkOut = null
        )
    }

    private fun maskYamlStageLabel(tags: List<String?>?): List<String> {
        if (tags.isNullOrEmpty()) return emptyList()
        return tags.filterNotNull().map { StageLabel.parseById(it).value }
    }

    private fun getCheckInForStage(stage: Stage): PreStageCheck? {
        val reviews = PreStageReviews(
            flows = stage.checkIn?.reviewGroups?.map {
                PreFlow(
                    it.name,
                    it.reviewers.ifEmpty { null },
                    it.groups.ifEmpty { null })
            },
            variables = stage.checkIn?.reviewParams?.associate {
                it.key to ReviewVariable(
                    label = it.chineseName ?: it.key,
                    type = when (it.valueType) {
                        ManualReviewParamType.TEXTAREA -> "TEXTAREA"
                        ManualReviewParamType.ENUM -> "SELECTOR"
                        ManualReviewParamType.MULTIPLE -> "SELECTOR-MULTIPLE"
                        ManualReviewParamType.BOOLEAN -> "BOOL"
                        ManualReviewParamType.CHECKBOX -> "CHECKBOX"
                        else -> "INPUT"
                    },
                    default = it.value,
                    values = it.options?.map { mit -> mit.key },
                    description = it.desc,
                    required = it.required
                )
            },
            description = stage.checkIn?.reviewDesc,
            contentFormat = ContentFormat.parse(stage.checkIn?.markdownContent).nullIfDefault(ContentFormat.TEXT)?.text,
            notifyType = stage.checkIn?.notifyType?.ifEmpty { null },
            notifyGroups = stage.checkIn?.notifyGroup?.ifEmpty { null }
        )
        if (reviews.flows.isNullOrEmpty()) {
            return null
        }
        return PreStageCheck(
            reviews = reviews,
            gates = null,
            timeoutHours = stage.checkIn?.timeout?.nullIfDefault(DEFAULT_CHECKIN_TIMEOUT_HOURS)
        )
    }

    private fun createStagePauseCheck(
        stageCheck: StageCheck?
    ): StagePauseCheck? {
        if (stageCheck == null) return null
        val check = StagePauseCheck()
        check.timeout = stageCheck.timeoutHours ?: DEFAULT_CHECKIN_TIMEOUT_HOURS
        if (stageCheck.reviews?.flows?.isNotEmpty() == true) {
            check.manualTrigger = true
            check.reviewDesc = stageCheck.reviews.description
            check.reviewParams = createReviewParams(stageCheck.reviews.variables)
            check.reviewGroups = stageCheck.reviews.flows.map {
                StageReviewGroup(
                    name = it.name,
                    reviewers = ModelCommon.parseReceivers(it.reviewers).toList(),
                    groups = ModelCommon.parseReceivers(it.groups).toList()
                )
            }.toMutableList()
            check.markdownContent = stageCheck.reviews.contentFormat == ContentFormat.MARKDOWN
            check.notifyType = stageCheck.reviews.notifyType?.toMutableList() ?: mutableListOf()
            check.notifyGroup = stageCheck.reviews.notifyGroups?.toMutableList() ?: mutableListOf()
        }
        return check
    }

    private fun createReviewParams(variables: Map<String, ReviewVariable>?): List<ManualReviewParam>? {
        if (variables.isNullOrEmpty()) return null
        val params = mutableListOf<ManualReviewParam>()
        variables.forEach { (key, variable) ->
            params.add(
                ManualReviewParam(
                    key = key,
                    value = when (variable.type) {
                        /* CHECKBOX 只能false，不允许修改 */
                        "CHECKBOX" -> false
                        else -> variable.default
                    },
                    required = variable.required ?: false,
                    valueType = when (variable.type) {
                        "TEXTAREA" -> ManualReviewParamType.TEXTAREA
                        "SELECTOR" -> ManualReviewParamType.ENUM
                        "SELECTOR-MULTIPLE" -> ManualReviewParamType.MULTIPLE
                        "BOOL" -> ManualReviewParamType.BOOLEAN
                        "CHECKBOX" -> ManualReviewParamType.CHECKBOX
                        else -> ManualReviewParamType.STRING
                    },
                    chineseName = variable.label,
                    desc = variable.description,
                    options = (variable.values.takeIf { it is List<*>? } as List<*>?)?.map {
                        ManualReviewParamPair(
                            it.toString(),
                            it.toString()
                        )
                    },
                    variableOption = variable.values.takeIf { it is String? } as String?
                )
            )
        }
        return params
    }

    private fun preCheckJob(
        job: Job,
        yamlInput: YamlTransferInput
    ) {
        if (job.runsOn.hwSpec != null) {
            val hw = transferCacheService.getDockerResource(
                userId = yamlInput.userId,
                projectId = yamlInput.projectCode,
                buildType = transferCreator.defaultLinuxDispatchType()
            )?.dockerResourceOptionsMaps?.find { it.dockerResourceOptionsShow.description == job.runsOn.hwSpec }
            job.runsOn.hwSpec = hw?.id
        }
    }
}
