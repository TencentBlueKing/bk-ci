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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.security.http

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.credentials.AnonymousCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Http请求认证拦截器
 * 拦截器中使用了FeignClient，不能使用构造器注入，否则会有循环依赖错误
 */
class HttpAuthInterceptor : HandlerInterceptorAdapter() {

    @Autowired
    private lateinit var httpAuthSecurity: HttpAuthSecurity

    private val pathMatcher = AntPathMatcher()

    @PostConstruct
    private fun init() {
        if (httpAuthSecurity.getAuthHandlerList().isEmpty()) {
            logger.warn("No http auth handler was configured.")
        }
        httpAuthSecurity.getAuthHandlerList().forEach {
            logger.info("Initializing http auth handler[${it::class.simpleName}].")
        }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val requestUri = request.requestURI
        httpAuthSecurity.getAuthHandlerList().forEach { authHandler ->
            val isLoginRequest = authHandler.getLoginEndpoint()?.let { pathMatcher.match(it, requestUri) } ?: false
            if (authHandler.getLoginEndpoint() == null || isLoginRequest) {
                try {
                    val authCredentials = authHandler.extractAuthCredentials(request)
                    if (authCredentials !is AnonymousCredentials) {
                        val userId = authHandler.onAuthenticate(request, authCredentials)
                        request.setAttribute(USER_KEY, userId)
                        authHandler.onAuthenticateSuccess(request, response, userId)
                        logger.debug("User[${SecurityUtils.getPrincipal()}] authenticate success by ${authHandler.javaClass.simpleName}.")
                        return true
                    } else if (isLoginRequest) {
                        throw AuthenticationException()
                    }
                } catch (authenticationException: AuthenticationException) {
                    authHandler.onAuthenticateFailed(request, response, authenticationException)
                    return false
                }
            }
        }
        // 没有合适的认证handler或为匿名用户
        if (httpAuthSecurity.isAnonymousEnabled()) {
            logger.debug("None of the auth handler authenticate success, set anonymous user.")
            request.setAttribute(USER_KEY, ANONYMOUS_USER)
            return true
        } else {
            throw AuthenticationException()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpAuthInterceptor::class.java)
    }
}
