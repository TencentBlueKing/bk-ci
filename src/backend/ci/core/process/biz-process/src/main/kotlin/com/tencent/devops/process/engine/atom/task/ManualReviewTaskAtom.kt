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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.bean.PipelineUrlBean
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_PARAMS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_SUGGEST
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * 人工审核插件
 */
class ManualReviewTaskAtom(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineUrlBean: PipelineUrlBean,
    private val pipelineVariableService: BuildVariableService
) : IAtomTask<ManualReviewUserTaskElement> {

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

        if (reviewUsers.isBlank()) {
            logger.warn("[$buildId]|taskId=$taskId|Review user is empty")
            return AtomResponse(BuildStatus.FAILED)
        }

        // 开始进入人工审核步骤，需要打印日志，并发送通知给审核人
        buildLogPrinter.addYellowLine(
            buildId = task.buildId,
            message = "============步骤等待审核============",
            tag = taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId,
            message = "待审核人：$reviewUsers",
            tag = taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = task.buildId,
            message = "审核说明：$reviewDesc",
            tag = taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )
        buildLogPrinter.addLine(
            buildId = buildId,
            message = "审核参数：${param.params.map { "{key=${it.key}, value=${it.value}" }}",
            tag = taskId,
            jobId = task.containerHashId,
            executeCount = task.executeCount ?: 1
        )

        val pipelineName = runVariables[PIPELINE_NAME].toString()
        val reviewUrl = pipelineUrlBean.genBuildDetailUrl(projectCode, pipelineId, buildId)
        val reviewAppUrl = pipelineUrlBean.genAppBuildDetailUrl(projectCode, pipelineId, buildId)
        val date = DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss")
        val projectName = client.get(ServiceProjectResource::class).get(projectCode).data!!.projectName
        val buildNo = runVariables[PIPELINE_BUILD_NUM] ?: "1"

        sendReviewNotify(
            receivers = reviewUsers.split(",").toMutableSet(),
            reviewDesc = reviewDesc,
            reviewUrl = reviewUrl,
            reviewAppUrl = reviewAppUrl,
            projectName = projectName,
            pipelineName = pipelineName,
            dataTime = date,
            buildNo = buildNo
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
        val taskParam = JsonUtil.toMutableMapSkipEmpty(task.taskParams)
        logger.info("[$buildId]|TRY_FINISH|${task.taskName}|taskId=$taskId|action=$manualAction")
        if (manualAction.isNotEmpty()) {
            val manualActionUserId = task.getTaskParam(BS_MANUAL_ACTION_USERID)
            val suggestContent = taskParam[BS_MANUAL_ACTION_SUGGEST]
            buildLogPrinter.addYellowLine(
                buildId = task.buildId,
                message = "============步骤审核结束============",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "审核人：$manualActionUserId",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "审核意见：$suggestContent",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            val reviewParamKey = if (param.namespace.isNullOrBlank()) {
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
                buildId = buildId,
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                varName = reviewParamKey,
                varValue = manualActionUserId
            )
            val response = when (ManualReviewAction.valueOf(manualAction)) {
                ManualReviewAction.PROCESS -> {
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "审核结果：继续",
                        tag = taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = "审核参数：${JsonUtil.getObjectMapper().readValue(taskParam[BS_MANUAL_ACTION_PARAMS].toString(), List::class.java)}",
                        tag = taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    AtomResponse(BuildStatus.SUCCEED)
                }
                ManualReviewAction.ABORT -> {
                    buildLogPrinter.addRedLine(
                        buildId = buildId,
                        message = "审核结果：驳回",
                        tag = taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    AtomResponse(BuildStatus.REVIEW_ABORT)
                }
            }
            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = "output(except): $reviewParamKey=$manualActionUserId",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = "output(except): $suggestParamKey=$suggestContent",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            return response
        }
        return AtomResponse(BuildStatus.REVIEWING)
    }

    private fun sendReviewNotify(
        receivers: MutableSet<String>,
        reviewDesc: String,
        reviewUrl: String,
        reviewAppUrl: String,
        dataTime: String,
        projectName: String,
        pipelineName: String,
        buildNo: String
    ) {
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = PIPELINE_MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE,
            receivers = receivers,
            cc = receivers,
            titleParams = mapOf(
                "projectName" to projectName,
                "pipelineName" to pipelineName,
                "buildNo" to buildNo
            ),
            bodyParams = mapOf(
                "projectName" to projectName,
                "pipelineName" to pipelineName,
                "buildNo" to buildNo,
                "reviewDesc" to reviewDesc,
                "reviewUrl" to reviewUrl,
                "reviewAppUrl" to reviewAppUrl,
                "dataTime" to dataTime
            )
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("[$buildNo]|sendReviewNotify|ManualReviewTaskAtom|result=$sendNotifyResult")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManualReviewTaskAtom::class.java)
        const val MANUAL_REVIEW_ATOM_REVIEWER = "MANUAL_REVIEWER"
        const val MANUAL_REVIEW_ATOM_SUGGEST = "MANUAL_REVIEW_SUGGEST"
    }
}
