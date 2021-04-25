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

package com.tencent.bkrepo.auth.util

import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

object HttpUtils {
    fun doRequest(
        okHttpClient: OkHttpClient,
        request: Request,
        retry: Int = 0,
        acceptCode: Set<Int> = setOf()
    ): ApiResponse {
        try {
            val response = okHttpClient.newBuilder().build().newCall(request).execute()
            val responseCode = response.code()
            val responseContent = response.body()!!.string()
            if (response.isSuccessful || acceptCode.contains(responseCode)) {
                return ApiResponse(responseCode, responseContent)
            }
            logger.warn("http request failed, code: $responseCode, responseContent: $responseContent")
        } catch (e: Exception) {
            if (retry > 0) {
                logger.warn("http request error, cause: ${e.message}")
            } else {
                logger.error("http request error", e)
            }
        }
        if (retry > 0) {
            Thread.sleep(500)
            return doRequest(okHttpClient, request, retry - 1, acceptCode)
        } else {
            throw RuntimeException("http request error")
        }
    }

    private val logger = LoggerFactory.getLogger(HttpUtils::class.java)
}
