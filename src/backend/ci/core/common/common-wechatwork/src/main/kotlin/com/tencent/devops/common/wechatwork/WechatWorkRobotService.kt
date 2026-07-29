package com.tencent.devops.common.wechatwork

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.wechatwork.model.robot.MsgInfo
import com.tencent.devops.common.wechatwork.model.robot.RobotMarkdownSendMsg
import com.tencent.devops.common.wechatwork.model.robot.RobotTextSendMsg
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WechatWorkRobotService @Autowired constructor(
    val robotCustomConfig: WeworkRobotCustomConfig
) {

    fun send(jsonString: String) {
        val url = "${robotCustomConfig.weworkUrl}/cgi-bin/webhook/send?key=${robotCustomConfig.robotKey}"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonString)
        logger.info("sendRobot: $url, body:$jsonString")
        val sendRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("$responseContent")
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    /**
     * 群机器人发消息。
     *
     * [mentionUsers] 会写入 webhook text/markdown 的 mentioned_list，用于 @指定成员（userid）。
     * 后续可扩展（暂未实现）：
     * - @all：mentioned_list 中增加 "@all"（或 mentioned_mobile_list 中增加 "@all"）
     * - content 内 @：按企微文档在 content 中拼接 <@userid>（官方 markdown_v2 不支持该语法）
     */
    fun sendByRobot(
        chatId: String,
        content: String,
        markerDownFlag: Boolean,
        mentionUsers: List<String> = emptyList()
    ) {
        logger.info("send group msg by robot: $chatId, $content")
        // mentioned_list 渲染在正文末尾；正文不以换行结尾时 @ 会粘在同一行，补换行以单独成行
        val finalContent = appendNewlineBeforeMention(content, mentionUsers)
        val msgInfo = MsgInfo(
            content = finalContent,
            mentionedList = mentionUsers.ifEmpty { null }
        )
        val msg: Any = if (markerDownFlag) {
            RobotMarkdownSendMsg(
                chatId = chatId,
                markdown = msgInfo
            )
        } else {
            RobotTextSendMsg(
                chatId = chatId,
                text = msgInfo
            )
        }
        send(JsonUtil.toJson(msg, false))
    }

    private fun appendNewlineBeforeMention(content: String, mentionUsers: List<String>): String {
        if (mentionUsers.isEmpty() || content.endsWith("\n")) {
            return content
        }
        return "$content\n"
    }

    companion object {
        val logger = LoggerFactory.getLogger(WechatWorkRobotService::class.java)
    }
}
