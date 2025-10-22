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

package com.tencent.devops.process.engine.atom.parser

import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.service.BuildVariableService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DispatchTypeBuilder @Autowired constructor(
    private val dispatchTypeParser: DispatchTypeParser,
    private val buildVariableService: BuildVariableService
) {
    fun getDispatchType(task: PipelineBuildTask, param: VMBuildContainer): DispatchType {
        val dispatchType: DispatchType
        /**
         * 新版的构建环境直接传入指定的构建机方式
         */
        if (param.dispatchType != null) {
            dispatchType = param.dispatchType!!
        } else {
            // 第三方构建机ID
            val agentId = param.thirdPartyAgentId ?: ""
            // 构建环境ID
            val envId = param.thirdPartyAgentEnvId ?: ""
            val workspace = param.thirdPartyWorkspace ?: ""
            dispatchType = if (agentId.isNotBlank()) {
                ThirdPartyAgentIDDispatchType(
                    displayName = agentId,
                    workspace = workspace,
                    agentType = AgentType.ID,
                    dockerInfo = null,
                    reusedInfo = null
                )
            } else if (envId.isNotBlank()) {
                ThirdPartyAgentEnvDispatchType(
                    envName = envId,
                    envProjectId = null,
                    workspace = workspace,
                    agentType = AgentType.ID,
                    dockerInfo = null,
                    reusedInfo = null
                )
            } // docker建机指定版本(旧)
            else if (!param.dockerBuildVersion.isNullOrBlank()) {
                DockerDispatchType(param.dockerBuildVersion!!)
            } else {
                DockerDispatchType(param.dockerBuildVersion!!)
            }
        }

        // 处理dispatchType中的BKSTORE镜像信息
        dispatchTypeParser.parse(
            userId = task.starter, projectId = task.projectId,
            pipelineId = task.pipelineId, buildId = task.buildId, dispatchType = dispatchType
        )

        dispatchType.replaceVariable(
            buildVariableService.getAllVariable(
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                buildId = task.buildId
            )
        )
        return dispatchType
    }
}
