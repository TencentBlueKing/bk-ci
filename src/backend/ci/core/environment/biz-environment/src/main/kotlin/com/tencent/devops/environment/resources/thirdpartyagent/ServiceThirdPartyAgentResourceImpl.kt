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

package com.tencent.devops.environment.resources.thirdpartyagent

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.AgentPipelineRefRequest
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.slave.SlaveGateway
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentPipelineRef
import com.tencent.devops.environment.pojo.thirdpartyagent.AskHeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchFetchAgentData
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchUpdateAgentEnvVar
import com.tencent.devops.environment.pojo.thirdpartyagent.EnvNodeAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.UpdateAgentInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.pipeline.PipelineSeqId
import com.tencent.devops.environment.service.NodeService
import com.tencent.devops.environment.service.NodeTagService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.service.thirdpartyagent.AgentPipelineService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartAgentService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartyAgentMgrService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartyAgentPipelineService
import com.tencent.devops.environment.service.thirdpartyagent.UpgradeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentMgrService,
    private val upgradeService: UpgradeService,
    private val thirdPartyAgentPipelineService: ThirdPartyAgentPipelineService,
    private val agentPipelineService: AgentPipelineService,
    private val slaveGatewayService: SlaveGatewayService,
    private val nodeService: NodeService,
    private val nodeTagService: NodeTagService,
    private val agentService: ThirdPartAgentService
) : ServiceThirdPartyAgentResource {
    override fun getAgentById(projectId: String, agentId: String): AgentResult<ThirdPartyAgent?> {
        return thirdPartyAgentService.getAgent(projectId, agentId)
    }

    @Deprecated("getAgentById")
    override fun getAgentByIdGlobal(projectId: String, agentId: String): AgentResult<ThirdPartyAgent?> {
        return thirdPartyAgentService.getAgentGlobal(projectId, agentId)
    }

    override fun getAgentByDisplayName(projectId: String, displayName: String): AgentResult<ThirdPartyAgent?> {
        return thirdPartyAgentService.getAgentByDisplayName(projectId, displayName)
    }

    override fun getAgentsByEnvId(projectId: String, envId: String) =
        Result(thirdPartyAgentService.getAgentByEnvId(projectId, envId))

    override fun getAgentsByEnvName(projectId: String, envName: String): Result<List<EnvNodeAgent>> {
        val (_, res) = thirdPartyAgentService.getAgentByEnvName(projectId, envName)
        return Result(res)
    }

    override fun upgrade(projectId: String, agentId: String, secretKey: String, tag: String) =
        thirdPartyAgentService.checkIfCanUpgrade(projectId, agentId, secretKey, tag)

    override fun upgradeByVersion(
        projectId: String,
        agentId: String,
        secretKey: String,
        version: String?,
        masterVersion: String?
    ) = upgradeService.checkUpgrade(projectId, agentId, secretKey, version, masterVersion)

    override fun upgradeByVersionNew(
        projectId: String,
        agentId: String,
        secretKey: String,
        info: ThirdPartyAgentUpgradeByVersionInfo
    ) = upgradeService.checkUpgradeNew(projectId, agentId, secretKey, info)

    override fun scheduleAgentPipeline(
        userId: String,
        projectId: String,
        nodeId: String,
        pipeline: PipelineCreate
    ): Result<PipelineSeqId> {
        return Result(PipelineSeqId(thirdPartyAgentPipelineService.addPipeline(projectId, nodeId, userId, pipeline)))
    }

    override fun getAgentPipelineResponse(projectId: String, nodeId: String, seqId: String): Result<PipelineResponse> {
        return Result(thirdPartyAgentPipelineService.getPipelineResult(projectId, nodeId, seqId))
    }

    override fun listAgents(userId: String, projectId: String, os: OS): Result<List<ThirdPartyAgentInfo>> {
        return Result(thirdPartyAgentService.listAgents(userId, projectId, os))
    }

    override fun agentTaskStarted(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        agentId: String
    ): Result<Boolean> {
        thirdPartyAgentService.agentTaskStarted(projectId, pipelineId, buildId, vmSeqId, agentId)
        return Result(true)
    }

    override fun listPipelineRef(
        userId: String,
        projectId: String,
        nodeHashId: String,
        sortBy: String?,
        sortDirection: String?
    ): Result<List<AgentPipelineRef>> {
        val pipelineRefs = agentPipelineService.listPipelineRef(projectId, nodeHashId)
        return Result(sortPipelineRef(pipelineRefs, sortBy, sortDirection))
    }

    private fun sortPipelineRef(
        list: List<AgentPipelineRef>,
        sortBy: String?,
        sortDirection: String?
    ): List<AgentPipelineRef> {
        return when (sortBy) {
            "pipelineName" -> if (sortDirection == "DESC") {
                list.sortedByDescending { it.pipelineName }
            } else {
                list.sortedBy { it.pipelineName }
            }

            "lastBuildTime" -> if (sortDirection == "DESC") {
                list.sortedByDescending { it.lastBuildTime }
            } else {
                list.sortedBy { it.lastBuildTime }
            }

            else -> list
        }
    }

    override fun updatePipelineRef(
        userId: String,
        projectId: String,
        request: AgentPipelineRefRequest
    ): Result<Boolean> {
        agentPipelineService.updatePipelineRef(userId, projectId, request)
        return Result(true)
    }

    override fun getAgentDetail(
        userId: String,
        projectId: String,
        agentHashId: String,
        checkPermission: Boolean?
    ): Result<ThirdPartyAgentDetail?> {
        return Result(
            thirdPartyAgentService.getAgentDetailById(
                userId = userId,
                projectId = projectId,
                agentHashId = agentHashId,
                checkPermission = checkPermission ?: false
            )
        )
    }

    override fun getGateways(): Result<List<SlaveGateway>> {
        return Result(slaveGatewayService.getGateway())
    }

    @AuditEntry(actionId = ActionId.ENV_NODE_VIEW)
    override fun getNodeDetail(
        userId: String,
        projectId: String,
        nodeHashId: String?,
        nodeName: String?
    ): Result<ThirdPartyAgentDetail?> {
        val hashId = when {
            nodeHashId != null -> nodeHashId
            nodeName != null -> nodeService.getByDisplayNameNotWithPermission(
                userId,
                projectId,
                nodeName,
                listOf(NodeType.THIRDPARTY.name)
            ).firstOrNull()?.nodeHashId

            else -> null
        } ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NAME_OR_ID_INVALID)
        return Result(thirdPartyAgentService.getAgentDetail(userId, projectId, hashId))
    }

    override fun getNodeDetailSimple(
        userId: String,
        projectId: String,
        nodeHashId: String?,
        agentHashId: String?,
        checkPermission: Boolean?
    ): Result<ThirdPartyAgentDetail?> {
        return Result(
            thirdPartyAgentService.getAgentDetailSimple(
                userId = userId,
                projectId = projectId,
                nodeHashId = nodeHashId,
                agentHashId = agentHashId,
                checkPermission = checkPermission ?: false
            )
        )
    }

    override fun listAgentBuilds(
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
        val hashId = when {
            nodeHashId != null -> nodeHashId
            nodeName != null -> nodeService.getByDisplayNameNotWithPermission(
                userId,
                projectId,
                nodeName,
                listOf(NodeType.THIRDPARTY.name)
            ).firstOrNull()?.nodeHashId

            agentHashId != null -> thirdPartyAgentService.getAgent(
                projectId = projectId,
                agentHashId
            ).data?.nodeId

            else -> null
        } ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NAME_OR_ID_INVALID)
        return Result(
            thirdPartyAgentService.listAgentBuilds(
                userId = userId,
                projectId = projectId,
                nodeHashId = hashId,
                status = status,
                pipelineId = pipelineId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun newHeartbeat(
        projectId: String,
        agentId: String,
        secretKey: String,
        heartbeatInfo: NewHeartbeatInfo
    ): Result<AskHeartbeatResponse> {
        return Result(
            AskHeartbeatResponse(
                thirdPartyAgentService.newHeartbeat(projectId, agentId, secretKey, heartbeatInfo)
            )
        )
    }

    override fun getPipelines(projectId: String, agentId: String, secretKey: String): Result<ThirdPartyAgentPipeline?> {
        return Result(thirdPartyAgentPipelineService.getPipelines(projectId, agentId, secretKey))
    }

    override fun getAgentsByEnvNameWithId(
        projectId: String,
        envName: String
    ): Result<Pair<Long?, List<EnvNodeAgent>>> {
        return Result(thirdPartyAgentService.getAgentByEnvName(projectId, envName))
    }

    override fun fetchAgentEnv(
        userId: String,
        projectId: String,
        data: BatchFetchAgentData
    ): Result<Map<String, List<EnvVar>>> {
        return Result(
            agentService.fetchAgentEnv(
                userId = userId,
                projectId = projectId,
                nodeHashIds = data.nodeHashIds,
                agentHashIds = data.agentHashIds
            )
        )
    }

    override fun batchUpdateEnv(
        userId: String,
        projectId: String,
        data: BatchUpdateAgentEnvVar
    ): Result<Boolean> {
        return Result(
            agentService.batchUpdateAgentEnv(
                userId = userId,
                projectId = projectId,
                nodeHashIds = data.nodeHashIds,
                agentHashIds = data.agentHashIds,
                type = data.type,
                data = data.envVars
            )
        )
    }

    override fun updateAgentInfo(
        userId: String,
        projectId: String,
        data: UpdateAgentInfo
    ): Result<Boolean> {
        return Result(agentService.updateAgentInfo(userId, projectId, data))
    }

    override fun fetchTag(userId: String, projectId: String): Result<List<NodeTag>> {
        return Result(
            nodeTagService.fetchTagAndNodeCount(projectId)
        )
    }
}
