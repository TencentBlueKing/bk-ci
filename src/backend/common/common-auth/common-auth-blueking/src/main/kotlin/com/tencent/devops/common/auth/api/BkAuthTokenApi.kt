/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class BkAuthTokenApi constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation
) : AuthTokenApi {
    private val appCode = bkAuthProperties.appCode!!
    private val appSecret = bkAuthProperties.appSecret!!

    override fun refreshAccessToken(serviceCode: AuthServiceCode): String {
        return refreshAccessToken()
    }

    private fun refreshAccessToken(): String {
        val key = "auth_token_$appCode"
        redisOperation.delete(key)
        return getAccessToken()
    }

    /**
     *
     * @param serviceCode serviceCode参数用来兼容，为兼容旧版
     */
    override fun getAccessToken(serviceCode: AuthServiceCode): String {
        return getAccessToken()
    }

    private fun getAccessToken(): String {
        while (true) {
            val key = "auth_token_$appCode"
            val value = redisOperation.get(key)
            if (value != null) return value

            val lockKey = "auth_token_lock_$appCode"
            val redisLock = RedisLock(redisOperation, lockKey, expiredTimeInSeconds)
            redisLock.use {
                if (!redisLock.tryLock()) {
                    logger.error("auth try lock $lockKey fail")
                    Thread.sleep(SleepMills)
                    return@use
                }

                val accessToken = createAccessToken(appCode, appSecret)
                redisOperation.set(key, accessToken, AccessTokenExpiredInSecond)
                redisLock.unlock()
                return accessToken
            }
        }
    }

    // 参数并未用到，为兼容旧版
    private fun createAccessToken(appCode: String?, appSecret: String?): String {
        return createAccessToken()
    }

    private fun createAccessToken(): String {
        val url = "${bkAuthProperties.url}/bkiam/api/v1/auth/access-tokens"
//        logger.info("The url to get access_token is url: ($url) ")
        val accessTokenRequest = mapOf(
            "id_provider" to "client",
            "grant_type" to "client_credentials"
        )

        val content = objectMapper.writeValueAsString(accessTokenRequest)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url)
            .header("X-BK-APP-CODE", this.appCode)
            .header("X-BK-APP-SECRET", this.appSecret)
            .post(requestBody)
            .build()

        var accessToken = ""
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
//            logger.info("Get accessToken response: $responseContent")
            if (!response.isSuccessful) {
                logger.error("Get accessToken response: $responseContent")
                throw RemoteServiceException("Fail to get access_token")
            }

            val responseObject = objectMapper.readValue<Map<String, Any>>(responseContent)
            if (!(responseObject["result"] as Boolean)) {
                logger.error("Fail to create get accessToken. $responseContent")
                throw RemoteServiceException("Fail to create get accessToken")
            }
            val responseData: Map<String, String> = responseObject["data"] as Map<String, String>
            if (responseData["access_token"].isNullOrEmpty()) {
                logger.error("Fail to get accessToken. $responseContent")
                throw RemoteServiceException("Fail to get accessToken")
            }

            accessToken = responseData["access_token"] as String
        }
        return accessToken
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthTokenApi::class.java)
        private const val SleepMills = 100L
        private const val AccessTokenExpiredInSecond: Long = 3600 * 3
        private const val expiredTimeInSeconds: Long = 10
    }
}