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

package com.tencent.bkrepo.common.service.log

import com.tencent.bkrepo.common.api.constant.ACCESS_LOGGER_NAME
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.EXCEPTION_LOGGER_NAME
import com.tencent.bkrepo.common.api.constant.JOB_LOGGER_NAME
import com.tencent.bkrepo.common.api.constant.MS_REQUEST_KEY
import com.tencent.bkrepo.common.api.constant.PLATFORM_KEY
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

object LoggerHolder {

    /**
     * 异常logger
     */
    val exceptionLogger: Logger = LoggerFactory.getLogger(EXCEPTION_LOGGER_NAME)

    /**
     * Job logger
     */
    val jobLogger: Logger = LoggerFactory.getLogger(JOB_LOGGER_NAME)

    /**
     * Access logger
     */
    val accessLogger: Logger = LoggerFactory.getLogger(ACCESS_LOGGER_NAME)

    fun logErrorCodeException(exception: ErrorCodeException, message: String) {
        val systemError = exception.status.isServerError()
        logException(exception, message, systemError)
    }

    fun logException(exception: Exception, message: String?, systemError: Boolean) {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val userId = request?.getAttribute(USER_KEY) ?: ANONYMOUS_USER
        val platformId = request?.getAttribute(PLATFORM_KEY)
        val principal = platformId?.let { "$it-$userId" } ?: userId
        val channel = determineAccessChannel(request)
        val uri = request?.requestURI
        val method = request?.method
        val exceptionMessage = message ?: exception.message.orEmpty()
        val exceptionName = exception.javaClass.simpleName
        val cause = if (exception is ErrorCodeException && exception.cause != null) {
            exception.cause
        } else {
            exception
        }
        val fullMessage = "User[$principal] $method [$uri] from [$channel] failed[$exceptionName]: $exceptionMessage"
        if (systemError) {
            exceptionLogger.error(fullMessage, cause)
        } else {
            exceptionLogger.warn(fullMessage)
        }
    }

    private fun determineAccessChannel(request: HttpServletRequest?): String {
        return when {
            request == null -> "None"
            request.getAttribute(MS_REQUEST_KEY) != null -> "MicroService"
            else -> "Api"
        }
    }
}
