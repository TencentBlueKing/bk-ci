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

package com.tencent.devops.plugin.codecc

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.plugin.codecc.pojo.CodeccMeasureInfo
import java.net.URLEncoder
import javax.ws.rs.HttpMethod
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

@Suppress("ALL")
open class CodeccApi constructor(
    private val codeccApiUrl: String,
    private val codeccApiProxyUrl: String,
    private val codeccHost: String,
    private val codeccGrayProjectId: String? = null
) {

    companion object {
        private val objectMapper = JsonUtil.getObjectMapper()
        private val logger = LoggerFactory.getLogger(CodeccApi::class.java)
        private const val CONTENT_TYPE = "Content-Type"
        private const val CONTENT_TYPE_JSON = "application/json"
    }

    private fun taskExecution(
        body: Map<String, Any>,
        path: String,
        headers: Map<String, String>? = null,
        method: String = "GET"
    ): String {
        val jsonBody = objectMapper.writeValueAsString(body)
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody
        )

        val builder = Request.Builder()
            .url(getExecUrl(path))

        when (method) {
            "GET" -> {
            }
            "POST" -> {
                builder.post(requestBody)
            }
            "DELETE" -> {
                builder.delete(requestBody)
            }
            "PUT" -> {
                builder.put(requestBody)
            }
        }

        if (headers != null && headers.isNotEmpty()) {
            headers.forEach { (t, u) ->
                builder.addHeader(t, u)
            }
        }

        val request = builder.build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body!!.string()
            if (!response.isSuccessful) {
                throw RemoteServiceException("Fail to invoke codecc request")
            }
            logger.info("Get the task response body - $responseBody")
            return responseBody
        }
    }

    private fun getExecUrl(path: String): String {
        val execUrl = if (codeccApiProxyUrl.isBlank()) {
            codeccApiUrl + path
        } else {
            "$codeccApiProxyUrl?url=${URLEncoder.encode(codeccApiUrl + path, "UTF-8")}"
        }
        logger.info("taskExecution url: $execUrl")
        return execUrl
    }

    fun installCheckerSet(projectId: String, userId: String, type: String, checkerSetId: String): Result<Boolean> {
        val headers = mapOf(
            AUTH_HEADER_DEVOPS_PROJECT_ID to projectId,
            AUTH_HEADER_DEVOPS_USER_ID to userId
        )
        val body = mapOf(
            "type" to type,
            "projectId" to projectId
        )
        val result = taskExecution(
            body = body,
            path = "/ms/defect/api/service/checkerSet/$checkerSetId/relationships",
            headers = headers,
            method = "POST"
        )
        return objectMapper.readValue(result)
    }

    private fun generateCodeccHeaders(
        repoId: String,
        buildId: String?
    ): MutableMap<String, String> {
        val headers = mutableMapOf("repoId" to repoId)
        headers[CONTENT_TYPE] = CONTENT_TYPE_JSON
        if (null != buildId) headers["buildId"] = buildId
        return headers
    }

    fun getCodeccMeasureInfo(repoId: String, buildId: String? = null): Result<CodeccMeasureInfo?> {
        val result = taskExecution(
            body = mapOf(),
            headers = generateCodeccHeaders(repoId, buildId),
            path = "/ms/defect/api/service/defect/repo/measurement",
            method = HttpMethod.GET
        )
        return objectMapper.readValue(result)
    }

    fun getCodeccTaskStatusInfo(repoId: String, buildId: String? = null): Result<Int> {
        val result = taskExecution(
            body = mapOf(),
            headers = generateCodeccHeaders(repoId, buildId),
            path = "/ms/task/api/service/task/repo/status",
            method = HttpMethod.GET
        )
        return objectMapper.readValue(result)
    }

    fun startCodeccTask(repoId: String, commitId: String? = null): Result<String> {
        val result = taskExecution(
            body = mapOf(),
            path = "/ms/task/api/service/openScan/trigger/repo",
            headers = generateCodeccHeaders(repoId, commitId),
            method = HttpMethod.POST
        )
        return objectMapper.readValue(result)
    }

    fun createCodeccPipeline(repoId: String, languages: List<String>): Result<Boolean> {
        val result = taskExecution(
            body = mapOf("langs" to languages),
            path = "/ms/task/api/service/task/repo/create",
            headers = generateCodeccHeaders(repoId, null),
            method = HttpMethod.POST
        )
        return objectMapper.readValue(result)
    }

    fun getCodeccOpensourceMeasurement(atomCodeSrc: String): Result<Map<String, Any>> {
        val url = "http://$codeccHost/ms/defect/api/service/defect/opensource/measurement?url=$atomCodeSrc"
        val headers = mutableMapOf<String, String>()
        if (!codeccGrayProjectId.isNullOrBlank()) {
            headers[AUTH_HEADER_PROJECT_ID] = codeccGrayProjectId
        }
        val httpReq = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .get()
            .build()
        OkhttpUtils.doHttp(httpReq).use { response ->
            val body = response.body!!.string()
            logger.info("codecc opensource measurement response: $body")
            if (!response.isSuccessful) {
                throw RemoteServiceException(
                    errorCode = response.code,
                    errorMessage = "get codecc opensource measurement response fail.$body"
                )
            }
            return objectMapper.readValue(body)
        }
    }
}
