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

package com.tencent.devops.common.web

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SERVICE_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_GATEWAY_TAG
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.REQUEST_CHANNEL
import com.tencent.devops.common.client.ms.MicroServiceTarget
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.trace.TraceTag
import feign.RequestInterceptor
import feign.Target.HardCodedTarget
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Configuration
class FeignConfiguration @Autowired constructor(
    private val bkTag: BkTag
) {
    private val logger = LoggerFactory.getLogger(FeignConfiguration::class.java)

    /**
     * feign调用拦截器
     */
    @Bean
    @Primary
    @Suppress("ComplexMethod")
    fun requestInterceptor(@Autowired jwtManager: JwtManager): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            requestTemplate.decodeSlash(false)

            if (!requestTemplate.headers().containsKey(AUTH_HEADER_PROJECT_ID)) {
            // 增加X-HEAD-CONSUL-TAG供下游服务获取相同的consul tag
                val tag = bkTag.getFinalTag()
                requestTemplate.header(AUTH_HEADER_GATEWAY_TAG, tag)
                logger.debug("gateway tag is : $tag")
            }

            // 设置traceId
            requestTemplate.header(
                TraceTag.X_DEVOPS_RID,
                MDC.get(TraceTag.BIZID)?.ifBlank { TraceTag.buildBiz() } ?: TraceTag.buildBiz()
            )

            // 增加X-DEVOPS-JWT验证头部
            if (!requestTemplate.headers().containsKey(AUTH_HEADER_DEVOPS_JWT_TOKEN)) {
                // 只有jwt验证发送启动的时候才设置头部
                if (jwtManager.isSendEnable()) {
                    requestTemplate.header(AUTH_HEADER_DEVOPS_JWT_TOKEN, jwtManager.getToken() ?: "")
                }
            }

            // 新增devopsToken给网关校验
            val devopsToken = EnvironmentUtil.gatewayDevopsToken()
            if (devopsToken != null) {
                requestTemplate.header("X-DEVOPS-TOKEN", devopsToken)
            }

            val attributes =
                RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return@RequestInterceptor
            val request = attributes.request
            val languageHeaderName = "Accept-Language"
            val languageHeaderValue = request.getHeader(languageHeaderName)
            if (!languageHeaderValue.isNullOrBlank()) {
                requestTemplate.header(languageHeaderName, languageHeaderValue) // 设置Accept-Language请求头
            }
            // 设置用户ID
            val userId = request.getHeader(AUTH_HEADER_USER_ID)
            if (!userId.isNullOrBlank()) {
                requestTemplate.header(AUTH_HEADER_USER_ID, userId)
            }
            // 设置请求渠道信息
            val requestChannel =
                (request.getAttribute(REQUEST_CHANNEL) ?: request.getHeader(REQUEST_CHANNEL))?.toString()
            if (!requestChannel.isNullOrBlank()) {
                requestTemplate.header(REQUEST_CHANNEL, requestChannel)
            }
            // 设置服务名称
            val serviceName = when (val target = requestTemplate.feignTarget()) {
                is MicroServiceTarget -> {
                    target.name()
                }

                is HardCodedTarget -> {
                    val nameRegex = Regex("/([a-z]+)/api")
                    val nameMatchResult = nameRegex.find(target.name())
                    nameMatchResult?.groupValues?.get(1) ?: target.name()
                }

                else -> {
                    target.name()
                }
            }
            if (!serviceName.isNullOrBlank()) {
                requestTemplate.header(AUTH_HEADER_DEVOPS_SERVICE_NAME, serviceName)
            }
            val cookies = request.cookies
            if (cookies != null && cookies.isNotEmpty()) {
                val cookieBuilder = StringBuilder()
                cookies.forEach {
                    cookieBuilder.append(it.name).append("=").append(it.value).append(";")
                }
                requestTemplate.header("Cookie", cookieBuilder.toString()) // 设置cookie信息
            }
        }
    }
}
