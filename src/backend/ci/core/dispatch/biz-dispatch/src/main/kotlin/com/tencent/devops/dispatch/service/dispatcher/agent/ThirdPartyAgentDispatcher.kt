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
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.common.web.mq.alert.AlertUtils
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.dispatch.utils.ThirdPartyAgentEnvLock
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyRedisBuild
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentDispatcher @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
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
                buildByAgentId(pipelineAgentStartupEvent, dispatchType)
            }
            is ThirdPartyDevCloudDispatchType -> {
                val originDispatchType = pipelineAgentStartupEvent.dispatchType as ThirdPartyDevCloudDispatchType
                buildByAgentId(
                    pipelineAgentStartupEvent = pipelineAgentStartupEvent,
                    dispatchType = ThirdPartyAgentIDDispatchType(
                        displayName = originDispatchType.displayName,
                        workspace = originDispatchType.workspace,
                        agentType = originDispatchType.agentType
                    )
                )
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
        try {
            thirdPartyAgentBuildService.finishBuild(
                buildId = pipelineAgentShutdownEvent.buildId,
                vmSeqId = pipelineAgentShutdownEvent.vmSeqId,
                success = pipelineAgentShutdownEvent.buildResult
            )
        } finally {
            try {
                sendDispatchMonitoring(
                    client = client,
                    projectId = pipelineAgentShutdownEvent.projectId,
                    pipelineId = pipelineAgentShutdownEvent.pipelineId,
                    buildId = pipelineAgentShutdownEvent.buildId,
                    vmSeqId = pipelineAgentShutdownEvent.vmSeqId ?: "",
                    actionType = pipelineAgentShutdownEvent.actionType.name,
                    retryTime = pipelineAgentShutdownEvent.retryTime,
                    routeKeySuffix = pipelineAgentShutdownEvent.routeKeySuffix ?: "third",
                    startTime = 0L,
                    stopTime = System.currentTimeMillis(),
                    errorCode = "0",
                    errorMessage = ""
                )
            } catch (e: Exception) {
                logger.error("[${pipelineAgentShutdownEvent.projectId}|${pipelineAgentShutdownEvent.pipelineId}|${pipelineAgentShutdownEvent.buildId}] shutdown third sendDispatchMonitoring error.")
            }
        }
    }

    private fun buildByAgentId(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentIDDispatchType
    ) {

        val agentResult = if (dispatchType.agentType == AgentType.ID) {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentById(pipelineAgentStartupEvent.projectId, dispatchType.displayName)
        } else {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentByDisplayName(pipelineAgentStartupEvent.projectId, dispatchType.displayName)
        }

        if (agentResult.agentStatus != AgentStatus.IMPORT_OK) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.USER,
                errorCode = ErrorCodeEnum.VM_STATUS_ERROR.errorCode,
                errorMsg = "第三方构建机状态异常/Bad build agent status (${agentResult.agentStatus?.name})"
            )
            return
        }

        if (agentResult.isNotOk()) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.errorCode,
                errorMsg = "获取第三方构建机失败/Fail to get build agent($dispatchType) because of ${agentResult.message}"
            )
            return
        }

        if (agentResult.data == null) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.USER,
                errorCode = ErrorCodeEnum.FOUND_AGENT_ERROR.errorCode,
                errorMsg = "获取第三方构建机失败/Can not found agent by type($dispatchType)"
            )
            return
        }

        if (!buildByAgentId(pipelineAgentStartupEvent, agentResult.data!!, dispatchType.workspace)) {
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL.errorCode,
                errorMessage = "获取第三方构建机失败/Load build agent（${dispatchType.displayName}）fail!"
            )
        } else {
            // 上报monitor数据
            try {
                sendDispatchMonitoring(
                    client = client,
                    projectId = pipelineAgentStartupEvent.projectId,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    buildId = pipelineAgentStartupEvent.buildId,
                    vmSeqId = pipelineAgentStartupEvent.vmSeqId,
                    actionType = pipelineAgentStartupEvent.actionType.name,
                    retryTime = pipelineAgentStartupEvent.retryTime,
                    routeKeySuffix = pipelineAgentStartupEvent.routeKeySuffix ?: "third",
                    startTime = System.currentTimeMillis(),
                    stopTime = 0L,
                    errorCode = "0",
                    errorMessage = ""
                )
            } catch (e: Exception) {
                logger.error("[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|${pipelineAgentStartupEvent.buildId}] startUp third sendDispatchMonitoring error.")
            }
        }
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

                redisUtils.setThirdPartyBuild(
                    agent.secretKey,
                    ThirdPartyRedisBuild(
                        projectId = pipelineAgentStartupEvent.projectId,
                        pipelineId = pipelineAgentStartupEvent.pipelineId,
                        buildId = pipelineAgentStartupEvent.buildId,
                        agentId = agentId,
                        vmSeqId = pipelineAgentStartupEvent.vmSeqId,
                        vmName = agent.hostname,
                        channelCode = pipelineAgentStartupEvent.channelCode,
                        atoms = pipelineAgentStartupEvent.atoms
                    )
                )
                thirdPartyAgentBuildService.queueBuild(
                    projectId = pipelineAgentStartupEvent.projectId,
                    agentId = agentId,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    buildId = pipelineAgentStartupEvent.buildId,
                    vmSeqId = pipelineAgentStartupEvent.vmSeqId,
                    thirdPartyAgentWorkspace = workspace ?: "",
                    pipelineName = pipelineAgentStartupEvent.pipelineName,
                    buildNo = pipelineAgentStartupEvent.buildNo,
                    taskName = pipelineAgentStartupEvent.taskName
                )
                saveAgentInfoToBuildDetail(
                    projectId = pipelineAgentStartupEvent.projectId,
                    pipelineId = pipelineAgentStartupEvent.pipelineId,
                    buildId = pipelineAgentStartupEvent.buildId,
                    vmSeqId = pipelineAgentStartupEvent.vmSeqId,
                    agent = agent
                )
                logger.info(
                    "Start the third party build agent($agentId) " +
                        "of build(${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.buildId}|${pipelineAgentStartupEvent.vmSeqId})"
                )
                buildLogPrinter.addLine(
                    buildId = pipelineAgentStartupEvent.buildId,
                    message = "Start up the agent ${agent.hostname}/${agent.ip} for the build ${pipelineAgentStartupEvent.buildId}",
                    tag = "",
                    jobId = pipelineAgentStartupEvent.containerHashId,
                    executeCount = pipelineAgentStartupEvent.executeCount ?: 1
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

    private fun saveAgentInfoToBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        agent: ThirdPartyAgent

    ) {
        client.get(ServiceBuildResource::class).saveBuildVmInfo(
            projectId,
            pipelineId,
            buildId,
            vmSeqId,
            VmInfo(agent.ip, agent.ip)
        )
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

        val errorMessage = "获取第三方构建机环境（${dispatchType.envName}）失败/Load build agent（${dispatchType.envName}）fail!"
        if (agentsResult.isNotOk()) {
            logger.warn("Fail to get the agents by env($dispatchType) because of ${agentsResult.message}")
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL.errorCode,
                errorMessage = errorMessage
            )
            return
        }

        if (agentsResult.data == null) {
            logger.warn("Get null agents by env($dispatchType)")
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.USER,
                errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL.errorCode,
                errorMessage = errorMessage
            )
            return
        }

        if (agentsResult.data!!.isEmpty()) {
            logger.warn("The third party agents is empty of env($dispatchType)")
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.USER,
                errorCode = ErrorCodeEnum.VM_NODE_NULL.errorCode,
                errorMessage = "第三方构建机环境（${dispatchType.envName}）的节点为空/The build agent (${dispatchType.envName}) have no node id"
            )
            return
        }

        val redisLock =
            ThirdPartyAgentEnvLock(redisOperation, pipelineAgentStartupEvent.projectId, dispatchType.envName)
        redisLock.lock()
        try {
            /**
             * 1. 现获取当前正常的agent列表
             * 2. 获取可用的agent列表
             * 3. 优先调用可用的agent执行任务
             * 4. 如果启动可用的agent失败再调用有任务的agent
             */
            val activeAgents = agentsResult.data!!.filter {
                it.status == AgentStatus.IMPORT_OK &&
                    pipelineAgentStartupEvent.os == it.os
            }.toHashSet()
            val agentMaps = activeAgents.map { it.agentId to it }.toMap()

            val preBuildAgents = HashSet<ThirdPartyAgent>()
            thirdPartyAgentBuildService.getPreBuildAgents(
                projectId = pipelineAgentStartupEvent.projectId,
                pipelineId = pipelineAgentStartupEvent.pipelineId,
                vmSeqId = pipelineAgentStartupEvent.vmSeqId
            ).forEach {
                val agent = agentMaps[it.agentId]
                if (agent != null) {
                    preBuildAgents.add(agent)
                }
            }
            logger.info("Get the pre build agents($preBuildAgents) of env($dispatchType) of pipeline(${pipelineAgentStartupEvent.pipelineId})")

            val hasTryAgents = HashSet<String>()
            val runningBuildsMapper = HashMap<String/*AgentId*/, Int/*running builds*/>()

            /**
             * 1. 最高优先级的agent:
             *     a. 最近构建机中使用过这个构建机
             *     b. 当前没有任何构建机任务
             * 2. 次高优先级的agent:
             *     a. 最近构建机中使用过这个构建机
             *     b. 当前有构建任务，但是构建任务数量没有达到当前构建机的最大并发数
             * 3. 第三优先级的agent:
             *     a. 当前没有任何构建机任务
             * 4. 第四优先级的agent:
             *     a. 当前有构建任务，但是构建任务数量没有达到当前构建机的最大并发数
             * 5. 最低优先级：
             *     a. 都没有满足以上条件的
             *
             */

            logger.info(
                "[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|" +
                    "${pipelineAgentStartupEvent.buildId}|${pipelineAgentStartupEvent.vmSeqId}]" +
                    " Start to check the empty task agents - $preBuildAgents"
            )
            /**
             * 根据哪些agent没有任何任务并且是在最近构建中使用到的Agent
             */
            if (startEmptyAgents(
                    event = pipelineAgentStartupEvent,
                    dispatchType = dispatchType,
                    agents = preBuildAgents,
                    hasTryAgents = hasTryAgents,
                    runningBuildsMapper = runningBuildsMapper
                )
            ) {
                return
            }

            logger.info(
                "[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|" +
                    "${pipelineAgentStartupEvent.buildId}|${pipelineAgentStartupEvent.vmSeqId}]" +
                    " Start to check the available task agents of pre build agents"
            )
            /**
             * 根据哪些agent有任务并且是在最近构建中使用到的Agent，同时当前构建任务还没到达该Agent最大并行数
             */
            if (startAvailableAgents(
                    event = pipelineAgentStartupEvent,
                    dispatchType = dispatchType,
                    agents = preBuildAgents,
                    hasTryAgents = hasTryAgents,
                    runningBuildsMapper = runningBuildsMapper
                )
            ) {
                return
            }

            logger.info(
                "[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|" +
                    "${pipelineAgentStartupEvent.buildId}|${pipelineAgentStartupEvent.vmSeqId}]" +
                    " Start to check the empty task agents - $activeAgents"
            )
            /**
             * 根据哪些agent没有任何任务
             */
            if (startEmptyAgents(
                    event = pipelineAgentStartupEvent,
                    dispatchType = dispatchType,
                    agents = activeAgents,
                    hasTryAgents = hasTryAgents,
                    runningBuildsMapper = runningBuildsMapper
                )
            ) {
                return
            }

            logger.info(
                "[${pipelineAgentStartupEvent.projectId}|${pipelineAgentStartupEvent.pipelineId}|" +
                    "${pipelineAgentStartupEvent.buildId}|${pipelineAgentStartupEvent.vmSeqId}]" +
                    " Start to check the available task agents of active agents"
            )
            /**
             * 根据哪些agent有任务，同时当前构建任务还没到达该Agent最大并行数
             */
            if (startAvailableAgents(
                    event = pipelineAgentStartupEvent,
                    dispatchType = dispatchType,
                    agents = activeAgents,
                    hasTryAgents = hasTryAgents,
                    runningBuildsMapper = runningBuildsMapper
                )
            ) {
                return
            }

            if (pipelineAgentStartupEvent.retryTime == 1) {
                buildLogPrinter.addLine(
                    buildId = pipelineAgentStartupEvent.buildId,
                    message = "All eligible agents are disabled or offline, Waiting for an available agent...",
                    tag = "",
                    jobId = pipelineAgentStartupEvent.containerHashId,
                    executeCount = pipelineAgentStartupEvent.executeCount ?: 1
                )
            }
            logger.info("Fail to find the fix agents for the build(${pipelineAgentStartupEvent.buildId})")
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = pipelineAgentStartupEvent,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL.errorCode,
                errorMessage = errorMessage)
        } finally {
            redisLock.unlock()
        }
    }

    override fun retry(
        client: Client,
        buildLogPrinter: BuildLogPrinter,
        pipelineEventDispatcher: PipelineEventDispatcher,
        event: PipelineAgentStartupEvent,
        errorType: ErrorType?,
        errorCode: Int?,
        errorMessage: String?
    ) {
        if (event.retryTime > 60) {
            // 置为失败
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorType = errorType ?: ErrorType.SYSTEM,
                errorCode = errorCode ?: ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                errorMsg = errorMessage ?: "Fail to start up after 60 retries"
            )
            AlertUtils.doAlert(
                level = AlertLevel.HIGH, title = "DevOps Alert Notify",
                message = "Job tailed to dispatch(60) the pipeline(${event.pipelineId}) buildId(${event.buildId}) type(${event.dispatchType})"
            )
            return
        }
        event.retryTime += 1
        event.delayMills = 10000
        pipelineEventDispatcher.dispatch(event)
    }

    private fun startEmptyAgents(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: HashSet<ThirdPartyAgent>,
        hasTryAgents: HashSet<String>,
        runningBuildsMapper: HashMap<String, Int>
    ): Boolean {
        return startAgentsForEnvBuild(
            pipelineAgentStartupEvent = event,
            dispatchType = dispatchType,
            agents = agents,
            hasTryAgents = hasTryAgents,
            runningBuildsMapper = runningBuildsMapper,
            agentMatcher = object : AgentMatcher {
                override fun match(runningCnt: Int, agent: ThirdPartyAgent): Boolean {
                    return runningCnt == 0
                }
            }
        )
    }

    private fun startAvailableAgents(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: HashSet<ThirdPartyAgent>,
        hasTryAgents: HashSet<String>,
        runningBuildsMapper: HashMap<String, Int>
    ): Boolean {
        return startAgentsForEnvBuild(
            pipelineAgentStartupEvent = event,
            dispatchType = dispatchType,
            agents = agents,
            hasTryAgents = hasTryAgents,
            runningBuildsMapper = runningBuildsMapper,
            agentMatcher = object : AgentMatcher {
                override fun match(runningCnt: Int, agent: ThirdPartyAgent): Boolean {
                    if (agent.parallelTaskCount != null &&
                        agent.parallelTaskCount!! > 0 &&
                        agent.parallelTaskCount!! > runningCnt
                    ) {
                        return true
                    }
                    return false
                }
            }
        )
    }

    private fun startAgentsForEnvBuild(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: HashSet<ThirdPartyAgent>,
        hasTryAgents: HashSet<String>,
        runningBuildsMapper: HashMap<String, Int>,
        agentMatcher: AgentMatcher
    ): Boolean {
        if (agents.isNotEmpty()) {
            agents.forEach {
                if (hasTryAgents.contains(it.agentId)) {
                    return@forEach
                }
                val runningCnt = getRunningCnt(it.agentId, runningBuildsMapper)
                if (agentMatcher.match(runningCnt, it)) {
                    if (startEnvAgentBuild(pipelineAgentStartupEvent, it, dispatchType, hasTryAgents)) {
                        logger.info(
                            "[${it.projectId}|$[${pipelineAgentStartupEvent.pipelineId}|${pipelineAgentStartupEvent.buildId}|${it.agentId}] " +
                                "Success to start the build"
                        )
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun startEnvAgentBuild(
        pipelineAgentStartupEvent: PipelineAgentStartupEvent,
        agent: ThirdPartyAgent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        hasTryAgents: HashSet<String>
    ): Boolean {
        if (hasTryAgents.contains(agent.agentId)) {
            return false
        }
        hasTryAgents.add(agent.agentId)
        if (buildByAgentId(pipelineAgentStartupEvent, agent, dispatchType.workspace)) {
            return true
        }
        return false
    }

    private fun getRunningCnt(agentId: String, runningBuildsMapper: HashMap<String, Int>): Int {
        var runningCnt = runningBuildsMapper[agentId]
        if (runningCnt == null) {
            runningCnt = thirdPartyAgentBuildService.getRunningBuilds(agentId)
            runningBuildsMapper[agentId] = runningCnt
        }
        return runningCnt
    }

    interface AgentMatcher {
        fun match(runningCnt: Int, agent: ThirdPartyAgent): Boolean
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentDispatcher::class.java)
    }
}
