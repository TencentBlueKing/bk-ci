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

package com.tencent.devops.remotedev.service.expert

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 瘦终端一键求助离线 JWT Token 生成器。
 *
 * 与易事厅约定：蓝盾用双方登记的 appId + jwtSecret 生成带签名的 JWT，
 * 客户端携带 token 跳转易事厅鉴权页，重放校验由易事厅侧完成。
 */
@Component
class ExpertSupportTokenGenerator {

    @Value("\${expertsupport.appId:anydev}")
    private lateinit var appId: String

    @Value("\${expertsupport.jwtSecret}")
    private lateinit var jwtSecret: String

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    /**
     * 生成一键求助 JWT。
     *
     * @param userId 用户ID，离岸账号形如 anydev_odc@tai
     * @param recordId 一键求助入库记录id
     */
    fun generate(userId: String, recordId: Long): String {
        val now = Date()
        val expiration = Date(now.time + TimeUnit.MINUTES.toMillis(TOKEN_EXPIRATION_MINUTES))
        val (sub, user, type) = resolveUserClaims(userId)

        val token = Jwts.builder()
            .claim(CLAIM_APP_ID, appId)
            .subject(sub)
            .claim(CLAIM_USER, user)
            .claim(CLAIM_TYPE, type)
            .issuedAt(now)
            .expiration(expiration)
            .claim(CLAIM_RECORD_ID, recordId.toString())
            .signWith(signingKey)
            .compact()

        logger.info("generate expert support token|userId=$userId|recordId=$recordId|type=$type")
        return token
    }

    /**
     * 按 @tai 约定推导 sub/user/type：
     * - 离岸账号(含 @tai)：type=tai, user=@tai 前缀, sub=完整 userId
     * - 内网账号：type=oa, sub=user=userId
     */
    private fun resolveUserClaims(userId: String): Triple<String, String, String> {
        return if (userId.contains(TAI_SUFFIX)) {
            Triple(userId, userId.substringBefore(TAI_SUFFIX), TYPE_TAI)
        } else {
            Triple(userId, userId, TYPE_OA)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExpertSupportTokenGenerator::class.java)
        private const val TOKEN_EXPIRATION_MINUTES = 3L
        private const val TAI_SUFFIX = "@tai"
        private const val TYPE_TAI = "tai"
        private const val TYPE_OA = "oa"
        private const val CLAIM_APP_ID = "appId"
        private const val CLAIM_USER = "user"
        private const val CLAIM_TYPE = "type"
        private const val CLAIM_RECORD_ID = "recordId"
    }
}
