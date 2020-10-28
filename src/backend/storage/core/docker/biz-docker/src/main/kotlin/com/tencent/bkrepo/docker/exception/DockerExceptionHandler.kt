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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.exception

import com.netflix.client.ClientException
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.service.exception.ExternalErrorCodeException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.docker.constant.AUTH_CHALLENGE_SERVICE_SCOPE
import com.tencent.bkrepo.docker.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.docker.constant.ERROR_MESSAGE
import com.tencent.bkrepo.docker.constant.REGISTRY_SERVICE
import com.tencent.bkrepo.docker.errors.DockerV2Errors
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.ws.rs.core.MediaType

/**
 * docker repo exception handler
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class DockerExceptionHandler {

    @Value("\${auth.url}")
    private var authUrl: String = StringPool.EMPTY

    @ExceptionHandler(ExternalErrorCodeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleExternalErrorCodeException(exception: ExternalErrorCodeException): ResponseEntity<Any> {
        logger.warn("failed with external error code exception:[${exception.errorCode}-${exception.errorMessage}]")
        return DockerV2Errors.internalError(exception.errorMessage)
    }

    @ExceptionHandler(ErrorCodeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleErrorCodeException(exception: ErrorCodeException): ResponseEntity<Any> {
        logger.warn("failed with error code exception:[${exception.message}]")
        return DockerV2Errors.internalError(exception.message)
    }

    @ExceptionHandler(ClientException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleClientException(exception: ClientException): ResponseEntity<Any> {
        logger.error("failed with client exception:[$exception]", exception)
        return DockerV2Errors.internalError(exception.errorMessage)
    }

    @ExceptionHandler(HystrixRuntimeException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleHystrixRuntimeException(exception: HystrixRuntimeException): ResponseEntity<Any> {
        logger.error("failed with hystrix exception:[${exception.failureType}-${exception.message}]", exception)
        return DockerV2Errors.internalError(exception.message)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleException(exception: AuthenticationException) {
        logger.warn("Failed with authentication exception:[${exception.message}]")
        val response = HttpContextHolder.getResponse()
        val scopeStr = "repository:*/*/tb:push,pull"
        response.setHeader(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, AUTH_CHALLENGE_SERVICE_SCOPE.format(authUrl, REGISTRY_SERVICE, scopeStr))
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON
        response.writer.print(ERROR_MESSAGE.format("UNAUTHORIZED", "authentication required", "BAD_CREDENTIAL"))
        response.writer.flush()
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(exception: Exception): ResponseEntity<Any> {
        logger.error("Failed with other exception:[${exception.message}]", exception)
        return DockerV2Errors.internalError(exception.message)
    }

    @ExceptionHandler(DockerRepoNotFoundException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleExternalDockerRepoNotFoundException(exception: DockerRepoNotFoundException): ResponseEntity<Any> {
        logger.warn("failed with repo not found exception:[${exception.message}]")
        return DockerV2Errors.repoInvalid(exception.message!!)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }
}
