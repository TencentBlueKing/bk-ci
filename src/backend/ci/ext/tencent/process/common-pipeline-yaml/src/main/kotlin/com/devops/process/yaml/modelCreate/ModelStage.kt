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

package com.devops.process.yaml.modelCreate

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.Resources
import com.tencent.devops.common.ci.v2.stageCheck.ReviewVariable
import com.tencent.devops.common.ci.v2.stageCheck.StageCheck
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.StageReviewGroup
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamPair
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v3.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.pojo.enum.RuleOperation
import org.slf4j.LoggerFactory
import com.tencent.devops.common.ci.v2.Stage as GitCIV2Stage
import com.devops.process.yaml.modelCreate.inner.ModelCreateEvent
import com.devops.process.yaml.modelCreate.inner.ModelCreateInner
import com.devops.process.yaml.utils.PathMatchUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.notify.enums.NotifyType

class ModelStage constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val inner: ModelCreateInner
) {
    private val modelContainer = ModelContainer(client, objectMapper)
    private val modelElement = ModelElement(client, inner)

    companion object {
        private val logger = LoggerFactory.getLogger(ModelStage::class.java)
    }

    fun createStage(
        stage: GitCIV2Stage,
        event: ModelCreateEvent,
        stageIndex: Int,
        finalStage: Boolean = false,
        resources: Resources? = null,
        jobBuildTemplateAcrossInfos: Map<String, BuildTemplateAcrossInfo>?
    ): Stage {
        val containerList = mutableListOf<Container>()
        val stageEnable = PathMatchUtils.isIncludePathMatch(stage.ifModify, event.changeSet)

        stage.jobs.forEachIndexed { jobIndex, job ->
            val jobEnable = stageEnable && PathMatchUtils.isIncludePathMatch(job.ifModify, event.changeSet)
            val elementList = modelElement.makeElementList(
                job = job,
                changeSet = event.changeSet,
                jobEnable = jobEnable,
                event = event
            )

            if (job.runsOn.poolName == JobRunsOnType.AGENT_LESS.type) {
                modelContainer.addNormalContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    jobEnable = jobEnable,
                    finalStage = finalStage
                )
            } else {
                modelContainer.addVmBuildContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    projectCode = event.projectCode,
                    finalStage = finalStage,
                    jobEnable = jobEnable,
                    resources = resources,
                    buildTemplateAcrossInfo = jobBuildTemplateAcrossInfos?.get(job.id)
                )
            }
        }

        // 根据if设置stageController
        var stageControlOption = StageControlOption()
        if (!finalStage && !stage.ifField.isNullOrBlank()) {
            stageControlOption = StageControlOption(
                runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = stage.ifField.toString()
            )
        }
        stageControlOption = stageControlOption.copy(enable = stageEnable)

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
            checkIn = event.pipelineInfo?.let {
                createStagePauseCheck(
                    stageCheck = stage.checkIn,
                    position = ControlPointPosition.BEFORE_POSITION,
                    event = event,
                    stageId = stageId
                )
            },
            checkOut = event.pipelineInfo?.let {
                createStagePauseCheck(
                    stageCheck = stage.checkOut,
                    position = ControlPointPosition.AFTER_POSITION,
                    event = event,
                    stageId = stageId
                )
            }
        )
    }

    private fun createStagePauseCheck(
        stageCheck: StageCheck?,
        position: String,
        event: ModelCreateEvent,
        stageId: String
    ): StagePauseCheck? {
        if (stageCheck == null) return null
        val check = StagePauseCheck()
        check.timeout = stageCheck.timeoutHours
        if (stageCheck.reviews?.flows?.isNotEmpty() == true) {
            check.manualTrigger = true
            check.reviewDesc = stageCheck.reviews?.description
            check.reviewParams = createReviewParams(stageCheck.reviews?.variables)
            check.reviewGroups = stageCheck.reviews?.flows?.map { it ->
                StageReviewGroup(name = it.name, reviewers = it.reviewers)
            }?.toMutableList()
        }
        if (stageCheck.gates?.isNotEmpty() == true) {
            check.ruleIds = createRules(
                stageCheck = stageCheck,
                event = event,
                position = position,
                stageId = stageId
            )
        }
        return check
    }

    private fun createReviewParams(variables: Map<String, ReviewVariable>?): List<ManualReviewParam>? {
        if (variables.isNullOrEmpty()) return null
        val params = mutableListOf<ManualReviewParam>()
        variables.forEach { (key, variable) ->
            params.add(ManualReviewParam(
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
                options = variable.values?.map { ManualReviewParamPair(it, it) }
            ))
        }
        return params
    }

    /**
     * 根据规则创建红线
     * 规则实例： CodeccCheckAtomDebug.coverity_serious_defect <= 2
     */
    private fun createRules(
        stageCheck: StageCheck,
        event: ModelCreateEvent,
        position: String,
        stageId: String
    ): List<String>? {
        // 根据顺序，先匹配 <= 和 >= 在匹配 = > <因为 >= 包含 > 和 =
        val operations = mapOf(
            QualityOperation.convertToSymbol(QualityOperation.GE) to QualityOperation.GE,
            QualityOperation.convertToSymbol(QualityOperation.LE) to QualityOperation.LE,
            QualityOperation.convertToSymbol(QualityOperation.GT) to QualityOperation.GT,
            QualityOperation.convertToSymbol(QualityOperation.LT) to QualityOperation.LT,
            QualityOperation.convertToSymbol(QualityOperation.EQ) to QualityOperation.EQ
        )
        val ruleList: MutableList<RuleCreateRequestV3> = mutableListOf()
        stageCheck.gates?.forEach GateEach@{ gate ->
            val indicators = gate.rule.map { rule ->
                // threshold可能包含小数，所以把最后的一部分都取出来在分割
                val (atomCode, mid) = getAtomCodeAndOther(rule)
                var op = ""
                run breaking@{
                    operations.keys.forEach {
                        if (mid.contains(it)) {
                            op = it
                            return@breaking
                        }
                    }
                }
                if (op.isBlank()) {
                    logger.warn(
                        "GitProject: ${event.projectCode} event: ${event.streamData?.requestEventId} " +
                                "rule: $rule not find operations"
                    )
                    return@GateEach
                }
                val enNameAndthreshold = mid.split(op)
                RuleCreateRequestV3.CreateRequestIndicator(
                    atomCode = atomCode,
                    enName = enNameAndthreshold.first().trim(),
                    operation = operations[op]!!.name,
                    threshold = enNameAndthreshold.last().trim()
                )
            }
            val opList = mutableListOf<RuleCreateRequestV3.CreateRequestOp>()
            gate.notifyOnFail.forEach NotifyEach@{ notify ->
                val type = getNotifyByYaml(notify.type) ?: return@NotifyEach
                opList.add(
                    RuleCreateRequestV3.CreateRequestOp(
                        operation = RuleOperation.END,
                        notifyTypeList = listOf(type),
                        // 通知接受人未填缺省触发人
                        notifyUserList = if (notify.receivers.isNullOrEmpty()) {
                            listOf(event.userId)
                        } else {
                            notify.receivers?.toList()
                        },
                        notifyGroupList = null,
                        auditUserList = null,
                        auditTimeoutMinutes = null
                    )
                )
            }
            ruleList.add(
                RuleCreateRequestV3(
                    name = gate.name,
                    desc = "",
                    indicators = indicators,
                    position = position,
                    range = listOf(event.pipelineInfo!!.pipelineId),
                    templateRange = null,
                    gatewayId = null,
                    opList = opList,
                    stageId = stageId,
                    gateKeepers = gate.continueOnFail?.gatekeepers
                )
            )
        }
        logger.info(
            "GitProject: ${event.projectCode} event: ${event.streamData?.requestEventId}" +
                    " ruleList: $ruleList create gates"
        )
        try {
            val resultList = client.get(ServiceQualityRuleResource::class).create(
                userId = event.userId,
                projectId = event.projectCode,
                pipelineId = event.pipelineInfo!!.pipelineId,
                ruleList = ruleList
            ).data
            if (!resultList.isNullOrEmpty()) return resultList.map { it.ruleBuildId }
        } catch (ignore: Throwable) {
            logger.warn("Failed to save quality rules with error: ", ignore.message)
            if (ignore is RemoteServiceException) {
                throw QualityRulesException(ignore.errorMessage, ignore.errorCode.toString())
            } else {
                throw QualityRulesException(ignore.message ?: "")
            }
        }
        return null
    }

    private fun getAtomCodeAndOther(rule: String): Pair<String, String> {
        val index = rule.indexOfFirst { it == '.' }
        return Pair(
            rule.substring(0 until index),
            rule.substring((index + 1) until rule.length)
        )
    }

    // 获取蓝盾通知支持的类型
    private fun getNotifyByYaml(yamlText: String): NotifyType? {
        return when (yamlText) {
            "email" -> {
                NotifyType.EMAIL
            }
            "wework-message" -> {
                NotifyType.RTX
            }
            else -> {
                return null
            }
        }
    }
}
