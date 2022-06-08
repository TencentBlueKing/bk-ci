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
import com.tencent.devops.dispatch.bcs.api.service.ServiceBcsDebugResource
import com.tencent.devops.dispatch.docker.service.debug.DebugInterface
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BcsDebugServiceImpl @Autowired constructor(
    private val client: Client
) : DebugInterface {
    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String
    ): String {
        val bcsDebugResult = client.get(ServiceBcsDebugResource::class).startDebug(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            buildId = buildId
        )

        logger.info("$userId $pipelineId| bcs debug response: ${JsonUtil.toJson(bcsDebugResult.data ?: "")}")
        return bcsDebugResult.data?.websocketUrl ?: throw ErrorCodeException(
            errorCode = "2103503",
            defaultMessage = "Can not found debug container.",
            params = arrayOf(pipelineId)
        )
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Boolean {
        logger.info("$userId $pipelineId| stop bcs debug.")
        return client.get(ServiceBcsDebugResource::class).stopDebug(
            userId, pipelineId, vmSeqId, containerName
        ).data ?: false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BcsDebugServiceImpl::class.java)
    }
}
