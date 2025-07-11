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

package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.openapi.api.apigw.v3.environment.ApigwEnvironmentAgentResourceV3
import com.tencent.devops.openapi.constant.OpenAPIMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwEnvironmentAgentResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwEnvironmentAgentResourceV3 {

    override fun thirdPartAgentList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<NodeBaseInfo>> {
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V3|$userId|third part agent list|$projectId")
        return client.get(ServiceNodeResource::class).listNodeByNodeType(projectId, NodeType.THIRDPARTY)
    }

    override fun getNodeStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashId: String
    ): Result<NodeWithPermission?> {
        logger.info("OPENAPI_ENVIRONMENT_AGENT_V3|$userId|get node status|$projectId|$nodeHashId")
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

    companion object {
        val logger = LoggerFactory.getLogger(ApigwEnvironmentAgentResourceV3Impl::class.java)
    }
}
