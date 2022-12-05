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

package com.tencent.bkrepo.common.service.exception

import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.condition.ConditionalOnMicroService
import com.tencent.bkrepo.common.service.log.LoggerHolder.logException
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 服务调用异常处理
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnMicroService
@RestControllerAdvice
class ServiceExceptionHandler {

    @ExceptionHandler(RemoteErrorCodeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: RemoteErrorCodeException): Response<Void> {
        logException(exception, "[${exception.methodKey}][${exception.errorCode}]${exception.errorMessage}", false)
        return ResponseBuilder.fail(exception.errorCode, exception.errorMessage.orEmpty())
    }

    @ExceptionHandler(NoFallbackAvailableException::class)
    fun handleException(exception: NoFallbackAvailableException): ResponseEntity<Response<Void>> {
        val cause = exception.cause
        if (cause is RemoteErrorCodeException) {
            return ResponseEntity(handleException(cause), HttpStatus.BAD_REQUEST)
        }

        val messageCode = if (cause is CallNotPermittedException) {
            CommonMessageCode.SERVICE_CIRCUIT_BREAKER
        } else {
            CommonMessageCode.SERVICE_CALL_ERROR
        }

        logException(exception, "${exception.message} Cause: ${cause?.message}", true)

        val errorMessage = LocaleMessageUtils.getLocalizedMessage(messageCode, null)
        val response = ResponseBuilder.fail(messageCode.getCode(), errorMessage)
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
