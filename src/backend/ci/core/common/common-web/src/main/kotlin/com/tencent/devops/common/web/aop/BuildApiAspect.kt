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

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.annotation.BuildApiPermission
import com.tencent.devops.common.web.service.ServiceBuildApiPermissionResource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
class BuildApiAspect constructor(private val client: Client) {

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
        val types = method.getAnnotation(BuildApiPermission::class.java)?.values
        logger.info("[doBefore] the method 【$methodName】")
        types?.forEach {
            when(it) {
                "auth" -> {
                    // 参数value
                    val parameterValue = jp.args
                    // 参数key
                    val parameterNames = (jp.signature as MethodSignature).parameterNames
                    authPermission(parameterNames, parameterValue)
                }
            }
        }

    }

    private fun authPermission(parameterNames: Array<String>, parameterValue: Array<Any>) {
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
        val authBuildId = request.getHeader(AUTH_HEADER_DEVOPS_BUILD_ID)
        val authProjectId = request.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        if (!parameterNames.contains("authProjectId") || !parameterNames.contains("authBuildId")) return
        var projectId: String? = null
        var pipelineId: String? = null
        parameterNames.forEach {
            logger.info("ParamName[$it]")
        }

        parameterValue.forEach {
            logger.info("ParamValue[$it]")
        }
        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "projectId" -> {
                    projectId = parameterValue[index].toString()
                }
                "pipelineId" -> {
                    pipelineId = parameterValue[index].toString()
                }
            }
        }
        logger.info("Build ProjectId[$authProjectId], BuildID[$authBuildId],user project param[$projectId], " +
                "user pipeline param[$pipelineId]")
        if (projectId != null && pipelineId != null) {
            val buildTriggerUser = client.get(ServiceBuildApiPermissionResource::class)
                .getTriggerUser(authProjectId!!, authBuildId!!).data!!
            logger.info("verify that user [$buildTriggerUser] has permission to access information " +
                    "in pipeline [$pipelineId] under project [$projectId].")
            val checkPipelinePermissionResult = client.get(ServiceBuildApiPermissionResource::class).verifyApi(
                userId = buildTriggerUser,
                projectId = projectId,
                pipelineId = pipelineId
            ).data
            if (checkPipelinePermissionResult == true) {
                logger.info("verify that user [$buildTriggerUser] has permission to access information " +
                        "in pipeline [$pipelineId] under project [$projectId].【verify succeed】")
            } else {
                logger.info("verify that user [$buildTriggerUser] has permission to access information " +
                        "in pipeline [$pipelineId] under project [$projectId].【verify fail】")
            }
        }
    }
}
