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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsVO
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.SystemInfoUtil
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DockerHostBuildResourceApi constructor(
    private val dockerHostConfig: DockerHostConfig
) : AbstractBuildResourceApi(dockerHostConfig) {
    private val logger = LoggerFactory.getLogger(DockerHostBuildResourceApi::class.java)

    fun postLog(
        buildId: String,
        red: Boolean,
        message: String,
        tag: String? = "",
        jobId: String? = ""
    ) {
        try {
            val path = "/${getUrlPrefix()}/api/dockerhost/postlog?buildId=$buildId&red=$red&tag=$tag&jobId=$jobId"
            val request = buildPost(
                path = path,
                requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), message)
            )

            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                }
            }
        } catch (e: Exception) {
            logger.error("DockerHostBuildResourceApi postLog error.", e)
        }
    }

    fun getResourceConfig(pipelineId: String, vmSeqId: String, retryCount: Int = 3): Result<DockerResourceOptionsVO>? {
        try {
            val path = "/${getUrlPrefix()}/api/dockerhost/resource-config/pipelines/$pipelineId/vmSeqs/$vmSeqId"
            OkhttpUtils.doHttp(buildGet(path)).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("[$pipelineId]|[$vmSeqId] Get resourceConfig $path fail. $responseContent")
                    throw TaskExecuteException(
                        errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                        errorType = ErrorType.SYSTEM,
                        errorMsg = "Get resourceConfig $path fail")
                }
                return objectMapper.readValue(responseContent)
            }
        } catch (e: Exception) {
            val localRetryCount = retryCount - 1
            return if (localRetryCount > 0) {
                getResourceConfig(pipelineId, vmSeqId, localRetryCount)
            } else {
                return null
            }
        }
    }

    fun getQpcGitProjectList(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: Int,
        retryCount: Int = 3
    ): Result<List<String>>? {
        try {
            val path = "/${getUrlPrefix()}/api/dockerhost/qpc/projects/$projectId/builds/$buildId/" +
                    "vmSeqs/$vmSeqId?poolNo=$poolNo"
            OkhttpUtils.doHttp(buildGet(path)).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("[$projectId]|[$buildId]|[$vmSeqId] Get resourceConfig $path fail. $responseContent")
                    throw TaskExecuteException(
                        errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                        errorType = ErrorType.SYSTEM,
                        errorMsg = "Get resourceConfig $path fail")
                }
                return objectMapper.readValue(responseContent)
            }
        } catch (e: Exception) {
            val localRetryCount = retryCount - 1
            return if (localRetryCount > 0) {
                getQpcGitProjectList(projectId, buildId, vmSeqId, poolNo, retryCount)
            } else {
                return null
            }
        }
    }

    fun refreshDockerIpStatus(port: String, containerNum: Int): Result<Boolean>? {
        val dockerIp = CommonUtils.getInnerIP(dockerHostConfig.dockerhostLocalIp)
        val path = "/${getUrlPrefix()}/api/dockerhost/dockerIp/$dockerIp/refresh"
        val dockerIpInfoVO = DockerIpInfoVO(
            id = 0L,
            dockerIp = dockerIp,
            dockerHostPort = port.toInt(),
            capacity = 100,
            usedNum = containerNum,
            averageCpuLoad = SystemInfoUtil.getAverageCpuLoad(),
            averageMemLoad = SystemInfoUtil.getAverageMemLoad(),
            averageDiskLoad = SystemInfoUtil.getAverageDiskLoad(),
            averageDiskIOLoad = SystemInfoUtil.getAverageDiskIOLoad(),
            enable = true,
            grayEnv = null,
            specialOn = null,
            createTime = null
        )

        val request = buildPost(
            path,
            RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(dockerIpInfoVO))
        )

        logger.info("Start refreshDockerIpStatus $path")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
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
        val path = "/${getUrlPrefix()}/gw/build/docker.jar"
        val request = buildHeader(path)

        OkhttpUtils.doHttp(request).use { response ->
            val contentLength = response.header("Content-Length")?.toLong()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. ${response.code}")
                throw TaskExecuteException(
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                    errorType = ErrorType.SYSTEM,
                    errorMsg = "DockerHostBuildResourceApi $path fail")
            }
            return contentLength
        }
    }

    fun getPublicImages(): Result<List<ImageRepoInfo>> {
        val path = "/${getUrlPrefix()}/api/dockerhost/public/images"
        val request = buildGet(path)

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
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
