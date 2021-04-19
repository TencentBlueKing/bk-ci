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

package com.tencent.bkrepo.dockerapi.util

import com.tencent.bkrepo.dockerapi.client.BkEsbClient
import com.tencent.bkrepo.dockerapi.pojo.JwtData
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.JCERSAPublicKey
import org.bouncycastle.openssl.PEMReader
import org.bouncycastle.openssl.PasswordFinder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Security

@Component
class JwtUtils @Autowired constructor(
    private val bkEsbClient: BkEsbClient
) {
    var jwtKey: String? = null

    fun parseJwtToken(jwtToken: String): JwtData? {
        val jwtKey = getHarborApiKey()
        Security.addProvider(BouncyCastleProvider())
        val bais = ByteArrayInputStream(jwtKey.toByteArray())
        val reader = PEMReader(InputStreamReader(bais), PasswordFinder { "".toCharArray() })
        return try {
            val keyPair = reader.readObject() as JCERSAPublicKey
            val jwtParser = Jwts.parser().setSigningKey(keyPair)
            val claims = jwtParser.parse(jwtToken).body as Claims
            val user = claims["user"] as Map<*, *>
            val userName = user["username"] as String
            JwtData(userName)
        } catch (ex: Exception) {
            logger.warn("parse jwt token failed", ex)
            null
        }
    }

    fun getHarborApiKey(): String {
        if (!jwtKey.isNullOrBlank()) {
            return jwtKey!!
        }

        try {
            val paasResponse = bkEsbClient.getHarborApiKeyFromApigw()
            if (paasResponse.data == null) {
                throw RuntimeException("get api key from apigw failed")
            }
            jwtKey = paasResponse.data.publicKey
            return jwtKey!!
        } catch (e: Exception) {
            logger.error("get api key from apigw failed", e.message)
            throw RuntimeException("get api key from apigw failed")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JwtUtils::class.java)
    }
}
