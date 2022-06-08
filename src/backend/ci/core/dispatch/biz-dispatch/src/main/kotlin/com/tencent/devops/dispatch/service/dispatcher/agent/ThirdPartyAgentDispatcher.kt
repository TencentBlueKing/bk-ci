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

package com.tencent.devops.dispatch.service.dispatcher.agent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.VMBaseOS
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
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyRedisBuild
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
@Suppress("NestedBlockDepth")
class ThirdPartyAgentDispatcher @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val thirdPartyAgentBuildService: ThirdPartyAgentService
) : Dispatcher {
    override fun canDispatch(event: PipelineAgentStartupEvent) =
        event.dispatchType is ThirdPartyAgentIDDispatchType ||
                event.dispatchType is ThirdPartyAgentEnvDispatchType ||
                event.dispatchType is ThirdPartyDevCloudDispatchType

    override fun startUp(event: PipelineAgentStartupEvent) {
        when (event.dispatchType) {
            is ThirdPartyAgentIDDispatchType -> {
                val dispatchType = event.dispatchType as ThirdPartyAgentIDDispatchType
                buildByAgentId(event, dispatchType)
            }
            is ThirdPartyDevCloudDispatchType -> {
                val originDispatchType = event.dispatchType as ThirdPartyDevCloudDispatchType
                buildByAgentId(
                    event = event,
                    dispatchType = ThirdPartyAgentIDDispatchType(
                        displayName = originDispatchType.displayName,
                        workspace = originDispatchType.workspace,
                        agentType = originDispatchType.agentType
                    )
                )
            }
            is ThirdPartyAgentEnvDispatchType -> {
                val dispatchType = event.dispatchType as ThirdPartyAgentEnvDispatchType
                buildByEnvId(event, dispatchType)
            }
            else -> {
                throw InvalidParamException("Unknown agent type - ${event.dispatchType}")
            }
        }
    }

    override fun shutdown(event: PipelineAgentShutdownEvent) {
        try {
            thirdPartyAgentBuildService.finishBuild(
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                success = event.buildResult
            )
        } finally {
            try {
                sendDispatchMonitoring(
                    client = client,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId ?: "",
                    actionType = event.actionType.name,
                    retryTime = event.retryTime,
                    routeKeySuffix = event.routeKeySuffix ?: "third",
                    startTime = 0L,
                    stopTime = System.currentTimeMillis(),
                    errorCode = "0",
                    errorMessage = "",
                    errorType = ""
                )
            } catch (ignore: Exception) {
                logger.warn("${event.buildId}]SHUTDOWN_THIRD_PARTY_ERROR|e=$ignore", ignore)
            }
        }
    }

    private fun buildByAgentId(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentIDDispatchType
    ) {

        val agentResult = if (dispatchType.agentType == AgentType.ID) {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentById(event.projectId, dispatchType.displayName)
        } else {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentByDisplayName(event.projectId, dispatchType.displayName)
        }

        if (agentResult.isNotOk()) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorCodeEnum = ErrorCodeEnum.GET_BUILD_AGENT_ERROR,
                errorMsg = "获取第三方构建机信息失败(System Error) - ${agentResult.message}"
            )
            return
        }

        if (agentResult.agentStatus != AgentStatus.IMPORT_OK) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorCodeEnum = ErrorCodeEnum.VM_STATUS_ERROR,
                errorMsg = "第三方构建机状态异常，请在环境管理中检查第三方构建机状态(Agent offline) " +
                        "- ${dispatchType.displayName}| status: (${agentResult.agentStatus?.name})"
            )
            return
        }

        if (agentResult.data == null) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorCodeEnum = ErrorCodeEnum.FOUND_AGENT_ERROR,
                errorMsg = "获取第三方构建机信息失败(System Error) - $dispatchType agent is null"
            )
            return
        }

        if (!buildByAgentId(event, agentResult.data!!, dispatchType.workspace)) {
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = event,
                errorCodeEnum = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
                errorMessage = "第三方构建机Agent正在升级中 或 排队重试超时，请检查agent（${dispatchType.displayName}）并发任务数设置并稍后重试."
            )
        } else {
            // 上报monitor数据
            try {
                sendDispatchMonitoring(
                    client = client,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    actionType = event.actionType.name,
                    retryTime = event.retryTime,
                    routeKeySuffix = event.routeKeySuffix ?: "third",
                    startTime = System.currentTimeMillis(),
                    stopTime = 0L,
                    errorCode = "0",
                    errorMessage = "",
                    errorType = ""
                )
            } catch (ignore: Exception) {
                logger.error("${event.buildId}]START_THIRD_PARTY_ERROR|e=$ignore", ignore)
            }
        }
    }

    private fun buildByAgentId(event: PipelineAgentStartupEvent, agent: ThirdPartyAgent, workspace: String?): Boolean {
        val redisLock = ThirdPartyAgentLock(redisOperation, event.projectId, agent.agentId)
        try {
            if (redisLock.tryLock()) {
                if (thirdPartyAgentBuildRedisUtils.isThirdPartyAgentUpgrading(event.projectId, agent.agentId)) {
                    logger.warn("The agent(${agent.agentId}) of project(${event.projectId}) is upgrading")
                    log(event, "构建机升级中，重新调度(Agent is upgrading) - ${agent.hostname}/${agent.ip}")
                    return false
                }

                // #5806 入库失败就不再写Redis
                inQueue(agent = agent, event = event, agentId = agent.agentId, workspace = workspace)
                // 保存构建详情
                saveAgentInfoToBuildDetail(event = event, agent = agent)

                logger.info("${event.buildId}|START_AGENT_BY_ID|j(${event.vmSeqId})|agent=${agent.agentId}")
                log(event, "调度构建机(Scheduling selected Agent): ${agent.hostname}/${agent.ip}")
                return true
            } else {
                log(event, "构建机正忙,重新调度(Agent is busy) - ${agent.hostname}/${agent.ip}")
                return false
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun log(event: PipelineAgentStartupEvent, logMessage: String) {
        buildLogPrinter.addLine(
            buildId = event.buildId,
            message = logMessage,
            tag = VMUtils.genStartVMTaskId(event.vmSeqId),
            jobId = event.containerHashId,
            executeCount = event.executeCount ?: 1
        )
    }

    private fun inQueue(agent: ThirdPartyAgent, event: PipelineAgentStartupEvent, agentId: String, workspace: String?) {

        thirdPartyAgentBuildService.queueBuild(
            agent = agent,
            thirdPartyAgentWorkspace = workspace ?: "",
            event = event
        )

        thirdPartyAgentBuildRedisUtils.setThirdPartyBuild(
            agent.secretKey,
            ThirdPartyRedisBuild(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                agentId = agentId,
                vmSeqId = event.vmSeqId,
                vmName = agent.hostname,
                channelCode = event.channelCode,
                atoms = event.atoms
            )
        )
    }

    private fun saveAgentInfoToBuildDetail(event: PipelineAgentStartupEvent, agent: ThirdPartyAgent) {
        client.get(ServiceBuildResource::class).saveBuildVmInfo(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            vmInfo = VmInfo(ip = agent.ip, name = agent.ip)
        )
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun buildByEnvId(event: PipelineAgentStartupEvent, dispatchType: ThirdPartyAgentEnvDispatchType) {

        val agentsResult = when (dispatchType.agentType) {
            AgentType.ID -> {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvId(
                        event.projectId,
                        dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                            ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName)
            }
            AgentType.NAME -> {
                try {
                    client.get(ServiceThirdPartyAgentResource::class)
                        .getAgentsByEnvName(
                            event.projectId,
                            dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                                ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName)
                } catch (e: Exception) {
                    onFailBuild(
                        client = client,
                        buildLogPrinter = buildLogPrinter,
                        event = event,
                        errorType = ErrorCodeEnum.GET_VM_ERROR.errorType,
                        errorCode = ErrorCodeEnum.GET_VM_ERROR.errorCode,
                        errorMsg = if (e is RemoteServiceException) {
                            e.errorMessage
                        } else {
                            e.message ?: "${ErrorCodeEnum.GET_VM_ERROR.formatErrorMessage}(${dispatchType.envName})"
                        }
                    )
                    Result(data = null)
                }
            }
        }

        if (agentsResult.status == Response.Status.FORBIDDEN.statusCode) {
            logger.warn(
                "${event.buildId}|START_AGENT_FAILED_FORBIDDEN|" +
                        "j(${event.vmSeqId})|dispatchType=$dispatchType|err=${agentsResult.message}"
            )
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorType = ErrorCodeEnum.GET_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.GET_VM_ERROR.errorCode,
                errorMsg = agentsResult.message ?: ""
            )
            return
        }

        if (agentsResult.isNotOk()) {
            logger.warn(
                "${event.buildId}|START_AGENT_FAILED|" +
                    "j(${event.vmSeqId})|dispatchType=$dispatchType|err=${agentsResult.message}"
            )
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = event,
                errorCodeEnum = ErrorCodeEnum.FOUND_AGENT_ERROR,
                errorMessage = "获取第三方构建机信息失败(System Error) - ${dispatchType.envName}: ${agentsResult.message}"
            )
            return
        }

        if (agentsResult.data == null) {
            logger.warn(
                "${event.buildId}|START_AGENT_FAILED|j(${event.vmSeqId})|dispatchType=$dispatchType|err=null agents"
            )
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = event,
                errorCodeEnum = ErrorCodeEnum.FOUND_AGENT_ERROR,
                errorMessage = "获取第三方构建机信息失败(System Error) - ${dispatchType.envName}: agent is null"
            )
            return
        }

        if (agentsResult.data!!.isEmpty()) {
            logger.warn(
                "${event.buildId}|START_AGENT_FAILED|j(${event.vmSeqId})|dispatchType=$dispatchType|err=empty agents"
            )
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = event,
                errorCodeEnum = ErrorCodeEnum.VM_NODE_NULL,
                errorMessage = "构建机环境（${dispatchType.envName}）的节点为空，请检查环境管理配置，" +
                        "构建集群： ${dispatchType.envName} (env(${dispatchType.envName}) is empty)"
            )
            return
        }

        val redisLock = ThirdPartyAgentEnvLock(redisOperation, event.projectId, dispatchType.envName)
        try {
            val lock = redisLock.tryLock(timeout = 5000) // # 超时尝试锁定，防止环境过热锁定时间过长，影响其他环境构建
            if (lock) {
                /**
                 * 1. 现获取当前正常的agent列表
                 * 2. 获取可用的agent列表
                 * 3. 优先调用可用的agent执行任务
                 * 4. 如果启动可用的agent失败再调用有任务的agent
                 */
                val activeAgents = agentsResult.data!!.filter {
                    it.status == AgentStatus.IMPORT_OK &&
                            (event.os == it.os || event.os == VMBaseOS.ALL.name)
                }.toHashSet()
                val agentMaps = activeAgents.associateBy { it.agentId }

                val preBuildAgents = HashSet<ThirdPartyAgent>()
                thirdPartyAgentBuildService.getPreBuildAgents(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    vmSeqId = event.vmSeqId
                ).forEach {
                    val agent = agentMaps[it.agentId]
                    if (agent != null) {
                        preBuildAgents.add(agent)
                    }
                }

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

                /**
                 * 根据哪些agent没有任何任务并且是在最近构建中使用到的Agent
                 */
                logDebug(
                    buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                            "开始查找最近使用过并且当前没有任何任务的空闲构建机...(Searching Agent: Most recently used and idle)"
                )
                if (startEmptyAgents(
                        event = event,
                        dispatchType = dispatchType,
                        agents = preBuildAgents,
                        hasTryAgents = hasTryAgents,
                        runningBuildsMapper = runningBuildsMapper
                    )
                ) {
                    logger.info(
                        "${event.buildId}|START_AGENT|" +
                                "j(${event.vmSeqId})|dispatchType=$dispatchType|get preBuildAgents"
                    )
                    return
                }

                logger.info(
                    "[${event.projectId}|${event.pipelineId}|" +
                            "${event.buildId}|${event.vmSeqId}]" +
                            " Start to check the available task agents of pre build agents"
                )
                logDebug(
                    buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                            "查找最近使用过并且未达到最大构建数的构建机...(Searching Agent: Recently used and parallel available)"
                )
                /**
                 * 根据哪些agent有任务并且是在最近构建中使用到的Agent，同时当前构建任务还没到达该Agent最大并行数
                 */
                if (startAvailableAgents(
                        event = event,
                        dispatchType = dispatchType,
                        agents = preBuildAgents,
                        hasTryAgents = hasTryAgents,
                        runningBuildsMapper = runningBuildsMapper
                    )
                ) {
                    logger.info(
                        "${event.buildId}|START_AGENT|" +
                                "j(${event.vmSeqId})|dispatchType=$dispatchType|get Available preBuildAgents"
                    )
                    return
                }

                logDebug(
                    buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                            "开始查找没有任何任务的空闲构建机...(Searching Agent: Most idle)"
                )
                /**
                 * 根据哪些agent没有任何任务
                 */
                if (startEmptyAgents(
                        event = event,
                        dispatchType = dispatchType,
                        agents = activeAgents,
                        hasTryAgents = hasTryAgents,
                        runningBuildsMapper = runningBuildsMapper
                    )
                ) {
                    logger.info(
                        "${event.buildId}|START_AGENT|" +
                                "j(${event.vmSeqId})|dispatchType=$dispatchType|get activeAgents"
                    )
                    return
                }

                logDebug(
                    buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                            "开始查找当前构建任务还没到达最大并行数构建机...(Searching Agent: Parallel available)"
                )
                /**
                 * 根据哪些agent有任务，同时当前构建任务还没到达该Agent最大并行数
                 */
                if (startAvailableAgents(
                        event = event,
                        dispatchType = dispatchType,
                        agents = activeAgents,
                        hasTryAgents = hasTryAgents,
                        runningBuildsMapper = runningBuildsMapper
                    )
                ) {
                    logger.info(
                        "${event.buildId}|START_AGENT|" +
                                "j(${event.vmSeqId})|dispatchType=$dispatchType|get Available activeAgents"
                    )
                    return
                }

                if (event.retryTime == 1) {
                    log(buildLogPrinter, event, message = "没有可用Agent，等待Agent释放...(No Agent available, wait)")
                }
            } else {
                log(buildLogPrinter, event, message = "构建环境并发保护，稍后重试...(Env busy, wait)")
            }

            logger.info(
                "${event.buildId}|START_AGENT|j(${event.vmSeqId})|dispatchType=$dispatchType|Not Found, Retry!"
            )
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = event,
                errorCodeEnum = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
                errorMessage = "${event.buildId}|${event.vmSeqId} " +
                        " 构建环境无可分配构建机，等待超时（queue-timeout-minutes=${event.queueTimeoutMinutes}）"
            )
        } finally {
            redisLock.unlock()
        }
    }

    override fun retry(
        client: Client,
        buildLogPrinter: BuildLogPrinter,
        pipelineEventDispatcher: PipelineEventDispatcher,
        event: PipelineAgentStartupEvent,
        errorCodeEnum: ErrorCodeEnum?,
        errorMessage: String?
    ) {
        if (event.retryTime > 6 * (event.queueTimeoutMinutes ?: 10)) {
            // 置为失败
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorType = errorCodeEnum?.errorType ?: ErrorCodeEnum.SYSTEM_ERROR.errorType,
                errorCode = errorCodeEnum?.errorCode ?: ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                errorMsg = errorMessage ?: "Fail to start up after 60 retries"
            )
            AlertUtils.doAlert(
                level = AlertLevel.HIGH, title = "DevOps Alert Notify",
                message =
                "Start Build Fail! pipeline(${event.pipelineId}) buildId(${event.buildId}) type(${event.dispatchType})"
            )
            return
        }
        logDebug(buildLogPrinter, event, "构建机繁忙，继续重试(Agent is busy) - retry: ${event.retryTime + 1}")

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
            event = event,
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
            event = event,
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
        event: PipelineAgentStartupEvent,
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
                    if (startEnvAgentBuild(event, it, dispatchType, hasTryAgents)) {
                        logger.info(
                            "[${it.projectId}|$[${event.pipelineId}|${event.buildId}|${it.agentId}] " +
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
        event: PipelineAgentStartupEvent,
        agent: ThirdPartyAgent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        hasTryAgents: HashSet<String>
    ): Boolean {
        if (hasTryAgents.contains(agent.agentId)) {
            return false
        }
        hasTryAgents.add(agent.agentId)
        if (buildByAgentId(event, agent, dispatchType.workspace)) {
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
