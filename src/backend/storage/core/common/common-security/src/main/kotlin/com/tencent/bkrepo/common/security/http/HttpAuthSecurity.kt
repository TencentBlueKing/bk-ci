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

import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.security.constant.ANY_URI_PATTERN
import com.tencent.bkrepo.common.security.http.basic.BasicAuthHandler
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthHandler
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.http.platform.PlatformAuthHandler
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

class HttpAuthSecurity {

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var serviceUserResource: ServiceUserResource

    @Autowired
    private lateinit var jwtAuthProperties: JwtAuthProperties

    @Autowired
    private lateinit var customizers: ObjectProvider<HttpAuthSecurityCustomizer>

    private var anonymousEnabled: Boolean = true
    private var basicAuthEnabled: Boolean = true
    private var platformAuthEnabled: Boolean = true
    private var jwtAuthEnabled: Boolean = true
    private val includedPatterns: MutableSet<String> = mutableSetOf()
    private val excludedPatterns: MutableSet<String> = mutableSetOf()
    private val authHandlerList: MutableList<HttpAuthHandler> = mutableListOf()
    private val customizedAuthHandlerList: MutableList<HttpAuthHandler> = mutableListOf()

    @PostConstruct
    fun init() {
        customizers.forEach { it.customize(this) }

        if (basicAuthEnabled) {
            val basicAuthHandler = BasicAuthHandler(authenticationManager)
            authHandlerList.add(basicAuthHandler)
        }
        if (platformAuthEnabled) {
            authHandlerList.add(PlatformAuthHandler(authenticationManager, serviceUserResource))
        }
        if (jwtAuthEnabled) {
            authHandlerList.add(JwtAuthHandler(jwtAuthProperties))
        }
        authHandlerList.addAll(customizedAuthHandlerList)
    }

    /**
     * 禁用BasicAuth认证，默认开启
     */
    fun disableBasicAuth(): HttpAuthSecurity {
        basicAuthEnabled = false
        return this
    }

    /**
     * 禁用Platform认证，默认开启
     */
    fun disablePlatformAuth(): HttpAuthSecurity {
        platformAuthEnabled = false
        return this
    }

    /**
     * 禁用JWT认证，默认开启
     */
    fun disableJwtAuth(): HttpAuthSecurity {
        jwtAuthEnabled = false
        return this
    }

    /**
     * 禁用匿名登录
     * 开启后, 当没有任何auth handler认证成功，会抛出异常
     * 若不开启，则以匿名用户访问
     */
    fun disableAnonymous(): HttpAuthSecurity {
        anonymousEnabled = false
        return this
    }

    /**
     * 添加新的认证方式
     */
    fun addHttpAuthHandler(httpAuthHandler: HttpAuthHandler): HttpAuthSecurity {
        customizedAuthHandlerList.add(httpAuthHandler)
        return this
    }

    /**
     * 添加认证路径，如果不指定任何路径，那么默认会对所有接口进行认证
     */
    fun includePattern(pattern: String): HttpAuthSecurity {
        includedPatterns.add(pattern)
        return this
    }

    /**
     * 排除认证路径
     */
    fun excludePattern(pattern: String): HttpAuthSecurity {
        excludedPatterns.add(pattern)
        return this
    }

    fun isAnonymousEnabled(): Boolean {
        return anonymousEnabled
    }

    fun getIncludedPatterns(): List<String> {
        if (includedPatterns.isEmpty()) {
            includedPatterns.add(ANY_URI_PATTERN)
        }
        return includedPatterns.toList()
    }

    fun getExcludedPatterns(): List<String> {
        return excludedPatterns.toList()
    }

    fun getAuthHandlerList(): List<HttpAuthHandler> {
        return authHandlerList
    }
}
