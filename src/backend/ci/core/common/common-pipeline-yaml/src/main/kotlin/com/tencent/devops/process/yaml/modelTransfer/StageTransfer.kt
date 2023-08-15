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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.StageReviewGroup
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamPair
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.yaml.modelCreate.ModelCommon
import com.tencent.devops.process.yaml.modelCreate.ModelContainer
import com.tencent.devops.process.yaml.modelCreate.ModelCreateException
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v2.stageCheck.PreFlow
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageCheck
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageReviews
import com.tencent.devops.process.yaml.v2.stageCheck.ReviewVariable
import com.tencent.devops.process.yaml.v2.stageCheck.StageCheck
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.tencent.devops.process.yaml.v2.models.stage.Stage as StreamV2Stage

@Component
class StageTransfer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val modelContainer: ModelContainer,
    val modelElement: ElementTransfer
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StageTransfer::class.java)
        private const val VARIABLE_PREFIX = "variables."
    }

    fun yaml2TriggerStage(yamlInput: YamlTransferInput, stageIndex: Int): Stage {
        // 第一个stage，触发类
        val triggerElementList = mutableListOf<Element>()
        modelElement.yaml2Triggers(yamlInput.yaml.formatTriggerOn(yamlInput.defaultScmType), triggerElementList)

        val triggerContainer = TriggerContainer(
            id = "0",
            name = I18nUtil.getCodeLanMessage(CommonMessageCode.BK_BUILD_TRIGGER),
            elements = triggerElementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = getBuildFormPropertyFromYmlVariable(yamlInput.yaml.formatVariables())
        )

        val stageId = VMUtils.genStageId(stageIndex)
        return Stage(listOf(triggerContainer), id = stageId, name = stageId)
    }

    fun yaml2FinallyStage(
        stageIndex: Int,
        finallyJobs: List<Job>,
        yamlInput: YamlTransferInput
    ): Stage {
        return yaml2Stage(
            stage = StreamV2Stage(
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
        stage: StreamV2Stage,
        stageIndex: Int,
        yamlInput: YamlTransferInput,
        finalStage: Boolean = false
    ): Stage {
        val containerList = mutableListOf<Container>()

        stage.jobs.forEachIndexed { jobIndex, job ->
            val elementList = modelElement.yaml2Elements(
                job = job,
                yamlInput = yamlInput
            )

            if (job.runsOn.poolName == JobRunsOnType.AGENT_LESS.type) {
                modelContainer.addNormalContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    finalStage = finalStage
                )
            } else {
                modelContainer.addVmBuildContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    projectCode = yamlInput.projectCode,
                    finalStage = finalStage,
                    resources = yamlInput.yaml.formatResources(),
                    buildTemplateAcrossInfo = yamlInput.jobTemplateAcrossInfo?.get(job.id)
                )
            }
        }

        // 根据if设置stageController
        val stageControlOption = if (!finalStage && !stage.ifField.isNullOrBlank()) {
            StageControlOption(
                runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = ModelCreateUtil.removeIfBrackets(stage.ifField.toString())
            )
        } else StageControlOption()

        val stageId = VMUtils.genStageId(stageIndex)
        return Stage(
            id = stageId,
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
        stage: Stage
    ): PreStage {
        val jobs = stage.containers.associate { job ->

            val steps = modelElement.model2YamlSteps(job)

            (job.jobId ?: "job_${job.id}") to when (job.getClassType()) {
                NormalContainer.classType -> modelContainer.addYamlNormalContainer(job as NormalContainer, steps)
                VMBuildContainer.classType -> modelContainer.addYamlVMBuildContainer(job as VMBuildContainer, steps)
                else -> throw ModelCreateException("unknown classType:(${job.getClassType()})")
            }
        }
        return PreStage(
            name = stage.name,
            label = stage.tag?.ifEmpty { null },
            ifField = when (stage.stageControlOption?.runCondition) {
                StageRunCondition.CUSTOM_CONDITION_MATCH -> stage.stageControlOption?.customCondition
                StageRunCondition.CUSTOM_VARIABLE_MATCH -> ModelCommon.customVariableMatch(
                    stage.stageControlOption?.customVariables
                )
                StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> ModelCommon.customVariableMatchNotRun(
                    stage.stageControlOption?.customVariables
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

    private fun createStagePauseCheck(
        stageCheck: StageCheck?
    ): StagePauseCheck? {
        if (stageCheck == null) return null
        val check = StagePauseCheck()
        check.timeout = stageCheck.timeoutHours
        if (stageCheck.reviews?.flows?.isNotEmpty() == true) {
            check.manualTrigger = true
            check.reviewDesc = stageCheck.reviews.description
            check.reviewParams = createReviewParams(stageCheck.reviews.variables)
            check.reviewGroups = stageCheck.reviews.flows.map {
                StageReviewGroup(
                    name = it.name,
                    reviewers = ModelCommon.parseReceivers(it.reviewers).toList()
                )
            }.toMutableList()
        }
        return check
    }

    private fun createReviewParams(variables: Map<String, ReviewVariable>?): List<ManualReviewParam>? {
        if (variables.isNullOrEmpty()) return null
        val params = mutableListOf<ManualReviewParam>()
        variables.forEach { (key, variable) ->
            params.add(
                ManualReviewParam(
                    key = "variables.$key",
                    value = variable.default,
                    required = true,
                    valueType = when (variable.type) {
                        "TEXTAREA" -> ManualReviewParamType.TEXTAREA
                        "SELECTOR" -> ManualReviewParamType.ENUM
                        "SELECTOR-MULTIPLE" -> ManualReviewParamType.MULTIPLE
                        "BOOL" -> ManualReviewParamType.BOOLEAN
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

    private fun getBuildFormPropertyFromYmlVariable(
        variables: Map<String, Variable>?
    ): List<BuildFormProperty> {
        if (variables.isNullOrEmpty()) {
            return emptyList()
        }
        val buildFormProperties = mutableListOf<BuildFormProperty>()
        variables.forEach { (key, variable) ->
            buildFormProperties.add(
                BuildFormProperty(
                    id = key,
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = variable.value ?: "",
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null,
                    readOnly = variable.readonly
                )
            )
        }
        return buildFormProperties
    }
}
