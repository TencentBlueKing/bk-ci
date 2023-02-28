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

package com.tencent.devops.dockerhost.dispatch

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_GATEWAY_TAG
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dockerhost.common.Constants
import com.tencent.devops.dockerhost.config.DockerHostConfig
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

@Suppress("ALL")
abstract class AbstractBuildResourceApi constructor(
    private val dockerHostConfig: DockerHostConfig
) {
    companion object {
        private val gateway: String by lazy {
            DockerEnv.getGatway().removePrefix("http://").removePrefix("https://")
        }

        private val buildArgs: Map<String, String> by lazy {
            initBuildArgs()
        }

        private fun initBuildArgs(): Map<String, String> {
            val buildType = BuildEnv.getBuildType()
            val map = mutableMapOf<String, String>()

            map[AUTH_HEADER_DEVOPS_BUILD_TYPE] = buildType.name
            when (buildType) {
                BuildType.DOCKER -> {
                    map[AUTH_HEADER_DEVOPS_PROJECT_ID] = DockerEnv.getProjectId()
                    map[AUTH_HEADER_DEVOPS_AGENT_ID] = DockerEnv.getAgentId()
                    map[AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY] = DockerEnv.getAgentSecretKey()
                }
                BuildType.DOCKER_HOST -> {
                    // Nothing to do
                }
                else -> {
                }
            }
            return map
        }
        private val logger = LoggerFactory.getLogger(AbstractBuildResourceApi::class.java)
    }

    protected val objectMapper = JsonUtil.getObjectMapper()

    fun buildGet(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(getAllHeaders(headers).toHeaders()).get().build()
    }

    fun buildHeader(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(getAllHeaders(headers).toHeaders()).head().build()
    }

    fun buildPost(path: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), "")
        return buildPost(path, requestBody, headers)
    }

    fun buildPost(path: String, requestBody: RequestBody, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(getAllHeaders(headers).toHeaders()).post(requestBody).build()
    }

    fun buildPut(path: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), "")
        return buildPut(path, requestBody, headers)
    }

    fun buildPut(path: String, requestBody: RequestBody, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(getAllHeaders(headers).toHeaders()).put(requestBody).build()
    }

    @Suppress("UNUSED")
    fun buildDelete(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(getAllHeaders(headers).toHeaders()).delete().build()
    }

    @Suppress("UNUSED")
    fun getJsonRequest(data: Any): RequestBody {
        return RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(data)
        )
    }

    @Suppress("UNUSED")
    fun encode(parameter: String): String {
        return URLEncoder.encode(parameter, "UTF-8")
    }

    private fun buildUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            fixUrl(gateway, path)
        }
    }

    private fun fixUrl(server: String, path: String): String {
        return if (server.startsWith("http://") || server.startsWith("https://")) {
            "$server/${path.removePrefix("/")}"
        } else {
            "http://$server/${path.removePrefix("/")}"
        }
    }

    private fun getAllHeaders(headers: Map<String, String>): Map<String, String> {
        if (dockerHostConfig.gatewayHeaderTag != null) {
            logger.info("Now is ${dockerHostConfig.gatewayHeaderTag} environment, request with the AUTH_HEADER_GATEWAY_TAG header.")
            return buildArgs.plus(headers).plus(mapOf(AUTH_HEADER_GATEWAY_TAG to dockerHostConfig.gatewayHeaderTag!!))
        }

        return buildArgs.plus(headers)
    }

    fun getUrlPrefix(): String {
        return dockerHostConfig.dispatchUrlPrefix ?: Constants.DISPATCH_DOCKER_PREFIX
    }
}
