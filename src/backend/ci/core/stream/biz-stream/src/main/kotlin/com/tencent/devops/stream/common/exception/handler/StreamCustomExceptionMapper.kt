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

package com.tencent.devops.stream.common.exception.handler

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkExceptionMapper
import com.tencent.devops.stream.common.exception.StreamCustomException
import org.slf4j.LoggerFactory
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper

/**
 * 针对stream的一些特殊返回定制的 custom response
 */
@BkExceptionMapper
class StreamCustomExceptionMapper : ExceptionMapper<StreamCustomException> {
    companion object {
        val logger = LoggerFactory.getLogger(StreamCustomExceptionMapper::class.java)!!
    }

    override fun toResponse(exception: StreamCustomException): Response {
        logger.warn("StreamCustomExceptionMapper|toResponse|Failed|exception|$exception")
        return Response.status(exception.status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(Result<Void>(exception.status, exception.message ?: "Internal Exception")).build()
    }
}
