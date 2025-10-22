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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.AgentPipelineRefInfo
import com.tencent.devops.environment.pojo.AgentPipelineRefRequest
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentPipelineRefService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val pipelineResourceDao: PipelineResourceDao
) {
    fun updateAgentPipelineRef(userId: String, action: String, projectId: String, pipelineId: String) {
        logger.info("updateAgentPipelineRef, [$userId|$action|$projectId|$pipelineId]")
        var model: Model? = null
        if (action != "delete_pipeline") {
            val modelString = pipelineResourceDao.getLatestVersionModelString(dslContext, projectId, pipelineId)
            if (modelString.isNullOrBlank()) {
                logger.warn("model not found: [$userId|$action|$projectId|$pipelineId]")
                return
            }
            try {
                model = objectMapper.readValue(modelString, Model::class.java)
            } catch (ignored: Exception) {
                logger.error("parse process($pipelineId) model fail", ignored)
                return
            }
        }
        try {
            analysisPipelineRefAndSave(userId, action, projectId, pipelineId, model)
        } catch (e: Exception) {
            logger.error("analysisPipelineRefAndSave failed", e)
        }
    }

    fun analysisPipelineRefAndSave(
        userId: String,
        action: String,
        projectId: String,
        pipelineId: String,
        pipelineModel: Model?
    ) {
        val agentPipelineRefInfos = mutableListOf<AgentPipelineRefInfo>()
        pipelineModel?.stages?.forEach { stage ->
            stage.containers.forEach { container ->
                if (container is VMBuildContainer && container.dispatchType is ThirdPartyAgentIDDispatchType) {
                    val agentHashId = (container.dispatchType!! as ThirdPartyAgentIDDispatchType).displayName
                    agentPipelineRefInfos.add(
                        AgentPipelineRefInfo(
                            agentHashId = agentHashId,
                            pipelineId = pipelineId,
                            pipelineName = pipelineModel.name,
                            vmSeqId = container.id,
                            jobId = container.containerHashId,
                            jobName = container.name
                        )
                    )
                }
            }
        }

        client.get(ServiceThirdPartyAgentResource::class).updatePipelineRef(
            userId = userId,
            projectId = projectId,
            request = AgentPipelineRefRequest(
                action = action,
                pipelineId = pipelineId,
                pipelineRefInfos = agentPipelineRefInfos
            )
        )
    }

    fun updatePipelineRef(userId: String, projectId: String, pipelineId: String) {
        updateAgentPipelineRef(userId, "op", projectId, pipelineId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentPipelineRefService::class.java)
    }
}
