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

package com.tencent.bkrepo.common.security.service

import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.security.constant.MS_AUTH_HEADER_SECURITY_TOKEN
import com.tencent.bkrepo.common.security.constant.MS_AUTH_HEADER_UID
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import feign.RequestInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(ServiceAuthProperties::class)
@Import(ServiceAuthManager::class)
class ServiceAuthConfiguration {

    @Bean
    fun securityRequestInterceptor(serviceAuthManager: ServiceAuthManager): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            requestTemplate.header(MS_AUTH_HEADER_SECURITY_TOKEN, serviceAuthManager.getSecurityToken())
            HttpContextHolder.getRequestOrNull()?.getAttribute(USER_KEY)?.let {
                requestTemplate.header(MS_AUTH_HEADER_UID, it as String)
            }
        }
    }

    @Bean
    fun serviceAuthInterceptor(
        serviceAuthManager: ServiceAuthManager,
        serviceAuthProperties: ServiceAuthProperties
    ): ServiceAuthInterceptor {
        return ServiceAuthInterceptor(serviceAuthManager, serviceAuthProperties)
    }

    @Bean
    fun serviceAuthWebMvcConfigurer(serviceAuthInterceptor: ServiceAuthInterceptor): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(serviceAuthInterceptor).addPathPatterns(listOf("/service/**"))
                super.addInterceptors(registry)
            }
        }
    }
}
