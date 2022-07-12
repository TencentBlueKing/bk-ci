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

package com.tencent.bkrepo.npm.exception

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.security.constant.BASIC_AUTH_PROMPT
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.pojo.NpmErrorResponse
import com.tencent.bkrepo.npm.pojo.auth.AuthFailInfo
import com.tencent.bkrepo.npm.pojo.auth.NpmAuthFailResponse
import com.tencent.bkrepo.npm.pojo.auth.NpmAuthResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.concurrent.ExecutionException

/**
 * 统一异常处理
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice("com.tencent.bkrepo.npm")
class NpmExceptionHandler {

    @ExceptionHandler(NpmBadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerBadRequestException(exception: NpmBadRequestException) {
        val responseObject = NpmErrorResponse("bad request", exception.message)
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmRepoNotFoundException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerRepoNotFoundException(exception: NpmRepoNotFoundException) {
        val responseObject = NpmErrorResponse("bad request", exception.message)
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(ExecutionException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerExecutionException(exception: ExecutionException) {
        val responseObject = NpmErrorResponse("execution exception", exception.message.orEmpty())
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmArgumentResolverException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handlerNpmArgumentResolverException(exception: NpmArgumentResolverException) {
        val fail = NpmAuthResponse.fail(exception.message)
        npmResponse(fail, exception)
    }

    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handlerClientAuthException(exception: AuthenticationException) {
        val responseObject = NpmErrorResponse("Unauthorized", "Authentication required")
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmClientAuthException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handlerNpmClientAuthException(exception: NpmClientAuthException) {
        val responseObject = NpmErrorResponse("Unauthorized", "Authentication required")
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmLoginFailException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handlerNpmLoginFailException(exception: NpmLoginFailException) {
        val fail = NpmAuthResponse.fail(exception.message)
        npmResponse(fail, exception)
    }

    @ExceptionHandler(NpmTokenIllegalException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handlerNpmTokenIllegalException(exception: NpmTokenIllegalException) {
        HttpContextHolder.getResponse().setHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC_AUTH_PROMPT)
        val authFailInfo = AuthFailInfo(
            HttpStatus.UNAUTHORIZED.value(),
            exception.message
        )
        val npmAuthFailResponse =
            NpmAuthFailResponse(errors = listOf(authFailInfo))
        npmResponse(npmAuthFailResponse, exception)
    }

    @ExceptionHandler(NpmArtifactNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlerNpmArtifactNotFoundException(exception: NpmArtifactNotFoundException) {
        val responseObject = NpmErrorResponse.notFound()
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmArgumentNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlerNpmArgumentNotFoundException(exception: NpmArgumentNotFoundException) {
        val responseObject = NpmErrorResponse.notFound()
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmArtifactExistException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handlerNpmArtifactExistException(exception: NpmArtifactExistException) {
        val responseObject = NpmErrorResponse(exception.message, "forbidden")
        npmResponse(responseObject, exception)
    }

    @ExceptionHandler(NpmTagNotExistException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlerNpmTagNotExistException(exception: NpmTagNotExistException) {
        val responseObject = NpmErrorResponse("not found", exception.message)
        npmResponse(responseObject, exception)
    }

    private fun npmResponse(responseObject: NpmErrorResponse, exception: NpmException) {
        logNpmException(exception)
        val responseString = JsonUtils.objectMapper.writeValueAsString(responseObject)
        val response = HttpContextHolder.getResponse()
        response.contentType = "application/json; charset=utf-8"
        response.writer.println(responseString)
    }

    private fun npmResponse(npmAuthResponse: NpmAuthResponse<Void>, exception: NpmException) {
        logNpmException(exception)
        val responseString = JsonUtils.objectMapper.writeValueAsString(npmAuthResponse)
        val response = HttpContextHolder.getResponse()
        response.contentType = "application/json; charset=utf-8"
        response.writer.println(responseString)
    }

    private fun npmResponse(responseObject: NpmErrorResponse, exception: Exception) {
        logNpmException(exception)
        val responseString = JsonUtils.objectMapper.writeValueAsString(responseObject)
        val response = HttpContextHolder.getResponse()
        response.contentType = "application/json; charset=utf-8"
        response.writer.println(responseString)
    }

    private fun npmResponse(npmAuthFailResponse: NpmAuthFailResponse, exception: NpmException) {
        logNpmException(exception)
        val responseString = JsonUtils.objectMapper.writeValueAsString(npmAuthFailResponse)
        val response = HttpContextHolder.getResponse()
        response.contentType = "application/json; charset=utf-8"
        response.writer.println(responseString)
    }

    private fun logNpmException(exception: Exception) {
        val userId = HttpContextHolder.getRequest().getAttribute(USER_KEY) ?: ANONYMOUS_USER
        val uri = HttpContextHolder.getRequest().requestURI
        logger.warn("User[$userId] access [$uri] failed[${exception.javaClass.simpleName}]: ${exception.message}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmExceptionHandler::class.java)
    }
}
