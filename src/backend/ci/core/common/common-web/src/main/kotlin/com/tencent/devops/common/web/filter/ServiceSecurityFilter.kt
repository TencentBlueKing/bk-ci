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

package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT_TOKEN
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.web.RequestFilter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
@DependsOn("environmentUtil")
class ServiceSecurityFilter(
    private val jwtManager: JwtManager,
    private val servletRequest: HttpServletRequest
) : ContainerRequestFilter {

    companion object {
        private val excludeVeritfyPath = listOf("/api/swagger.json", "/api/external/service/versionInfo")
        private val logger = LoggerFactory.getLogger((ServiceSecurityFilter::class.java))
    }

    override fun filter(requestContext: ContainerRequestContext?) {
        val uri = requestContext!!.uriInfo.requestUri.path
        if (shouldFilter(uri)) {

            val clientIp = servletRequest?.remoteAddr

            val jwt = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_JWT_TOKEN)
            if (jwt.isNullOrBlank()) {
                logger.warn("Invalid request, jwt is empty!Client ip:$clientIp,uri:$uri")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = CommonMessageCode.ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "Unauthorized:devops api jwt it empty."
                )
            }
            val checkResult: Boolean = jwtManager.verifyJwt(jwt)
            if (!checkResult) {
                logger.warn("Invalid request, jwt is invalid or expired!Client ip:$clientIp,uri:$uri")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = CommonMessageCode.ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "Unauthorized:devops api jwt it invalid or expired."
                )
            }
        }
    }

    private fun shouldFilter(uri: String): Boolean {
        if (!jwtManager.isAuthEnable() || !EnvironmentUtil.isProdProfileActive()) {
            return false
        }
        // 不拦截的接口
        excludeVeritfyPath.forEach {
            if (uri.startsWith(it)) {
                return false
            }
        }
        // 拦截api接口
        if (uri.startsWith("/api/")) {
            return true
        }
        // 默认不拦截
        return false
    }
}
