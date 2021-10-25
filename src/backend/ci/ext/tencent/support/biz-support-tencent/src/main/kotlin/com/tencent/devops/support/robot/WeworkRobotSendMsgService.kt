package com.tencent.devops.support.robot

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.support.robot.pojo.RobotTextSendMsg
import com.tencent.devops.support.robot.pojo.TextMsg
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WeworkRobotSendMsgService @Autowired constructor(
    val robotCustomConfig: WeworkRobotCustomConfig
) {
    fun sendTextMsgByRobot(chatId: String, context: String) {
        val url = "${robotCustomConfig.weworkUrl}/cgi-bin/webhook/send?key=${robotCustomConfig.robotKey}"
        val msg = RobotTextSendMsg(
            chatId = chatId,
            text = TextMsg(
                context = context
            )
        )
        send(url, msg.toJsonString())
    }

    private fun send(seanURL: String, jsonString: String) {
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)
        logger.info("sendRobot: $seanURL, body:$jsonString")
        val sendRequest = Request.Builder()
            .url(seanURL)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(sendRequest).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("$responseContent")
                throw RuntimeException("Fail to send msg to yqwx. $responseContent")
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(WeworkRobotSendMsgService::class.java)
    }
}
