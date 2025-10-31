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

package com.tencent.devops.artifactory.resources

import com.tencent.bkrepo.webhook.pojo.payload.node.NodeCreatedEventPayload
import com.tencent.devops.artifactory.api.service.OpenArtifactoryResource
import com.tencent.devops.artifactory.service.PipelineBuildArtifactoryService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.process.api.service.ServicePipelineRuntimeResource
import org.slf4j.LoggerFactory
import jakarta.ws.rs.core.Response

@RestResource
class OpenArtifactoryResourceImpl(
    private val clientTokenService: ClientTokenService,
    private val pipelineBuildArtifactoryService: PipelineBuildArtifactoryService,
    private val client: Client
) : OpenArtifactoryResource {

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun updateArtifactList(
        token: String,
        nodeCreatedEventPayload: NodeCreatedEventPayload
    ) {
        val validateTokenFlag = clientTokenService.checkToken(token)
        if (!validateTokenFlag) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(token)
            )
        }

        val userId = nodeCreatedEventPayload.user.userId
        val projectId = nodeCreatedEventPayload.node.projectId
        val pipelineId = nodeCreatedEventPayload.node.metadata["pipelineId"]?.toString()
        val buildId = nodeCreatedEventPayload.node.metadata["buildId"]?.toString()
            ?: nodeCreatedEventPayload.node.metadata["bk_ci_bid"]?.toString()

        if (pipelineId == null || buildId == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EMPTY,
                statusCode = Response.Status.BAD_REQUEST.statusCode
            )
        }

        val artifactList = pipelineBuildArtifactoryService.getArtifactList(userId, projectId, pipelineId, buildId)
        logger.info("[$pipelineId]|getArtifactList-$buildId artifact: ${JsonUtil.toJson(artifactList)}")
        val result = client.get(ServicePipelineRuntimeResource::class).updateArtifactList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            artifactoryFileList = artifactList
        )
        logger.info("[$buildId]|update artifact result: ${result.status} ${result.message}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenArtifactoryResourceImpl::class.java)
    }
}
