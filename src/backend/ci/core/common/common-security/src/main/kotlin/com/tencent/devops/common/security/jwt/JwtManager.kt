/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.cache.CacheBuilder
import io.jsonwebtoken.Jwts
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory


/**
 * JWT 配置数据类
 *
 * @property properties jwt kid配置列表
 * @property authEnable 是否启用jwt校验
 * @property issuer 签发者（可选）
 * @property audience 受众（可选）
 * @property expirationMinutes 过期时间（分钟）
 * @property notBeforeMinutes 生效时间（分钟）
 * @property validateIssuer 是否验证签发者
 * @property validateAudience 是否验证受众
 * @property validateNotBefore 是否验证生效时间
 */
data class JwtConfig(
    val properties: List<Property> = emptyList(),
    @field:JsonProperty("auth-enable")
    val authEnable: Boolean = false,
    val issuer: String? = null,
    val audience: String? = null,
    @field:JsonProperty("expiration-minutes")
    val expirationMinutes: Long = 10,
    @field:JsonProperty("not-before-minutes")
    val notBeforeMinutes: Long = 0,
    @field:JsonProperty("validate-issuer")
    val validateIssuer: Boolean = false,
    @field:JsonProperty("validate-audience")
    val validateAudience: Boolean = false,
    @field:JsonProperty("validate-not-before")
    val validateNotBefore: Boolean = false
) {
    data class Property(
        val kid: String,
        @field:JsonProperty("public-key")
        val publicKey: String,
        @field:JsonProperty("private-key")
        val privateKey: String,
        val active: Boolean
    )
}

/**
 * JWT生成请求数据类
 *
 * @property subject JWT主题
 * @property claims 自定义Claims
 * @property kid 指定使用的密钥ID（可选）
 * @property expirationMinutes 自定义过期时间（可选）
 */
data class JwtGenerationRequest(
    val subject: String,
    val claims: Map<String, Any> = emptyMap(),
    val kid: String? = null,
    val expirationMinutes: Long? = null
)

class JwtManager(
    private val jwtConfig: JwtConfig
) {
    private var token: String? = null
    private val keyConfigManager: JwtKeyConfigManager = JwtKeyConfigManager()
    private val jwtClaimsValidator: JwtClaimsValidator = JwtClaimsValidator(keyConfigManager)
    private val validationRules = buildValidationRules()

    //    private val securityJwtInfo: SecurityJwtInfo
    private val tokenCache = CacheBuilder.newBuilder()
        .maximumSize(9999).expireAfterWrite(5, TimeUnit.MINUTES).build<String, Long>()

    init {
        jwtConfig.properties.forEach {
            addKeyConfig(it.kid, it.publicKey, it.privateKey, it.active)
        }
    }

    /**
     * 添加新的密钥配置
     *
     * @param kid 新的密钥配置
     * @return 添加是否成功
     */
    fun addKeyConfig(kid: String, privateKey: String?, publicKey: String, active: Boolean): Boolean {
        return keyConfigManager.addKeyConfig(kid, privateKey, publicKey, active)
    }

    /**
     * 获取JWT jwt token
     *
     * @return
     */
    fun getToken(): String? {
        return token
    }

    fun generateToken(subject: String): String? {
        val request = JwtGenerationRequest(
            subject = subject,
            claims = emptyMap(),
            kid = null,
            expirationMinutes = jwtConfig.expirationMinutes
        )
        token = generateEnhancedToken(request)
        return token
    }

    /**
     * 验证JWT
     *
     * @param token jwt token
     * @return
     */
    fun verifyJwt(token: String): Boolean {
        // 使用增强的JWT验证逻辑，保持向后兼容
        val result = verifyEnhancedJwt(token)
        return result.isValid
    }

    /**
     * 生成增强的JWT Token
     *
     * 支持kid和强制算法alg=RS512和kid
     *
     * @param request JWT生成请求，包含主题、Claims和密钥配置
     * @return 生成的JWT Token字符串
     */
    fun generateEnhancedToken(request: JwtGenerationRequest): String {
        try {
            // 获取密钥配置
            val keyConfig = if (request.kid != null) {
                keyConfigManager.getKeyConfigByKid(request.kid)
            } else {
                keyConfigManager.getActiveKeyConfig()
            } ?: throw IllegalArgumentException("Key config not found for kid: ${request.kid ?: "default"}")

            // 构建JWT Claims
            val currentTime = System.currentTimeMillis()
            val expirationTime = currentTime + (request.expirationMinutes ?: jwtConfig.expirationMinutes) * 60 * 1000
            val notBeforeTime = currentTime + jwtConfig.notBeforeMinutes * 60 * 1000

            val jwtBuilder = Jwts.builder()
                .subject(request.subject)
                .issuedAt(Date(currentTime))
                .expiration(Date(expirationTime))

            // 设置可选Claims
            if (jwtConfig.issuer != null) {
                jwtBuilder.issuer(jwtConfig.issuer)
            }
            if (jwtConfig.audience != null) {
                jwtBuilder.audience().add(jwtConfig.audience)
            }
            if (jwtConfig.notBeforeMinutes > 0) {
                jwtBuilder.notBefore(Date(notBeforeTime))
            }

            // 添加自定义Claims
            request.claims.forEach { (key, value) ->
                jwtBuilder.claim(key, value)
            }

            // 设置Header
            jwtBuilder.header()
                .add(JwtSecurityConstants.JwtHeaders.KEY_ID, keyConfig.kid)

            return jwtBuilder.signWith(keyConfig.privateKey, Jwts.SIG.RS512).compact()
        } catch (e: Exception) {
            logger.error("Failed to generate enhanced JWT token", e)
            throw RuntimeException("JWT token generation failed", e)
        }
    }

    /**
     * 验证增强的JWT Token
     *
     * 支持kid和算法验证，使用JwtClaimsValidator实现增强JWT验证逻辑
     *
     * @param token JWT Token字符串
     * @return 验证结果，包含验证状态和错误信息
     */
    fun verifyEnhancedJwt(token: String): JwtValidationResult {
        val start = System.currentTimeMillis()

        try {
            // 检查缓存
            val tokenExpireAt = tokenCache.getIfPresent(token)
            if (tokenExpireAt != null && tokenExpireAt > Instant.now().toEpochMilli()) {
                return JwtValidationResult(
                    isValid = true,
                    claims = emptyMap()
                )
            }

            // 使用JwtClaimsValidator验证JWT Token
            val result = jwtClaimsValidator.verifyJwtTokenWithKeyConfig(
                token = token,
                validationRules = validationRules
            )

            // 缓存验证结果
            if (result.isValid) {
                val expClaim = result.claims[JwtSecurityConstants.JwtClaims.EXPIRATION]
                if (expClaim is Number) {
                    val expireAt = expClaim.toLong() * 1000 // 转换为毫秒
                    tokenCache.put(token, expireAt)
                }
            }

            return result

        } catch (e: Exception) {
            logger.warn("JWT verification failed", e)
            return JwtValidationResult(
                isValid = false,
                errorCode = JwtSecurityConstants.JwtErrorCodes.INVALID_SIGNATURE,
                errorMessage = "JWT verification failed: ${e.message}"
            )
        } finally {
            val cost = System.currentTimeMillis() - start
            if (cost > 100) {
                logger.warn("JWT verification cost too much time: {}ms", cost)
            }
        }
    }

    /**
     * 构建验证规则
     */
    private fun buildValidationRules(): List<ClaimsValidationRule> {
        val rules = mutableListOf<ClaimsValidationRule>()
        // 强制验证过期时间
        rules.add(
            ClaimsValidationRule(
                claimName = JwtSecurityConstants.JwtClaims.EXPIRATION,
                required = true,
                validator = { exp ->
                    val currentTime: Long = System.currentTimeMillis()
                    if (exp == null) {
                        logger.warn("JWT Claims missing exp field")
                        return@ClaimsValidationRule false
                    }

                    val expTime = when (exp) {
                        is Number -> exp.toLong() * 1000 // JWT exp通常是秒级时间戳
                        is String -> exp.toLongOrNull()?.times(1000) ?: return@ClaimsValidationRule false
                        else -> {
                            logger.warn("JWT exp field has invalid type: ${exp::class.java}")
                            return@ClaimsValidationRule false
                        }
                    }

                    val isValid = expTime > currentTime
                    if (!isValid) {
                        logger.warn("JWT token expired. exp: $expTime, current: $currentTime")
                    }
                    return@ClaimsValidationRule isValid
                }
            )
        )

        // 添加签发者验证规则
        if (jwtConfig.validateIssuer && jwtConfig.issuer != null) {
            rules.add(
                ClaimsValidationRule(
                    claimName = JwtSecurityConstants.JwtClaims.ISSUER,
                    required = true,
                    expectedValue = jwtConfig.issuer
                )
            )
        }

        // 添加受众验证规则
        if (jwtConfig.validateAudience && jwtConfig.audience != null) {
            rules.add(
                ClaimsValidationRule(
                    claimName = JwtSecurityConstants.JwtClaims.AUDIENCE,
                    required = true,
                    expectedValue = jwtConfig.audience
                )
            )
        }

        // 添加受众验证规则
        if (jwtConfig.validateNotBefore && jwtConfig.notBeforeMinutes != 0L) {
            rules.add(
                ClaimsValidationRule(
                    claimName = JwtSecurityConstants.JwtClaims.NOT_BEFORE,
                    required = true,
                    validator = { nbf ->
                        val currentTime: Long = System.currentTimeMillis()
                        if (nbf == null) {
                            // nbf是可选字段，不存在时认为验证通过
                            return@ClaimsValidationRule true
                        }

                        val nbfTime = when (nbf) {
                            is Number -> nbf.toLong() * 1000 // JWT nbf通常是秒级时间戳
                            is String -> nbf.toLongOrNull()?.times(1000) ?: return@ClaimsValidationRule false
                            else -> {
                                logger.warn("JWT nbf field has invalid type: ${nbf::class.java}")
                                return@ClaimsValidationRule false
                            }
                        }

                        val isValid = nbfTime <= currentTime
                        if (!isValid) {
                            logger.warn("JWT token not yet valid. nbf: $nbfTime, current: $currentTime")
                        }
                        return@ClaimsValidationRule isValid
                    }
                )
            )
        }

        return rules
    }


    fun isAuthEnable(): Boolean {
        // 只有authEnable=true，且privateKeyString、publicKeyString不为空的时候，才会验证
        return jwtConfig.authEnable && keyConfigManager.getActiveKeyConfig() != null
    }

    fun isSendEnable(): Boolean {
        // 只有authEnable=true，且privateKeyString、publicKeyString不为空的时候，才会验证
        return keyConfigManager.getActiveKeyConfig() != null
    }

    init {
        logger.info("Init JwtManager successfully!")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JwtManager::class.java)
    }
}
