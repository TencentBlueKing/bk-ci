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
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dockerhost.common.Constants
import com.tencent.devops.dockerhost.config.DockerHostConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BuildResourceApi constructor(
    dockerHostConfig: DockerHostConfig,
    gray: Gray
) : AbstractBuildResourceApi(dockerHostConfig, gray) {
    private val logger = LoggerFactory.getLogger(BuildResourceApi::class.java)

    fun dockerStartFail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        status: BuildStatus
    ): Result<Boolean>? {
        val path =
            "/ms/process/api/service/builds/$projectId/$pipelineId/$buildId/vmStatus?vmSeqId=$vmSeqId&status=${status.name}"
        val request = buildPut(path)
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw RemoteServiceException("BuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: String, containerId: String, hostTag: String): Result<Boolean>? {
        val path =
            "/${getUrlPrefix()}/api/dockerhost/containerId?buildId=$buildId&vmSeqId=$vmSeqId&containerId=$containerId&hostTag=$hostTag"
        val request = buildPost(path)
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("AgentThirdPartyAgentResourceApi $path fail. $responseContent")
                throw RemoteServiceException("AgentThirdPartyAgentResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
