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

package com.tencent.bkrepo.oci.exception

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.oci.artifact.auth.OciLoginAuthHandler
import com.tencent.bkrepo.oci.config.OciProperties
import com.tencent.bkrepo.oci.constant.UNAUTHORIZED_CODE
import com.tencent.bkrepo.oci.constant.UNAUTHORIZED_DESCRIPTION
import com.tencent.bkrepo.oci.constant.UNAUTHORIZED_MESSAGE
import com.tencent.bkrepo.oci.pojo.response.OciErrorResponse
import com.tencent.bkrepo.oci.pojo.response.OciResponse
import javax.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice("com.tencent.bkrepo.oci")
class OciExceptionHandler(
    private val ociProperties: OciProperties
) {

/**
     * 单独处理认证失败异常，需要添加WWW_AUTHENTICATE响应头触发浏览器登录
     */
    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleException(exception: AuthenticationException) {
        val response = HttpContextHolder.getResponse()
        response.contentType = MediaTypes.APPLICATION_JSON
        response.addHeader(
            HttpHeaders.WWW_AUTHENTICATE,
            OciLoginAuthHandler.AUTH_CHALLENGE_SERVICE_SCOPE.format(
                ociProperties.authUrl,
                OciLoginAuthHandler.REGISTRY_SERVICE,
                OciLoginAuthHandler.SCOPE_STR
            )
        )
        val responseObject = OciErrorResponse(UNAUTHORIZED_MESSAGE, UNAUTHORIZED_CODE, UNAUTHORIZED_DESCRIPTION)
        ociResponse(responseObject, exception, response)
    }

    @ExceptionHandler(OciRepoNotFoundException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerRepoNotFoundException(exception: OciRepoNotFoundException) {
        val responseObject = OciErrorResponse(exception.message, exception.code, exception.detail)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(OciFileNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlerOciFileNotFoundException(exception: OciFileNotFoundException) {
        val responseObject = OciErrorResponse(exception.message, exception.code, exception.detail)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(OciBadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: OciBadRequestException) {
        val responseObject = OciErrorResponse(exception.message, exception.code, exception.detail)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(OciForbiddenRequestException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(exception: OciForbiddenRequestException) {
        val responseObject = OciErrorResponse(exception.message, exception.code, exception.detail)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(OciFileAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: OciFileAlreadyExistsException) {
        val responseObject = OciErrorResponse(exception.message, exception.code, exception.detail)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(ArtifactNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleException(exception: ArtifactNotFoundException) {
        val responseObject = OciErrorResponse(exception.message, exception.messageCode, null)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(ErrorCodeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: ErrorCodeException) {
        val responseObject = OciErrorResponse(exception.message, exception.messageCode, null)
        ociResponse(responseObject, exception)
    }

    @ExceptionHandler(PermissionException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleException(exception: PermissionException) {
        val responseObject = OciErrorResponse(exception.message, exception.messageCode, null)
        ociResponse(responseObject, exception)
    }

    private fun ociResponse(
        responseObject: OciErrorResponse,
        exception: Exception,
        response: HttpServletResponse? = null
    ) {
        logOciException(exception)
        val responseString = JsonUtils.objectMapper.writeValueAsString(OciResponse.errorResponse(responseObject))
        val httpResponse = if (response == null) {
            val temp = HttpContextHolder.getResponse()
            temp.contentType = MediaTypes.APPLICATION_JSON
            temp
        } else {
            response
        }
        httpResponse.writer.println(responseString)
    }

    private fun logOciException(exception: Exception) {
        val userId = HttpContextHolder.getRequest().getAttribute(USER_KEY) ?: ANONYMOUS_USER
        val uri = HttpContextHolder.getRequest().requestURI
        logger.warn(
            "User[$userId] access oci resource[$uri] failed[${exception.javaClass.simpleName}]: ${exception.message}"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciExceptionHandler::class.java)
    }
}
