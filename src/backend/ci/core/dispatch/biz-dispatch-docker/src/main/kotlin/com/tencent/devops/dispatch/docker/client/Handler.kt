/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.dispatch.docker.client

import com.tencent.devops.dispatch.docker.client.context.HandlerContext
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import okhttp3.Request

abstract class Handler<T : HandlerContext> {
    protected var nextHandler = ThreadLocal<Handler<T>?>()

    abstract fun handlerRequest(handlerContext: T)

    fun setNextHandler(handler: Handler<T>): Handler<T> {
        this.nextHandler.set(handler)
        return this
    }

    fun getDockerHostProxyRequest(
        hostIp: String,
        hostPort: Int,
        hostUri: String
    ): Request.Builder {
        val url = if (hostIp.isBlank() || hostPort == 0) {
            throw DockerServiceException(
                errorType = ErrorCodeEnum.DOCKER_IP_NOT_AVAILABLE.errorType,
                errorCode = ErrorCodeEnum.DOCKER_IP_NOT_AVAILABLE.errorCode,
                errorMsg = "Docker IP: $hostIp is not available."
            )
        } else {
            "http://$hostIp:$hostPort$hostUri"
        }

        return Request.Builder().url(url)
            .addHeader("Accept", "application/json; charset=utf-8")
            .addHeader("Content-Type", "application/json; charset=utf-8")
    }
}
