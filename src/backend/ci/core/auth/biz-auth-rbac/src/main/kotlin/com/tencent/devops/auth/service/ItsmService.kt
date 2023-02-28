package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.ItsmCancelApplicationInfo
import com.tencent.devops.auth.pojo.ItsmResponseDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ItsmService @Autowired constructor(
    val objectMapper: ObjectMapper
) {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${itsm.application.cancel.url:#{null}}")
    private val itsmCancelApplicationUrl: String = ""

    @Value("\${itsm.token.verify.url:#{null}}")
    private val itsmVerifyTokenUrl: String = ""

    fun cancelItsmApplication(itsmCancelApplicationInfo: ItsmCancelApplicationInfo): Boolean {
        val itsmResponseDTO = doHttpPost(
            url = itsmCancelApplicationUrl,
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
        val itsmResponseDTO = doHttpPost(
            url = itsmVerifyTokenUrl,
            body = Pair("token", token)
        )
        val itsmApiResData = itsmResponseDTO.data as Map<String, String>
        val isPassed = itsmApiResData["is_passed"].toBoolean()
        if (!isPassed) {
            logger.warn("verify itsm token failed!$token")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_ITSM_APPLICATION_CANCEL_FAIL,
                defaultMessage = "verify itsm token failed!"
            )
        }
    }

    fun doHttpPost(url: String, body: Any): ItsmResponseDTO {
        val header: MutableMap<String, String> = HashMap()
        header["bk_app_code"] = appCode
        header["bk_app_secret"] = appSecret
        val headerStr = objectMapper.writeValueAsString(header).replace("\\s".toRegex(), "")
        val jsonBody = objectMapper.writeValueAsString(body)
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody)
        logger.info("headerStr:$headerStr")
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-bkapi-authorization", headerStr)
            .build()
        return doRequest(url, request)
    }

    fun doRequest(
        url: String,
        request: Request
    ): ItsmResponseDTO {
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("itsm request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("itsm request failed, response:($it)")
            }
            val responseStr = it.body()!!.string()
            val responseDTO = objectMapper.readValue<ItsmResponseDTO>(responseStr)
            if (responseDTO.code != 0L || responseDTO.result == false) {
                // 请求错误
                logger.warn("itsm request failed, url:($url)|response:($it)")
                throw RemoteServiceException("itsm request failed, response:(${responseDTO.message})")
            }
            logger.info("itsm request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItsmService::class.java)
    }
}
