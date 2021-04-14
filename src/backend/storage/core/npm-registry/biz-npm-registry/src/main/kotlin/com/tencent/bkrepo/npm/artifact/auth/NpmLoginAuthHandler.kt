/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.npm.artifact.auth

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.core.HttpAuthHandler
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import com.tencent.bkrepo.common.security.http.credentials.UsernamePasswordCredentials
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.npm.pojo.request.NpmLoginRequest
import com.tencent.bkrepo.npm.pojo.response.NpmResponse
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import java.security.Key
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * npm登录handler
 */
class NpmLoginAuthHandler(
    private val authenticationManager: AuthenticationManager,
    private val jwtProperties: JwtAuthProperties
) : HttpAuthHandler {

    private val signingKey: Key by lazy { JwtUtils.createSigningKey(jwtProperties.secretKey) }

    override fun getLoginEndpoint() = "/{projectId}/{repoName}/-/user/org.couchdb.user:*"
    override fun getLoginMethod() = HttpMethod.PUT.name

    override fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials {
        val loginRequest = request.inputStream.readJsonString<NpmLoginRequest>()
        return UsernamePasswordCredentials(loginRequest.name, loginRequest.password)
    }

    override fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String {
        require(authCredentials is UsernamePasswordCredentials)
        return authenticationManager.checkUserAccount(authCredentials.username, authCredentials.password)
    }

    override fun onAuthenticateSuccess(request: HttpServletRequest, response: HttpServletResponse, userId: String) {
        val token = JwtUtils.generateToken(signingKey, jwtProperties.expiration, userId)
        val npmResponse = NpmResponse.success().apply {
            set("id", "org.couchdb.user:$userId")
            set("token", token)
        }
        response.status = HttpStatus.CREATED.value
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(npmResponse.toJsonString())
    }

    override fun onAuthenticateFailed(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val npmResponse = NpmResponse.error(authenticationException.reason)
        response.writer.write(npmResponse.toJsonString())
    }
}
