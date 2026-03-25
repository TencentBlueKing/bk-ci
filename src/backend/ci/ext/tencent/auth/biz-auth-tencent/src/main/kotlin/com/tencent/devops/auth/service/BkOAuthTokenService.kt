package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.pojo.BkAccessTokenInfo
import com.tencent.devops.auth.pojo.BkOAuthTokenResponse
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.FormBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BkOAuthTokenService(
    private val objectMapper: ObjectMapper
) {

    @Value("\${bkoauth.appCode:}")
    private val appCode: String = ""

    @Value("\${bkoauth.appSecret:}")
    private val appSecret: String = ""

    @Value("\${bkoauth.url:}")
    private val bkOAuthUrl: String = ""

    @Value("\${bkoauth.envName:prod}")
    private val envName: String = ""

    fun getAccessToken(
        userId: String,
        bkTicket: String
    ): BkAccessTokenInfo {
        val url = "$bkOAuthUrl/auth_api/token/"
        val formBody = FormBody.Builder()
            .add("app_code", appCode)
            .add("app_secret", appSecret)
            .add("env_name", envName)
            .add("grant_type", "authorization_code")
            .add("rtx", userId)
            .add("bk_ticket", bkTicket)
            .add("need_new_token", "0")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.warn(
                    "BkOAuthTokenService|getAccessToken|" +
                        "request failed, url:$url, " +
                        "status:${response.code}"
                )
                throw RemoteServiceException(
                    "bkoauth request failed, " +
                        "status=${response.code}"
                )
            }
            val responseStr = response.body!!.string()
            val tokenResponse: BkOAuthTokenResponse =
                objectMapper.readValue(responseStr)
            if (!tokenResponse.result) {
                logger.warn(
                    "BkOAuthTokenService|getAccessToken|" +
                        "bkoauth error, url:$url, " +
                        "msg:${tokenResponse.message}"
                )
                throw RemoteServiceException(
                    "bkoauth error: ${tokenResponse.message}"
                )
            }
            val data = tokenResponse.data
                ?: throw RemoteServiceException(
                    "bkoauth returned empty data " +
                        "for user $userId"
                )
            val nowSeconds = System.currentTimeMillis() / 1000
            return BkAccessTokenInfo(
                accessToken = data.accessToken,
                expiredTime = nowSeconds + data.expiresIn,
                userId = userId
            )
        }
    }

    companion object {
        private val logger =
            LoggerFactory.getLogger(BkOAuthTokenService::class.java)
    }
}
