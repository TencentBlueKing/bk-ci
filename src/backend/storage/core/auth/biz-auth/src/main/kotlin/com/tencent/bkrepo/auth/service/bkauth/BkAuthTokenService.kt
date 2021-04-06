/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.bkauth

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.bkrepo.auth.config.BkAuthConfig
import com.tencent.bkrepo.auth.model.TBkAuthToken
import com.tencent.bkrepo.auth.pojo.BkAuthResponse
import com.tencent.bkrepo.auth.pojo.BkAuthToken
import com.tencent.bkrepo.auth.pojo.BkAuthTokenRequest
import com.tencent.bkrepo.auth.pojo.enums.BkAuthServiceCode
import com.tencent.bkrepo.auth.repository.BkAuthRepository
import com.tencent.bkrepo.auth.util.HttpUtils
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.common.api.util.toJsonString
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class BkAuthTokenService @Autowired constructor(
    private val bkAuthConfig: BkAuthConfig,
    private val lockProvider: LockProvider,
    private val bkAuthRepository: BkAuthRepository,
    private val mongoTemplate: MongoTemplate
) {
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(5L, TimeUnit.SECONDS)
        .writeTimeout(5L, TimeUnit.SECONDS)
        .build()

    private val accessTokenCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(LOCAL_CACHE_TIME, TimeUnit.SECONDS)
        .build<String, String>()

    private fun getValidTokenInDB(serviceCode: BkAuthServiceCode, invalidToken: String? = null): String? {
        logger.debug("getValidTokenInDB, invalidToken: $invalidToken")
        val authTokenInDB = bkAuthRepository.findOneByTokenKey(getTokenKey(serviceCode))
        if (authTokenInDB != null && authTokenInDB.expiredAt.isAfter(LocalDateTime.now())) {
            if (invalidToken == null || invalidToken != authTokenInDB.token) {
                logger.debug("valid token in db: ${authTokenInDB.token}")
                return authTokenInDB.token
            }
        }
        return null
    }

    private fun saveTokenToDB(tokenKey: String, token: String) {
        logger.debug("saveTokenToDB, tokenKey: $tokenKey, tokenKey: $token")
        val now = LocalDateTime.now()
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TBkAuthToken::tokenKey.name).`is`(tokenKey))
        update.set(TBkAuthToken::token.name, token)
        update.set(TBkAuthToken::createdAt.name, now)
        update.set(TBkAuthToken::expiredAt.name, now.plusSeconds(DB_CACHE__TIME))
        mongoTemplate.upsert(query, update, TBkAuthToken::class.java)
    }

    fun getAccessToken(serviceCode: BkAuthServiceCode, invalidToken: String? = null): String {
        val tokenKey = getTokenKey(serviceCode)
        val cachedToken = accessTokenCache.getIfPresent(tokenKey)
        if (cachedToken != null) {
            if (cachedToken == invalidToken) {
                logger.debug("token[$cachedToken] invalid in local cache")
                accessTokenCache.invalidate(serviceCode.value)
            } else {
                return cachedToken
            }
        }

        var authTokenInDB = getValidTokenInDB(serviceCode, invalidToken)
        if (authTokenInDB != null) {
            accessTokenCache.put(serviceCode.value, authTokenInDB)
            return authTokenInDB
        }
        val lockConfig = LockConfiguration(tokenKey, Duration.ofSeconds(5), Duration.ZERO)
        var lock = lockProvider.lock(lockConfig)
        if (!lock.isPresent) {
            Thread.sleep(5000)
            authTokenInDB = getValidTokenInDB(serviceCode, invalidToken)
            if (authTokenInDB != null) {
                accessTokenCache.put(serviceCode.value, authTokenInDB)
                return authTokenInDB
            }
            lock = lockProvider.lock(lockConfig)
            if (!lock.isPresent) {
                throw RuntimeException("get lock($tokenKey) failed")
            }
        }

        try {
            lockAssertStartLock(tokenKey)
            logger.debug("get lock($tokenKey) success")
            val token = createAccessToken(serviceCode.value, bkAuthConfig.getAppSecret(serviceCode)).accessToken
            saveTokenToDB(tokenKey, token)
            accessTokenCache.put(serviceCode.value, token)
            return token
        } finally {
            lockAssertEndLock()
            lock.get().unlock()
        }
    }

    private fun lockAssertStartLock(name: String) {
        // static void startLock(String name)
        val method = LockAssert::class.java.getDeclaredMethod("startLock", String::class.java)
        method.isAccessible = true
        method.invoke(null, name)
    }

    private fun lockAssertEndLock() {
        // static void endLock()
        val method = LockAssert::class.java.getDeclaredMethod("endLock")
        method.isAccessible = true
        method.invoke(null)
    }

    private fun createAccessToken(appCode: String, appSecret: String): BkAuthToken {
        val url = "${bkAuthConfig.getBkAuthServer()}/oauth/token"
        val bkAuthTokenRequest = BkAuthTokenRequest(bkAuthConfig.authEnvName, appCode, appSecret, ID_PROVIDER,
            GRANT_TYPE)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, bkAuthTokenRequest.toJsonString())
        val request = Request.Builder().url(url).post(requestBody).build()
        val apiResponse = HttpUtils.doRequest(okHttpClient, request, 2)
        val responseObject = objectMapper.readValue<BkAuthResponse<BkAuthToken>>(apiResponse.content)
        if (responseObject.data == null) {
            logger.error("create access token failed, requestUrl: $url, requestData: $bkAuthTokenRequest," +
                " response: $apiResponse")
            throw RuntimeException("create access token failed, data null")
        }
        return responseObject.data
    }

    private fun getTokenKey(serviceCode: BkAuthServiceCode): String = "$TOKEN_KEY_PREFIX${serviceCode.value}"

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthTokenService::class.java)

        private const val ID_PROVIDER = "client"
        private const val GRANT_TYPE = "client_credentials"

        private const val TOKEN_KEY_PREFIX = "bk_auth_token_"
        private const val LOCAL_CACHE_TIME = 210L
        private const val DB_CACHE__TIME = 600L
    }
}
