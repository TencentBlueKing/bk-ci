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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.SharedProjectInfoWrap
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentPipelineRef
import com.tencent.devops.openapi.api.apigw.v4.environment.ApigwEnvironmentResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwEnvironmentResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwEnvironmentResourceV4 {
    override fun listUsableServerNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<NodeWithPermission>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|list usable server nodes|$projectId")
        return client.get(ServiceNodeResource::class).listUsableServerNodes(userId, projectId)
    }

    override fun createEnv(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        environment: EnvCreateInfo
    ): Result<EnvironmentId> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|create env|$projectId|$environment")
        return client.get(ServiceEnvironmentResource::class).create(userId, projectId, environment)
    }

    override fun deleteEnv(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        envHashId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|delete env|$projectId|$envHashId")
        return client.get(ServiceEnvironmentResource::class).delete(userId, projectId, envHashId)
    }

    override fun envAddNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|env add nodes|$projectId|$envHashId|$nodeHashIds")
        return client.get(ServiceEnvironmentResource::class).addNodes(userId, projectId, envHashId, nodeHashIds)
    }

    override fun envDeleteNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        envHashId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|env delete nodes|$projectId|$envHashId|$nodeHashIds")
        return client.get(ServiceEnvironmentResource::class).deleteNodes(userId, projectId, envHashId, nodeHashIds)
    }

    override fun deleteNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashIds: List<String>
    ): Result<Boolean> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|delete nodes|$projectId|$nodeHashIds")
        return client.get(ServiceNodeResource::class).deleteNodes(userId, projectId, nodeHashIds)
    }

    override fun listUsableServerEnvs(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<EnvWithPermission>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|list usable server envs|$projectId")
        return client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(userId, projectId)
    }

    override fun listEnvRawByEnvNames(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        envNames: List<String>
    ): Result<List<EnvWithPermission>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|list env raw by env names|$projectId|$envNames")
        return client.get(ServiceEnvironmentResource::class).listRawByEnvNames(userId, projectId, envNames)
    }

    override fun listEnvRawByEnvHashIds(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Result<List<EnvWithPermission>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|list env raw by env hash ids|$projectId|$envHashIds")
        return client.get(ServiceEnvironmentResource::class).listRawByEnvHashIds(userId, projectId, envHashIds)
    }

    override fun listNodeRawByNodeHashIds(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|list node raw by node hash ids|$projectId|$nodeHashIds")
        return client.get(ServiceNodeResource::class).listRawByHashIds(userId, projectId, nodeHashIds)
    }

    override fun listNodeRawByEnvHashIds(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Result<Map<String, List<NodeBaseInfo>>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|list node raw by env hash ids|$projectId|$envHashIds")
        return client.get(ServiceNodeResource::class).listRawByEnvHashIds(userId, projectId, envHashIds)
    }

    override fun extListNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<NodeWithPermission>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|ext list nodes|$projectId")
        return client.get(ServiceNodeResource::class).extListNodes(userId, projectId)
    }

    override fun listPipelineRef(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeHashId: String,
        sortBy: String?,
        sortDirection: String?
    ): Result<List<AgentPipelineRef>> {
        logger.info(
            "OPENAPI_ENVIRONMENT_V4|$userId|list pipeline ref|$projectId|$nodeHashId|$sortBy" +
                "|$sortDirection"
        )
        return client.get(ServiceThirdPartyAgentResource::class).listPipelineRef(
            userId, projectId, nodeHashId,
            sortBy, sortDirection
        )
    }

    override fun setShareEnv(
        userId: String,
        projectId: String,
        envHashId: String,
        sharedProjects: SharedProjectInfoWrap
    ): Result<Boolean> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|set share env|$projectId|$envHashId|$sharedProjects")
        return client.get(ServiceEnvironmentResource::class).setShareEnv(userId, projectId, envHashId, sharedProjects)
    }

    override fun thirdPartyEnv2Nodes(
        userId: String,
        projectId: String,
        envHashId: String?,
        envName: String?
    ): Result<List<NodeWithPermission>> {
        logger.info("OPENAPI_ENVIRONMENT_V4|$userId|third party env2nodes|$projectId|$envHashId|$envName")
        return client.get(ServiceNodeResource::class).thirdPartyEnv2Nodes(userId, projectId, envHashId, envName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwEnvironmentResourceV4Impl::class.java)
    }
}
