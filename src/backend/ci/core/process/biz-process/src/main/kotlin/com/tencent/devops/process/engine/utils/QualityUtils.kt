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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.pojo.RuleCheckResult
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object QualityUtils {

    private val logger = LoggerFactory.getLogger(QualityUtils::class.java)

    private const val QUALITY_RESULT = "bsQualityResult"

    val QUALITY_CODECC_LAZY_ATOM = setOf("CodeccCheckAtom", "linuxCodeCCScript", "linuxPaasCodeCCScript")

    private val QUALITY_LAZY_TIME_GAP = listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20)

    /**
     * 动态插入原子
     */
    fun fillInOutElement(
        model: Model,
        startParams: Map<String, Any>,
        ruleMatchTaskList: List<Map<String, Any>>
    ): Model {
        val beforeElementSet =
            ruleMatchTaskList.filter { it["position"] as String == "BEFORE" }.map { it["taskId"] as String }
        val afterElementSet =
            ruleMatchTaskList.filter { it["position"] as String == "AFTER" }.map { it["taskId"] as String }
        val elementRuleMap = ruleMatchTaskList.groupBy { it["taskId"] as String }.toMap()

        val stageList = mutableListOf<Stage>()
        // 1、如果只有一个/零个控制点插件，按照目前的逻辑不变
        // 2、如果有超过一个控制点插件，先检查这些控制点插件的别名是否开头包含质量红线ID+“_”，若包含，则对该控制点设置红线。若没有控制点插件包含，则对所有控制点生效。
        with(model) {
            stages.forEach { stage ->

                val containerList = mutableListOf<Container>()

                stage.containers.forEach { container ->
                    val elementList = mutableListOf<Element>()
                    container.elements.forEach { element ->
                        val key = SkipElementUtils.getSkipElementVariableName(element.id)
                        val skip = if (startParams.containsKey(key)) {
                            val skipValue = startParams[key] as String
                            skipValue == "true"
                        } else {
                            false
                        }

                        if (!skip && beforeElementSet.contains(element.getAtomCode())) {
                            val insertElement = getInsertElement(element, elementRuleMap, true)
                            if (insertElement != null) elementList.add(insertElement)
                        }

                        elementList.add(element)

                        if (!skip && afterElementSet.contains(element.getAtomCode())) {
                            val insertElement = getInsertElement(element, elementRuleMap, false)
                            if (insertElement != null) elementList.add(insertElement)
                        }
                    }

                    val finalContainer = when (container) {
                        is VMBuildContainer -> {
                            VMBuildContainer(
                                containerId = container.containerId,
                                id = container.id,
                                name = container.name,
                                elements = elementList,
                                status = container.status,
                                startEpoch = container.startEpoch,
                                systemElapsed = container.systemElapsed,
                                elementElapsed = container.elementElapsed,
                                baseOS = container.baseOS,
                                vmNames = container.vmNames,
                                maxQueueMinutes = container.maxQueueMinutes,
                                maxRunningMinutes = container.maxRunningMinutes,
                                buildEnv = container.buildEnv,
                                customBuildEnv = container.customBuildEnv,
                                thirdPartyAgentId = container.thirdPartyAgentId,
                                thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                                thirdPartyWorkspace = container.thirdPartyWorkspace,
                                dockerBuildVersion = container.dockerBuildVersion,
                                tstackAgentId = container.tstackAgentId,
                                canRetry = container.canRetry,
                                enableExternal = container.enableExternal,
                                jobControlOption = container.jobControlOption,
                                mutexGroup = container.mutexGroup,
                                dispatchType = container.dispatchType,
                                showBuildResource = container.showBuildResource
                            )
                        }
                        is NormalContainer -> {
                            NormalContainer(
                                containerId = container.containerId,
                                id = container.id,
                                name = container.name,
                                elements = elementList,
                                status = container.status,
                                startEpoch = container.startEpoch,
                                systemElapsed = container.systemElapsed,
                                elementElapsed = container.elementElapsed,
                                enableSkip = container.enableSkip,
                                conditions = container.conditions,
                                canRetry = container.canRetry,
                                jobControlOption = container.jobControlOption,
                                mutexGroup = container.mutexGroup
                            )
                        }
                        else -> {
                            container
                        }
                    }
                    containerList.add(finalContainer)
                }
                stageList.add(
                    Stage(
                        containers = containerList,
                        id = stage.id,
                        name = stage.name,
                        tag = stage.tag,
                        status = stage.status,
                        startEpoch = stage.startEpoch,
                        elapsed = stage.elapsed,
                        customBuildEnv = stage.customBuildEnv,
                        fastKill = stage.fastKill,
                        stageControlOption = stage.stageControlOption
                    )
                )
            }

            return Model(
                name = name,
                desc = desc,
                stages = stageList,
                labels = labels,
                instanceFromTemplate = instanceFromTemplate,
                pipelineCreator = pipelineCreator,
                srcTemplateId = null,
                templateId = templateId
            )
        }
    }

    private fun getInsertElement(
        element: Element,
        elementRuleMap: Map<String, List<Map<String, Any>>>,
        isBefore: Boolean
    ): Element? {
        val position = if (isBefore) "BEFORE" else "AFTER"

        // 取出所有规则的gatewayIds
        val gatewayIds = mutableSetOf<String>()
        val elementList = elementRuleMap[element.getAtomCode()]?.filter { it["position"] as String == position }
        elementList?.forEach {
            // 处理包含某些rule没填gateway id的情况
            val itemGatewayIds = it.getValue("gatewayIds") as List<String>
            if (itemGatewayIds.isEmpty()) gatewayIds.add(("")) else gatewayIds.addAll(itemGatewayIds)
        }
        logger.info("elementName: ${element.name}, gatewayIds: $gatewayIds")
        return if (gatewayIds.isEmpty() || gatewayIds.any { element.name.toLowerCase().contains(it.toLowerCase()) }) {
            val id = "T-${UUIDUtil.generate()}"
            if (isBefore) {
                QualityGateInElement("质量红线(准入)", id, null, element.getAtomCode(), element.name)
            } else {
                QualityGateOutElement("质量红线(准出)", id, null, element.getAtomCode(), element.name)
            }
        } else {
            null
        }
    }

    fun getCheckResult(
        task: PipelineBuildTask,
        interceptTaskName: String?,
        interceptTask: String?,
        runVariables: Map<String, String>,
        buildLogPrinter: BuildLogPrinter,
        position: String,
        templateService: TemplateService,
        pipelineBuildQualityService: PipelineBuildQualityService
    ): RuleCheckResult {
        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val buildId = task.buildId
        val templateId = templateService.getTemplate(pipelineId)?.templateId
        val buildNo = runVariables[PIPELINE_BUILD_NUM].toString()
        val elementId = task.taskId

        if (interceptTask == null) {
            logger.warn("Fail to find quality gate intercept element")
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorType = ErrorType.USER,
                errorMsg = "Fail to find quality gate intercept element"
            )
        }

        val buildCheckParams = BuildCheckParams(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildNo = buildNo,
            interceptTaskName = interceptTaskName ?: "",
            startTime = LocalDateTime.now().timestamp(),
            taskId = interceptTask,
            position = position,
            templateId = templateId,
            runtimeVariable = runVariables
        )
        val result = if (position == ControlPointPosition.AFTER_POSITION && QUALITY_CODECC_LAZY_ATOM.contains(interceptTask)) {
            run loop@{
                QUALITY_LAZY_TIME_GAP.forEachIndexed { index, gap ->
                    val hasMetadata = pipelineBuildQualityService.hasCodeccHisMetadata(buildId)
                    if (hasMetadata) return@loop
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "第 $index 次轮询等待红线结果",
                        tag = elementId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    Thread.sleep(gap * 1000L)
                }
            }
            pipelineBuildQualityService.check(buildCheckParams, position)
        } else {
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "检测红线结果",
                tag = elementId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            pipelineBuildQualityService.check(buildCheckParams, position)
        }
        logger.info("quality gateway $position check result for ${task.buildId}: $result")
        return result
    }

    fun handleResult(
        position: String,
        task: PipelineBuildTask,
        interceptTask: String,
        checkResult: RuleCheckResult,
        buildLogPrinter: BuildLogPrinter,
        pipelineBuildDetailService: PipelineBuildDetailService,
        pipelineBuildQualityService: PipelineBuildQualityService
    ): AtomResponse {
        with(task) {
            val atomDesc = if (position == ControlPointPosition.BEFORE_POSITION) "准入" else "准出"
            val elementId = task.taskId

            if (checkResult.success) {
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "质量红线($atomDesc)检测已通过",
                    tag = elementId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )

                checkResult.resultList.forEach {
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "规则：${it.ruleName}",
                        tag = elementId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    it.messagePairs.forEach { message ->
                        buildLogPrinter.addLine(
                            buildId = buildId,
                            message = message.first + " " + message.second,
                            tag = elementId,
                            jobId = task.containerHashId,
                            executeCount = task.executeCount ?: 1
                        )
                    }
                }

                // 产生MQ消息，等待5秒时间
                logger.info("[$buildId]|QUALITY_$position|taskId=$elementId|quality check success wait end")
                task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = 5000
                task.taskParams[QUALITY_RESULT] = checkResult.success
            } else {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "质量红线($atomDesc)检测被拦截",
                    tag = elementId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )

                checkResult.resultList.forEach {
                    buildLogPrinter.addRedLine(
                        buildId = buildId,
                        message = "规则：${it.ruleName}",
                        tag = elementId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    it.messagePairs.forEach { message ->
                        buildLogPrinter.addRedLine(
                            buildId = buildId,
                            message = message.first + " " + message.second,
                            tag = elementId,
                            jobId = task.containerHashId,
                            executeCount = task.executeCount ?: 1
                        )
                    }
                }

                // 直接结束流水线的
                if (checkResult.failEnd) {
                    logger.info("[$buildId]|QUALITY_$position|taskId=$elementId|quality check fail stop directly")
                    // LogUtils.addFoldEndLine(rabbitTemplate, buildId, elementName, elementId, task.containerHashId,task.executeCount ?: 1)
                    return AtomResponse(
                        buildStatus = BuildStatus.QUALITY_CHECK_FAIL,
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_QUALITY_CHECK_FAIL,
                        errorMsg = "quality check fail"
                    ) // 拦截到直接失败
                }

                // 产生MQ消息，等待5分钟审核时间
                logger.info("quality check fail wait reviewing")
                val auditUsers = pipelineBuildQualityService.getAuditUserList(projectId, pipelineId, buildId, interceptTask)
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "质量红线($atomDesc)待审核!审核人：$auditUsers",
                    tag = elementId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = checkResult.auditTimeoutSeconds * 1000 // 15 min
                task.taskParams[QUALITY_RESULT] = checkResult.success
            }
            pipelineBuildDetailService.pipelineDetailChangeEvent(buildId)
        }
        return AtomResponse(BuildStatus.RUNNING)
    }

    fun tryFinish(
        task: PipelineBuildTask,
        buildLogPrinter: BuildLogPrinter
    ): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        val taskName = task.taskName
        val success = task.getTaskParam(QUALITY_RESULT)
        val actionUser = task.getTaskParam(BS_MANUAL_ACTION_USERID)

        return if (success.isNotEmpty()) {
            logger.info("[$buildId]|QUALITY_FINISH|taskName=$taskName|taskId=$taskId|success=$success")
            if (success.toBoolean()) {
                AtomResponse(BuildStatus.REVIEW_PROCESSED)
            } else {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "${taskName}审核超时",
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(BuildStatus.QUALITY_CHECK_FAIL)
            }
        } else {
            val manualAction = task.getTaskParam(BS_MANUAL_ACTION)
            logger.info("[$buildId]|QUALITY_FINISH|taskName=$taskName|taskId=${task.taskId}|action=$manualAction")
            if (manualAction.isNotEmpty()) {
                when (ManualReviewAction.valueOf(manualAction)) {
                    ManualReviewAction.PROCESS -> {
                        buildLogPrinter.addYellowLine(
                            buildId = buildId,
                            message = "步骤审核结束，审核结果：[继续]，审核人：$actionUser",
                            tag = taskId,
                            jobId = task.containerHashId,
                            executeCount = task.executeCount ?: 1
                        )
                        AtomResponse(BuildStatus.SUCCEED)
                    }
                    ManualReviewAction.ABORT -> {
                        buildLogPrinter.addYellowLine(
                            buildId = buildId,
                            message = "步骤审核结束，审核结果：[驳回]，审核人：$actionUser",
                            tag = taskId,
                            jobId = task.containerHashId,
                            executeCount = task.executeCount ?: 1
                        )
                        AtomResponse(BuildStatus.REVIEW_ABORT)
                    }
                }
            } else {
                AtomResponse(BuildStatus.REVIEWING)
            }
        }
    }
}