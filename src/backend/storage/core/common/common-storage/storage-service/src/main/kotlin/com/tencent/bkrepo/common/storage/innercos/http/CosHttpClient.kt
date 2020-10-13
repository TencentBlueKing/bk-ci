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

package com.tencent.bkrepo.common.storage.innercos.http

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

object CosHttpClient {
    private val logger = LoggerFactory.getLogger(CosHttpClient::class.java)

    private val client = OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun <T> execute(request: Request, handler: HttpResponseHandler<T>): T {
        val response = try {
            client.newCall(request).execute()
        } catch (exception: IOException) {
            val message = buildMessage(request)
            throw RuntimeException("Failed to execute http request: $message", exception)
        }

        response.useOnCondition(!handler.keepConnection()) {
            try {
                if (it.isSuccessful) {
                    return handler.handle(it)
                } else {
                    if (it.code() == 404) {
                        val handle404Result = handler.handle404()
                        if (handle404Result != null) {
                            return handle404Result
                        }
                    }
                    throw RuntimeException("Response status error")
                }
            } catch (exception: Exception) {
                val message = buildMessage(request, it)
                throw RuntimeException("Failed to execute http request: $message", exception)
            }
        }
    }

    private fun buildMessage(request: Request, response: Response? = null): String {
        val requestTitle = "${request.method()} ${request.url()} ${response?.protocol()}"

        val builder = StringBuilder()
            .append(">>>> ")
            .appendln(requestTitle)
            .appendln(request.headers())

        if (response != null) {
            builder.append("<<<< ")
                .append(requestTitle)
                .append(response.code())
                .appendln("[${response.message()}]")
                .appendln(response.headers())
                .appendln(response.body()?.bytes()?.toString(Charset.forName("GB2312")))
        }
        val message = builder.toString()
        logger.warn(message)
        return message
    }
}
