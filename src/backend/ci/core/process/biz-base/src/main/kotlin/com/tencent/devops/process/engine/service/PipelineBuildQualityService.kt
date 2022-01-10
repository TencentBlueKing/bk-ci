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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.common.quality.pojo.RuleCheckResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.core.Response

@Suppress(
    "LongParameterList",
    "NestedBlockDepth",
    "LongMethod",
    "ComplexMethod",
    "ThrowsCount",
    "MagicNumber",
    "ReturnCount"
)
@Service
class PipelineBuildQualityService(
    private val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService
) {
    companion object {

        private val logger = LoggerFactory.getLogger(PipelineBuildQualityService::class.java)

        private const val QUALITY_RESULT = "bsQualityResult"

        private val QUALITY_CODECC_LAZY_ATOM = setOf("CodeccCheckAtom", "linuxCodeCCScript", "linuxPaasCodeCCScript")

        private val QUALITY_LAZY_TIME_GAP =
            listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
    }

    fun buildManualQualityGateReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        action: ManualReviewAction,
        channelCode: ChannelCode
    ) {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )

        if (pipelineInfo.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )
        }

        val model = buildDetailService.getBuildModel(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                defaultMessage = "流水线构建不存在",
                params = arrayOf(buildId)
            )

        var find = false
        var taskType = ""
        model.stages.forEachIndexed nextStage@{ index, s ->
            if (index == 0) {
                return@nextStage
            }
            s.containers.forEach nextContainer@{ c ->
                c.elements.forEach nextElement@{ element ->
                    if (element.id != elementId) return@nextElement
                    logger.info("${element.id}, ${element.name}")
                    when (element) {
                        is QualityGateInElement -> {
                            find = true
                            taskType = element.interceptTask!!
                        }
                        is QualityGateOutElement -> {
                            find = true
                            taskType = element.interceptTask!!
                        }
                    }
                    return@nextStage
                }
                c.fetchGroupContainers()?.forEach {
                    it.elements.forEach nextElement@{ element ->
                        if (element.id != elementId || element !is MatrixStatusElement) return@nextElement
                        logger.info("${element.id}, ${element.name}")
                        if (element.originClassType == QualityGateInElement.classType ||
                            element.originClassType == QualityGateOutElement.classType
                        ) {
                            find = true
                            taskType = element.interceptTask!!
                        }
                        return@nextStage
                    }
                }
            }
        }

        if (!find) {
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.ERROR_QUALITY_TASK_NOT_FOUND,
                defaultMessage = "质量红线拦截的任务[$elementId]不存在",
                params = arrayOf(elementId)
            )
        }

        // 校验审核权限
        val auditUserSet = getAuditUserList(projectId, pipelineId, buildId, taskType)
        if (!auditUserSet.contains(userId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_QUALITY_REVIEWER_NOT_MATCH,
                defaultMessage = "用户($userId)不在审核人员名单中",
                params = arrayOf(userId)
            )
        }

        logger.info("[$buildId]|buildManualReview|taskId=$elementId|userId=$userId|action=$action")
        pipelineRuntimeService.manualDealBuildTask(
            projectId = projectId,
            buildId = buildId,
            taskId = elementId,
            userId = userId,
            manualAction = action
        )
    }

    fun addQualityGateReviewUsers(projectId: String, pipelineId: String, buildId: String, model: Model) {
        // TODO 逻辑过于复杂，下一版优化
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach nextElement@{ element ->
                    if (element.status != BuildStatus.REVIEWING.name) return@nextElement
                    if (element is QualityGateInElement) {
                        element.reviewUsers = getAuditUserList(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            taskId = element.interceptTask ?: ""
                        )
                    }
                    if (element is QualityGateOutElement) {
                        element.reviewUsers = getAuditUserList(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            taskId = element.interceptTask ?: ""
                        )
                    }
                }
                if (container.matrixGroupFlag == true) {
                    container.fetchGroupContainers()?.forEach {
                        it.elements.forEach nextElement@{ element ->
                            if (element.status != BuildStatus.REVIEWING.name) return@nextElement
                            if (element is MatrixStatusElement && element.originClassType ==
                                QualityGateInElement.classType) {
                                element.reviewUsers = getAuditUserList(
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    taskId = element.interceptTask ?: ""
                                ).toMutableList()
                            }
                            if (element is MatrixStatusElement && element.originClassType ==
                                QualityGateOutElement.classType) {
                                element.reviewUsers = getAuditUserList(
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    taskId = element.interceptTask ?: ""
                                ).toMutableList()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getMatchRuleList(projectId: String, pipelineId: String, templateId: String?): List<QualityRuleMatchTask> {
        val startTime = System.currentTimeMillis()
        return try {
            client.get(ServiceQualityRuleResource::class).matchRuleList(
                projectId = projectId,
                pipelineId = pipelineId,
                templateId = templateId,
                startTime = LocalDateTime.now().timestamp()
            ).data ?: listOf()
        } catch (ignore: Exception) {
            logger.error("quality get match rule list fail: ${ignore.message}", ignore)
            return listOf()
        } finally {
            LogUtils.costTime("call rule", startTime)
        }
    }

    fun getAuditUserList(projectId: String, pipelineId: String, buildId: String, taskId: String): Set<String> {
        return try {
            val auditUserSet = client.get(ServiceQualityRuleResource::class).getAuditUserList(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            ).data ?: setOf()

            auditUserSet.map { buildVariableService.replaceTemplate(projectId, buildId, it) }.toSet()
        } catch (ignore: Exception) {
            logger.error("quality get audit user list fail: ${ignore.message}", ignore)
            setOf()
        }
    }

    fun check(buildCheckParams: BuildCheckParams, position: String): RuleCheckResult {
        return try {
            client.get(ServiceQualityRuleResource::class).check(buildCheckParams).data!!
        } catch (ignore: Exception) {
            logger.error("Quality Gate check in fail", ignore)
            val atomDesc = if (position == ControlPointPosition.BEFORE_POSITION) "准入" else "准出"
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "质量红线($atomDesc)检测失败"
            )
        }
    }

    fun hasCodeccHisMetadata(buildId: String): Boolean {
        val hisMetadata = client.get(ServiceQualityRuleResource::class).getHisMetadata(buildId).data ?: listOf()
        return hisMetadata.any { it.elementType in QUALITY_CODECC_LAZY_ATOM }
    }

    fun generateQualityRuleElement(
        ruleMatchList: List<QualityRuleMatchTask>
    ): Triple<List<String>?, List<String>?, Map<String, List<Map<String, Any>>>?> {
        val ruleMatchTaskList = ruleMatchList.map { ruleMatch ->
            val gatewayIds =
                ruleMatch.ruleList.filter { rule -> !rule.gatewayId.isNullOrBlank() }.map { it.gatewayId!! }
            mapOf(
                "position" to ruleMatch.controlStage.name,
                "taskId" to ruleMatch.taskId,
                "gatewayIds" to gatewayIds
            )
        }
        val beforeElementSet =
            ruleMatchTaskList.filter { it["position"] as String == "BEFORE" }.map { it["taskId"] as String }
        val afterElementSet =
            ruleMatchTaskList.filter { it["position"] as String == "AFTER" }.map { it["taskId"] as String }
        val elementRuleMap = ruleMatchTaskList.groupBy { it["taskId"] as String }.toMap()
        return Triple(beforeElementSet, afterElementSet, elementRuleMap)
    }

    fun handleResult(
        position: String,
        task: PipelineBuildTask,
        interceptTask: String,
        checkResult: RuleCheckResult,
        buildLogPrinter: BuildLogPrinter
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
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "质量红线($atomDesc)检测被拦截",
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
                        if (message.third) {
                            buildLogPrinter.addLine(
                                buildId = buildId,
                                message = message.first + " " + message.second,
                                tag = elementId,
                                jobId = task.containerHashId,
                                executeCount = task.executeCount ?: 1
                            )
                        } else {
                            buildLogPrinter.addRedLine(
                                buildId = buildId,
                                message = message.first + " " + message.second,
                                tag = elementId,
                                jobId = task.containerHashId,
                                executeCount = task.executeCount ?: 1
                            )
                        }
                    }
                }

                // 直接结束流水线的
                if (checkResult.failEnd) {
                    logger.info("[$buildId]|QUALITY_$position|taskId=$elementId|quality check fail stop directly")
                    return AtomResponse(
                        buildStatus = BuildStatus.QUALITY_CHECK_FAIL,
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_QUALITY_CHECK_FAIL,
                        errorMsg = "quality check fail"
                    ) // 拦截到直接失败
                }

                // 产生MQ消息，等待5分钟审核时间
                val auditUsers = getAuditUserList(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = interceptTask
                )
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

            pipelineEventDispatcher.dispatch(
                PipelineBuildWebSocketPushEvent(
                    source = "QualityGate($position)",
                    projectId = task.projectId,
                    pipelineId = task.pipelineId,
                    userId = task.starter,
                    buildId = task.buildId,
                    refreshTypes = RefreshType.DETAIL.binary
                )
            )
        }
        return AtomResponse(BuildStatus.RUNNING)
    }

    fun getCheckResult(
        task: PipelineBuildTask,
        interceptTaskName: String?,
        interceptTask: String?,
        runVariables: Map<String, String>,
        buildLogPrinter: BuildLogPrinter,
        position: String,
        templateId: String?
    ): RuleCheckResult {
        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val buildId = task.buildId
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
            stageId = "",
            runtimeVariable = runVariables
        )
        val result = if (position == ControlPointPosition.AFTER_POSITION &&
            QUALITY_CODECC_LAZY_ATOM.contains(interceptTask)
        ) {
            run loop@{
                QUALITY_LAZY_TIME_GAP.forEachIndexed { index, gap ->
                    val hasMetadata = hasCodeccHisMetadata(buildId)
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
            check(buildCheckParams, position)
        } else {
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "检测红线结果",
                tag = elementId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            check(buildCheckParams, position)
        }
        logger.info("quality gateway $position check result for ${task.buildId}: $result")
        return result
    }

    fun tryFinish(task: PipelineBuildTask, buildLogPrinter: BuildLogPrinter): AtomResponse {
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
                AtomResponse(
                    buildStatus = BuildStatus.QUALITY_CHECK_FAIL,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_QUALITY_CHECK_FAIL,
                    errorMsg = "quality check fail"
                )
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
                        AtomResponse(
                            buildStatus = BuildStatus.REVIEW_ABORT,
                            errorType = ErrorType.USER,
                            errorCode = ErrorCode.USER_QUALITY_CHECK_FAIL,
                            errorMsg = "quality review abort"
                        )
                    }
                }
            } else {
                AtomResponse(BuildStatus.REVIEWING)
            }
        }
    }
}
