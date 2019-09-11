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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.resources.thirdPartyAgent

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineSeqId
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentPipelineService
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentMgrService
import com.tencent.devops.environment.service.thirdPartyAgent.UpgradeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentMgrService: ThirdPartyAgentMgrService,
    private val upgradeService: UpgradeService,
    private val thirdPartyAgentPipelineService: ThirdPartyAgentPipelineService
) : ServiceThirdPartyAgentResource {
    override fun getAgentById(projectId: String, agentId: String): AgentResult<ThirdPartyAgent?> {
        return thirdPartyAgentMgrService.getAgent(projectId, agentId)
    }

    override fun getAgentByDisplayName(projectId: String, displayName: String): AgentResult<ThirdPartyAgent?> {
        return thirdPartyAgentMgrService.getAgentByDisplayName(projectId, displayName)
    }

    override fun getAgentsByEnvId(projectId: String, envId: String) =
        Result(thirdPartyAgentMgrService.getAgentByEnvId(projectId, envId))

    override fun getAgentsByEnvName(projectId: String, envName: String): Result<List<ThirdPartyAgent>> =
        Result(thirdPartyAgentMgrService.getAgnetByEnvName(projectId, envName))

    override fun upgradeByVersion(
        projectId: String,
        agentId: String,
        secretKey: String,
        version: String?,
        masterVersion: String?
    ) = upgradeService.checkUpgrade(projectId, agentId, secretKey, version, masterVersion)

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
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(thirdPartyAgentMgrService.listAgents(userId, projectId, os))
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("UserId is illegal")
        }
    }

    private fun checkProjectId(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("ProjectId is illegal")
        }
    }
}