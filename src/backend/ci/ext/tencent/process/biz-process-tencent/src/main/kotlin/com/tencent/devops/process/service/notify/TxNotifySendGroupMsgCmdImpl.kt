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

package com.tencent.devops.process.service.notify

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.common.api.constant.I18NConstant.BK_VIEW_DETAILS
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.robot.MsgInfo
import com.tencent.devops.common.wechatwork.model.robot.RobotMarkdownSendMsg
import com.tencent.devops.common.wechatwork.model.robot.RobotTextSendMsg
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.NotifyCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class TxNotifySendGroupMsgCmdImpl @Autowired constructor(
    val wechatWorkService: WechatWorkService,
    val wechatWorkRobotService: WechatWorkRobotService
) : NotifyCmd {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val setting = commandContext.pipelineSetting
        val buildStatus = commandContext.buildStatus
        logger.info("send weworkGroup msg: ${setting.pipelineId}|${commandContext.buildStatus}")
        var groups = mutableSetOf<String>()
        var content = ""
        var markerDownFlag = false
        var detailFlag = false
        if (buildStatus.isFailure()) {
            if (emptyGroup(setting.failSubscription.wechatGroup) || !setting.failSubscription.wechatGroupFlag) {
                return
            }
            groups.addAll(setting.failSubscription.wechatGroup.split("[,;]".toRegex()))
            content = "❌ " + commandContext.notifyValue["failContent"]!!
            markerDownFlag = setting.failSubscription.wechatGroupMarkdownFlag
            detailFlag = setting.failSubscription.detailFlag
        } else if (buildStatus.isSuccess()) {
            val successSubscription = setting.successSubscription
            if (emptyGroup(successSubscription.wechatGroup) || !successSubscription.wechatGroupFlag) {
                return
            }
            groups.addAll(successSubscription.wechatGroup.split("[,;]".toRegex()))
            content = "✔️" + commandContext.notifyValue["successContent"]!!
            markerDownFlag = successSubscription.wechatGroupMarkdownFlag
            detailFlag = successSubscription.detailFlag
        }
        logger.info("send weworkGroup msg: ${setting.pipelineId}|$groups|$markerDownFlag|$content")
        try {
            sendWeworkGroup(groups, markerDownFlag, content, commandContext.notifyValue, detailFlag)
        } catch (e: Exception) {
            logger.warn("sendweworkGroup msg fail: ${e.message}")
        }
        return
    }

    private fun sendWeworkGroup(
        weworkGroup: Set<String>,
        markerDownFlag: Boolean,
        content: String,
        vars: Map<String, String>,
        detailFlag: Boolean
    ) {
        val detailUrl = vars["detailUrl"]
        weworkGroup.forEach {
            if (it.startsWith("ww")) { // 应用号逻辑
                sendByApp(it, content, markerDownFlag, detailFlag, detailUrl!!)
            } else if (Pattern.matches(chatPatten, it)) { // 机器人逻辑
                sendByRobot(it, content, markerDownFlag, detailFlag, detailUrl!!)
            }
        }
    }

    private fun sendByApp(
        chatId: String,
        content: String,
        markerDownFlag: Boolean,
        detailFlag: Boolean,
        detailUrl: String
    ) {
        logger.info("send group msg by app: $chatId")
        if (markerDownFlag) {
            wechatWorkService.sendMarkdownGroup(content!!.replace("\\n", "\n"), chatId)
        } else {
            val receiver = Receiver(ReceiverType.group, chatId)
            val richtextContentList = mutableListOf<RichtextContent>()
            richtextContentList.add(
                RichtextText(RichtextTextText(content))
            )
            if (detailFlag) {
                richtextContentList.add(
                    RichtextView(
                        RichtextViewLink(text = MessageUtil.getMessageByLocale(
                            messageCode = BK_VIEW_DETAILS,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ), key = detailUrl, browser = 1)
                    )
                )
            }
            val richtextMessage = RichtextMessage(receiver, richtextContentList)
            wechatWorkService.sendRichText(richtextMessage)
        }
    }

    private fun sendByRobot(
        chatId: String,
        content: String,
        markerDownFlag: Boolean,
        detailFlag: Boolean,
        detailUrl: String
    ) {
        logger.info("send group msg by robot: $chatId, $content")
        if (markerDownFlag) {
            val textContent = if (detailFlag) {
                "$content\n[" + MessageUtil.getMessageByLocale(
                    messageCode = BK_VIEW_DETAILS,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ) + "]($detailUrl)"
            } else content
            val msg = RobotMarkdownSendMsg(
                chatId = chatId,
                markdown = MsgInfo(
                    content = textContent
                )
            )
            wechatWorkRobotService.send(msg.toJsonString())
        } else {
            val textContent = if (detailFlag) {
                "$content\n\n" + MessageUtil.getMessageByLocale(
                        messageCode = BK_VIEW_DETAILS,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ) + ": $detailUrl"
            } else content
            val msg = RobotTextSendMsg(
                chatId = chatId,
                text = MsgInfo(
                    content = textContent
                )
            )
            wechatWorkRobotService.send(msg.toJsonString())
        }
    }

    fun emptyGroup(groups: String): Boolean {
        if (groups.isNullOrBlank()) {
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxNotifySendGroupMsgCmdImpl::class.java)
        private const val roomPatten = "ww\\w" // ww 开头且接数字的正则表达式, 适用于应用号获取的roomid
        private const val chatPatten = "^[A-Za-z0-9_-]+\$" // 数字和字母组成的群chatId正则表达式
    }
}
