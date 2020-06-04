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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.util.JsonUtil
import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.Ordered
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Configuration
@PropertySource("classpath:/common-web.properties")
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(JerseyAutoConfiguration::class)
@EnableConfigurationProperties(SwaggerProperties::class)
class WebAutoConfiguration @Autowired constructor(private val profile: com.tencent.devops.common.service.Profile) {

    @Bean
    @Profile("prod")
    fun jerseyConfig() = JerseyConfig()

    @Bean
    @Profile("!prod") // 非生产环境的Swagger配置,方便开发调试
    fun jerseySwaggerConfig() = JerseySwaggerConfig(profile)

    @Bean
    @Primary
    fun objectMapper() = JsonUtil.getObjectMapper()

//    @Bean("jasyptStringEncryptor")
//    @Primary
//    fun stringEncryptor() = DefaultEncryptor(jasyptPasswd)

    @Bean
    fun versionInfoResource() = VersionInfoResource()

    @Bean
    fun jmxAutoConfiguration() = JmxAutoConfiguration()

    private val languageHeaderName = "Accept-Language"
    /**
     * feign调用拦截器
     */
    @Bean(name = ["normalRequestInterceptor"])
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                    ?: return@RequestInterceptor
            val request = attributes.request
            val languageHeaderValue = request.getHeader(languageHeaderName)
            if (!languageHeaderValue.isNullOrBlank())
                requestTemplate.header(languageHeaderName, languageHeaderValue) // 设置Accept-Language请求头
        }
    }


    @Bean(name = ["devopsRequestInterceptor"])
    fun bsRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                    ?: return@RequestInterceptor
            val request = attributes.request
            val bkTicket = request.getHeader(AUTH_HEADER_DEVOPS_BK_TICKET)
            val userName = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID)
            requestTemplate.header(AUTH_HEADER_DEVOPS_BK_TICKET, bkTicket)
            requestTemplate.header(AUTH_HEADER_DEVOPS_USER_ID, userName)

        }
    }
}
