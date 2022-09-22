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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.environment.agent.AgentGrayUtils
import com.tencent.devops.common.environment.agent.AgentUpgradeType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.OpThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.UpdateAgentRequest
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineSeqId
import com.tencent.devops.environment.service.thirdPartyAgent.AgentPipelineService
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentMgrService
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentPipelineService
import com.tencent.devops.environment.service.thirdPartyAgent.UpgradeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentMgrService,
    private val upgradeService: UpgradeService,
    private val thirdPartyAgentPipelineService: ThirdPartyAgentPipelineService,
    private val agentPipelineService: AgentPipelineService,
    private val agentGrayUtils: AgentGrayUtils
) : OpThirdPartyAgentResource {

    override fun listEnableProjects(): Result<List<String>> {
        return Result(thirdPartyAgentService.listEnableThirdPartyAgentProjects())
    }

    override fun setAgentUpgrade(version: String): Result<Boolean> {
        upgradeService.setUpgrade(version)
        return Result(true)
    }

    override fun setMasterVersion(version: String): Result<Boolean> {
        upgradeService.setMasterVersion(version)
        return Result(true)
    }

    override fun getAgentVersion(): Result<String> {
        return Result(upgradeService.getWorkerVersion())
    }

    override fun getAgentMasterVersion(): Result<String> {
        return Result(upgradeService.getAgentVersion())
    }

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

    override fun setForceUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        agentGrayUtils.setForceUpgradeAgents(agentIds, AgentUpgradeType.find(agentUpgradeType))
        return Result(true)
    }

    override fun unsetForceUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        agentGrayUtils.unsetForceUpgradeAgents(agentIds, AgentUpgradeType.find(agentUpgradeType))
        return Result(true)
    }

    override fun getAllForceUpgradeAgents(agentUpgradeType: String?): Result<List<Long>> {
        return Result(agentGrayUtils.getAllForceUpgradeAgents(AgentUpgradeType.find(agentUpgradeType)))
    }

    override fun cleanAllForceUpgradeAgents(agentUpgradeType: String?): Result<Boolean> {
        agentGrayUtils.cleanAllForceUpgradeAgents(AgentUpgradeType.find(agentUpgradeType))
        return Result(true)
    }

    override fun setLockUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        agentGrayUtils.setLockUpgradeAgents(agentIds, AgentUpgradeType.find(agentUpgradeType))
        return Result(true)
    }

    override fun unsetLockUpdateAgents(agentIds: List<Long>, agentUpgradeType: String?): Result<Boolean> {
        agentGrayUtils.unsetLockUpgradeAgents(agentIds, AgentUpgradeType.find(agentUpgradeType))
        return Result(true)
    }

    override fun getAllLockUpgradeAgents(agentUpgradeType: String?): Result<List<Long>> {
        return Result(agentGrayUtils.getAllLockUpgradeAgents(AgentUpgradeType.find(agentUpgradeType)))
    }

    override fun cleanAllLockUpgradeAgents(agentUpgradeType: String?): Result<Boolean> {
        agentGrayUtils.cleanAllLockUpgradeAgents(AgentUpgradeType.find(agentUpgradeType))
        return Result(true)
    }

    override fun enableProject(projectId: String, enable: Boolean): Result<Boolean> {
        thirdPartyAgentService.enableThirdPartyAgent(projectId, enable)
        return Result(true)
    }

    override fun updateAgentGateway(updateAgentRequest: UpdateAgentRequest): Result<Boolean> {
        thirdPartyAgentService.updateAgentGateway(updateAgentRequest)
        return Result(true)
    }
}
