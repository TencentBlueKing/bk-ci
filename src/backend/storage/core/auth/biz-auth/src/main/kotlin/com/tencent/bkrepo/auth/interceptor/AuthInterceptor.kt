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

package com.tencent.bkrepo.auth.interceptor

import com.tencent.bkrepo.auth.constant.AUTHORIZATION
import com.tencent.bkrepo.auth.constant.AUTH_FAILED_RESPONSE
import com.tencent.bkrepo.auth.constant.AUTH_PROJECT_SUFFIX
import com.tencent.bkrepo.auth.constant.AUTH_REPO_SUFFIX
import com.tencent.bkrepo.auth.constant.BASIC_AUTH_HEADER_PREFIX
import com.tencent.bkrepo.auth.constant.PLATFORM_AUTH_HEADER_PREFIX
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.PLATFORM_KEY
import com.tencent.bkrepo.common.api.constant.StringPool.COLON
import com.tencent.bkrepo.common.api.constant.USER_KEY
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.HandlerInterceptor
import java.util.Base64
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthInterceptor : HandlerInterceptor {

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var userService: UserService

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val basicAuthHeader = request.getHeader(AUTHORIZATION).orEmpty()
        val authFailStr = String.format(AUTH_FAILED_RESPONSE, basicAuthHeader)
        try {
            // 项目内操作，优先使用项目管理员权限
            if (basicAuthHeader.startsWith(BASIC_AUTH_HEADER_PREFIX) &&
                (request.requestURI.contains(AUTH_REPO_SUFFIX) || request.requestURI.contains(AUTH_PROJECT_SUFFIX))
            ) {
                val encodedCredentials = basicAuthHeader.removePrefix(BASIC_AUTH_HEADER_PREFIX)
                val decodedHeader = String(Base64.getDecoder().decode(encodedCredentials))
                val parts = decodedHeader.split(COLON)
                require(parts.size == 2)
                userService.findUserByUserToken(parts[0], parts[1]) ?: run {
                    logger.warn("find no user [${parts[0]}]")
                    throw IllegalArgumentException("check credential fail")
                }
                request.setAttribute(USER_KEY, parts[0])
                return true
            }
            if (!basicAuthHeader.startsWith(PLATFORM_AUTH_HEADER_PREFIX)) {
                throw IllegalArgumentException("platform not found")
            }
            val encodedCredentials = basicAuthHeader.removePrefix(PLATFORM_AUTH_HEADER_PREFIX)
            val decodedHeader = String(Base64.getDecoder().decode(encodedCredentials))
            val parts = decodedHeader.split(COLON)
            require(parts.size == 2)
            val appId = accountService.checkCredential(parts[0], parts[1]) ?: run {
                logger.warn("find no account [$parts[0]]")
                throw IllegalArgumentException("check credential fail")
            }
            request.setAttribute(PLATFORM_KEY, appId)
            return true
        } catch (e: IllegalArgumentException) {
            response.status = HttpStatus.UNAUTHORIZED.value
            response.writer.print(authFailStr)
            logger.warn("check account exception [$e]")
            return false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthInterceptor::class.java)
    }
}
