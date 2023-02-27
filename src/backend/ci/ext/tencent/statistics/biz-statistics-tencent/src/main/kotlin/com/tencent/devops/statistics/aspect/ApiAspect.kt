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
package com.tencent.devops.statistics.aspect

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.statistics.filter.ApiFilter
import com.tencent.devops.statistics.service.openapi.op.AppCodeService
import com.tencent.devops.statistics.util.openapi.ApiGatewayUtil
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class ApiAspect(
    private val appCodeService: AppCodeService,
    private val apiGatewayUtil: ApiGatewayUtil
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiFilter::class.java)
    }

    /**
     * 前置增强：目标方法执行之前执行
     *
     * @param jp
     */
    @Before("within(com.tencent.devops.statistics.openapi.resources.apigw..*)")
    fun beforeMethod(jp: JoinPoint) {
        if (!apiGatewayUtil.isAuth()) {
            logger.info("Openapi非apigw接口，不需要鉴权。")
            return
        }

        val methodName: String = jp.signature.name
        logger.info("【前置增强】the method 【$methodName】")
        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        var projectId: String? = null
        var appCode: String? = null
        var apigwType: String? = null
        parameterNames.forEach {
            logger.info("参数名[$it]")
        }

        parameterValue.forEach {
            logger.info("参数值[$it]")
        }
        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "projectId" -> {
                    projectId = parameterValue[index]?.toString()
                }

                "appCode" -> {
                    appCode = parameterValue[index]?.toString()
                }

                "apigwType" -> {
                    apigwType = parameterValue[index]?.toString()
                }

                else -> null
            }
        }
        logger.info("请求类型apigwType[$apigwType],appCode[$appCode],项目[$projectId]")
        if (projectId != null && appCode != null && (apigwType == "apigw-app")) {
            logger.info("判断！！！！请求类型apigwType[$apigwType],appCode[$appCode],是否有项目[$projectId]的权限.")
            if (appCodeService.validAppCode(appCode, projectId)) {
                logger.info("请求类型apigwType[$apigwType],appCode[$appCode],是否有项目[$projectId]的权限【验证通过】")
            } else {
                val message = "请求类型apigwType[$apigwType],appCode[$appCode],是否有项目[$projectId]的权限【验证失败】"
                throw PermissionForbiddenException(
                    message = message
                )
            }
        }
    }
}
