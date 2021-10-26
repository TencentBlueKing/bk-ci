package com.tencent.devops.process.service.notify

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.robot.RobotTextSendMsg
import com.tencent.devops.common.wechatwork.model.robot.TextMsg
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
            content = commandContext.notifyValue["failContent"]!!
            markerDownFlag = setting.failSubscription.wechatGroupMarkdownFlag
            detailFlag = setting.failSubscription.detailFlag
        } else if (buildStatus.isSuccess()) {
            val successSubscription = setting.successSubscription
            if (emptyGroup(successSubscription.wechatGroup) || !successSubscription.wechatGroupFlag) {
                return
            }
            groups.addAll(successSubscription.wechatGroup.split("[,;]".toRegex()))
            content = commandContext.notifyValue["successContent"]!!
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

            if (Pattern.matches(roomPatten, it)) { // 应用号逻辑
                logger.info("send group msg by app: $it")
                if (markerDownFlag) {
                    wechatWorkService.sendMarkdownGroup(content!!, it)
                } else {
                    val receiver = Receiver(ReceiverType.group, it)
                    val richtextContentList = mutableListOf<RichtextContent>()
                    richtextContentList.add(
                        RichtextText(RichtextTextText(content))
                    )
                    if (detailFlag) {
                        richtextContentList.add(
                            RichtextView(
                                RichtextViewLink(text = "查看详情", key = detailUrl!!, browser = 1)
                            )
                        )
                    }
                    val richtextMessage = RichtextMessage(receiver, richtextContentList)
                    wechatWorkService.sendRichText(richtextMessage)
                }
            } else if (Pattern.matches(chatPatten, it)) { // 机器人逻辑
                logger.info("send group msg by robot: $it, $content")
                if (markerDownFlag) {
                    // TODO: 机器人发送markDown
                } else {
                    val msg = RobotTextSendMsg(
                        chatId = it,
                        text = TextMsg(
                            content = content
                        )
                    )
                    wechatWorkRobotService.send(msg.toJsonString())
                }

            }
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
        private val roomPatten = "ww\\w" // ww 开头且接数字的正则表达式, 适用于应用号获取的roomid
        private val chatPatten = "^[A-Za-z0-9]+\$" // 数字和字母组成的群chatId正则表达式
    }
}
