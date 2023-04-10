package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.ItsmCancelApplicationInfo
import com.tencent.devops.auth.pojo.ItsmResponseDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class ItsmService @Autowired constructor(
    val objectMapper: ObjectMapper
) {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${itsm.url:#{null}}")
    private val itsmUrlPrefix: String = ""

    fun cancelItsmApplication(itsmCancelApplicationInfo: ItsmCancelApplicationInfo): Boolean {
        val itsmResponseDTO = executeHttpPost<ItsmResponseDTO>(
            urlSuffix = ITSM_APPLICATION_CANCEL_URL_SUFFIX,
            body = itsmCancelApplicationInfo
        )
        if (itsmResponseDTO.message != "success") {
            logger.warn("cancel itsm application failed!$itsmCancelApplicationInfo")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_ITSM_APPLICATION_CANCEL_FAIL,
                params = arrayOf(itsmCancelApplicationInfo.sn),
                defaultMessage = "cancel itsm application failed!sn(${itsmCancelApplicationInfo.sn})"
            )
        }
        return true
    }

    fun verifyItsmToken(token: String) {
        val param = mapOf("token" to token)
        val itsmResponseDTO = executeHttpPost<ItsmResponseDTO>(ITSM_TOKEN_VERITY_URL_SUFFIX, param)
        val itsmApiResData = itsmResponseDTO.data as Map<*, *>
        logger.info("itsmApiResData:$itsmApiResData")

        if (!itsmApiResData["is_passed"].toString().toBoolean()) {
            logger.warn("verify itsm token failed!$token")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_ITSM_VERIFY_TOKEN_FAIL,
                defaultMessage = "verify itsm token failed!"
            )
        }
    }

    private inline fun <reified T> executeHttpPost(urlSuffix: String, body: Any): T {
        val headerStr = objectMapper.writeValueAsString(mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret))

        val requestBody = objectMapper.writeValueAsString(body)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val url = itsmUrlPrefix + urlSuffix

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-bkapi-authorization", headerStr)
            .build()
        return executeHttpRequest(url, request)
    }

    private inline fun <reified T> executeHttpRequest(url: String, request: Request): T {
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("itsm request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("itsm request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val responseDTO = objectMapper.readValue<ItsmResponseDTO>(responseStr)
            if (responseDTO.code != 0L || !responseDTO.result) {
                // 请求错误
                logger.warn("itsm request failed, url:($url)|response:($it)")
                throw RemoteServiceException("itsm request failed, response:(${responseDTO.message})")
            }
            logger.info("itsm request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO as T
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItsmService::class.java)
        private const val ITSM_APPLICATION_CANCEL_URL_SUFFIX = "/operate_ticket/"
        private const val ITSM_TOKEN_VERITY_URL_SUFFIX = "/token/verify/"
    }
}
