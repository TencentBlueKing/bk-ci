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

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.client.consul.ConsulConstants.PROJECT_TAG_REDIS_KEY
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.openapi.IgnoreProjectId
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.openapi.service.op.AppCodeService
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before

import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Aspect
@Component
class ApiAspect(
    private val appCodeService: AppCodeService,
    private val apiGatewayUtil: ApiGatewayUtil,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiAspect::class.java)
    }

    @Value("\${openapi.verify.project: #{null}}")
    val verifyProjectFlag: String = "false"

    /**
     * 前置增强：目标方法执行之前执行
     *
     * @param jp
     */
    // 所有controller包下面的所有方法的所有参数
    @Before("execution(* com.tencent.devops.openapi.resources.apigw..*.*(..))")
    @Suppress("ComplexMethod")
    fun beforeMethod(jp: JoinPoint) {
        if (!apiGatewayUtil.isAuth()) {
            return
        }
        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        var projectId: String? = null
        var appCode: String? = null
        var apigwType: String? = null

        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "projectId" -> projectId = parameterValue[index]?.toString()
                "gitProjectId" -> projectId = "git_" + parameterValue[index]?.toString()
                "projectCode" -> projectId = parameterValue[index]?.toString()
                "appCode" -> appCode = parameterValue[index]?.toString()
                "apigwType" -> apigwType = parameterValue[index]?.toString()
                else -> Unit
            }
        }

        if (logger.isDebugEnabled) {

            val methodName: String = jp.signature.name
            logger.debug("【前置增强】the method 【{}】", methodName)

            parameterNames.forEach {
                logger.debug("参数名[{}]", it)
            }

            parameterValue.forEach {
                logger.debug("参数值[{}]", it)
            }
            logger.debug("ApiAspect|apigwType[$apigwType],appCode[$appCode],projectId[$projectId]")
        }

        if (projectId.isNullOrEmpty()) {
            logger.info("${jp.signature.name} miss projectId")
            val ignoreProjectId = (jp.signature as MethodSignature).method.getAnnotation(IgnoreProjectId::class.java)
            // 设置开关 若打开，则直接报错。否则只打日志标记
            if (ignoreProjectId == null || !ignoreProjectId.ignore) {
                logger.warn("${(jp.signature as MethodSignature)} miss projectId and miss @IgnoreProjectId")
                if (verifyProjectFlag.contains("true")) {
                    throw PermissionForbiddenException(
                        message = "interface miss projectId and miss @IgnoreProjectId"
                    )
                }
            }
        }

        if (projectId != null && appCode != null && (apigwType == "apigw-app")) {
            if (!appCodeService.validAppCode(appCode, projectId)) {
                throw PermissionForbiddenException(
                    message = "Permission denied: apigwType[$apigwType],appCode[$appCode],ProjectId[$projectId]"
                )
            }
        }

        if (projectId != null) {
            // openAPI 网关无法判别项目信息, 切面捕获project信息。 剩余一种URI内无${projectId}的情况,接口自行处理
            val projectConsulTag = redisOperation.hget(PROJECT_TAG_REDIS_KEY, projectId)
            if (!projectConsulTag.isNullOrEmpty()) {
                bkTag.setGatewayTag(projectConsulTag)
            }
        }
    }

    /**
     * 后置增强：目标方法执行之前执行
     *
     */
    // 所有controller包下面的所有方法的所有参数
    @Before("execution(* com.tencent.devops.openapi.resources.apigw..*.*(..))")
    fun afterMethod() {
        // 删除线程ThreadLocal数据,防止线程池复用。导致流量指向被污染
        bkTag.removeGatewayTag()
    }
}
