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

package com.tencent.bkrepo.common.service.util

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.StringPool
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.StringTokenizer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object HttpContextHolder {
    fun getRequest(): HttpServletRequest {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        require(requestAttributes is ServletRequestAttributes)
        return requestAttributes.request
    }

    fun getRequestOrNull(): HttpServletRequest? {
        val requestAttributes = RequestContextHolder.getRequestAttributes() ?: return null
        require(requestAttributes is ServletRequestAttributes)
        return requestAttributes.request
    }

    fun getResponse(): HttpServletResponse {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        require(requestAttributes is ServletRequestAttributes)
        return requestAttributes.response!!
    }

    fun getClientAddress(): String {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return if (requestAttributes is ServletRequestAttributes) {
            val request = requestAttributes.request
            var address = request.getHeader(HttpHeaders.X_FORWARDED_FOR)
            address = if (address.isNullOrBlank()) {
                request.getHeader(HttpHeaders.X_REAL_IP)
            } else {
                StringTokenizer(address, StringPool.COMMA).nextToken()
            }
            if (address.isNullOrBlank()) {
                address = request.getHeader(HttpHeaders.PROXY_CLIENT_IP)
            }
            if (address.isNullOrBlank()) {
                address = request.remoteAddr
            }
            if (address.isNullOrBlank()) {
                address = StringPool.UNKNOWN
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
            return address
        } else StringPool.UNKNOWN
    }

    fun getXForwardedFor(): String {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return if (requestAttributes is ServletRequestAttributes) {
            val request = requestAttributes.request
            return request.getHeader(HttpHeaders.X_FORWARDED_FOR)
        } else StringPool.UNKNOWN
    }
}
