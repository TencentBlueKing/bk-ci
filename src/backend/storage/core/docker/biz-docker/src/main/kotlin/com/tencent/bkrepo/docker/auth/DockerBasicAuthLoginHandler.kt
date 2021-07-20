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

package com.tencent.bkrepo.docker.auth

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.basic.BasicAuthHandler
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.docker.constant.AUTH_CHALLENGE_TOKEN
import com.tencent.bkrepo.docker.constant.DOCKER_API_PREFIX
import com.tencent.bkrepo.docker.constant.DOCKER_API_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_UNAUTHED_BODY
import com.tencent.bkrepo.docker.util.TimeUtil
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * docker basic auth logon handler
 */
class DockerBasicAuthLoginHandler(
    authenticationManager: AuthenticationManager,
    private val jwtProperties: JwtAuthProperties
) : BasicAuthHandler(authenticationManager) {

    private val signingKey = JwtUtils.createSigningKey(jwtProperties.secretKey)

    override fun getLoginEndpoint() = DOCKER_API_PREFIX + DOCKER_API_SUFFIX

    override fun onAuthenticateSuccess(request: HttpServletRequest, response: HttpServletResponse, userId: String) {
        val token = JwtUtils.generateToken(signingKey, jwtProperties.expiration, userId)
        val issuedAt = TimeUtil.getGMTTime()
        val tokenUrl = AUTH_CHALLENGE_TOKEN.format(token, token, issuedAt)
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.setHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        response.writer.print(tokenUrl)
        response.writer.flush()
        super.onAuthenticateSuccess(request, response, userId)
    }

    override fun onAuthenticateFailed(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {
        request.getHeader(HttpHeaders.AUTHORIZATION) ?: run {
            logger.info("empty user pull,push ,change to  [$this]")
            return onAuthenticateSuccess(request, response, ANONYMOUS_USER)
        }

        logger.warn("Authenticate failed: [$authenticationException]")
        response.status = HttpStatus.UNAUTHORIZED.value
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.setHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        response.writer.print(DOCKER_UNAUTHED_BODY)
        response.writer.flush()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerBasicAuthLoginHandler::class.java)
    }
}
