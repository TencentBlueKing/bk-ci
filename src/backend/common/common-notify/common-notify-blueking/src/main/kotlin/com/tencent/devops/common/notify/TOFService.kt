package com.tencent.devops.common.notify

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Random

@Service
class TOFService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val CONTENT_TYPE = "application/json; charset=utf-8"
        private val logger = LoggerFactory.getLogger(TOFService::class.java)

        val EMAIL_URL = "/api/v1/Message/SendMailInfo"
        val RTX_URL = "/api/v1/Message/SendRTXInfo"
        val SMS_URL = "/api/v1/Message/SendSMSInfo"
        val WECHAT_URL = "/api/v1/Message/SendWeiXinInfo"
    }

    private val okHttpClient = OkHttpClient()
    private val random = Random()

    fun post(url: String, postData: Any, tofConf: Map<String, String>): TOFResult {

        val body: String
        try {
            body = objectMapper.writeValueAsString(postData)
        } catch (e: JsonProcessingException) {
            logger.error(String.format("TOF error, post tof data cannot serialize, url: %s", url), e)
            return TOFResult("TOF error, post tof data cannot serialize")
        }

        val requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE), body)
        val headers = generateHeaders(tofConf["sys-id"] ?: "", tofConf["app-key"] ?: "")
        if (headers == null) {
            logger.error(String.format("TOF error, generate signature failure, url: %s", url))
            return TOFResult("TOF error, generate signature failure")
        }
        logger.info("[$url] Start to request tof with body size: ${body.length}")
        val request = Request.Builder()
                .url(String.format("%s%s", tofConf["host"], url))
                .post(requestBody)
                .headers(headers)
                .build()
        var responseBody = ""
        try {
            okHttpClient.newCall(request).execute().use { response ->

                responseBody = response.body()!!.string()
                if (!response.isSuccessful) {
                    // logger.error("[id--${headers["timestamp"]}]request >>>> $body")
                    logger.error(String.format("TOF error, post data response failure, url: %s, status code: %d, errorMsg: %s", url, response.code(), responseBody))
                    return TOFResult("TOF error, post data response failure")
                }
            }
            val result = objectMapper.readValue(responseBody, TOFResult::class.java)
            if (result.Ret != 0 || result.ErrCode != 0) {
                logger.error("[id--${headers["timestamp"]}]request >>>> $body")
                logger.error("[id--${headers["timestamp"]}]response >>>>$responseBody")
            }
            return result
        } catch (e: Throwable) {
            logger.error(String.format("TOF error, server response serialize failure, url: %s, response: %s", url, responseBody), e)
            return TOFResult("TOF error, server response serialize failure")
        }
    }

    /**
     * 生成TOF请求头
     * @return TOF请求头对象
     */
    private fun generateHeaders(tofSysId: String, tofAppKey: String): Headers? {
        // 当前时间戳
        val t = System.currentTimeMillis()
        // 不要毫秒
        val timestamp = t.toString().substring(0, 10)
        val randomNumber = this.random.nextInt()
        val data = String.format("random%dtimestamp%s", randomNumber, timestamp)
        val signatureBytes: ByteArray
        try {
            signatureBytes = DesUtil.encrypt(data, tofSysId)
        } catch (e: Exception) {
            return null
        }

        val signature = DesUtil.toHexString(signatureBytes).toUpperCase()
        val headerMap = HashMap<String, String?>()
        headerMap.apply {
            put("appkey", tofAppKey)
            put("timestamp", timestamp)
            put("random", randomNumber.toString())
            put("signature", signature)
        }
        return Headers.of(headerMap)
    }
}