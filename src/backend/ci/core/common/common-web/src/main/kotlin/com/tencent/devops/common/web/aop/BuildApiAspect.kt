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
package com.tencent.devops.common.web.aop

import com.tencent.devops.common.web.annotation.BuildApiPermission
import com.tencent.devops.common.web.factory.BuildApiHandleFactory
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory

@Aspect
class BuildApiAspect {

    @Pointcut("@annotation(com.tencent.devops.common.web.annotation.BuildApiPermission)")
    fun pointCut() = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(BuildApiAspect::class.java)
    }

    /**
     * 前置增强：目标方法执行之前执行
     *
     * @param jp
     */
    @Before("pointCut()")
    fun doBefore(jp: JoinPoint) {
        val method = (jp.signature as MethodSignature).method
        val methodName: String = method.name
        val types = method.getAnnotation(BuildApiPermission::class.java)?.types?.toList()
        logger.info("[doBefore] the method 【$methodName】 types$types")
        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        types?.forEach { type ->
            BuildApiHandleFactory.createBuildApiHandleService(type).handleBuildApiService(
                parameterNames,
                parameterValue
            )
        }
    }
}
