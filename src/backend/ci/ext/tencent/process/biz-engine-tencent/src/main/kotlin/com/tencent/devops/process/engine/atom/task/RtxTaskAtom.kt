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

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_VIEW_DETAILS
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.element.SendRTXNotifyElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.process.constant.ProcessMessageCode.BK_COMPUTER_VIEW_DETAILS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_EMPTY_TITLE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_MESSAGE_CONTENT_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_RECEIVER_EMPTY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SEND_WECOM_MESSAGE
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
                    message = MessageUtil.getMessageByLocale(
                        messageCode = BK_RECEIVER_EMPTY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = MessageUtil.getMessageByLocale(
                        messageCode = BK_RECEIVER_EMPTY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            }
            if (param.body.isBlank()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = MessageUtil.getMessageByLocale(
                        messageCode = BK_MESSAGE_CONTENT_EMPTY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = MessageUtil.getMessageByLocale(
                        messageCode = BK_MESSAGE_CONTENT_EMPTY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            }
            if (param.title.isEmpty()) {
                buildLogPrinter.addRedLine(
                    buildId = buildId,
                    message = MessageUtil.getMessageByLocale(
                        messageCode = BK_EMPTY_TITLE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = taskId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = MessageUtil.getMessageByLocale(
                        messageCode = BK_EMPTY_TITLE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            }

            val sendDetailFlag = param.detailFlag != null && param.detailFlag!!

            val detailUrl = detailUrl(projectId, pipelineId, buildId)

            val detailOuterUrl = detailOuterUrl(projectId, pipelineId, buildId)

            val bodyStrOrigin = parseVariable(param.body, runVariables)
            // 企业微信通知是否加上详情
            val bodyStr = if (sendDetailFlag) {
                MessageUtil.getMessageByLocale(
                    messageCode = BK_COMPUTER_VIEW_DETAILS,//{0}\n\n电脑查看详情：{1}\n手机查看详情：{2}
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(bodyStrOrigin, detailUrl, detailOuterUrl)
                )
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
                message = MessageUtil.getMessageByLocale(
                    messageCode = BK_SEND_WECOM_MESSAGE,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(message.body, receiversStr)
                ),
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
                                    MessageUtil.getMessageByLocale(
                                        messageCode = BK_VIEW_DETAILS,
                                        language = I18nUtil.getDefaultLocaleLanguage()
                                    ),
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
