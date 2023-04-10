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

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.JobWrapper
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.SendWechatNotifyElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.process.constant.ProcessMessageCode.BK_COMPUTER_VIEW_DETAILS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_INVALID_NOTIFICATION_RECIPIENT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SEND_WECOM_CONTENT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SEND_WECOM_CONTENT_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SEND_WECOM_CONTENT_SUCCESSFULLY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_WECOM_NOTICE
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PROJECT_NAME
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WechatTaskAtom @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) :
    IAtomTask<SendWechatNotifyElement> {
    override fun getParamElement(task: PipelineBuildTask): SendWechatNotifyElement {
        return JsonUtil.mapTo(task.taskParams, SendWechatNotifyElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: SendWechatNotifyElement, runVariables: Map<String, String>): AtomResponse {
        val taskId = task.taskId
        val buildId = task.buildId
        if (param.receivers.isEmpty()) {
            buildLogPrinter.addRedLine(buildId, I18nUtil.getCodeLanMessage(
                messageCode = BK_INVALID_NOTIFICATION_RECIPIENT
            ) + "[${param.receivers}]", taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = I18nUtil.getCodeLanMessage(
                    messageCode = BK_INVALID_NOTIFICATION_RECIPIENT
                ) + "[${param.receivers}]"
            )
        }
        if (param.body.isBlank()) {
            buildLogPrinter.addRedLine(buildId, I18nUtil.getCodeLanMessage(
                messageCode = BK_WECOM_NOTICE
            ) + "[${param.body}]", taskId, task.containerHashId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = I18nUtil.getCodeLanMessage(
                    messageCode = BK_WECOM_NOTICE
                ) + "[${param.body}]"
            )
        }
        val sendDetailFlag = param.detailFlag ?: false

        var bodyStr = parseVariable(param.body, runVariables)

        // 启动短信的查看详情
        if (sendDetailFlag) {
            val outerUrl = "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&" +
                    "projectId=${runVariables[PROJECT_NAME]}&" +
                    "pipelineId=${runVariables[PIPELINE_ID]}&" +
                    "buildId=$buildId"
            val innerUrl = "${HomeHostUtil.innerServerHost()}/console/pipeline/${runVariables[PROJECT_NAME]}/${runVariables[PIPELINE_ID]}/detail/$buildId"
            bodyStr = I18nUtil.getCodeLanMessage(
                messageCode = BK_COMPUTER_VIEW_DETAILS,
                params = arrayOf(bodyStr, innerUrl, outerUrl)
            )
        }
        val message = WechatNotifyMessage().apply {
            body = bodyStr
        }
        val receiversStr = parseVariable(param.receivers.joinToString(","), runVariables)
        buildLogPrinter.addLine(buildId,
            I18nUtil.getCodeLanMessage(
                messageCode = BK_SEND_WECOM_CONTENT,
                params = arrayOf(message.body, receiversStr)
            ), taskId, task.containerHashId, task.executeCount ?: 1)

        message.addAllReceivers(receiversStr.split(",").toSet())

        val success = (object : JobWrapper {
            override fun doIt(): Boolean {
                val resp = client.get(ServiceNotifyResource::class).sendWechatNotify(message)
                if (resp.isOk()) {
                    if (resp.data!!) {
                        buildLogPrinter.addLine(buildId,
                            I18nUtil.getCodeLanMessage(
                                messageCode = BK_SEND_WECOM_CONTENT_SUCCESSFULLY,
                                params = arrayOf(message.body, receiversStr)
                            ), taskId, task.containerHashId, task.executeCount ?: 1)
                        return true
                    }
                }
                buildLogPrinter.addRedLine(buildId, I18nUtil.getCodeLanMessage(
                    messageCode = BK_SEND_WECOM_CONTENT_FAILED,
                    params = arrayOf(message.body, receiversStr)
                ) + "${resp.message}", taskId, task.containerHashId, task.executeCount ?: 1)
                return false
            }
        }).tryDoIt()

        return if (success) AtomResponse(BuildStatus.SUCCEED) else defaultFailAtomResponse
    }
}
