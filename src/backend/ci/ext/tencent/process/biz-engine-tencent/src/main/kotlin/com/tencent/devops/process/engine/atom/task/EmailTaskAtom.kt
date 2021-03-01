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

import com.tencent.devops.common.api.pojo.ErrorCode.USER_INPUT_INVAILD
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.common.pipeline.element.SendEmailNotifyElement
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.ServiceHomeUrlUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class EmailTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<SendEmailNotifyElement> {

    override fun getParamElement(task: PipelineBuildTask): SendEmailNotifyElement {
        return JsonUtil.mapTo(task.taskParams, SendEmailNotifyElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: SendEmailNotifyElement, runVariables: Map<String, String>): AtomResponse {
        val buildId = task.buildId
        val taskId = task.taskId
        val containerId = task.containerHashId
        if (param.receivers.isEmpty()) {
            buildLogPrinter.addRedLine(buildId, "收件人为空", taskId, containerId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "收件人为空"
            )
        }
        if (param.body.isBlank()) {
            buildLogPrinter.addRedLine(buildId, "邮件通知内容为空", taskId, containerId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "邮件通知内容为空"
            )
        }
        if (param.title.isBlank()) {
            buildLogPrinter.addRedLine(buildId, "邮件主题为空", taskId, containerId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_INPUT_INVAILD,
                errorMsg = "邮件主题为空"
            )
        }

        val projectId = task.projectId
        val pipelineId = task.pipelineId

        val detailUrl = detailUrl(projectId, pipelineId, buildId)
        val emailBody = parseVariable(param.body, runVariables)
        val message = EmailNotifyMessage().apply {
            format = EnumEmailFormat.HTML
            body = "$emailBody<br/><br/><a target='_blank' href=\"$detailUrl\">查看详情</a>"
            title = parseVariable(param.title, runVariables)
        }

        val receiversStr = parseVariable(param.receivers.joinToString(","), runVariables)
        buildLogPrinter.addLine(buildId, "send Email message ($emailBody) to $receiversStr", taskId, containerId, task.executeCount ?: 1)

        message.addAllReceivers(getSet(receiversStr))
        message.addAllCcs(getCcSet(parseVariable(param.cc.joinToString(","), runVariables)))
        client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun detailUrl(projectId: String, pipelineId: String, buildId: String) =
            "${ServiceHomeUrlUtils.server()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"

    private fun getSet(receiverStr: String): Set<String> {
        val set = mutableSetOf<String>()
        receiverStr.split(",").forEach {
            set.add(it)
        }
        return set
    }

    private fun getCcSet(receiverStr: String): Set<String> {
        if (receiverStr == "##") return setOf()
        val set = mutableSetOf<String>()
        receiverStr.split(",").forEach {
            set.add(it.trim())
        }
        return set
    }
}
