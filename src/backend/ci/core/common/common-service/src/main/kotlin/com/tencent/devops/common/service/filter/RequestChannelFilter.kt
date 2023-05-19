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

package com.tencent.devops.common.service.filter

import com.tencent.devops.common.api.constant.REQUEST_CHANNEL
import com.tencent.devops.common.api.enums.RequestChannelTypeEnum
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Component
class RequestChannelFilter : Filter {
    override fun destroy() = Unit

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        if (request == null || chain == null) {
            return
        }
        val httpServletRequest = request as HttpServletRequest
        val requestUrl = httpServletRequest.requestURI
        // 根据接口路径设置请求渠道信息
        val channel = if (requestUrl.contains("/api/build/")) {
            RequestChannelTypeEnum.BUILD.name
        } else if (requestUrl.contains("/api/user/")) {
            RequestChannelTypeEnum.USER.name
        } else if (requestUrl.contains("/api/op/")) {
            RequestChannelTypeEnum.OP.name
        } else if (requestUrl.contains("/api/open/")) {
            RequestChannelTypeEnum.OPEN.name
        } else {
            null
        }
        channel?.let { request.setAttribute(REQUEST_CHANNEL, channel) }
        chain.doFilter(request, response)
    }

    override fun init(filterConfig: FilterConfig?) = Unit
}
