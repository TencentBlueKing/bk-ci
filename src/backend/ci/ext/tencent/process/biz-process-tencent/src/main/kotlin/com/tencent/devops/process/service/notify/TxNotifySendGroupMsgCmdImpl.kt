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
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_VIEW_DETAILS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
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
import com.tencent.devops.process.notify.command.impl.BluekingNotifySendCmd
import com.tencent.devops.process.notify.command.impl.NotifySendCmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
@Suppress("ComplexMethod", "NestedBlockDepth")
class TxNotifySendGroupMsgCmdImpl @Autowired constructor(
    client: Client,
    val bsAuthProjectApi: AuthProjectApi,
    val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    val wechatWorkService: WechatWorkService,
    val wechatWorkRobotService: WechatWorkRobotService
) : NotifySendCmd(client) {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val replaceWithEmpty = true
        val setting = commandContext.pipelineSetting
        val buildStatus = commandContext.buildStatus
        val shutdownType = when {
            buildStatus.isCancel() -> BluekingNotifySendCmd.TYPE_SHUTDOWN_CANCEL
            buildStatus.isFailure() -> BluekingNotifySendCmd.TYPE_SHUTDOWN_FAILURE
            else -> BluekingNotifySendCmd.TYPE_SHUTDOWN_SUCCESS
        }
        when {
            buildStatus.isSuccess() -> {
                setting.successSubscriptionList?.forEach { successSubscription ->
                    // 内容为null的时候处理为空字符串
                    val successContent = EnvUtils.parseEnv(
                        successSubscription.content, commandContext.variables, replaceWithEmpty
                    )
                    commandContext.notifyValue["successContent"] = successContent
                    commandContext.notifyValue["emailSuccessContent"] = successContent
                    val receivers = successSubscription.users.split(",").map {
                        EnvUtils.parseEnv(
                            command = it,
                            data = commandContext.variables,
                            replaceWithEmpty = true
                        )
                    }.toMutableSet()
                    if (!emptyGroup(successSubscription.groups)) {
                        logger.info("success notify config group: ${successSubscription.groups}")
                        val projectRoleUsers = bsAuthProjectApi.getProjectGroupAndUserList(
                            serviceCode = bsPipelineAuthServiceCode,
                            projectCode = commandContext.projectId)
                        projectRoleUsers.forEach {
                            if (it.roleName in successSubscription.groups) {
                                receivers.addAll(it.userIdList)
                            }
                        }
                    }
                    sendNotify(
                        shutdownType = shutdownType,
                        subscription = successSubscription,
                        receivers = receivers,
                        params = commandContext.notifyValue,
                        setting = setting,
                        buildStatus = buildStatus,
                        variables = commandContext.variables,
                        content = "✔️ $successContent"
                    )
                }
            }
            buildStatus.isFailure() -> {
                setting.failSubscriptionList?.forEach { failSubscription ->
                    // 内容为null的时候处理为空字符串
                    val failContent = EnvUtils.parseEnv(
                        failSubscription.content, commandContext.variables, replaceWithEmpty
                    )
                    commandContext.notifyValue["failContent"] = failContent
                    commandContext.notifyValue["emailFailContent"] = failContent
                    val receivers = failSubscription.users.split(",").map {
                        EnvUtils.parseEnv(
                            command = it,
                            data = commandContext.variables,
                            replaceWithEmpty = true
                        )
                    }.toMutableSet()
                    if (!emptyGroup(failSubscription.groups)) {
                        logger.info("fail notify config group: ${failSubscription.groups}")
                        val projectRoleUsers = bsAuthProjectApi.getProjectGroupAndUserList(
                            serviceCode = bsPipelineAuthServiceCode,
                            projectCode = commandContext.projectId)
                        projectRoleUsers.forEach {
                            if (it.roleName in failSubscription.groups) {
                                receivers.addAll(it.userIdList)
                            }
                        }
                    }
                    sendNotify(
                        shutdownType = shutdownType,
                        subscription = failSubscription,
                        receivers = receivers,
                        params = commandContext.notifyValue,
                        setting = setting,
                        buildStatus = buildStatus,
                        variables = commandContext.variables,
                        content = "❌ $failContent"
                    )
                }
            }
            else -> Result<Any>(0)
        }
    }

    private fun sendNotify(
        shutdownType: Int,
        subscription: Subscription,
        receivers: MutableSet<String>,
        params: Map<String, String>,
        setting: PipelineSetting,
        buildStatus: BuildStatus,
        variables: Map<String, String>,
        content: String
    ) {
        sendNotifyByTemplate(
            templateCode = getNotifyTemplateCode(shutdownType, subscription.detailFlag),
            receivers = receivers,
            notifyType = subscription.types.map { it.name }.toMutableSet(),
            titleParams = params,
            bodyParams = params
        )
        logger.info("send weworkGroup msg: ${setting.pipelineId}|$buildStatus")
        val group = EnvUtils.parseEnv(
            command = subscription.wechatGroup,
            data = variables,
            replaceWithEmpty = true
        )
        val groups = group.split("[,;]".toRegex())
        val markDownFlag = subscription.wechatGroupMarkdownFlag
        val detailFlag = subscription.detailFlag
        logger.info("send weworkGroup msg: ${setting.pipelineId}|$groups|$markDownFlag|$content")
        try {
            sendWeworkGroup(groups, markDownFlag, content, params, detailFlag)
        } catch (e: Exception) {
            logger.warn("send weworkGroup msg fail: ${e.message}")
        }
    }

    private fun emptyGroup(groups: Set<String>): Boolean {
        if (groups.isEmpty()) {
            return true
        } else {
            if (groups.size == 1 && groups.first().isEmpty()) {
                return true
            }
        }
        return false
    }

    private fun sendWeworkGroup(
        weworkGroup: List<String>,
        markDownFlag: Boolean,
        content: String,
        vars: Map<String, String>,
        detailFlag: Boolean
    ) {
        val detailUrl = vars["detailUrl"]
        weworkGroup.forEach {
            if (it.startsWith("ww")) { // 应用号逻辑
                sendByApp(it, content, markDownFlag, detailFlag, detailUrl!!)
            } else if (Pattern.matches(chatPatten, it)) { // 机器人逻辑
                sendByRobot(it, content, markDownFlag, detailFlag, detailUrl!!)
            }
        }
    }

    private fun sendByApp(
        chatId: String,
        content: String,
        markDownFlag: Boolean,
        detailFlag: Boolean,
        detailUrl: String
    ) {
        logger.info("send group msg by app: $chatId")
        if (markDownFlag) {
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
                        RichtextViewLink(
                            text = I18nUtil.getCodeLanMessage(
                                messageCode = BK_VIEW_DETAILS
                            ),
                            key = detailUrl, browser = 1
                        )
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
                "$content\n[" + I18nUtil.getCodeLanMessage(
                    messageCode = BK_VIEW_DETAILS
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
                "$content\n\n" + I18nUtil.getCodeLanMessage(
                    messageCode = BK_VIEW_DETAILS
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
