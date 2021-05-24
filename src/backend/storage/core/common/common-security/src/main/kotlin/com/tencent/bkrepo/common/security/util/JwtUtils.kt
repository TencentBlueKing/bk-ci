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

package com.tencent.bkrepo.common.security.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import java.security.Key
import java.time.Duration
import java.util.Date

/**
 * Json web token 工具类
 */
object JwtUtils {

    private const val BIT_LENGTH = 8
    private val SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512
    private val SECRET_KEY_MIN_LENGTH = SIGNATURE_ALGORITHM.minKeyLength / BIT_LENGTH

    fun generateToken(
        signingKey: Key,
        expireDuration: Duration,
        subject: String? = null,
        claims: Map<String, Any>? = null
    ): String {
        val now = Date()
        val expiration = expireDuration.toMillis().takeIf { it > 0 }?.let { Date(now.time + it) }
        return Jwts.builder()
            .setIssuedAt(now)
            .setSubject(subject)
            .setExpiration(expiration)
            .addClaims(claims)
            .signWith(signingKey, SIGNATURE_ALGORITHM)
            .compact()
    }

    @Throws(
        ExpiredJwtException::class,
        UnsupportedJwtException::class,
        MalformedJwtException::class,
        SignatureException::class,
        IllegalArgumentException::class
    )
    fun validateToken(signingKey: Key, token: String): Jws<Claims> {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token)
    }

    fun createSigningKey(secretKey: String): Key {
        return Keys.hmacShaKeyFor(secretKey.padEnd(SECRET_KEY_MIN_LENGTH).toByteArray())
    }
}
