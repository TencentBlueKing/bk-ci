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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.environment.api.TencentServiceNodeService
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineStartupPermissionExtService
import com.tencent.devops.process.utils.NODE_AGENT_ID
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.support.api.service.ServiceIMateResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 内部版启动权限校验：目前仅针对团队项目（projectScope=0）的节点启动进行授权检查。
 *
 * 通过 [NODE_AGENT_ID] 获取节点对应的 workspaceName，作为 clientUuid 调用 iMate 授权接口，
 * 当启动人对节点无操作权限时阻断启动。
 */
@Service
class TxPipelineStartupPermissionExtServiceImpl @Autowired constructor(
    private val client: Client
) : PipelineStartupPermissionExtService {

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineStartupPermissionExtServiceImpl::class.java)
    }

    override fun checkStartupPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipelineParamMap: Map<String, BuildParameters>,
        channelCode: ChannelCode
    ) {

        // 获取agent hashId
        val agentHashId = pipelineParamMap[NODE_AGENT_ID]?.value?.toString()
        if (agentHashId.isNullOrBlank()) {
            return
        }

        // 获取项目信息，仅团队项目（projectScope == 0）需要校验
        val projectVO = client.get(ServiceProjectResource::class).get(projectId).data
        if (projectVO?.projectScope != 0) {
            return
        }

        // 通过节点获取详情
        val nodeAgentDetail = client.get(TencentServiceNodeService::class)
            .getNodeAgentDetail(userId, projectId, agentHashId).data
        val workspaceName = nodeAgentDetail?.workspaceName
        if (workspaceName.isNullOrBlank()) {
            logger.warn(
                "workspaceName not found, skip permission check|$projectId|$pipelineId|agentHashId=$agentHashId"
            )
            return
        }

        val authorized = client.get(ServiceIMateResource::class)
            .checkAuthorization(username = userId, clientUuid = workspaceName).data?.authorized ?: false
        if (!authorized) {
            val nodeDisplayInfo = "${nodeAgentDetail.displayName}（${nodeAgentDetail.ip}）"
            logger.warn(
                "startup permission denied|$projectId|$pipelineId|$userId|" +
                    "agentHashId=$agentHashId|workspace=$workspaceName"
            )
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_START_NODE_NO_PERMISSION,
                params = arrayOf(userId, nodeDisplayInfo)
            )
        }
    }
}
