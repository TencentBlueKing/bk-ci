package com.tencent.devops.common.wechatwork

import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WechatWorkRobotService {

    fun send(seanURL: String, jsonString: String) {
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
        val logger = LoggerFactory.getLogger(WechatWorkRobotService::class.java)
    }
}
