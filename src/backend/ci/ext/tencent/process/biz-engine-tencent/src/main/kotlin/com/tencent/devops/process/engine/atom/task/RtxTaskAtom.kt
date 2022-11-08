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
import com.tencent.devops.common.pipeline.element.SendRTXNotifyElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.ServiceHomeUrlUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Suppress("ALL", "UNUSED")
class RtxTaskAtom @Autowired constructor(
    private val client: Client,
    private val wechatWorkService: WechatWorkService,
    private val buildLogPrinter: BuildLogPrinter
) :
    IAtomTask<SendRTXNotifyElement> {
    override fun getParamElement(task: PipelineBuildTask): SendRTXNotifyElement {
        return JsonUtil.mapTo(task.taskParams, SendRTXNotifyElement::class.java)
    }

    private val logger = LoggerFactory.getLogger(RtxTaskAtom::class.java)

    override fun execute(
        task: PipelineBuildTask,
        param: SendRTXNotifyElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        with(task) {

            logger.info("Enter RtxTaskDelegate run...")
            if (param.receivers.isEmpty()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "Message Receivers is empty(接收人为空)",
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "Message Receivers is empty(接收人为空)"
                )
            }
            if (param.body.isBlank()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "Message Body is empty(消息内容为空)",
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "Message Body is empty(消息内容为空)"
                )
            }
            if (param.title.isEmpty()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = "Message Title is empty(标题为空)",
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "Message Title is empty(标题为空)"
                )
            }

            val sendDetailFlag = param.detailFlag != null && param.detailFlag!!

            val detailUrl = detailUrl(projectId, pipelineId, buildId)

            val detailOuterUrl = detailOuterUrl(projectId, pipelineId, buildId)

            val bodyStrOrigin = parseVariable(param.body, runVariables)
            // 企业微信通知是否加上详情
            val bodyStr = if (sendDetailFlag) {
                "$bodyStrOrigin\n\n电脑查看详情：$detailUrl\n手机查看详情：$detailOuterUrl"
            } else {
                bodyStrOrigin
            }
            val titleStr = parseVariable(param.title, runVariables)

            val message = RtxNotifyMessage().apply {
                body = bodyStr
                title = titleStr
            }

            val receiversStr = parseVariable(param.receivers.joinToString(","), runVariables)
            buildLogPrinter.addLine(
                buildId = buildId,
                message = "send enterprise wechat message(发送企业微信消息):\n${message.body}\nto\n$receiversStr",
                tag = taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )

            message.addAllReceivers(getReceivers(receiversStr))
            client.get(ServiceNotifyResource::class).sendRtxNotify(message)

            // 发送企业微信群消息
            val sendWechatGroupFlag = param.wechatGroupFlag != null && param.wechatGroupFlag!!
            if (sendWechatGroupFlag) {
                val wechatGroups = mutableSetOf<String>()
                val wechatGroupsStr = parseVariable(param.wechatGroup, runVariables)
                wechatGroups.addAll(wechatGroupsStr.split(",|;".toRegex()))
                wechatGroups.forEach {
                    val receiver = Receiver(ReceiverType.group, it)
                    val richtextContentList = mutableListOf<RichtextContent>()
                    richtextContentList.add(
                        RichtextText(
                            RichtextTextText(
                            "$titleStr\n\n$bodyStrOrigin\n"
                    )
                        )
                    )
                    // 企业微信群是否加上查看详情
                    if (sendDetailFlag) {
                        richtextContentList.add(
                            RichtextView(
                                RichtextViewLink(
                                "查看详情",
                                detailUrl,
                                1
                        )
                            )
                        )
                    }
                    val richtextMessage = RichtextMessage(receiver, richtextContentList)
                    wechatWorkService.sendRichText(richtextMessage)
                }
            }
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun getReceivers(receiverStr: String): Set<String> {
        val set = mutableSetOf<String>()
        receiverStr.split(",").forEach {
            set.add(it.trim())
        }
        return set
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
            "${ServiceHomeUrlUtils.server()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"

    private fun detailOuterUrl(projectId: String, pipelineId: String, processInstanceId: String) =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&" +
                    "projectId=$projectId&" +
                    "pipelineId=$pipelineId&" +
                    "buildId=$processInstanceId"
}
