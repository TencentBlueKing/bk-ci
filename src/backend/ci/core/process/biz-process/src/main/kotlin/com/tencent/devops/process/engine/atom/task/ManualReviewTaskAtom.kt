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
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.bean.PipelineUrlBean
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Date

/**
 * 人工审核插件
 */
class ManualReviewTaskAtom(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineUrlBean: PipelineUrlBean
) : IAtomTask<ManualReviewUserTaskElement> {

    override fun getParamElement(task: PipelineBuildTask): ManualReviewUserTaskElement {
        return JsonUtil.mapTo((task.taskParams), ManualReviewUserTaskElement::class.java)
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
        logger.info("[$buildId]|TRY_FINISH|${task.taskName}|taskId=$taskId|action=$manualAction")
        if (manualAction.isNotEmpty()) {
            val manualActionUserId = task.getTaskParam(BS_MANUAL_ACTION_USERID)
            return when (ManualReviewAction.valueOf(manualAction)) {
                ManualReviewAction.PROCESS -> {
                    LogUtils.addYellowLine(
                        rabbitTemplate = rabbitTemplate,
                        buildId = buildId,
                        message = "步骤审核结束，审核结果：[继续]，审核人：$manualActionUserId",
                        tag = taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    AtomResponse(BuildStatus.SUCCEED)
                }
                ManualReviewAction.ABORT -> {
                    LogUtils.addYellowLine(
                        rabbitTemplate = rabbitTemplate,
                        buildId = buildId,
                        message = "步骤审核结束，审核结果：[驳回]，审核人：$manualActionUserId",
                        tag = taskId,
                        jobId = task.containerHashId,
                        executeCount = task.executeCount ?: 1
                    )
                    AtomResponse(BuildStatus.REVIEW_ABORT)
                }
            }
        }
        return AtomResponse(BuildStatus.REVIEWING)
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
        if (reviewUsers.isBlank()) {
            logger.error("[$buildId]|taskId=$taskId|Review user is empty")
            return AtomResponse(BuildStatus.FAILED)
        }

        // 开始进入人工审核步骤，需要打印日志，并发送通知给审核人
        LogUtils.addYellowLine(
            rabbitTemplate, task.buildId, "步骤等待审核，审核人：$reviewUsers\n==============================",
            taskId, task.containerHashId, task.executeCount ?: 1
        )

        val pipelineName = runVariables[PIPELINE_NAME].toString()

        val reviewUrl = pipelineUrlBean.genBuildDetailUrl(projectCode, pipelineId, buildId)
        val reviewAppUrl = pipelineUrlBean.genAppBuildDetailUrl(projectCode, pipelineId, buildId)

        val date = DateTimeUtil.formatDate(Date(), "yyyy-MM-dd HH:mm:ss")

        val projectName = client.get(ServiceProjectResource::class).get(projectCode).data!!.projectName

        val buildNo = runVariables[PIPELINE_BUILD_NUM] ?: "1"
        val message = EmailNotifyMessage().apply {
            addAllReceivers(reviewUsers.split(",").toSet())
            format = EnumEmailFormat.HTML
            body = NotifyTemplateUtils.getReviewEmailBody(reviewUrl, date, projectName, pipelineName, buildNo)
            title = "【蓝盾流水线审核通知】[BKDevOps Pipeline Review Notice]"
            sender = "DevOps"
        }
        logger.info("[$buildId]|START|taskId=$taskId|Start to send the email message to $reviewUsers")
        val result = client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        if (result.isNotOk() || result.data == null) {
            logger.warn("[$buildId]|taskId=$taskId|Fail to send the email message($message) because of ${result.message}")
        }

        val bodyMessage =
            NotifyTemplateUtils.getReviewRtxMsgBody(reviewUrl, reviewAppUrl, projectName, pipelineName, buildNo)
        val bodyMessageWeixin =
            NotifyTemplateUtils.getRevieWeixinMsgBody(reviewUrl, reviewAppUrl, projectName, pipelineName, buildNo)

        val rtxMessage = RtxNotifyMessage().apply {
            addAllReceivers(reviewUsers.split(",").toSet())
            body = bodyMessage
            title = "【蓝盾流水线审核通知】[BKDevOps Pipeline Review Notice]"
            sender = "DevOps"
        }

        val rtxResult = client.get(ServiceNotifyResource::class).sendRtxNotify(rtxMessage)
        if (rtxResult.isNotOk() || rtxResult.data == null) {
            logger.warn("[$buildId]|START|taskId=$taskId|Fail to send the rtx message($message) because of ${result.message}")
        }

        val wechatMessage = WechatNotifyMessage().apply {
            addAllReceivers(reviewUsers.split(",").toSet())
            body = bodyMessageWeixin
        }
        val wechatResult = client.get(ServiceNotifyResource::class).sendWechatNotify(wechatMessage)
        if (wechatResult.isNotOk() || wechatResult.data == null) {
            logger.warn("[$buildId]|START|taskId=$taskId|Fail to send the wechatResult message($message) because of ${wechatResult.message}")
        }

        return AtomResponse(BuildStatus.REVIEWING)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManualReviewTaskAtom::class.java)
    }
}
