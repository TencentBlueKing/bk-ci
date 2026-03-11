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
 *
 */

package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.service.ScmProxy
import com.tencent.devops.scm.api.ServiceScmApiProxyResource
import com.tencent.devops.scm.api.exception.ScmApiException
import com.tencent.devops.scm.pojo.ScmApiParameter
import com.tencent.devops.scm.pojo.ScmApiRequest
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service

/**
 * scm api接口代理
 */
@Aspect
@Service
class ScmApiManagerProxyAspect @Autowired constructor(
    private val client: Client
) {

    @Around("@within(com.tencent.devops.repository.service.ScmProxy)")
    @Throws(Throwable::class)
    fun aroundAdvice(pjp: ProceedingJoinPoint): Any? {
        val parameterValues = pjp.args
        val properties = parameterValues.find { it is ScmProviderProperties }?.let { it as ScmProviderProperties }
            ?: throw ErrorCodeException(errorCode = "")
        return try {
            if (properties.proxyEnabled == true) {
                proxy(pjp)
            } else {
                pjp.proceed()
            }
        } catch (ignored: ScmApiException) {
            throw RemoteServiceException(
                errorCode = ignored.statusCode,
                errorMessage = ignored.message ?: ""
            )
        }
    }

    fun proxy(pjp: ProceedingJoinPoint): Any? {
        val scmProxy = AnnotationUtils.findAnnotation(pjp.target::class.java, ScmProxy::class.java)!!
        val serviceName = scmProxy.value.ifBlank { pjp.target::class.java.simpleName }

        // 转换方法参数
        val signature = pjp.signature as MethodSignature
        val method = signature.method
        val methodName = signature.name
        val parameterValues = pjp.args
        val parameterNames = signature.parameterNames
        val parameterTypes = method.parameterTypes
        val genericParameterTypes = method.genericParameterTypes
        val parameters = parameterNames.mapIndexed { index, name ->
            val parameterType = parameterTypes[index]
            ScmApiParameter(
                parameterIndex = index,
                parameterName = name,
                parameterType = parameterType,
                parameterValue = parameterValues[index]?.let {
                    if (ReflectUtil.isPrimitiveOrStringType(parameterType)) {
                        it
                    } else {
                        JsonUtil.toJsonForType(it, genericParameterTypes[index])
                    }
                }
            )
        }

        val request = ScmApiRequest(
            serviceName = serviceName,
            methodName = signature.name,
            parameters = parameters
        )
        logger.info("proxy scm api request|serviceName:$serviceName|method:$methodName")
        val data = client.getScm(ServiceScmApiProxyResource::class).proxy(
            serviceName = serviceName,
            methodName = methodName,
            request = request
        ).data
        return data?.let {
            if (ReflectUtil.isPrimitiveOrStringType(method.returnType)) {
                it
            } else {
                JsonUtil.toForType(it.toString(), signature.method.genericReturnType)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmApiManagerProxyAspect::class.java)
    }
}
