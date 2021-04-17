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

package com.tencent.bkrepo.npm.utils

import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OkHttpUtil {

    private val okHttpClient: OkHttpClient by lazy {
        HttpClientBuilderFactory.create().readTimeout(TIMEOUT, TimeUnit.SECONDS).build()
    }

    @Retryable(Exception::class, maxAttempts = 3, backoff = Backoff(delay = 5 * 1000, multiplier = 1.0))
    fun doGet(url: String, headers: Map<String, String> = emptyMap()): Response {
        val requestBuilder = Request.Builder().url(url).get()
        if (headers.isNotEmpty()) {
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()
        if (!checkResponse(response)) {
            throw Exception("request http url: [$url], headers:[$headers] failed")
        }
        return response
    }

    @Recover
    fun recover(exception: Exception, url: String, headers: Map<String, String> = emptyMap()): Response {
        logger.error("recover, retry http send url: [$url], headers:[$headers] failed, exception: $exception")
        throw ArtifactNotFoundException("http send url: [$url] failed.")
    }

    /**
     * 检查下载响应
     */
    private fun checkResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Download file from remote failed: [${response.code()}]")
            return false
        }
        return true
    }

    companion object {
        const val TIMEOUT = 5 * 60L
        val logger: Logger = LoggerFactory.getLogger(OkHttpUtil::class.java)
    }
}
