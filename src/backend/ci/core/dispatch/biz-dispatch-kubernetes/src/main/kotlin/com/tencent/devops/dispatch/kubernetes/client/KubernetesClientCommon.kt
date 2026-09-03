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

package com.tencent.devops.dispatch.kubernetes.client

import com.tencent.devops.dispatch.kubernetes.interfaces.CommonService
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class KubernetesClientCommon @Autowired constructor(
    private val commonService: CommonService
) {

    companion object {
        private const val TOKEN_KEY = "Devops-Token"
        private const val USER_ID_KEY = "X-DEVOPS-UID"
        private const val PROJECT_ID_KEY = "X-DEVOPS-PROJECT-ID"
        private const val TENANT_ID_KEY = "X-DEVOPS-TENANT-ID"
    }

    @Value("\${kubernetes.token}")
    val kubernetesToken: String = ""

    @Value("\${kubernetes.apiUrl}")
    val kubernetesApiUrl: String = ""

    fun baseRequest(
        userId: String,
        url: String,
        headers: Map<String, String>? = null,
        projectId: String = ""
    ): Request.Builder {
        return Request.Builder().url(commonService.getProxyUrl(kubernetesApiUrl + url)).headers(
            headers(identityHeaders(userId, projectId) + (headers ?: emptyMap()))
        )
    }

    fun microBaseRequest(
        url: String,
        headers: Map<String, String>? = null,
        userId: String = "",
        projectId: String = ""
    ): Request.Builder {
        return Request.Builder().url(kubernetesApiUrl + url).headers(
            headers(identityHeaders(userId, projectId) + (headers ?: emptyMap()))
        )
    }

    fun identityHeaders(userId: String, projectId: String = "", tenantId: String = ""): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (userId.isNotBlank()) {
            result[USER_ID_KEY] = userId
        }
        if (projectId.isNotBlank()) {
            result[PROJECT_ID_KEY] = projectId
        }
        if (tenantId.isNotBlank()) {
            result[TENANT_ID_KEY] = tenantId
        }
        return result
    }

    fun headers(otherHeaders: Map<String, String>? = null): Headers {
        val result = mutableMapOf<String, String>()

        val headers = mapOf(TOKEN_KEY to kubernetesToken)
        result.putAll(headers)

        if (!otherHeaders.isNullOrEmpty()) {
            result.putAll(otherHeaders)
        }

        return result.toHeaders()
    }
}
