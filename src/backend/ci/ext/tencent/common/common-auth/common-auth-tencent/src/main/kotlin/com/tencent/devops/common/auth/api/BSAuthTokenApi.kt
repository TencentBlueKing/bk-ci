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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
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
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.api.pojo.BkAuthTokenCreate
import com.tencent.devops.common.auth.api.pojo.BkAuthTokenCreateRequest
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSAuthServiceCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

class BSAuthTokenApi @Autowired constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation
) : AuthTokenApi {

    override fun refreshAccessToken(serviceCode: AuthServiceCode): String {
        val appCodeAndSecret = getAppCodeAndSecret(serviceCode)
        val appCode = appCodeAndSecret.first

        val key = "auth_token_$appCode"
        redisOperation.delete(key)
        return getAccessToken(serviceCode)
    }

    override fun getAccessToken(serviceCode: AuthServiceCode): String {
        val appCodeAndSecret = getAppCodeAndSecret(serviceCode)
        val appCode = appCodeAndSecret.first
        val appSecret = appCodeAndSecret.second

        while (true) {
            val key = "auth_token_$appCode"
            val value = redisOperation.get(key)
            if (value != null) return value

            logger.info("[$serviceCode|$appCode|$appSecret] Try to refresh the access token")
            val lockKey = "auth_token_lock_$appCode"
            val redisLock = RedisLock(redisOperation, lockKey, expiredTimeInSeconds)

            redisLock.use {
                if (!redisLock.tryLock()) {
                    logger.info("auth try lock $lockKey fail")
                    Thread.sleep(SleepMills)
                    return@use
                }

                val bkAuthTokenCreate = createAccessToken(appCode, appSecret)
                logger.info("[$serviceCode|$appCode|$appSecret|${bkAuthTokenCreate.accessToken}] Set the access token")
                redisOperation.set(key, bkAuthTokenCreate.accessToken, AccessTokenExpiredInSecond)
                redisLock.unlock()
                return bkAuthTokenCreate.accessToken
            }
        }
    }

    private fun createAccessToken(appCode: String, appSecret: String): BkAuthTokenCreate {
        val url = "${bkAuthProperties.url}/oauth/token"
        val bkAuthTokenRequest = BkAuthTokenCreateRequest(
            envName = bkAuthProperties.envName!!,
            appCode = appCode,
            appSecret = appSecret,
            idProvider = bkAuthProperties.idProvider!!,
            grantType = bkAuthProperties.grantType!!
        )
        val content = objectMapper.writeValueAsString(bkAuthTokenRequest)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        logger.info("[$appCode|$appSecret]|createAccessToken|url($url) and body($bkAuthTokenRequest)")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to create access token. $responseContent")
                throw RemoteServiceException("Fail to create access token")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<BkAuthTokenCreate>>(responseContent)
            if (responseObject.data == null) {
                logger.error("Fail to create access token. $responseContent")
                throw RemoteServiceException("Fail to create access token")
            }
            return responseObject.data
        }
    }

    val secretMap = mutableMapOf<String, String?>()

    @PostConstruct
    fun init() {
        secretMap[BSAuthServiceCode.BCS.value] = bkAuthProperties.bcsSecret
        secretMap[BSAuthServiceCode.CODE.value] = bkAuthProperties.codeSecret
        secretMap[BSAuthServiceCode.PIPELINE.value] = bkAuthProperties.pipelineSecret
        secretMap[BSAuthServiceCode.ARTIFACTORY.value] = bkAuthProperties.artifactorySecret
        secretMap[BSAuthServiceCode.TICKET.value] = bkAuthProperties.ticketSecret
        secretMap[BSAuthServiceCode.ENVIRONMENT.value] = bkAuthProperties.environmentSecret
        secretMap[BSAuthServiceCode.EXPERIENCE.value] = bkAuthProperties.experienceSecret
        secretMap[BSAuthServiceCode.VS.value] = bkAuthProperties.vsSecret
        secretMap[BSAuthServiceCode.QUALITY.value] = bkAuthProperties.qualitySecret
        secretMap[BSAuthServiceCode.WETEST.value] = bkAuthProperties.wetestSecret
        secretMap[BSAuthServiceCode.PROJECT.value] = bkAuthProperties.pipelineSecret
        secretMap[BSAuthServiceCode.COMMON.value] = bkAuthProperties.authSecret

        secretMap.forEach { (key, value) ->
            logger.info("[secretMap] key: $key , value: $value")
        }
        logger.info("auth.url: ${bkAuthProperties.url}")
    }

    override fun checkToken(token: String): Boolean {
        return true
    }

    private fun getAppCodeAndSecret(serviceCode: AuthServiceCode): Pair<String, String> {
        return serviceCode.id() to secretMap[serviceCode.id()]!!
//        val secret = when (serviceCode as BkAuthServiceCode) {
//            BkAuthServiceCode.BCS -> bkAuthProperties.bcsSecret
//            BkAuthServiceCode.CODE -> bkAuthProperties.codeSecret
//            BkAuthServiceCode.PIPELINE -> bkAuthProperties.pipelineSecret
//            BkAuthServiceCode.ARTIFACTORY -> bkAuthProperties.artifactorySecret
//            BkAuthServiceCode.TICKET -> bkAuthProperties.ticketSecret
//            BkAuthServiceCode.ENVIRONMENT -> bkAuthProperties.environmentSecret
//            BkAuthServiceCode.EXPERIENCE -> bkAuthProperties.experienceSecret
//            BkAuthServiceCode.VS -> bkAuthProperties.vsSecret
//            BkAuthServiceCode.QUALITY -> bkAuthProperties.qualitySecret
//            BkAuthServiceCode.WETEST -> bkAuthProperties.wetestSecret
//        }
//        return Pair(serviceCode.value, secret!!)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BSAuthTokenApi::class.java)
        private const val SleepMills = 100L
        private const val AccessTokenExpiredInSecond: Long = 3600 * 3
        private const val expiredTimeInSeconds: Long = 10
    }
}
