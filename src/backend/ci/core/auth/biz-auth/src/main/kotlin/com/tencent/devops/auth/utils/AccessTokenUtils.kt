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

package com.tencent.devops.auth.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey
import kotlin.collections.HashMap

object AccessTokenUtils {

    /**
     * 返回令牌是否有效
     */
    fun isValidToken(token: String, secret: String?): Boolean {
        return try {
            !extractExpiration(token, secret).before(Date())
        } catch (e: ExpiredJwtException) {
            false
        }
    }

    /**
     * 为给定的 UserDetails 和有效期生成 JWT 令牌
     */
    fun generateToken(userDetails: String, expirationTime: Int?, secret: String?): String {
        val claims = HashMap<String, Any>()
        return createToken(claims, userDetails, expirationTime, secret)
    }

    /**
     * 用于生成 JWT 令牌的加密密钥
     */
    private fun generalKeyByDecoders(secret: String?): SecretKey {
        return Keys.hmacShaKeyFor(secret!!.toByteArray())
    }

    /**
     * 创建具有给定声明、主题和有效期的令牌
     */
    private fun createToken(
        claims: Map<String, Any>,
        subject: String,
        expirationTime: Int?,
        secret: String?
    ): String {
        //5 hours validity
        val expirationDate = Date(System.currentTimeMillis() + (expirationTime ?: 14400000))

        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(expirationDate)
            .signWith(generalKeyByDecoders(secret), SignatureAlgorithm.HS512).compact()
    }

    /**
     * 提取给定 JWT 令牌的所有声明
     */
    private fun extractAllClaims(
        token: String,
        secret: String?
    ): Claims = Jwts.parser().setSigningKey(generalKeyByDecoders(secret))
        .parseClaimsJws(token).body

    /**
     * 从给定的令牌和解析器函数中提取单个声明
     */
    private fun <T> extractClaim(token: String, secret: String?, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token, secret)
        return claimsResolver(claims)
    }

    /**
     * 提取给定 JWT 令牌的到期日期
     */
    private fun extractExpiration(
        token: String,
        secret: String?
    ): Date = extractClaim(token, secret, Claims::getExpiration)
}
