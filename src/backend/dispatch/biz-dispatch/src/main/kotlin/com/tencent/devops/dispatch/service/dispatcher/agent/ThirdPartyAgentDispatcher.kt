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

package com.tencent.devops.dispatch.service.dispatcher.agent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyRedisBuild
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentDispatcher @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate,
    private val redisUtils: RedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val thirdPartyAgentBuildService: ThirdPartyAgentService
) : Dispatcher {
    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is ThirdPartyAgentIDDispatchType ||
            pipelineAgentStartupEvent.dispatchType is ThirdPartyAgentEnvDispatchType ||
            pipelineAgentStartupEvent.dispatchType is ThirdPartyDevCloudDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {

        when (pipelineAgentStartupEvent.dispatchType) {
            is ThirdPartyAgentIDDispatchType -> {
                val dispatchType = pipelineAgentStartupEvent.dispatchType as ThirdPartyAgentIDDispatchType
                if (!buildByAgentId(pipelineAgentStartupEvent, dispatchType)) {
                    retry(
                        client,
                        rabbitTemplate,
                        pipelineEventDispatcher,
                        pipelineAgentStartupEvent,
                        "获取第三方构建机节点（${dispatchType.displayName}）失败"
                    )
                }
            }
            is ThirdPartyDevCloudDispatchType -> {
                val originDispatchType = pipelineAgentStartupEvent.dispatchType as ThirdPartyDevCloudDispatchType
                val dispatchType = ThirdPartyAgentIDDispatchType(
                    originDispatchType.displayName,
                    originDispatchType.workspace,
                    originDispatchType.agentType
                )
                if (!buildByAgentId(pipelineAgentStartupEvent, dispatchType)) {
                    retry(
                        client,
                        rabbitTemplate,
                        pipelineEventDispatcher,
                        pipelineAgentStartupEvent,
                        "获取第三方构建机节点（${dispatchType.displayName}）失败"
                    )
                }
            }
            is ThirdPartyAgentEnvDispatchType -> {
                val dispatchType = pipelineAgentStartupEvent.dispatchType as ThirdPartyAgentEnvDispatchType
                buildByEnvId(pipelineAgentStartupEvent, dispatchType)
            }
            else -> {
                throw InvalidParamException("Unknown agent type - ${pipelineAgentStartupEvent.dispatchType}")
            }
        }
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        thirdPartyAgentBuildService.finishBuild(
            pipelineAgentShutdownEvent.buildId,
            pipelineAgentShutdownEvent.vmSeqId,
            pipelineAgentShutdownEvent.buildResult
        )
    }

    private fun buildByAgentId(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentIDDispatchType
    ): Boolean {

        val agentResult = if (dispatchType.agentType == AgentType.ID) {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentById(pipelineAgentStartupEvent.projectId, dispatchType.displayName)
        } else {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentByDisplayName(pipelineAgentStartupEvent.projectId, dispatchType.displayName)
        }

        if (agentResult.agentStatus != AgentStatus.IMPORT_OK) {
            return false
        }
        if (agentResult.isNotOk()) {
            logger.warn("Fail to get the third party agent($dispatchType) because of ${agentResult.message}")
            return false
        }

        if (agentResult.data == null) {
            logger.warn("Get the null third party agent($dispatchType)")
            return false
        }
        return buildByAgentId(pipelineAgentStartupEvent, agentResult.data!!, dispatchType.workspace)
    }

    private fun getAgent(projectId: String, agentId: String): ThirdPartyAgent? {
        val agentResult = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentById(projectId, agentId)

        if (agentResult.agentStatus != AgentStatus.IMPORT_OK) {
            return null
        }
        if (agentResult.isNotOk()) {
            logger.warn("Fail to get the third party agent($agentId) because of ${agentResult.message}")
            return null
        }

        if (agentResult.data == null) {
            logger.warn("[$projectId|$agentId] The agent is not exist")
        }
        return agentResult.data
    }

    private fun buildByAgentId(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        agent: ThirdPartyAgent,
        workspace: String?
    ): Boolean {
        val agentId = agent.agentId
        val redisLock = ThirdPartyAgentLock(redisOperation, pipelineAgentStartupEvent.projectId, agentId)
        try {

            if (redisLock.tryLock()) {
                if (redisUtils.isThirdPartyAgentUpgrading(pipelineAgentStartupEvent.projectId, agentId)) {
                    logger.warn("The agent($agentId) of project(${pipelineAgentStartupEvent.projectId}) is upgrading")
                    return false
                }
//                if (thirdPartyAgentService.isBuildRunningOnAgent(pipelineAgentStartupEvent.projectId, agentId)) {
//                    logger.warn("There are builds on running on the current agent, retry")
//                    return false
//                }

                redisUtils.setThirdPartyBuild(
                    agent.secretKey,
                    ThirdPartyRedisBuild(
                        pipelineAgentStartupEvent.projectId,
                        pipelineAgentStartupEvent.pipelineId,
                        pipelineAgentStartupEvent.buildId,
                        agentId,
                        pipelineAgentStartupEvent.vmSeqId,
                        agent.hostname,
                        pipelineAgentStartupEvent.channelCode,
                        pipelineAgentStartupEvent.atoms
                    )
                )
                thirdPartyAgentBuildService.queueBuild(
                    pipelineAgentStartupEvent.projectId,
                    agentId,
                    pipelineAgentStartupEvent.pipelineId,
                    pipelineAgentStartupEvent.buildId,
                    pipelineAgentStartupEvent.vmSeqId,
                    workspace ?: "",
                    pipelineAgentStartupEvent.pipelineName,
                    pipelineAgentStartupEvent.buildNo,
                    pipelineAgentStartupEvent.taskName
                )
                logger.info(
                    "Start the third party build agent($agentId) " +
                        "of build(${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.buildId}|${pipelineAgentStartupEvent.vmSeqId})"
                )
                LogUtils.addLine(
                    rabbitTemplate,
                    pipelineAgentStartupEvent.buildId,
                    "Start up the agent ${agent.hostname}/${agent.ip} for the build ${pipelineAgentStartupEvent.buildId}",
                    "",
                    pipelineAgentStartupEvent.executeCount ?: 1
                )
                return true
            } else {
                logger.warn("Fail to lock third party agent($agentId)")
                return false
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun buildByEnvId(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType
    ) {
        val agentsResult = when (dispatchType.agentType) {
            AgentType.ID -> {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvId(pipelineAgentStartupEvent.projectId, dispatchType.envName)
            }
            AgentType.NAME -> {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvName(pipelineAgentStartupEvent.projectId, dispatchType.envName)
            }
        }

        val errorMessage = "获取第三方构建机环境（${dispatchType.envName}）失败"
        if (agentsResult.isNotOk()) {
            logger.warn("Fail to get the agents by env($dispatchType) because of ${agentsResult.message}")
            retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent, errorMessage)
            return
        }

        if (agentsResult.data == null) {
            logger.warn("Get null agents by env($dispatchType)")
            retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent, errorMessage)
            return
        }

        if (agentsResult.data!!.isEmpty()) {
            logger.warn("The third party agents is empty of env($dispatchType)")
            retry(
                client,
                rabbitTemplate,
                pipelineEventDispatcher,
                pipelineAgentStartupEvent,
                "第三方构建机环境（${dispatchType.envName}）节点为空"
            )
            return
        }

        val activeAgents = agentsResult.data!!.filter {
            it.status == AgentStatus.IMPORT_OK &&
                pipelineAgentStartupEvent.os == it.os
        }

        val preBuildAgents = thirdPartyAgentBuildService.getPreBuildAgents(
            pipelineAgentStartupEvent.projectId,
            pipelineAgentStartupEvent.pipelineId,
            pipelineAgentStartupEvent.vmSeqId
        )
        val commonAgents = preBuildAgents.filter {
            activeAgents.forEach { agent ->
                if (agent.agentId == it.agentId) {
                    return@filter true
                }
            }
            false
        }

        logger.info("Get the pre build agents($commonAgents) of env($dispatchType) of pipeline(${pipelineAgentStartupEvent.pipelineId})")

        val hasTryAgents = ArrayList<String>()

        if (commonAgents.isNotEmpty()) {
            commonAgents.forEach {
                if (hasTryAgents.contains(it.agentId)) {
                    return@forEach
                }
                hasTryAgents.add(it.agentId)
                val agent = getAgent(pipelineAgentStartupEvent.projectId, it.agentId) ?: return@forEach
                if (buildByAgentId(pipelineAgentStartupEvent, agent, dispatchType.workspace)) {
                    return
                }
            }
        }

        activeAgents.forEach {
            if (hasTryAgents.contains(it.agentId)) {
                return@forEach
            }
            hasTryAgents.add(it.agentId)
            val agent = getAgent(pipelineAgentStartupEvent.projectId, it.agentId) ?: return@forEach
            if (buildByAgentId(pipelineAgentStartupEvent, agent, dispatchType.workspace)) {
                return
            }
        }
        logger.info("Fail to find the fix agents for the build(${pipelineAgentStartupEvent.buildId})")
        retry(client, rabbitTemplate, pipelineEventDispatcher, pipelineAgentStartupEvent, errorMessage)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentDispatcher::class.java)
    }
}
