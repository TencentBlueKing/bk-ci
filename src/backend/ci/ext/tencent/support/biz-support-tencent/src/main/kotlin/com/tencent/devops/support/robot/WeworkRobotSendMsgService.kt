package com.tencent.devops.support.robot

import com.tencent.devops.common.api.util.OkhttpUtils
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
    fun sendTextMsgByRobot(msg: String) {
        val url = buildHost("/cgi-bin/webhook/send?key=${robotCustomConfig.robotKey}")
        send(url, msg)
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

    private fun buildHost(uri: String): String {
        val host = robotCustomConfig.weworkUrl
        var newHost = ""
        newHost = if (host!!.endsWith("/")) {
            if (uri.startsWith("/")) {
                host.substring(0, host.length - 1) + uri
            } else {
                host + uri
            }
        } else {
            if (uri.startsWith("/")) {
                host + uri
            } else {
                "$host/$uri"
            }
        }
        return newHost.replace("//", "/")
    }

    companion object {
        val logger = LoggerFactory.getLogger(WeworkRobotSendMsgService::class.java)
    }
}
