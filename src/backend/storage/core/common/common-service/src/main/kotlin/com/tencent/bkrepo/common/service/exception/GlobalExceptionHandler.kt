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

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Response
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 全局统一异常处理
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
class GlobalExceptionHandler : AbstractExceptionHandler() {

    @ExceptionHandler(ErrorCodeException::class)
    fun handleException(exception: ErrorCodeException): Response<Void> {
        return response(exception)
    }

    /**
     * 参数处理异常
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleException(exception: MissingServletRequestParameterException): Response<Void> {
        val errorCodeException = ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, exception.parameterName)
        return response(errorCodeException)
    }

    /**
     * 参数处理异常
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleException(exception: HttpMessageNotReadableException): Response<Void> {
        val errorCodeException = ErrorCodeException(CommonMessageCode.REQUEST_CONTENT_INVALID)
        return response(errorCodeException)
    }

    /**
     * header参数异常
     */
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleException(exception: MissingRequestHeaderException): Response<Void> {
        val errorCodeException = ErrorCodeException(CommonMessageCode.HEADER_MISSING, exception.headerName)
        return response(errorCodeException)
    }

    /**
     * 参数处理异常
     */
    @ExceptionHandler(MissingKotlinParameterException::class)
    fun handleException(exception: MissingKotlinParameterException): Response<Void> {
        val parameterName = exception.parameter.name.orEmpty()
        val errorCodeException = ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, parameterName)
        return response(errorCodeException)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleException(exception: HttpRequestMethodNotSupportedException): Response<Void> {
        val errorCodeException = ErrorCodeException(
            HttpStatus.METHOD_NOT_ALLOWED,
            CommonMessageCode.METHOD_NOT_ALLOWED,
            arrayOf(exception.message.orEmpty())
        )
        return response(errorCodeException)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleException(exception: HttpMediaTypeNotSupportedException): Response<Void> {
        val errorCodeException = ErrorCodeException(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            messageCode = CommonMessageCode.MEDIA_TYPE_UNSUPPORTED
        )
        return response(errorCodeException)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): Response<Void> {
        return response(exception)
    }
}
