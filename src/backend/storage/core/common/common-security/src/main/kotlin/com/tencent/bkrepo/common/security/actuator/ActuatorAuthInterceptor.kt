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

package com.tencent.bkrepo.common.security.actuator

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.security.constant.BASIC_AUTH_PREFIX
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.PrincipalType
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.util.Base64
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ActuatorAuthInterceptor(
    private val authenticationManager: AuthenticationManager,
    private val permissionManager: PermissionManager
) : HandlerInterceptorAdapter() {

    private val antPathMatcher = AntPathMatcher()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val uri = request.requestURI
        if (antPathMatcher.match(HEALTH_ENDPOINT, uri) || antPathMatcher.match(INFO_ENDPOINT, uri)) {
            return true
        }
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?: throw AuthenticationException("Empty authorization value.")
        try {
            val encodedCredentials = authorizationHeader.removePrefix(BASIC_AUTH_PREFIX)
            val decodedHeader = String(Base64.getDecoder().decode(encodedCredentials))
            val parts = decodedHeader.split(StringPool.COLON)
            require(parts.size >= 2)
            val userId = authenticationManager.checkUserAccount(parts[0], parts[1])
            permissionManager.checkPrincipal(userId, PrincipalType.ADMIN)
            return true
        } catch (exception: AuthenticationException) {
            throw exception
        } catch (exception: PermissionException) {
            throw exception
        } catch (exception: IllegalArgumentException) {
            throw AuthenticationException("Invalid authorization value.")
        }
    }

    companion object {
        private const val HEALTH_ENDPOINT = "/actuator/health/**"
        private const val INFO_ENDPOINT = "/actuator/info/**"
    }
}
