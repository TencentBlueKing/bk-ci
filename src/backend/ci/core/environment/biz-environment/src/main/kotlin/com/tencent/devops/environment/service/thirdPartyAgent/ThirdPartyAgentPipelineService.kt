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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentPipelineDao
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentPipeline
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.CommandPipeline
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.CommandPipelineCreate
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.FilePipeline
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.FilePipelineCreate
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineCreate
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineResponse
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineStatus
import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineType
import com.tencent.devops.model.environment.tables.records.TEnvironmentAgentPipelineRecord
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// 第三方构建机管道控制，比如命令和文件管道
@Service
class ThirdPartyAgentPipelineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val thirdPartyAgentPipelineDao: ThirdPartyAgentPipelineDao,
    private val objectMapper: ObjectMapper
) {

    fun getPipelines(
        projectId: String,
        agentId: String,
        secretKey: String
    ): ThirdPartyAgentPipeline? {
        logger.info("Trying to get the agent pipelines of agent $agentId and project $projectId")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId) ?: return null
        authAgent(agentRecord, secretKey)
        val pipelineRecord = thirdPartyAgentPipelineDao.getPipeline(dslContext, id, PipelineStatus.PENDING)
            ?: return null
        return getPipeline(pipelineRecord)
    }

    fun updatePipelineStatus(
        projectId: String,
        agentId: String,
        secretKey: String,
        statusResponse: PipelineResponse
    ): Boolean {
        logger.info("Update the status response $statusResponse of agent $agentId and project $projectId")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
        if (agentRecord == null) {
            logger.warn("The agent $id of project $projectId is not exist")
            return false
        }
        authAgent(agentRecord, secretKey)
        val seqId = HashUtil.decodeIdToLong(statusResponse.seqId)
        return thirdPartyAgentPipelineDao.updateStatus(
            dslContext,
            seqId,
            statusResponse.status,
            statusResponse.response
        ) == 1
    }

    fun addPipeline(
        projectId: String,
        nodeId: String,
        userId: String,
        create: PipelineCreate
    ): String {
        logger.info("Schedule agent $nodeId pipeline $create of project $projectId")
        val agentRecord = getAgentByNode(nodeId, projectId)

        val seqId = thirdPartyAgentPipelineDao.add(
            dslContext,
            agentRecord.id,
            projectId,
            userId,
            PipelineStatus.PENDING,
            objectMapper.writeValueAsString(create)
        )
        return HashUtil.encodeLongId(seqId)
    }

    fun getPipelineResult(
        projectId: String,
        nodeId: String,
        seqIdStr: String
    ): PipelineResponse {
        val agentRecord = getAgentByNode(nodeId, projectId)
        val seqId = HashUtil.decodeIdToLong(seqIdStr)
        val pipelineRecord = thirdPartyAgentPipelineDao.getPipeline(dslContext, seqId, agentRecord.id)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_PIPE_NOT_FOUND)
        return PipelineResponse(
            seqIdStr,
            PipelineStatus.from(pipelineRecord.status),
            pipelineRecord.response ?: ""
        )
    }

    private fun getAgentByNode(nodeId: String, projectId: String): TEnvironmentThirdpartyAgentRecord {
        val id = HashUtil.decodeIdToLong(nodeId)
        val record = thirdPartyAgentDao.getAgentByNodeId(dslContext, id, projectId)
        if (record == null) {
            logger.warn("The node $nodeId is not third party agent")
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NOT_THIRD_PARTY_BUILD_MACHINE)
        }
        return record
    }

    private fun authAgent(
        agentRecord: TEnvironmentThirdpartyAgentRecord,
        secretKey: String
    ) {
        val key = SecurityUtil.decrypt(agentRecord.secretKey)
        if (key != secretKey) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_AGENT_SECRET_KEY_INVALID,
                defaultMessage = "Illegal agent secret key"
            )
        }
    }

    private fun getPipeline(pipelineRecord: TEnvironmentAgentPipelineRecord): ThirdPartyAgentPipeline {
        val create = objectMapper.readValue(pipelineRecord.pipeline, PipelineCreate::class.java)
        return when (create.type) {
            PipelineType.COMMAND -> {
                create as CommandPipelineCreate
                CommandPipeline(create.command, HashUtil.encodeLongId(pipelineRecord.id))
            }
            PipelineType.FILE -> {
                create as FilePipelineCreate
                FilePipeline(create.operation, create.file, HashUtil.encodeLongId(pipelineRecord.id))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentPipelineService::class.java)
    }
}
