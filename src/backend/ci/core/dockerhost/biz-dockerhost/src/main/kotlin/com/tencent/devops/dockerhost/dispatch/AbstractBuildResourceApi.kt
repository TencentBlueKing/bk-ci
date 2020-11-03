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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dockerhost.common.Constants
import com.tencent.devops.dockerhost.config.DockerHostConfig
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

abstract class AbstractBuildResourceApi constructor(
    private val dockerHostConfig: DockerHostConfig,
    private val gray: Gray
) {
    private val grayProject = "grayproject"

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
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).get().build()
    }

    fun buildHeader(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).head().build()
    }

    fun buildPost(path: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "")
        return buildPost(path, requestBody, headers)
    }

    fun buildPost(path: String, requestBody: RequestBody, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).post(requestBody).build()
    }

    fun buildPut(path: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "")
        return buildPut(path, requestBody, headers)
    }

    fun buildPut(path: String, requestBody: RequestBody, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).put(requestBody).build()
    }

    fun buildDelete(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).delete().build()
    }

    fun getJsonRequest(data: Any): RequestBody {
        return RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            objectMapper.writeValueAsString(data)
        )
    }

    fun encode(parameter: String): String {
        return URLEncoder.encode(parameter, "UTF-8")
    }

    private fun buildUrl(path: String): String = "http://$gateway/${path.removePrefix("/")}"

    private fun getAllHeaders(headers: Map<String, String>): Map<String, String> {
        logger.info("=================== ${gray.isGray()}====================")
        val gray = System.getProperty("gray.project", "none")
        if (gray == grayProject) {
            logger.info("Now is gray environment, request with the x-devops-project-id header.")
            return buildArgs.plus(headers).plus(mapOf("x-devops-project-id" to grayProject))
        }
        return buildArgs.plus(headers)
    }

    fun getUrlPrefix(): String {
        return if ("codecc_build" == dockerHostConfig.dockerhostMode) {
            Constants.DISPATCH_CODECC_PREFIX
        } else {
            Constants.DISPATCH_DOCKER_PREFIX
        }
    }
}