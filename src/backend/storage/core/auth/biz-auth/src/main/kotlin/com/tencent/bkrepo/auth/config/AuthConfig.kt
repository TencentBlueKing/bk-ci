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

package com.tencent.bkrepo.auth.config

import com.tencent.bkrepo.auth.interceptor.AuthInterceptor
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurity
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AuthConfig : WebMvcConfigurer {

    @Value("\${auth.security.enablePrefix:false}")
    var enablePrefix: Boolean = false
    override fun addInterceptors(registry: InterceptorRegistry) {
        val httpAuthSecurity = HttpAuthSecurity()
            .withPrefix("/auth")
            .includePattern("/api/**")
            .excludePattern("/external/**")
            .excludePattern("/api/user/login")
            .excludePattern("/api/user/info")
            .excludePattern("/api/user/verify")
            .excludePattern("/api/user/rsa")
            .excludePattern("/api/oauth/token")
        if (enablePrefix) {
            httpAuthSecurity.enablePrefix()
        }
        registry.addInterceptor(clientAuthInterceptor())
            .addPathPatterns(httpAuthSecurity.getIncludedPatterns())
            .excludePathPatterns(httpAuthSecurity.getExcludedPatterns())
            .order(0)
        super.addInterceptors(registry)
    }

    @Bean
    fun clientAuthInterceptor() = AuthInterceptor()
}
