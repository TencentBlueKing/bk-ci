/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT_TOKEN
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.web.utils.I18nUtil
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.net.InetAddress
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component
@DependsOn("environmentUtil")
class ServiceSecurityFilter(
    private val jwtManager: JwtManager,
    private val servletRequest: HttpServletRequest
) : Filter {

    companion object {
        private val excludeVerifyPath = listOf(
            "/api/swagger.json",
            "/api/external/service/versionInfo",
            "/management/health/livenessState",
            "/management/health/readinessState",
            "/management/prometheus",
            "/management/userPrometheus"
        )
        private val logger = LoggerFactory.getLogger((ServiceSecurityFilter::class.java))
        private val jwtNullError = ErrorCodeException(
            statusCode = 401,
            errorCode = CommonMessageCode.ERROR_SERVICE_NO_AUTH,
            defaultMessage = "Unauthorized:devops api jwt it empty."
        )
        private val jwtCheckFailError = ErrorCodeException(
            statusCode = 401,
            errorCode = CommonMessageCode.ERROR_SERVICE_NO_AUTH,
            defaultMessage = "Unauthorized:devops api jwt it invalid or expired."
        )
    }

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        if (request == null || chain == null) {
            return
        }
        val httpServletRequest = request as HttpServletRequest
        val uri = httpServletRequest.requestURI
        val clientIp = servletRequest.remoteAddr

        val jwt = httpServletRequest.getHeader(AUTH_HEADER_DEVOPS_JWT_TOKEN)
        val flag = shouldFilter(uri, clientIp)
        var error: ErrorCodeException? = null
        if (flag && jwtManager.isSendEnable()) {
            error = check(jwt, clientIp, uri)
        }
        if (error != null && jwtManager.isAuthEnable()) {
            response as HttpServletResponse
            val errorResult = I18nUtil.generateResponseDataObject(
                messageCode = error.errorCode,
                params = error.params,
                data = null,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                defaultMessage = error.defaultMessage
            )
            response.status = error.statusCode
            response.contentType = "application/json;charset=UTF-8"
            response.writer.print(JsonUtil.toJson(errorResult, false))
            response.writer.flush()
            return
        }
        chain.doFilter(request, response)
    }

    private fun check(
        jwt: String?,
        clientIp: String?,
        uri: String?
    ): ErrorCodeException? {
        if (jwt.isNullOrBlank()) {
            logger.warn("Invalid request, jwt is empty!Client ip:$clientIp,uri:$uri")
            return jwtNullError
        }
        val checkResult: Boolean = jwtManager.verifyJwt(jwt)
        if (!checkResult) {
            logger.warn("Invalid request, jwt is invalid or expired!Client ip:$clientIp,uri:$uri")
            return jwtCheckFailError
        }
        return null
    }

    private fun shouldFilter(uri: String, clientIp: String): Boolean {
        val localhost = kotlin.runCatching {
            InetAddress.getByName(clientIp).isLoopbackAddress
        }.getOrNull() ?: false
        // 不拦截本机请求
        if (localhost) {
            return false
        }

        // 不拦截的接口
        excludeVerifyPath.forEach {
            if (uri.startsWith(it)) {
                return false
            }
        }
        // 其余接口进行拦截
        return true
    }
}
