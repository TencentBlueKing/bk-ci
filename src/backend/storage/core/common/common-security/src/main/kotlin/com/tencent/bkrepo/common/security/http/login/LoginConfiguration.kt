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

package com.tencent.bkrepo.common.security.http.login

import com.tencent.bkrepo.common.security.http.HttpAuthSecurity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.annotation.PostConstruct
import kotlin.reflect.jvm.javaMethod

@Configuration
class LoginConfiguration {

    @Autowired
    private lateinit var requestMappingHandlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var httpAuthSecurity: HttpAuthSecurity

    @PostConstruct
    fun init() {
        httpAuthSecurity.getAuthHandlerList().forEach { handler ->
            handler.getLoginEndpoint()?.let { registerLoginEndpoint(it) }
        }
    }

    private fun registerLoginEndpoint(endpoint: String) {
        val mappingInfo = RequestMappingInfo.paths(endpoint).build()
        requestMappingHandlerMapping.registerMapping(mappingInfo, this, this::anonymous.javaMethod!!)
    }

    /**
     * a trick method for registering request mapping dynamiclly in spring interceptor
     */
    @ResponseBody
    private fun anonymous() { }
}
