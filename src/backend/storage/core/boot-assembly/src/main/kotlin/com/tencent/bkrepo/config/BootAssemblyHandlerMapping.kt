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

package com.tencent.bkrepo.config

import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.Method

/**
 * 单体应用打包，需要对请求路径映射做修改
 * 1. 注册request handler的时候是不带服务名前缀的，但请求路径{host}/{service}/xxx带了服务名，需要在注册的时候添加上去
 * 2. 过滤掉/service/开头的微服务接口
 */
class BootAssemblyHandlerMapping : RequestMappingHandlerMapping() {

    private val regex = Regex("""com\.tencent\.bkrepo\.(\w+)\..*""")

    override fun isHandler(beanType: Class<*>): Boolean {
        return AnnotatedElementUtils.hasAnnotation(beanType, Controller::class.java) ||
            AnnotatedElementUtils.hasAnnotation(beanType, RestController::class.java)
    }

    override fun registerHandlerMethod(handler: Any, method: Method, mapping: RequestMappingInfo) {
        updateMapping(mapping, method)?.let {
            super.registerHandlerMethod(handler, method, it)
        }
    }

    /**
     * 更新RequestMappingInfo
     */
    @Suppress("SpreadOperator")
    private fun updateMapping(mapping: RequestMappingInfo, method: Method): RequestMappingInfo? {
        val declaringClassName = method.declaringClass.name
        val serviceName = regex.find(declaringClassName)?.groupValues?.get(1)
        val newPatterns = updatePatterns(mapping.patternsCondition.patterns, serviceName)
        if (newPatterns.isEmpty()) {
            return null
        }
        val patternsCondition = PatternsRequestCondition(*newPatterns)
        return RequestMappingInfo(
            patternsCondition, mapping.methodsCondition, mapping.paramsCondition,
            mapping.headersCondition, mapping.consumesCondition, mapping.producesCondition,
            mapping.customCondition
        )
    }

    /**
     * 更新及过滤匹配pattern，如果经过过滤后的patterns为空，则返回null
     */
    private fun updatePatterns(patterns: Set<String>, serviceName: String?): Array<String> {
        return patterns.stream()
            .filter { !it.startsWith("/service/") }
            .map { withPrefix(it, serviceName) }
            .toArray<String> { length -> arrayOfNulls(length) }
    }

    /**
     * 添加前缀
     */
    private fun withPrefix(pattern: String, prefix: String?): String {
        return if (prefix == null) pattern else "/$prefix$pattern"
    }
}
