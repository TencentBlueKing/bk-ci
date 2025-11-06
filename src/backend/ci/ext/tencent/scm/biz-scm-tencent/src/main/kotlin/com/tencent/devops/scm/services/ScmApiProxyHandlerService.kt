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

package com.tencent.devops.scm.services

import com.tencent.devops.common.api.constant.HttpStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.service.ScmProxy
import com.tencent.devops.scm.api.exception.ScmApiException
import com.tencent.devops.scm.pojo.ScmApiRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import java.util.concurrent.ConcurrentHashMap

@Service
class ScmApiProxyHandlerService : BeanPostProcessor {

    private val apiHandlerMap = ConcurrentHashMap<String, Any>()

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        val scmProxy = AnnotationUtils.findAnnotation(bean::class.java, ScmProxy::class.java)
        if (scmProxy != null) {
            val serviceName = scmProxy.value.ifBlank { bean::class.java.simpleName }
            logger.info("load scm api proxy handler $serviceName")
            apiHandlerMap[serviceName] = bean
        }
        return bean
    }

    @SuppressWarnings("NestedBlockDepth")
    fun invoke(request: ScmApiRequest): Any? {
        with(request) {
            logger.info("handle scm api request|serviceName:$serviceName|methodName:$methodName")
            val handler = apiHandlerMap[serviceName] ?: throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_SCM_PROXY_SERVICE_NOT_FOUND,
                params = arrayOf(serviceName)
            )
            val parameterTypes = parameters.map { it.parameterType }
            val method = ReflectionUtils.findMethod(
                handler::class.java, methodName, *parameterTypes.toTypedArray()
            ) ?: throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_SCM_PROXY_SERVICE_METHOD_NOT_FOUND,
                params = arrayOf(serviceName, methodName)
            )
            val genericParameterTypes = method.genericParameterTypes
            val parameterValues: List<Any?> = parameters.mapIndexed { index, parameter ->
                parameter.parameterValue?.let {
                    if (ReflectUtil.isPrimitiveOrStringType(parameter.parameterType)) {
                        it
                    } else {
                        JsonUtil.toForType(it.toString(), genericParameterTypes[index])
                    }
                }
            }
            return try {
                ReflectionUtils.invokeMethod(method, handler, *parameterValues.toTypedArray())?.let {
                    if (ReflectUtil.isPrimitiveOrStringType(method.returnType)) {
                        it
                    } else {
                        JsonUtil.toJsonForType(it, method.genericReturnType)
                    }
                }
            } catch (ignored: ScmApiException) {
                throw RemoteServiceException(
                    httpStatus = ignored.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR.value,
                    errorMessage = ignored.message ?: ""
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmApiProxyHandlerService::class.java)
    }
}
