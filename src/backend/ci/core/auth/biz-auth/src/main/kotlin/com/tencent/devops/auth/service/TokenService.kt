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

package com.tencent.devops.auth.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey
import kotlin.collections.HashMap

@Service
@Suppress("UNUSED")
class TokenService @Autowired constructor(
    val dslContext: DSLContext
) {

    @Value("\${auth.token.expirationTime:#{null}}")
    private val expirationTime: Int? = null

    @Value("\${auth.token.secret:#{null}}")
    private val secret: String? = null

    fun verifyJWT(token: String): Boolean {
        return isValidToken(token)
    }

    fun verifyJWT(userId: String, token: String): Boolean {
        return isValidToken(token = token, userId = userId)
    }

    fun generateUserToken(userDetails: String): String {
        return generateToken(userDetails)
    }

    /**
     * 用于生成 JWT 令牌的加密密钥
     */
    private fun generalKeyByDecoders(): SecretKey {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret!!))
    }

    /**
     * 为给定的 UserDetails 和有效期生成 JWT 令牌
     */
    private fun generateToken(userDetails: String): String {
        val claims = HashMap<String, Any>()
        return createToken(claims, userDetails)
    }

    /**
     * 创建具有给定声明、主题和有效期的令牌
     */
    private fun createToken(claims: Map<String, Any>, subject: String): String {
        //5 hours validity
        val expirationDate = Date(System.currentTimeMillis() + (expirationTime ?: 14400000))

        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(expirationDate)
            .signWith(generalKeyByDecoders(), SignatureAlgorithm.HS512).compact()
    }

    /**
     * 提取给定 JWT 令牌的所有声明
     */
    private fun extractAllClaims(token: String): Claims = Jwts.parser().setSigningKey(generalKeyByDecoders())
        .parseClaimsJws(token).body

    /**
     * 从给定的令牌和解析器函数中提取单个声明
     */
    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    /**
     * 返回令牌是否有效
     */
    private fun isValidToken(token: String, userId: String = ""): Boolean {
        return try {
            val result = extractExpiration(token).before(Date())
            if (userId.isEmpty()) {
                !result
            }
            extractUsername(token) == userId
        } catch (e: ExpiredJwtException) {
            false
        }
    }

    /**
     * 提取给定 JWT 令牌的到期日期
     */
    private fun extractExpiration(token: String): Date = extractClaim(token, Claims::getExpiration)

    /**
     * 提取给定 JWT 令牌的用户名
     */
    private fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    private fun getTokenFromAuthHeader(authHeader: String): String? {
        return if (authHeader.startsWith("Bearer")) {
            authHeader.substring(7)
        } else {
            null
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TokenService::class.java)
    }
}
