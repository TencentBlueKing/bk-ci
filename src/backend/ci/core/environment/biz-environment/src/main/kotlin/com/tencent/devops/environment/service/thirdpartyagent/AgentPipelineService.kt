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

package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.AgentPipelineRefDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.AgentPipelineRefInfo
import com.tencent.devops.environment.pojo.AgentPipelineRefRequest
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentPipelineRef
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class AgentPipelineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val agentPipelineRefDao: AgentPipelineRefDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao
) {
    fun updatePipelineRef(
        userId: String,
        projectId: String,
        request: AgentPipelineRefRequest
    ) {
        logger.info("updatePipelineRef: [$userId|$projectId|${request.action}|${request.pipelineId}}]")
        when (request.action) {
            "create_pipeline", "update_pipeline", "restore_pipeline", "op" -> {
                if (request.pipelineRefInfos.isEmpty()) {
                    logger.warn("no reference to handle")
                    return
                }
                savePipelineRefInfo(projectId, request.pipelineId, request.pipelineRefInfos)
            }
            "delete_pipeline" -> {
                cleanPipelineRef(projectId, request.pipelineId)
            }
            else -> {
                logger.warn("action(${request.action}) not supported")
            }
        }
    }

    private fun savePipelineRefInfo(
        projectId: String,
        pipelineId: String,
        agentPipelineRefInfos: List<AgentPipelineRefInfo>
    ) {
        val agentBuffer = mutableMapOf<Long, TEnvironmentThirdpartyAgentRecord>()
        val agentPipelineRefs = mutableListOf<AgentPipelineRef>()
        agentPipelineRefInfos.forEach next@{ refInfo ->
            val agentId = HashUtil.decodeIdToLong(refInfo.agentHashId)
            val agent = agentBuffer[agentId] ?: thirdPartyAgentDao.getAgent(dslContext, agentId) ?: return@next
            agentPipelineRefs.add(
                AgentPipelineRef(
                    agentId = agent.id,
                    nodeId = agent.nodeId,
                    projectId = projectId,
                    pipelineId = refInfo.pipelineId,
                    pipelineName = refInfo.pipelineName,
                    vmSeqId = refInfo.vmSeqId,
                    jobId = refInfo.jobId,
                    jobName = refInfo.jobName
                )
            )
        }
        savePipelineRef(projectId, pipelineId, agentPipelineRefs)
    }

    private fun savePipelineRef(projectId: String, pipelineId: String, agentPipelineRefs: List<AgentPipelineRef>) {
        val modifiedAgentIds = mutableSetOf<Long>()
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val existPipelineRefs = agentPipelineRefDao.list(transactionContext, projectId, pipelineId)
            val existRefMap = existPipelineRefs.associateBy { "${it.agentId!!}_${it.vmSeqId}_${it.jobId}" }
            val refMap = agentPipelineRefs.associateBy { "${it.agentId!!}_${it.vmSeqId}_${it.jobId}" }
            val toDeleteRefMap = existRefMap.filterKeys { !refMap.containsKey(it) }
            val toAddRefMap = refMap.filterKeys { !existRefMap.containsKey(it) }

            val toDeleteRef = toDeleteRefMap.values
            val toAddRef = toAddRefMap.values

            agentPipelineRefDao.batchDelete(transactionContext, toDeleteRef.map { it.id })
            agentPipelineRefDao.batchAdd(transactionContext, toAddRef)
            modifiedAgentIds.addAll(toDeleteRef.map { it.agentId })
            modifiedAgentIds.addAll(toAddRef.map { it.agentId!! })
        }

        logger.info("savePipelineRef, modifiedAgentIds: $modifiedAgentIds")
        modifiedAgentIds.forEach {
            updateRefCount(it)
        }
    }

    fun cleanPipelineRef(projectId: String, pipelineId: String) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val existPipelineRefs = agentPipelineRefDao.list(transactionContext, projectId, pipelineId)
            agentPipelineRefDao.batchDelete(transactionContext, existPipelineRefs.map { it.id })
        }
    }

    private fun updateRefCount(agentId: Long) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val agent = thirdPartyAgentDao.getAgent(transactionContext, agentId)
            if (agent != null) {
                val agentRefCount = agentPipelineRefDao.countPipelineRef(transactionContext, agentId)
                nodeDao.updatePipelineRefCount(transactionContext, agent.nodeId, agentRefCount)
            } else {
                logger.warn("agent[$agentId] not found")
            }
        }
    }

    fun listPipelineRef(projectId: String, nodeHashId: String): List<AgentPipelineRef> {
        val nodeLongId = HashUtil.decodeIdToLong(nodeHashId)
        return agentPipelineRefDao.listByNodeId(dslContext, projectId, nodeLongId).map {
            AgentPipelineRef(
                nodeHashId = HashUtil.encodeLongId(it.nodeId),
                agentHashId = HashUtil.encodeLongId(it.agentId),
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                pipelineName = it.pieplineName,
                vmSeqId = it.vmSeqId,
                jobId = it.jobId,
                jobName = it.jobName,
                lastBuildTime = if (null == it.lastBuildTime) {
                    ""
                } else {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(it.lastBuildTime)
                }
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentPipelineService::class.java)
    }
}
