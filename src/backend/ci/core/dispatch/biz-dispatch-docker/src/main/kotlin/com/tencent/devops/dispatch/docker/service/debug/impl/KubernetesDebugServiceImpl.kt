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

package com.tencent.devops.dispatch.docker.service.debug.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerRoutingType
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.service.debug.DebugInterface
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceBaseDebugResource
import com.tencent.devops.dispatch.kubernetes.pojo.base.StartDebugReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.StopDebugReq
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KubernetesDebugServiceImpl @Autowired constructor(
    private val client: Client
) : DebugInterface {
    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        dockerRoutingType: DockerRoutingType
    ): String {
        val bcsDebugResult = client.get(ServiceBaseDebugResource::class).startDebug(
            userId = userId,
            startDebugReq = StartDebugReq(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                dockerRoutingType = dockerRoutingType.name
            )
        )

        logger.info("$userId $pipelineId| kubernetes debug response: ${JsonUtil.toJson(bcsDebugResult.data ?: "")}")
        return bcsDebugResult.data?.websocketUrl ?: throw ErrorCodeException(
            errorCode = "${ErrorCodeEnum.NO_CONTAINER_IS_READY_DEBUG.errorCode}",
            defaultMessage = "Can not found debug container.",
            params = arrayOf(pipelineId)
        )
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String,
        dockerRoutingType: DockerRoutingType
    ): Boolean {
        logger.info("$userId $pipelineId| stop kubernetes debug.")
        return client.get(ServiceBaseDebugResource::class).stopDebug(
            userId = userId,
            stopDebugReq = StopDebugReq(
                projectId = projectId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                containerName = containerName,
                dockerRoutingType = dockerRoutingType.name
            )
        ).data ?: false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesDebugServiceImpl::class.java)
    }
}
