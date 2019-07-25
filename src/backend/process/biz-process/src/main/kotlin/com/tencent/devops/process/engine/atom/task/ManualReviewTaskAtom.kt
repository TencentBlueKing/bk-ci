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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION
import com.tencent.devops.process.engine.common.BS_MANUAL_ACTION_USERID
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * 人工审核插件
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ManualReviewTaskAtom(private val rabbitTemplate: RabbitTemplate) : IAtomTask<ManualReviewUserTaskElement> {

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

        val reviewUsers = parseVariable(param.reviewUsers.joinToString(","), runVariables)
        if (reviewUsers.isBlank()) {
            logger.error("[$buildId]|taskId=$taskId|Review user is empty")
            return AtomResponse(BuildStatus.FAILED)
        }

        // 开始进入人工审核步骤，需要打印日志，并发送通知给审核人
        LogUtils.addYellowLine(rabbitTemplate, task.buildId, "步骤等待审核，审核人：$reviewUsers", taskId, task.executeCount ?: 1)
        logger.info("[$buildId]|START|taskId=$taskId|Start to send the email message to $reviewUsers")

        return AtomResponse(BuildStatus.REVIEWING)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManualReviewTaskAtom::class.java)
    }
}
