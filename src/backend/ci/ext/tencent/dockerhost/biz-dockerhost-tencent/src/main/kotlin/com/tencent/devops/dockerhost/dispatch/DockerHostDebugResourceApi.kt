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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DockerHostDebugResourceApi constructor(
    dockerHostConfig: DockerHostConfig
) : AbstractBuildResourceApi(dockerHostConfig) {
    private val logger = LoggerFactory.getLogger(DockerHostDebugResourceApi::class.java)

    fun startDebug(hostTag: String): Result<ContainerInfo>? {
        val path = "/dispatch/api/dockerhost/startDebug?hostTag=$hostTag"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun reportDebugContainerId(pipelineId: String, vmSeqId: String, containerId: String): Result<Boolean>? {
        val path = "/dispatch/api/dockerhost/reportDebugContainerId?pipelineId=$pipelineId&vmSeqId=$vmSeqId&containerId=$containerId"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun endDebug(hostTag: String): Result<ContainerInfo>? {
        val path = "/dispatch/api/dockerhost/endDebug?hostTag=$hostTag"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun rollbackDebug(pipelineId: String, vmSeqId: String, shutdown: Boolean? = false, msg: String? = ""): Result<Boolean>? {
        val path = "/dispatch/api/dockerhost/rollbackDebug?pipelineId=$pipelineId&vmSeqId=$vmSeqId&shutdown=$shutdown"
        val request = if (msg.isNullOrBlank()) {
            buildPost(path)
        } else {
            buildPost(path, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), msg))
        }

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostDebugResourceApi $path fail. $responseContent")
                throw RuntimeException("DockerHostDebugResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getDockerJarLength(): Long? {
        val path = "/dispatch/gw/build/docker.jar"
        val request = buildHeader(path)

        OkhttpUtils.doHttp(request).use { response ->
            val contentLength = response.header("Content-Length")?.toLong()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. ${response.code()}")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            return contentLength
        }
    }
}