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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.pojo.DockerHostInfo
import com.tencent.devops.dispatch.pojo.DockerIpInfoVO
import com.tencent.devops.dockerhost.common.Constants
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.SigarUtil
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class DockerHostBuildResourceApi constructor(
    private val urlPrefix: String = Constants.DISPATCH_DOCKER_PREFIX
) : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(DockerHostBuildResourceApi::class.java)

    fun startBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val path = "/$urlPrefix/api/dockerhost/startBuild?hostTag=$hostTag"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail"
                )
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: Int, containerId: String): Result<Boolean>? {
        val path = "/$urlPrefix/api/dockerhost/containerId?buildId=$buildId&vmSeqId=$vmSeqId&containerId=$containerId"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("AgentThirdPartyAgentResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "AgentThirdPartyAgentResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun endBuild(hostTag: String): Result<DockerHostBuildInfo>? {
        val path = "/$urlPrefix/api/dockerhost/endBuild?hostTag=$hostTag"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun rollbackBuild(buildId: String, vmSeqId: Int, shutdown: Boolean): Result<Boolean>? {
        val path = "/$urlPrefix/api/dockerhost/rollbackBuild?buildId=$buildId&vmSeqId=$vmSeqId&shutdown=$shutdown"
        val request = buildPost(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun getHost(hostTag: String): Result<DockerHostInfo>? {
        val path = "/$urlPrefix/api/dockerhost/host?hostTag=$hostTag"
        val request = buildGet(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun postLog(buildId: String, red: Boolean, message: String, tag: String? = "", jobId: String? = "") {
        try {
            val path = "/$urlPrefix/api/dockerhost/postlog?buildId=$buildId&red=$red&tag=$tag&jobId=$jobId"
            val request = buildPost(path, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), message))

            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                }
            }
        } catch (e: Exception) {
            logger.error("DockerHostBuildResourceApi postLog error.", e)
        }
    }

    fun refreshDockerIpStatus(port: String, containerNum: Int): Result<Boolean>? {
        val dockerIp = CommonUtils.getInnerIP()
        val path = "/$urlPrefix/api/dockerhost/dockerIp/$dockerIp/refresh"
        val dockerIpInfoVO = DockerIpInfoVO(
            id = 0L,
            dockerIp = dockerIp,
            dockerHostPort = port.toInt(),
            capacity = 100,
            usedNum = containerNum,
            averageCpuLoad = SigarUtil.getAverageCpuLoad(),
            averageMemLoad = SigarUtil.getAverageMemLoad(),
            averageDiskLoad = SigarUtil.getAverageDiskLoad(),
            averageDiskIOLoad = SigarUtil.getAverageDiskIOLoad(),
            enable = true,
            grayEnv = null,
            specialOn = null,
            createTime = null
        )

        val request = buildPost(
            path,
            RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(dockerIpInfoVO))
        )

        logger.info("Start refreshDockerIpStatus $path")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            logger.info("End refreshDockerIpStatus.")
            return objectMapper.readValue(responseContent)
        }
    }

    fun getDockerJarLength(): Long? {
        val path = "/$urlPrefix/gw/build/docker.jar"
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

    fun getPublicImages(): Result<List<ImageRepoInfo>> {
        val path = "/$urlPrefix/api/dockerhost/public/images"
        val request = buildGet(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
