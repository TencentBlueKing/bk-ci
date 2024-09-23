package com.tencent.devops.remotedev.service.tai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.pojo.tai.Moa2faReqData
import com.tencent.devops.remotedev.pojo.tai.Moa2faRespData
import com.tencent.devops.remotedev.pojo.tai.Moa2faVerifyReqData
import com.tencent.devops.remotedev.pojo.tai.Moa2faVerifyRespData
import com.tencentcloudapi.common.Sign.sha256Hex
import java.util.Date
import java.util.Locale
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class TaiService {
    @Value("\${tai.url:}")
    private val taiUrl: String = ""

    @Value("\${tai.paasid:}")
    private val taiPassid: String = ""

    @Value("\${tai.token:}")
    private val taiToken: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TaiService::class.java)
    }

    // 发起moa 2fa二次验证
    fun createMoa2faRequest(userId: String, moa2faReqData: Moa2faReqData): Moa2faRespData {
        val url = "$taiUrl/ebus/tof4/api/2fa/public/request"
        val body = ObjectMapper().writeValueAsString(moa2faReqData)
        logger.info("User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        return OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info(
                "User $userId create moa 2fa response: " +
                    "|${response.code}|$responseContent"
            )
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    errorMessage = "request api[${request.url.toUrl()}] status code[${response.code}]" +
                        " error message[${response.message}]",
                    errorCode = HTTP_400
                )
            }
            val moa2faRespData: Moa2faRespData = jacksonObjectMapper().readValue(responseContent)
            if (moa2faRespData.ret != 0) {
                throw RemoteServiceException(
                    errorMessage = "request api[${request.url.toUrl()}] status code[${response.code}] " +
                        "error message[${response.message}]",
                    errorCode = HTTP_400
                )
            }
            moa2faRespData
        }
    }

    // 验证结果
    fun verifyMoa2faRequest(userId: String, moa2faVerifyReqData: Moa2faVerifyReqData): Moa2faVerifyRespData {
        val url = "$taiUrl/ebus/tof4/api/2fa/public/verify"
        val body = ObjectMapper().writeValueAsString(moa2faVerifyReqData)
        logger.info("User $userId request url: $url, body: $body")
        val request = Request.Builder()
            .url(url)
            .headers(makeHeaders().toHeaders())
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        return OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info(
                "User $userId verify moa 2fa response: " +
                    "|${response.code}|$responseContent"
            )
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    errorMessage = "request api[${request.url.toUrl()}] status code[${response.code}]" +
                        " error message[${response.message}]",
                    errorCode = HTTP_400
                )
            }
            val moa2faVerifyRespData: Moa2faVerifyRespData = jacksonObjectMapper().readValue(responseContent)
            if (moa2faVerifyRespData.ret != 0 && moa2faVerifyRespData.errCode != 40004) {
                throw RemoteServiceException(
                    errorMessage = "request api[${request.url.toUrl()}] status code[${response.code}]" +
                        " error message[${response.message}]",
                    errorCode = HTTP_400
                )
            }
            moa2faVerifyRespData
        }
    }

    private fun makeHeaders(): Map<String, String> {
        // 生成时间戳，注意服务器的时间与标准时间差不能大于180秒
        val now = Date().time
        val timestamp = (now / 1000).toString()
        // 随机字符串，十分钟内不重复即可
        val nonce = "${now.toString(16)}-${(Math.random() * 0xFFFFFF).toInt().toString(16)}"
        // 计算签名并转换为大写
        val signature = sha256Hex("$timestamp$taiToken$nonce$timestamp").uppercase(Locale.getDefault())

        return mapOf(
            "x-rio-paasid" to taiPassid,
            "x-rio-timestamp" to timestamp,
            "x-rio-signature" to signature,
            "x-rio-nonce" to nonce
        )
    }
}
