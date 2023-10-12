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

package com.tencent.devops.dispatch.bcs.client

import com.tencent.devops.dispatch.kubernetes.interfaces.CommonService
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BcsClientCommon @Autowired constructor(
    private val commonService: CommonService
) {

    companion object {
        private const val BCS_TOKEN_KEY = "BK-Devops-Token"
    }

    @Value("\${bcs.token}")
    val bcsToken: String = ""

    @Value("\${bcs.apiUrl}")
    val bcsApiUrl: String = ""

    fun baseRequest(userId: String, url: String, headers: Map<String, String>? = null): Request.Builder {
        return Request.Builder().url(commonService.getProxyUrl(bcsApiUrl + url)).headers(headers(headers))
    }

    fun headers(otherHeaders: Map<String, String>? = null): Headers {
        val result = mutableMapOf<String, String>()

        val bcsHeaders = mapOf(BCS_TOKEN_KEY to bcsToken)
        result.putAll(bcsHeaders)

        if (!otherHeaders.isNullOrEmpty()) {
            result.putAll(otherHeaders)
        }

        return result.toHeaders()
    }
}
