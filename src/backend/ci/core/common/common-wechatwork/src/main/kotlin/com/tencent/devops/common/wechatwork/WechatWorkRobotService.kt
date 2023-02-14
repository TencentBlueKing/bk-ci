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

    fun sendByRobot(
        chatId: String,
        content: String,
        markerDownFlag: Boolean
    ) {
        logger.info("send group msg by robot: $chatId, $content")
        val msg: Any = if (markerDownFlag) {
            RobotMarkdownSendMsg(
                chatId = chatId,
                markdown = MsgInfo(
                    content = content
                )
            )
        } else {
            RobotTextSendMsg(
                chatId = chatId,
                text = MsgInfo(
                    content = content
                )
            )
        }
        send(JsonUtil.toJson(msg, false))
    }

    companion object {
        val logger = LoggerFactory.getLogger(WechatWorkRobotService::class.java)
    }
}
