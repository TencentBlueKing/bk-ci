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
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class KubernetesClientCommon @Autowired constructor(
    private val commonService: CommonService
) {

    companion object {
        private const val TOKEN_KEY = "Devops-Token"
        private const val USER_ID_KEY = "X-DEVOPS-UID"
        private const val PROJECT_ID_KEY = "X-DEVOPS-PROJECT-ID"
        private const val TENANT_ID_KEY = "X-DEVOPS-TENANT-ID"
        private const val IDENTITY_SIG_KEY = "X-DEVOPS-IDENTITY-SIG"
        private const val IDENTITY_TS_KEY = "X-DEVOPS-IDENTITY-TS"
        // 曾经写进仓库的公开串，仅作拒绝名单；不得再当 @Value 兜底。
        private const val PUBLISHED_DEFAULT_IDENTITY_KEY = "bkci-k8s-manager-identity-sig-change-in-prod"
    }

    @Value("\${kubernetes.token}")
    val kubernetesToken: String = ""

    @Value("\${kubernetes.apiUrl}")
    val kubernetesApiUrl: String = ""

    // 仅 dispatch 持有，禁止注入构建容器。Helm 同 namespace 挂 kubernetes-manager-auth。
    // 空配置或公开串不发 SIG，manager 会丢弃自称头。
    @Value("\${kubernetes.identitySigningKey:}")
    val identitySigningKey: String = ""

    fun baseRequest(
        userId: String,
        url: String,
        headers: Map<String, String>? = null,
        projectId: String
    ): Request.Builder {
        return Request.Builder().url(commonService.getProxyUrl(kubernetesApiUrl + url)).headers(
            headers(identityHeaders(userId, projectId) + (headers ?: emptyMap()))
        )
    }

    fun microBaseRequest(
        url: String,
        headers: Map<String, String>? = null,
        userId: String = "",
        projectId: String
    ): Request.Builder {
        return Request.Builder().url(kubernetesApiUrl + url).headers(
            headers(identityHeaders(userId, projectId) + (headers ?: emptyMap()))
        )
    }

    fun identityHeaders(userId: String, projectId: String, tenantId: String = ""): Map<String, String> {
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
        val key = effectiveIdentitySigningKey()
        if (result.isNotEmpty() && key.isNotBlank()) {
            val ts = Instant.now().epochSecond.toString()
            result[IDENTITY_TS_KEY] = ts
            result[IDENTITY_SIG_KEY] = signIdentity(
                userId = result[USER_ID_KEY] ?: "",
                projectId = result[PROJECT_ID_KEY] ?: "",
                tenantId = result[TENANT_ID_KEY] ?: "",
                ts = ts,
                key = key
            )
        }
        return result
    }

    private fun effectiveIdentitySigningKey(): String {
        val key = identitySigningKey.trim()
        if (key.isBlank() || key == PUBLISHED_DEFAULT_IDENTITY_KEY) {
            return ""
        }
        return key
    }

    private fun signIdentity(userId: String, projectId: String, tenantId: String, ts: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val payload = "$userId|$projectId|$tenantId|$ts"
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
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
