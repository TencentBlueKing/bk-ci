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

package com.tencent.bkrepo.common.security.http.platform

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.CharPool.COLON
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.PLATFORM_KEY
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.security.constant.AUTH_HEADER_UID
import com.tencent.bkrepo.common.security.constant.PLATFORM_AUTH_PREFIX
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.core.HttpAuthHandler
import com.tencent.bkrepo.common.security.http.credentials.AnonymousCredentials
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.servlet.http.HttpServletRequest

/**
 * 平台账号认证
 */
open class PlatformAuthHandler(private val authenticationManager: AuthenticationManager) : HttpAuthHandler {

    override fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials {
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty().trim()
        return if (authorizationHeader.startsWith(PLATFORM_AUTH_PREFIX)) {
            try {
                val encodedCredentials = authorizationHeader.removePrefix(PLATFORM_AUTH_PREFIX)
                val decodedHeader = String(Base64.getDecoder().decode(encodedCredentials))
                val parts = decodedHeader.split(COLON)
                require(parts.size >= 2)
                PlatformAuthCredentials(parts[0], parts[1])
            } catch (exception: IllegalArgumentException) {
                throw AuthenticationException("Invalid authorization value")
            }
        } else AnonymousCredentials()
    }

    override fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String {
        require(authCredentials is PlatformAuthCredentials)
        val (accessKey, secretKey) = authCredentials
        val appId = authenticationManager.checkPlatformAccount(accessKey, secretKey)
        val userId = request.getHeader(AUTH_HEADER_UID).orEmpty().trim()
            .takeIf { it.isNotEmpty() }?.apply { checkUserId(this) } ?: ANONYMOUS_USER
        request.setAttribute(PLATFORM_KEY, appId)
        request.setAttribute(USER_KEY, userId)
        return userId
    }

    private fun checkUserId(userId: String) {
        if (authenticationManager.findUserAccount(userId) == null) {
            authenticationManager.createUserAccount(userId)
            logger.info("Create user [$userId] success.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PlatformAuthHandler::class.java)
    }
}
