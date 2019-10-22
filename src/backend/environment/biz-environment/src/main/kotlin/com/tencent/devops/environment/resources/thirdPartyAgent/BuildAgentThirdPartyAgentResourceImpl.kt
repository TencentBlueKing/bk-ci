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

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdPartyAgent.BuildAgentThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.HeartbeatInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.HeartbeatResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineResponse
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentPipelineService
import com.tencent.devops.environment.service.thirdPartyAgent.ThirdPartyAgentMgrService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildAgentThirdPartyAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentMgrService: ThirdPartyAgentMgrService,
    private val thirdPartyAgentPipelineService: ThirdPartyAgentPipelineService
) : BuildAgentThirdPartyAgentResource {


    override fun agentStartup(
        projectId: String,
        agentId: String,
        secretKey: String,
        startInfo: ThirdPartyAgentStartInfo
    ): Result<AgentStatus> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentMgrService.agentStartup(projectId, agentId, secretKey, startInfo))
    }

    override fun getAgentStatus(
        projectId: String,
        agentId: String,
        secretKey: String
    ): Result<AgentStatus> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentMgrService.getAgentStatus(projectId, agentId, secretKey))
    }

    override fun heartbeat(projectId: String, agentId: String, secretKey: String, heartbeatInfo: HeartbeatInfo): Result<HeartbeatResponse> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentMgrService.heartbeat(projectId, agentId, secretKey, heartbeatInfo))
    }

    override fun getPipelines(projectId: String, agentId: String, secretKey: String): Result<ThirdPartyAgentPipeline?> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentPipelineService.getPipelines(projectId, agentId, secretKey))
    }

    override fun updatePipelineStatus(
        projectId: String,
        agentId: String,
        secretKey: String,
        response: PipelineResponse
    ): Result<Boolean> {
        checkParam(projectId, agentId, secretKey)
        return Result(thirdPartyAgentPipelineService.updatePipelineStatus(projectId, agentId, secretKey, response))
    }

    private fun checkParam(
        projectId: String,
        agentId: String,
        secretKey: String
    ) {
        if (projectId.isBlank()) {
            throw ParamBlankException("无效的项目ID")
        }
        if (agentId.isBlank()) {
            throw ParamBlankException("无效的Agent ID")
        }
        if (secretKey.isBlank()) {
            throw ParamBlankException("无效的Secret Key")
        }
    }
}