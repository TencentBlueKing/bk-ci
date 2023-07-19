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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_AUDIT_RESULTS_APPROVE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_AUDIT_RESULTS_REJECT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_DESCRIPTION
import com.tencent.devops.process.constant.ProcessMessageCode.BK_FINAL_APPROVAL
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PARAMS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PENDING_APPROVAL
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REVIEWER
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REVIEWERS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REVIEW_COMMENTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineBuildNotifyEvent
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import java.time.LocalDateTime
import java.util.Date
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 人工审核插件
 */
@Suppress("UNUSED")
@Component
class ManualReviewTaskAtom(
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineVariableService: BuildVariableService
) : IAtomTask<ManualReviewUserTaskElement> {

    @Value("\${esb.appSecret:#{null}}")
    private val appSecret: String? = null
    override fun getParamElement(task: PipelineBuildTask): ManualReviewUserTaskElement {
        return JsonUtil.mapTo((task.taskParams), ManualReviewUserTaskElement::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: ManualReviewUserTaskElement,
        runVariables: Map<String, String>
    ): AtomResponse {

        val buildId = task.buildId
        val taskId = task.taskId
        val projectCode = task.projectId
        val pipelineId = task.pipelineId

        val reviewUsers = parseVariable(param.reviewUsers.joinToString(","), runVariables)
        val reviewDesc = parseVariable(param.desc, runVariables)
        val notifyTitle = parseVariable(param.notifyTitle, runVariables)

        if (reviewUsers.isBlank()) {
            logger.warn("[$buildId]|taskId=$taskId|Review user is empty")
            return AtomResponse(BuildStatus.FAILED)
        }
        val reviewUsersList = reviewUsers.split(",")

        taskBuildRecordService.updateTaskRecord(
            projectId = projectCode, pipelineId = pipelineId, buildId = buildId,
            taskId = taskId, executeCount = task.executeCount ?: 1, buildStatus = null,
            taskVar = mapOf(ManualReviewUserTaskElement::reviewUsers.name to reviewUsersList),
            operation = "manualReviewTaskStart#${task.taskId}",
            timestamps = mapOf(
                BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to
                    BuildRecordTimeStamp(LocalDateTime.now().timestampmilli(), null)
            )
        )
        // #7983 兜底只有一个审核插件的job未刷新执行状态
        containerBuildRecordService.updateContainerRecord(
            projectId = projectCode, pipelineId = pipelineId, buildId = buildId,
            containerId = task.containerId, executeCount = task.executeCount ?: 1,
            buildStatus = BuildStatus.RUNNING, containerVar = mapOf(), timestamps = mapOf()
        )

        // 开始进入人工审核步骤，需要打印日志，并发送通知给审核人
        buildLogPrinter.addYellowLine(
            buildId = task.buildId,
            message = "============${getI18nByLocal(BK_PENDING_APPROVAL)}============",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "${getI18nByLocal(BK_REVIEWERS)}：$reviewUsers",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "${getI18nByLocal(BK_DESCRIPTION)}：$reviewDesc",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = buildId,
            message = getI18nByLocal(BK_PARAMS) +
                    "：${param.params.map { "{key=${it.key}, value=${it.value}}" }}",
            tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )

        val pipelineName = runVariables[PIPELINE_NAME] ?: pipelineId
        val projectName = runVariables[PROJECT_NAME_CHINESE] ?: projectCode
        pipelineEventDispatcher.dispatch(
            PipelineBuildReviewBroadCastEvent(
                source = "ManualReviewTaskAtom",
                projectId = projectCode, pipelineId = pipelineId,
                buildId = buildId, userId = task.starter,
                reviewType = buildReviewType,
                status = BuildStatus.REVIEWING.name,
                stageId = task.stageId, taskId = taskId
            ),
            PipelineBuildNotifyEvent(
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE.name,
                source = "ManualReviewTaskAtom", projectId = projectCode, pipelineId = pipelineId,
                userId = task.starter, buildId = buildId,
                receivers = reviewUsersList,
                notifyType = NotifyUtils.checkNotifyType(param.notifyType),
                titleParams = mutableMapOf(
                    "content" to notifyTitle
                ),
                bodyParams = mutableMapOf(
                    "buildNum" to (runVariables[PIPELINE_BUILD_NUM] ?: "1"),
                    "projectName" to projectName,
                    "pipelineName" to pipelineName,
                    "dataTime" to DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss"),
                    "reviewDesc" to reviewDesc,
                    "manualReviewParam" to JsonUtil.toJson(param.params),
                    "checkParams" to param.params.isNotEmpty().toString(),
                    // 企业微信组
                    NotifyUtils.WEWORK_GROUP_KEY to (param.notifyGroup?.joinToString(separator = ",") ?: "")
                ),
                position = null,
                stageId = null,
                callbackData = mapOf(
                    "projectId" to projectCode,
                    "pipelineId" to pipelineId,
                    "buildId" to buildId,
                    "elementId" to (param.id ?: ""),
                    "reviewUsers" to reviewUsers,
                    "signature" to ShaUtils.sha256(projectCode + buildId + (param.id ?: "") + appSecret)
                ),
                markdownContent = param.markdownContent
            )
        )

        return AtomResponse(BuildStatus.REVIEWING)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: ManualReviewUserTaskElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {

        val taskId = task.taskId
        val buildId = task.buildId
        val manualAction = task.getTaskParam(BS_MANUAL_ACTION)
        val taskParam = JsonUtil.toMutableMap(task.taskParams)
        logger.info("[$buildId]|TRY_FINISH|${task.taskName}|taskId=$taskId|action=$manualAction")
        if (manualAction.isBlank()) {
            return AtomResponse(BuildStatus.REVIEWING)
        }

        val manualActionUserId = task.getTaskParam(BS_MANUAL_ACTION_USERID)
        val suggestContent = beforePrint(
            task = task,
            taskParam = taskParam,
            manualActionUserId = manualActionUserId
        )

        val response = when (ManualReviewAction.valueOf(manualAction)) {
            ManualReviewAction.PROCESS -> {
                buildLogPrinter.addLine(
                    buildId = buildId, message = getI18nByLocal(BK_AUDIT_RESULTS_APPROVE),
                    tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                buildLogPrinter.addLine(
                    buildId = buildId, message = "${getI18nByLocal(BK_PARAMS)}：${getParamList(taskParam)}",
                    tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                pipelineEventDispatcher.dispatch(
                    PipelineBuildReviewBroadCastEvent(
                        source = "tasks(${task.taskId}) reviewed with PROCESSED",
                        projectId = task.projectId, pipelineId = task.pipelineId,
                        buildId = task.buildId, userId = manualActionUserId,
                        reviewType = buildReviewType, status = BuildStatus.REVIEW_PROCESSED.name,
                        stageId = task.stageId, taskId = task.taskId
                    )
                )
                AtomResponse(BuildStatus.SUCCEED)
            }
            ManualReviewAction.ABORT -> {
                buildLogPrinter.addRedLine(
                    buildId = buildId, message = getI18nByLocal(BK_AUDIT_RESULTS_REJECT),
                    tag = taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                pipelineEventDispatcher.dispatch(
                    PipelineBuildReviewBroadCastEvent(
                        source = "tasks(${task.taskId}) reviewed with ABORT",
                        projectId = task.projectId, pipelineId = task.pipelineId,
                        buildId = task.buildId, userId = manualActionUserId,
                        reviewType = buildReviewType, status = BuildStatus.REVIEW_ABORT.name,
                        stageId = task.stageId, taskId = task.taskId
                    )
                )
                AtomResponse(BuildStatus.REVIEW_ABORT)
            }
        }
        val reviewUsers = parseVariable(param.reviewUsers.joinToString(","), runVariables)
        pipelineEventDispatcher.dispatch(
            // 发送审批取消通知
            PipelineBuildNotifyEvent(
                notifyCompleteCheck = true,
                notifyTemplateEnum = PipelineNotifyTemplateEnum.PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE.name,
                source = "ManualReviewTaskAtomFinish", projectId = task.projectId, pipelineId = task.pipelineId,
                userId = task.starter, buildId = buildId,
                receivers = reviewUsers.split(","),
                notifyType = NotifyUtils.checkNotifyType(param.notifyType),
                titleParams = mutableMapOf(),
                bodyParams = mutableMapOf(),
                position = null,
                stageId = null,
                callbackData = mapOf(
                    "signature" to ShaUtils.sha256(task.projectId + buildId + (param.id ?: "") + appSecret)
                )
            )
        )
        postPrint(param = param, task = task, suggestContent = suggestContent)
        return response
    }

    private fun getParamList(taskParam: MutableMap<String, Any>) =
        try {
            JsonUtil.getObjectMapper().readValue(taskParam[BS_MANUAL_ACTION_PARAMS].toString(), List::class.java)
        } catch (ignored: Exception) {
            null
        }

    private fun postPrint(param: ManualReviewUserTaskElement, task: PipelineBuildTask, suggestContent: Any?) {
        val manualAction = task.getTaskParam(BS_MANUAL_ACTION)
        val manualActionUserId = task.getTaskParam(BS_MANUAL_ACTION_USERID)
        val reviewResultParamKey = if (param.namespace.isNullOrBlank()) {
            MANUAL_REVIEW_ATOM_RESULT
        } else {
            "${param.namespace}_$MANUAL_REVIEW_ATOM_RESULT"
        }
        val reviewerParamKey = if (param.namespace.isNullOrBlank()) {
            MANUAL_REVIEW_ATOM_REVIEWER
        } else {
            "${param.namespace}_$MANUAL_REVIEW_ATOM_REVIEWER"
        }
        val suggestParamKey = if (param.namespace.isNullOrBlank()) {
            MANUAL_REVIEW_ATOM_SUGGEST
        } else {
            "${param.namespace}_$MANUAL_REVIEW_ATOM_SUGGEST"
        }
        pipelineVariableService.setVariable(
            buildId = task.buildId, projectId = task.projectId, pipelineId = task.pipelineId,
            varName = reviewResultParamKey, varValue = manualAction
        )
        pipelineVariableService.setVariable(
            buildId = task.buildId, projectId = task.projectId, pipelineId = task.pipelineId,
            varName = reviewerParamKey, varValue = manualActionUserId
        )
        pipelineVariableService.setVariable(
            buildId = task.buildId, projectId = task.projectId, pipelineId = task.pipelineId,
            varName = suggestParamKey, varValue = suggestContent ?: ""
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "output(except): $reviewResultParamKey=$manualAction",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "output(except): $reviewerParamKey=$manualActionUserId",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "output(except): $suggestParamKey=$suggestContent",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
    }

    private fun beforePrint(
        task: PipelineBuildTask,
        taskParam: MutableMap<String, Any>,
        manualActionUserId: String
    ): Any? {
        val suggestContent = taskParam[BS_MANUAL_ACTION_SUGGEST]
        buildLogPrinter.addYellowLine(
            buildId = task.buildId, message = "============${getI18nByLocal(BK_FINAL_APPROVAL)}============",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "${getI18nByLocal(BK_REVIEWER)}：$manualActionUserId",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId, message = "${getI18nByLocal(BK_REVIEW_COMMENTS)}：$suggestContent",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        return suggestContent
    }

    private fun getI18nByLocal(messageCode: String, params: Array<String>? = null): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = messageCode,
            language = I18nUtil.getDefaultLocaleLanguage(),
            params = params
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManualReviewTaskAtom::class.java)
        private val buildReviewType = BuildReviewType.TASK_REVIEW
        const val MANUAL_REVIEW_ATOM_REVIEWER = "MANUAL_REVIEWER"
        const val MANUAL_REVIEW_ATOM_SUGGEST = "MANUAL_REVIEW_SUGGEST"
        const val MANUAL_REVIEW_ATOM_RESULT = "MANUAL_REVIEW_RESULT"
    }
}
