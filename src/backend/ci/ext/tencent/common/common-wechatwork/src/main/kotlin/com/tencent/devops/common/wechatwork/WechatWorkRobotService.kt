package com.tencent.devops.common.wechatwork

import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
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

    companion object {
        val logger = LoggerFactory.getLogger(WechatWorkRobotService::class.java)
    }
}
