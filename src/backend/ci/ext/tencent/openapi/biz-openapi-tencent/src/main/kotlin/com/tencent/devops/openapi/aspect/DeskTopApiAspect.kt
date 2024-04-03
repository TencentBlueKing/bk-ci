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
package com.tencent.devops.openapi.aspect

import com.tencent.devops.common.api.auth.DEVX_HEADER_GW_TOKEN
import com.tencent.devops.common.api.auth.DEVX_HEADER_NGGW_CLIENT_ADDRESS
import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.ws.rs.core.Response

@Aspect
@Component
class DeskTopApiAspect {

    @Value("\${devx.gwToken:}")
    val devxGwToken = ""

    @Volatile
    private var devGwTokens: HashSet<String>? = null

    /**
     * 前置增强：对桌面 SDK 方法都进行前置查询比对
     *
     * @param jp
     */
    @Suppress("ComplexMethod")
    @Before("within(com.tencent.devops.openapi.resources.apigw.desktop.*)")
    fun beforeMethod(jp: JoinPoint) {

        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames

        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

        val desktopIP: String? = request.getHeader(DEVX_HEADER_NGGW_CLIENT_ADDRESS)
        val devxToken: String? = request.getHeader(DEVX_HEADER_GW_TOKEN)

        for (index in parameterValue.indices) {
            val trueVal = parameterValue[index]?.toString()
            when (parameterNames[index]) {
                "desktopIP" -> {
                    Preconditions.checkTrue(
                        condition = trueVal == desktopIP,
                        exception = CustomException(
                            Response.Status.BAD_REQUEST,
                            I18nUtil.getCodeLanMessage(
                                messageCode = OpenAPIMessageCode.PARAM_VERIFY_FAIL,
                                params = arrayOf("云桌面IP可能伪造，真正IP：$desktopIP, 参数伪造IP：$trueVal")
                            )
                        )
                    )
                }
                "devxGwToken" -> {
                    Preconditions.checkTrue(
                        condition = trueVal == devxToken,
                        exception = CustomException(
                            Response.Status.BAD_REQUEST,
                            I18nUtil.getCodeLanMessage(
                                messageCode = OpenAPIMessageCode.PARAM_VERIFY_FAIL,
                                params = arrayOf("只接受云桌面访问过来的请求")
                            )
                        )
                    )
                }
                else -> Unit
            }
        }

        logger.debug("DeskTopApiAspect|desktop ip: {}", desktopIP)

        if (desktopIP.isNullOrBlank()) {
            throw PermissionForbiddenException(message = "无法获取到正确的云桌面IP")
        } else if (devxToken != devxGwToken) {
            // init 支持配置多个 Token 以便实现替换
            if (devGwTokens == null && devxGwToken.contains(",")) {
                synchronized(devxGwToken) {
                    if (devGwTokens == null) {
                        devGwTokens = devxGwToken.split(",").toHashSet()
                    }
                }
            }
            if (devGwTokens?.contains(devxToken) != true) {
                throw PermissionForbiddenException(message = "仅支持云桌面发起的请求")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DeskTopApiAspect::class.java)
    }
}
