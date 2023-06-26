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

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.pojo.TokenInfo
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_EXPIRED_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_SECRET_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_VALIDATE_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("MagicNumber")
class ApiAccessTokenService @Autowired constructor(
    val dslContext: DSLContext
) {
    @Value("\${auth.accessToken.expirationTime:#{null}}")
    private val expirationTime: Int? = null

    @Value("\${auth.accessToken.secret:#{null}}")
    private val secret: String? = null

    private val tokenCache = Caffeine.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*BuildId*/, TokenInfo/*token context*/>()

    fun verifyJWT(token: String): TokenInfo {
        val tokenInfo = getTokenInfo(token)
        if (tokenInfo.expirationTime < System.currentTimeMillis()) {
            throw ErrorCodeException(
                errorCode = PARAMETER_EXPIRED_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "Access token expired  in: ${tokenInfo.expirationTime}",
                params = arrayOf("token", "Access token expired in: ${tokenInfo.expirationTime}")
            )
        }
        return tokenInfo
    }

    fun generateUserToken(userDetails: String): TokenInfo {
        logger.info("AUTH | generateUserToken | userId = $userDetails")
        if (secret.isNullOrBlank()) {
            logger.error(
                "AUTH | generateUserToken failed, " +
                    "because config[auth.accessToken.secret] is not found"
            )
            throw ErrorCodeException(
                errorCode = PARAMETER_SECRET_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "AUTH| generateUserToken failed, " +
                    "because config[auth.accessToken.secret] is not found",
                params = arrayOf(
                    "config", "AUTH| generateUserToken failed, " +
                    "because config[auth.accessToken.secret] is not found"
                )
            )
        }
        val tokenInfo = TokenInfo(
            userId = userDetails,
            expirationTime = System.currentTimeMillis() + (expirationTime ?: EXPIRE_TIME_MILLS),
            accessToken = null
        )
        tokenInfo.accessToken = try {
            URLEncoder.encode(
                AESUtil.encrypt(
                    secret,
                    JsonUtil.toJson(tokenInfo, formatted = false)
                ), "UTF-8"
            )
        } catch (ignore: Throwable) {
            logger.error("AUTH | generateUserToken failed because $ignore ")
            throw ErrorCodeException(
                errorCode = PARAMETER_SECRET_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "AUTH| generateUserToken failed because encoding failed ",
                params = arrayOf("encoding", "AUTH| generateUserToken failed because encoding failed")
            )
        }
        return tokenInfo
    }

    private fun getTokenInfo(token: String): TokenInfo {
        val cacheToken = tokenCache.getIfPresent(token)
        if (cacheToken != null) return cacheToken

        val result = try {
            AESUtil.decrypt(secret!!, token)
        } catch (ignore: Throwable) {
            logger.error("AUTH|getTokenInfo Access token illegal : token = $token | error=$ignore")
            throw ErrorCodeException(
                errorCode = PARAMETER_ILLEGAL_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "Access token illegal",
                params = arrayOf("token", "Access token illegal")
            )
        }
        try {
            val tokenInfo = JsonUtil.to(result, TokenInfo::class.java)
            tokenCache.put(token, tokenInfo)
            return tokenInfo
        } catch (ignore: Throwable) {
            throw ErrorCodeException(
                errorCode = PARAMETER_VALIDATE_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "Access token invalid: ${ignore.message}",
                params = arrayOf("token", "Access token invalid: ${ignore.message}")
            )
        }
    }

    companion object {
        private const val EXPIRE_TIME_MILLS: Int = 14400000
        private val logger = LoggerFactory.getLogger(ApiAccessTokenService::class.java)
    }
}
