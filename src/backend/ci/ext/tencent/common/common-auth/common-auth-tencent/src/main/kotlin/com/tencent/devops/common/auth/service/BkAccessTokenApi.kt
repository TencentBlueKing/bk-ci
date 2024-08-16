package com.tencent.devops.common.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.api.pojo.BkAuthTokenCreate
import com.tencent.devops.common.auth.api.pojo.BkAuthTokenCreateRequest
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSAuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

/**
 * 获取蓝鲸accessToken服务类
 */
class BkAccessTokenApi @Autowired constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation
) {

    fun getPipelineAccessToken(): String {
        return getAccessToken(BSPipelineAuthServiceCode())
    }

    fun getProjectAccessToken(): String {
        return getAccessToken(BSProjectServiceCodec())
    }

    fun getAccessToken(serviceCode: AuthServiceCode): String {
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
            idProvider = v0IdProvider,
            grantType = bkAuthProperties.grantType!!
        )
        val content = objectMapper.writeValueAsString(bkAuthTokenRequest)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        logger.info("[$appCode|$appSecret]|createAccessToken|url($url) and body($bkAuthTokenRequest)")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
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

    fun getAppCodeAndSecret(serviceCode: AuthServiceCode): Pair<String, String> {
        return serviceCode.id() to secretMap[serviceCode.id()]!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAccessTokenApi::class.java)
        private const val SleepMills = 100L
        private const val AccessTokenExpiredInSecond: Long = 3600 * 3
        private const val expiredTimeInSeconds: Long = 10
        private const val v0IdProvider = "client"
    }
}
