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

package com.tencent.devops.plugin.quality.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.websocket.ChangeType
import com.tencent.devops.process.websocket.PipelineStatusChangeEvent
import com.tencent.devops.quality.QualityGateOutElement
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class QualityGateOutTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : IAtomTask<QualityGateOutElement> {

    override fun getParamElement(task: PipelineBuildTask): QualityGateOutElement {
        return JsonUtil.mapTo(task.taskParams, QualityGateOutElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: QualityGateOutElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        val pipelineId = task.pipelineId
        logger.info("[$pipelineId]")

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
                LogUtils.addRedLine(rabbitTemplate, buildId, "${taskName}审核超时", taskId, task.containerHashId, task.executeCount ?: 1)
                AtomResponse(BuildStatus.QUALITY_CHECK_FAIL)
            }
        } else {
            val manualAction = task.getTaskParam(BS_MANUAL_ACTION)
            logger.info("[$buildId]|QUALITY_FINISH|taskName=$taskName|taskId=${task.taskId}|action=$manualAction")
            if (manualAction.isNotEmpty()) {
                when (ManualReviewAction.valueOf(manualAction)) {
                    ManualReviewAction.PROCESS -> {
                        LogUtils.addYellowLine(rabbitTemplate, buildId, "步骤审核结束，审核结果：[继续]，审核人：$actionUser", taskId, task.containerHashId, task.executeCount ?: 1)
                        AtomResponse(BuildStatus.SUCCEED)
                    }
                    ManualReviewAction.ABORT -> {
                        LogUtils.addYellowLine(rabbitTemplate, buildId, "步骤审核结束，审核结果：[驳回]，审核人：$actionUser", taskId, task.containerHashId, task.executeCount ?: 1)
                        AtomResponse(BuildStatus.REVIEW_ABORT)
                    }
                }
            } else {
                AtomResponse(BuildStatus.REVIEWING)
            }
        }
    }

    override fun execute(
            task: PipelineBuildTask,
            param: QualityGateOutElement,
            runVariables: Map<String, String>
    ): AtomResponse {
        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val buildId = task.buildId
        val elementId = task.taskId
        val buildNo = runVariables[PIPELINE_BUILD_NUM].toString()
        val templateId = task.templateId
        val interceptTaskName = param.interceptTaskName ?: ""

        logger.info("[$buildId]|QUALITY_OUT|taskId=$elementId|quality check")

        val interceptTask = param.interceptTask

        if (interceptTask == null) {
            logger.error("[$buildId]|QUALITY_OUT|taskId=$elementId|Fail to find quality gate intercept element")
            throw RuntimeException("Fail to find quality gate intercept element")
        }

        val startTime = LocalDateTime.now()

        val checkResult = try {
            val buildCheckParams = BuildCheckParams(projectId, pipelineId, buildId, buildNo, interceptTaskName,
                    startTime.timestamp(), interceptTask, ControlPointPosition.AFTER_POSITION, templateId)
            QualityUtils.check(client, buildCheckParams)
        } catch (t: Throwable) {
            logger.error("[$buildId]|QUALITY_OUT|taskId=$elementId|quality check fail", t)
            throw RuntimeException("质量红线(准出)检测失败")
        }
        pipelineEventDispatcher.dispatch(PipelineStatusChangeEvent(
                source = "pipelineDetailChangeEvent",
                pipelineId = pipelineId,
                changeType = ChangeType.DETAIL,
                buildId = buildId,
                projectId = projectId,
                userId = task.starter
        ))

        if (checkResult.success) {
            LogUtils.addLine(rabbitTemplate, buildId, "质量红线(准出)检测已通过", elementId, task.containerHashId,task.executeCount ?: 1)

            checkResult.resultList.forEach {
                LogUtils.addLine(rabbitTemplate, buildId, "规则：${it.ruleName}", elementId, task.containerHashId,task.executeCount ?: 1)
                it.messagePairs.forEach { message ->
                    LogUtils.addLine(rabbitTemplate, buildId, message.first + " " + message.second, elementId, task.containerHashId,task.executeCount ?: 1)
                }
            }

            // 产生MQ消息，等待5秒时间
            logger.info("[$buildId]|QUALITY_OUT|taskId=$elementId|quality check success wait end")
            task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = 5000
            task.taskParams[QUALITY_RESULT] = checkResult.success
        } else {
            LogUtils.addRedLine(rabbitTemplate, buildId, "质量红线(准出)检测被拦截", elementId, task.containerHashId,task.executeCount ?: 1)

            checkResult.resultList.forEach {
                LogUtils.addRedLine(rabbitTemplate, buildId, "规则：${it.ruleName}", elementId, task.containerHashId,task.executeCount ?: 1)
                it.messagePairs.forEach { message ->
                    LogUtils.addRedLine(rabbitTemplate, buildId, message.first + " " + message.second, elementId, task.containerHashId,task.executeCount ?: 1)
                }
            }

            // 直接结束流水线的
            if (checkResult.failEnd) {
                logger.info("[$buildId]|QUALITY_OUT|taskId=$elementId|quality check fail stop directly")
                // LogUtils.addFoldEndLine(rabbitTemplate, buildId, elementName, elementId, task.containerHashId,task.executeCount ?: 1)
                return AtomResponse(BuildStatus.QUALITY_CHECK_FAIL) // 拦截到直接失败
            }

            // 产生MQ消息，等待5分钟审核时间
            logger.info("[$buildId]|QUALITY_OUT|taskId=$elementId|quality check fail wait reviewing")
            val auditUsers = QualityUtils.getAuditUserList(client, projectId, pipelineId, buildId, interceptTask)
            LogUtils.addLine(rabbitTemplate, buildId, "质量红线(准出)待审核!审核人：$auditUsers", elementId, task.containerHashId,task.executeCount ?: 1)
            task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = checkResult.auditTimeoutSeconds * 1000 // 60000*5
            task.taskParams[QUALITY_RESULT] = checkResult.success
        }
        return AtomResponse(BuildStatus.RUNNING)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityGateOutTaskAtom::class.java)
    }
}
