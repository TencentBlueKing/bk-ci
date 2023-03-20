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

package com.tencent.devops.auth.filter

import com.tencent.devops.auth.service.AuthUserBlackListService
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Aspect
@Component
class BlackListAspect @Autowired constructor(
    val authUserBlackListService: AuthUserBlackListService
) {
    @Before("within(com.tencent.devops.auth.api..*)")
    fun beforeMethod(jp: JoinPoint) {
        // 参数value
        val parameterValue = jp.args
        var userId: String? = null
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "userId" -> userId = parameterValue[index]?.toString()
                else -> Unit
            }
        }
        if (!userId.isNullOrEmpty()) {
            logger.info("BlackListAspect userId: $userId")
            val checkBlackList = authUserBlackListService.checkBlackListUser(userId)
            if (checkBlackList) {
                logger.warn("blackList user $userId call ${jp.signature.name}")
                throw PermissionForbiddenException("blackList user $userId")
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(BlackListAspect::class.java)
    }
}
