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

package com.tencent.bkrepo.common.security.http.oauth

import com.tencent.bkrepo.common.api.constant.AUTHORITIES_KEY
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.security.constant.OAUTH_AUTH_PREFIX
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.core.HttpAuthHandler
import com.tencent.bkrepo.common.security.http.credentials.AnonymousCredentials
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class OauthAuthHandler(val authenticationManager: AuthenticationManager) : HttpAuthHandler {
    override fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials {
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        return if (authorizationHeader.startsWith(OAUTH_AUTH_PREFIX)) {
            try {
                val accessToken = authorizationHeader.removePrefix(OAUTH_AUTH_PREFIX).trim()
                OauthAuthCredentials(accessToken)
            } catch (ignored: IllegalArgumentException) {
                throw AuthenticationException("Invalid authorization value")
            }
        } else AnonymousCredentials()
    }

    override fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String {
        require(authCredentials is OauthAuthCredentials)
        return authenticationManager.checkOauthToken(authCredentials.token)
    }

    override fun onAuthenticateSuccess(request: HttpServletRequest, response: HttpServletResponse, userId: String) {
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        val accessToken = authorizationHeader.removePrefix(OAUTH_AUTH_PREFIX).trim()
        val oauthToken = authenticationManager.findOauthToken(accessToken)
            ?: throw AuthenticationException("Invalid access token")
        request.setAttribute(AUTHORITIES_KEY, oauthToken.scope)
    }
}
