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
package com.tencent.devops.common.security.jwt

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.security.pojo.SecurityJwtInfo
import com.tencent.devops.common.security.util.EnvironmentUtil
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import java.net.InetAddress
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

class JwtManager(
    private val privateKeyString: String?,
    private val publicKeyString: String?,
    private val enable: Boolean
) : SchedulingConfigurer {
    private var token: String? = null
    private val publicKey: PublicKey?
    private val privateKey: PrivateKey?
    private val authEnable: Boolean
    private val securityJwtInfo: SecurityJwtInfo?

    //    private val securityJwtInfo: SecurityJwtInfo
    private val tokenCache = CacheBuilder.newBuilder()
        .maximumSize(9999).expireAfterWrite(5, TimeUnit.MINUTES).build<String, Long>()

    /**
     * 获取JWT jwt token
     *
     * @return
     */
    fun getToken(): String? {
        return if (token != null) {
            token
        } else generateToken()
    }

    private fun generateToken(): String? {
        // token 超时10min
        val expireAt = System.currentTimeMillis() + 1000 * 60 * 10
        val json = if (securityJwtInfo == null) "X-DEVOPS-JWT-TOKEN AUTH" else JsonUtil.toJson(securityJwtInfo)
        token = Jwts.builder().setSubject(json).setExpiration(Date(expireAt))
            .signWith(privateKey, SignatureAlgorithm.RS512).compact()
        return token
    }

    /**
     * 验证JWT
     *
     * @param token jwt token
     * @return
     */
    fun verifyJwt(token: String): Boolean {
        val start = System.currentTimeMillis()
        val tokenExpireAt = tokenCache.getIfPresent(token)
        if (tokenExpireAt != null) {
            // 如果未超时
            if (tokenExpireAt > Instant.now().epochSecond) {
                return true
            }
        }
        try {
            val claims = Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(token)
                .body
            logger.info("Verify jwt sub:${claims["sub"]}")
            val expireAt = claims.get("exp", Date::class.java)
            if (expireAt != null) {
                tokenCache.put(token, expireAt.time)
            }
        } catch (e: ExpiredJwtException) {
            logger.warn("Token is expire!", e)
            return false
        } catch (e: Exception) {
            logger.warn("Verify jwt caught exception", e)
            return false
        } finally {
            val cost = System.currentTimeMillis() - start
            if (cost > 100) {
                logger.warn("Verify jwt cost too much, cost:{}", cost)
            }
        }
        return true
    }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        if (isAuthEnable()) {
            taskRegistrar?.addFixedDelayTask(
                this@JwtManager::refreshToken,
                5 * 60 * 1000
            )
        }
    }

    fun refreshToken() {
        logger.info("Refresh service jwt token")
        generateToken()
    }

    fun isAuthEnable(): Boolean {
        // 只有authEnable=true，且privateKeyString、publicKeyString不为空的时候，才会验证
        return authEnable && !privateKeyString.isNullOrBlank() && !publicKeyString.isNullOrBlank()
    }

    fun isSendEnable(): Boolean {
        // 只有authEnable=true，且privateKeyString、publicKeyString不为空的时候，才会验证
        return !privateKeyString.isNullOrBlank() && !publicKeyString.isNullOrBlank()
    }

    init {
        if (privateKeyString.isNullOrBlank() || publicKeyString.isNullOrBlank()) {
            privateKey = null
            publicKey = null
            authEnable = false
        } else {
            val keyFactory = KeyFactory.getInstance("RSA")
            privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64Util.decode(privateKeyString)))
            publicKey = keyFactory.generatePublic(X509EncodedKeySpec(Base64Util.decode(publicKeyString)))
            authEnable = enable
        }
        securityJwtInfo = SecurityJwtInfo(
            ip = InetAddress.getLocalHost().hostAddress,
            applicationName = EnvironmentUtil.getApplicationName(),
            activeProfile = EnvironmentUtil.getActiveProfile(),
            serverPort = EnvironmentUtil.getServerPort()
        )
        logger.info("Init JwtManager successfully!")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JwtManager::class.java)
    }
}
