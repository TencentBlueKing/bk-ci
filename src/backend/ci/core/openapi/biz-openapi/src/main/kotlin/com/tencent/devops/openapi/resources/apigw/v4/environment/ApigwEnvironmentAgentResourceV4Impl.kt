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

package com.tencent.devops.openapi.resources.apigw.v4.environment

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchFetchAgentData
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchUpdateAgentEnvVar
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentDetail
import com.tencent.devops.openapi.api.apigw.v4.environment.ApigwEnvironmentAgentResourceV4
import com.tencent.devops.openapi.constant.OpenAPIMessageCode
import com.tencent.devops.openapi.utils.ApigwParamUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwEnvironmentAgentResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwEnvironmentAgentResourceV4 {

    override fun thirdPartAgentList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<NodeBaseInfo>> {
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V4|$userId|third part agent list|$projectId")
        logger.info("thirdPartAgentList userId $userId, project $projectId")
        return client.get(ServiceNodeResource::class).listNodeByNodeType(projectId, NodeType.THIRDPARTY)
    }

    override fun getNodeStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashId: String
    ): Result<NodeWithPermission?> {
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V4|$userId|get node status|$projectId|$nodeHashId")
        logger.info("getNodeStatus userId:$userId, projectId: $projectId, nodeHashId: $nodeHashId")
        val nodeList = client.get(ServiceNodeResource::class).listByHashIds(
            userId = userId,
            projectId = projectId,
            nodeHashIds = arrayListOf(nodeHashId)
        ).data
        if (nodeList != null && nodeList.isNotEmpty()) {
            return Result(nodeList[0])
        }
        throw ErrorCodeException(
            errorCode = OpenAPIMessageCode.ERROR_NODE_NOT_EXISTS,
            params = arrayOf(nodeHashId)
        )
    }

    override fun getNodeDetail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashId: String?,
        nodeName: String?,
        agentHashId: String?
    ): Result<ThirdPartyAgentDetail?> {
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V4|$userId|get node detail|$projectId|$nodeHashId|$agentHashId")
        if (!agentHashId.isNullOrBlank()) {
            return client.get(ServiceThirdPartyAgentResource::class).getAgentDetail(
                userId = userId, projectId = projectId, agentHashId = agentHashId
            )
        }
        return client.get(ServiceThirdPartyAgentResource::class).getNodeDetail(
            userId = userId,
            projectId = projectId,
            nodeHashId = nodeHashId,
            nodeName = nodeName
        )
    }

    override fun listAgentBuilds(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashId: String?,
        nodeName: String?,
        agentHashId: String?,
        status: String?,
        pipelineId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>> {
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V4|$userId|get listAgentBuilds|$projectId|$nodeHashId|$agentHashId")
        return client.get(ServiceThirdPartyAgentResource::class).listAgentBuilds(
            userId = userId,
            projectId = projectId,
            nodeHashId = nodeHashId,
            nodeName = nodeName,
            agentHashId = agentHashId,
            status = status,
            pipelineId = pipelineId,
            page = page ?: 1,
            pageSize = ApigwParamUtil.standardSize(pageSize) ?: 20
        )
    }

    override fun fetchAgentEnv(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        data: BatchFetchAgentData
    ): Result<Map<String, List<EnvVar>>> {
        if ((data.agentHashIds?.size ?: 0) > 100 || (data.nodeHashIds?.size ?: 0) > 100) {
            return Result(status = 1, message = "once max search node size 100", emptyMap())
        }
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V4|$userId|fetchAgentEnv|$projectId|$data")
        return client.get(ServiceThirdPartyAgentResource::class).fetchAgentEnv(
            userId = userId,
            projectId = projectId,
            data = data
        )
    }

    override fun batchUpdateEnv(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        data: BatchUpdateAgentEnvVar
    ): Result<Boolean> {
        if ((data.agentHashIds?.size ?: 0) > 100 || (data.nodeHashIds?.size ?: 0) > 100) {
            return Result(status = 1, message = "once max update node size 100", false)
        }
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V4|$userId|batchUpdateEnv|$projectId|$data")
        return client.get(ServiceThirdPartyAgentResource::class).batchUpdateEnv(
            userId = userId,
            projectId = projectId,
            data = data
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwEnvironmentAgentResourceV4Impl::class.java)
    }
}
