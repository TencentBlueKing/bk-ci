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

package com.tencent.devops.environment.resources.thirdPartyAgent

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.UserThirdPartyAgentResource
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentAction
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentLink
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStatusWithInfo
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.service.thirdPartyAgent.ImportService
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentMgrService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("TooManyFunctions")
@RestResource
class UserThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentMgrService,
    private val slaveGatewayService: SlaveGatewayService,
    private val importService: ImportService
) : UserThirdPartyAgentResource {
    override fun isProjectEnable(userId: String, projectId: String): Result<Boolean> {
        return Result(true)
    }

    override fun generateLink(
        userId: String,
        projectId: String,
        os: OS,
        zoneName: String?
    ): Result<ThirdPartyAgentLink> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.generateAgent(userId, projectId, os, zoneName))
    }

    override fun getGateway(
        userId: String,
        projectId: String,
        os: OS,
        visibility: Boolean?
    ): Result<List<SlaveGateway>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(slaveGatewayService.getGateway().filter { it.visibility == (visibility ?: true) })
    }

    override fun getLink(userId: String, projectId: String, nodeId: String): Result<ThirdPartyAgentLink> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkAgentId(nodeId)
        return Result(thirdPartyAgentService.getAgentLink(userId, projectId, nodeId))
    }

    override fun listAgents(userId: String, projectId: String, os: OS): Result<List<ThirdPartyAgentInfo>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.listAgents(userId, projectId, os))
    }

    override fun listAgents(userId: String, projectId: String): Result<List<ThirdPartyAgentInfo>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.listAgents(userId, projectId, null))
    }

    override fun getAgentStatus(
        userId: String,
        projectId: String,
        agentId: String
    ): Result<ThirdPartyAgentStatusWithInfo> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkAgentId(agentId)
        return Result(thirdPartyAgentService.getAgentStatusWithInfo(userId, projectId, agentId))
    }

    override fun importAgent(userId: String, projectId: String, agentId: String): Result<Boolean> {
        importService.importAgent(userId, projectId, agentId)
        return Result(true)
    }

    override fun deleteAgent(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        thirdPartyAgentService.deleteAgent(userId, projectId, nodeHashId)
        return Result(true)
    }

    override fun saveAgentEnvs(
        userId: String,
        projectId: String,
        nodeHashId: String,
        envs: List<EnvVar>
    ): Result<Boolean> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        thirdPartyAgentService.saveAgentEnv(userId, projectId, nodeHashId, envs)
        return Result(true)
    }

    override fun getAgentEnvs(userId: String, projectId: String, nodeHashId: String): Result<List<EnvVar>> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        return Result(thirdPartyAgentService.getAgentEnv(projectId, nodeHashId))
    }

    override fun setAgentParallelTaskCount(
        userId: String,
        projectId: String,
        nodeHashId: String,
        parallelTaskCount: Int
    ): Result<Boolean> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        thirdPartyAgentService.setParallelTaskCount(
            userId = userId,
            projectId = projectId,
            nodeHashId = nodeHashId,
            parallelTaskCount = parallelTaskCount,
            dockerParallelTaskCount = null
        )
        return Result(true)
    }

    override fun setAgentDockerParallelTaskCount(
        userId: String,
        projectId: String,
        nodeHashId: String,
        count: Int
    ): Result<Boolean> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        thirdPartyAgentService.setParallelTaskCount(
            userId = userId,
            projectId = projectId,
            nodeHashId = nodeHashId,
            parallelTaskCount = null,
            dockerParallelTaskCount = count
        )
        return Result(true)
    }

    override fun getThirdPartyAgentDetail(
        userId: String,
        projectId: String,
        nodeHashId: String
    ): Result<ThirdPartyAgentDetail?> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        return Result(
            thirdPartyAgentService.getAgentDetail(
                userId = userId,
                projectId = projectId,
                nodeHashId = nodeHashId
            )
        )
    }

    override fun listAgentBuilds(
        userId: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AgentBuildDetail>> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        return Result(thirdPartyAgentService.listAgentBuilds(userId, projectId, nodeHashId, page, pageSize))
    }

    override fun listAgentActions(
        userId: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ThirdPartyAgentAction>> {
        checkUserId(userId)
        checkProjectId(projectId)
        checkNodeId(nodeHashId)
        return Result(thirdPartyAgentService.listAgentActions(userId, projectId, nodeHashId, page, pageSize))
    }

    override fun queryCpuUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.queryCpuUsageMetrix(userId, projectId, nodeHashId, timeRange))
    }

    override fun queryMemoryUsageMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.queryMemoryUsageMetrix(userId, projectId, nodeHashId, timeRange))
    }

    override fun queryDiskioMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.queryDiskioMetrix(userId, projectId, nodeHashId, timeRange))
    }

    override fun queryNetMetrix(
        userId: String,
        projectId: String,
        nodeHashId: String,
        timeRange: String
    ): Result<Map<String, List<Map<String, Any>>>> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentService.queryNetMetrix(userId, projectId, nodeHashId, timeRange))
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("userId"))
        }
    }

    private fun checkProjectId(projectId: String) {
        if (projectId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("projectId"))
        }
    }

    private fun checkAgentId(agentHashId: String) {
        if (agentHashId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("agentId"))
        }
    }

    private fun checkNodeId(nodeHashId: String) {
        if (nodeHashId.isBlank()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("nodeId"))
        }
    }
}
