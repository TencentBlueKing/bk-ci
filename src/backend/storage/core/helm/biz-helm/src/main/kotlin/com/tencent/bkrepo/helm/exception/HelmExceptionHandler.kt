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

package com.tencent.bkrepo.helm.exception

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.security.constant.BASIC_AUTH_PROMPT
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.helm.pojo.HelmErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 统一异常处理
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice("com.tencent.bkrepo.helm")
class HelmExceptionHandler {

    @ExceptionHandler(HelmRepoNotFoundException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerRepoNotFoundException(exception: HelmRepoNotFoundException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(HelmBadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerBadRequestException(exception: HelmBadRequestException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(HelmForbiddenRequestException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handlerBadRequestException(exception: HelmForbiddenRequestException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handlerClientAuthException(exception: AuthenticationException) {
        HttpContextHolder.getResponse().setHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTH_PROMPT)
        val responseObject = HelmErrorResponse(HttpStatus.UNAUTHORIZED.reasonPhrase)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(HelmIndexFreshFailException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerHelmIndexFreshFailException(exception: HelmIndexFreshFailException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(HelmFileAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handlerHelmFileAlreadyExistsException(exception: HelmFileAlreadyExistsException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(HelmErrorInvalidProvenanceFileException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerHelmErrorInvalidProvenanceFileException(exception: HelmErrorInvalidProvenanceFileException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    @ExceptionHandler(HelmFileNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlerHelmFileNotFoundException(exception: HelmFileNotFoundException) {
        val responseObject = HelmErrorResponse(exception.message)
        helmResponse(responseObject, exception)
    }

    private fun helmResponse(responseObject: HelmErrorResponse, exception: Exception) {
        logHelmException(exception)
        val responseString = JsonUtils.objectMapper.writeValueAsString(responseObject)
        val response = HttpContextHolder.getResponse()
        response.contentType = "application/json; charset=utf-8"
        response.writer.println(responseString)
    }

    private fun logHelmException(exception: Exception) {
        val userId = HttpContextHolder.getRequest().getAttribute(USER_KEY) ?: ANONYMOUS_USER
        val uri = HttpContextHolder.getRequest().requestURI
        logger.warn(
            "User[$userId] access helm resource[$uri] failed[${exception.javaClass.simpleName}]: ${exception.message}"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HelmExceptionHandler::class.java)
    }
}
