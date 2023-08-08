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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.BK_AGENT_IS_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_BUSY
import com.tencent.devops.dispatch.constants.BK_MAX_BUILD_SEARCHING_AGENT
import com.tencent.devops.dispatch.constants.BK_NO_AGENT_AVAILABLE
import com.tencent.devops.dispatch.constants.BK_QUEUE_TIMEOUT_MINUTES
import com.tencent.devops.dispatch.constants.BK_SCHEDULING_SELECTED_AGENT
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT_MOST_IDLE
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT_PARALLEL_AVAILABLE
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
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("UNUSED", "ComplexMethod", "LongMethod", "NestedBlockDepth", "MagicNumber")
class ThirdPartyAgentDispatcher @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val thirdPartyAgentBuildService: ThirdPartyAgentService,
    private val dispatchService: DispatchService
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
                        agentType = originDispatchType.agentType,
                        dockerInfo = null
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
            thirdPartyAgentBuildService.finishBuild(event)
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
                errorMsg = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "(System Error) - ${agentResult.message}"
            )
            return
        }

        if (agentResult.agentStatus != AgentStatus.IMPORT_OK) {
            onFailBuild(
                client = client,
                buildLogPrinter = buildLogPrinter,
                event = event,
                errorCodeEnum = ErrorCodeEnum.VM_STATUS_ERROR,
                errorMsg = ErrorCodeEnum.THIRD_PARTY_BUILD_MACHINE_STATUS_ERROR.getErrorMessage() +
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
                errorMsg = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "(System Error) - $dispatchType agent is null"
            )
            return
        }

        if (!buildByAgentId(event, agentResult.data!!, dispatchType.workspace, dispatchType.dockerInfo)) {
            retry(
                client = client,
                buildLogPrinter = buildLogPrinter,
                pipelineEventDispatcher = pipelineEventDispatcher,
                event = event,
                errorCodeEnum = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
                errorMessage = ErrorCodeEnum.CONSTANT_AGENTS_UPGRADING_OR_TIMED_OUT.getErrorMessage(
                    params = arrayOf(dispatchType.displayName)
                )
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

    private fun buildByAgentId(
        event: PipelineAgentStartupEvent,
        agent: ThirdPartyAgent,
        workspace: String?,
        dockerInfo: ThirdPartyAgentDockerInfo?
    ): Boolean {
        val redisLock = ThirdPartyAgentLock(redisOperation, event.projectId, agent.agentId)
        try {
            if (redisLock.tryLock()) {
                if (thirdPartyAgentBuildRedisUtils.isThirdPartyAgentUpgrading(event.projectId, agent.agentId)) {
                    logger.warn("The agent(${agent.agentId}) of project(${event.projectId}) is upgrading")
                    log(
                        event,
                        ErrorCodeEnum.BUILD_MACHINE_UPGRADE_IN_PROGRESS.getErrorMessage(
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ) + " - ${agent.hostname}/${agent.ip}"
                    )
                    return false
                }

                // 生成docker构建机类型的id和secretKey
                val message = if (dockerInfo == null) {
                    null
                } else {
                    dispatchService.setRedisAuth(event)
                }

                // #5806 入库失败就不再写Redis
                inQueue(
                    agent = agent,
                    event = event,
                    agentId = agent.agentId,
                    workspace = workspace,
                    dockerInfo = if (dockerInfo == null) {
                        null
                    } else {
                        ThirdPartyAgentDockerInfoDispatch(
                            agentId = message!!.hashId,
                            secretKey = message.secretKey,
                            info = dockerInfo
                        )
                    }
                )

                // 保存构建详情
                saveAgentInfoToBuildDetail(event = event, agent = agent)

                logger.info("${event.buildId}|START_AGENT_BY_ID|j(${event.vmSeqId})|agent=${agent.agentId}")
                log(
                    event,
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SCHEDULING_SELECTED_AGENT,
                        params = arrayOf(agent.hostname, agent.ip),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
                return true
            } else {
                log(
                    event,
                    ErrorCodeEnum.BUILD_MACHINE_BUSY.getErrorMessage(
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + "(Agent is busy) - ${agent.hostname}/${agent.ip}"
                )
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

    private fun inQueue(
        agent: ThirdPartyAgent,
        event: PipelineAgentStartupEvent,
        agentId: String,
        workspace: String?,
        dockerInfo: ThirdPartyAgentDockerInfoDispatch?
    ) {
        thirdPartyAgentBuildService.queueBuild(
            agent = agent,
            thirdPartyAgentWorkspace = workspace ?: "",
            event = event,
            retryCount = 0,
            dockerInfo = dockerInfo
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

    @Suppress("ComplexMethod", "LongMethod", "NestedBlockDepth", "MagicNumber")
    private fun buildByEnvId(event: PipelineAgentStartupEvent, dispatchType: ThirdPartyAgentEnvDispatchType) {
        val agentsResult = try {
            when (dispatchType.agentType) {
                AgentType.ID -> {
                    client.get(ServiceThirdPartyAgentResource::class)
                        .getAgentsByEnvId(
                            event.projectId,
                            dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                                ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName)
                }

                AgentType.NAME -> {
                    client.get(ServiceThirdPartyAgentResource::class)
                        .getAgentsByEnvName(
                            event.projectId,
                            dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                                ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName)
                }
            }
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
                    e.message ?: (ErrorCodeEnum.GET_VM_ERROR.getErrorMessage() + "(${dispatchType.envName})")
                }
            )
            return
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
                errorMessage = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "(System Error) - ${dispatchType.envName}: ${agentsResult.message}"
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
                errorMessage = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "System Error) - ${dispatchType.envName}: agent is null"
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
                errorMessage = ErrorCodeEnum.BUILD_NODE_IS_EMPTY.getErrorMessage(
                    params = arrayOf(dispatchType.envName)
                ) + "build cluster： ${dispatchType.envName} (env(${dispatchType.envName}) is empty)"
            )
            return
        }

        ThirdPartyAgentEnvLock(redisOperation, event.projectId, dispatchType.envName).use { redisLock ->
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
                }
                // 没有可用构建机列表进入下一次重试, 修复获取最近构建构建机超过10次不构建会被驱逐出最近构建机列表的BUG
                if (activeAgents.isNotEmpty() && pickupAgent(activeAgents, event, dispatchType)) return
            } else {
                log(
                    buildLogPrinter,
                    event,
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = BK_ENV_BUSY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
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
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_QUEUE_TIMEOUT_MINUTES,
                            language = I18nUtil.getDefaultLocaleLanguage(),
                            params = arrayOf("${event.queueTimeoutMinutes}")
                        )
            )
        }
    }

    private fun pickupAgent(
        activeAgents: List<ThirdPartyAgent>,
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType
    ): Boolean {
        val agentMaps = activeAgents.associateBy { it.agentId }

        val preBuildAgents = ArrayList<ThirdPartyAgent>(agentMaps.size)
        thirdPartyAgentBuildService.getPreBuildAgentIds(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            vmSeqId = event.vmSeqId,
            size = activeAgents.size.coerceAtLeast(1)
        ).forEach { agentId -> agentMaps[agentId]?.let { agent -> preBuildAgents.add(agent) } }

        val hasTryAgents = HashSet<String>()
        val runningBuildsMapper = HashMap<String/*AgentId*/, Int/*running builds*/>()
        // docker和二进制任务区分开，所以单独设立一个
        val dockerRunningBuildsMapper = HashMap<String/*AgentId*/, Int/*running builds*/>()

        val pbAgents = sortAgent(event, dispatchType, preBuildAgents, runningBuildsMapper, dockerRunningBuildsMapper)
        /**
         * 1. 最高优先级的agent:
         *     a. 最近构建机中使用过这个构建机,并且
         *     b. 当前没有任何构建机任务
         * 2. 次高优先级的agent:
         *     a. 最近构建机中使用过这个构建机,并且
         *     b. 当前有构建任务,选当前正在运行任务最少的构建机(没有达到当前构建机的最大并发数)
         * 3. 第三优先级的agent:
         *     a. 当前没有任何构建机任务
         * 4. 第四优先级的agent:
         *     a. 当前有构建任务,选当前正在运行任务最少的构建机(没有达到当前构建机的最大并发数)
         */

        /**
         * 最高优先级的agent: 根据哪些agent没有任何任务并且是在最近构建中使用到的Agent
         */
        logDebug(
            buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SEARCHING_AGENT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        if (startEmptyAgents(event, dispatchType, pbAgents, hasTryAgents)) {
            logger.info("${event.buildId}|START_AGENT|j(${event.vmSeqId})|dispatchType=$dispatchType|Get Lv.1")
            return true
        }

        logDebug(
            buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_MAX_BUILD_SEARCHING_AGENT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        /**
         * 次高优先级的agent: 最近构建机中使用过这个构建机,并且当前有构建任务,选当前正在运行任务最少的构建机(没有达到当前构建机的最大并发数)
         */
        if (startAvailableAgents(event, dispatchType, pbAgents, hasTryAgents)) {
            logger.info("${event.buildId}|START_AGENT|j(${event.vmSeqId})|dispatchType=$dispatchType|Get Lv.2")
            return true
        }

        logDebug(
            buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SEARCHING_AGENT_MOST_IDLE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        val allAgents = sortAgent(event, dispatchType, activeAgents, runningBuildsMapper, dockerRunningBuildsMapper)
        /**
         * 第三优先级的agent: 当前没有任何构建机任务
         */
        if (startEmptyAgents(event, dispatchType, allAgents, hasTryAgents)) {
            logger.info("${event.buildId}|START_AGENT|j(${event.vmSeqId})|dispatchType=$dispatchType|pickup Lv.3")
            return true
        }

        logDebug(
            buildLogPrinter, event, message = "retry: ${event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SEARCHING_AGENT_PARALLEL_AVAILABLE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        /**
         * 第四优先级的agent: 当前有构建任务,选当前正在运行任务最少的构建机(没有达到当前构建机的最大并发数)
         */
        if (startAvailableAgents(event, dispatchType, allAgents, hasTryAgents)
        ) {
            logger.info("${event.buildId}|START_AGENT|j(${event.vmSeqId})|dispatchType=$dispatchType|Get Lv.4")
            return true
        }

        if (event.retryTime == 1) {
            log(
                buildLogPrinter = buildLogPrinter,
                event = event,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_NO_AGENT_AVAILABLE,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        }
        return false
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
            return
        }
        logDebug(
            buildLogPrinter = buildLogPrinter,
            event = event,
            message = I18nUtil.getCodeLanMessage(
                messageCode = BK_AGENT_IS_BUSY,
                language = I18nUtil.getDefaultLocaleLanguage()
            ) + " - retry: ${event.retryTime + 1}"
        )

        event.retryTime += 1
        event.delayMills = 10000
        pipelineEventDispatcher.dispatch(event)
    }

    private fun startEmptyAgents(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<Triple<ThirdPartyAgent, Int, Int>>,
        hasTryAgents: HashSet<String>
    ): Boolean {
        return startAgentsForEnvBuild(event, dispatchType, agents, hasTryAgents, idleAgentMatcher)
    }

    private fun startAvailableAgents(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<Triple<ThirdPartyAgent, Int, Int>>,
        hasTryAgents: HashSet<String>
    ): Boolean {
        return startAgentsForEnvBuild(event, dispatchType, agents, hasTryAgents, availableAgentMatcher)
    }

    private fun startAgentsForEnvBuild(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<Triple<ThirdPartyAgent, Int, Int>>,
        hasTryAgents: HashSet<String>,
        agentMatcher: AgentMatcher
    ): Boolean {
        if (agents.isNotEmpty()) {
            // 当前正在运行任务少升序开始遍历(通常任务少是负载最低,但不完全是,负载取决于构建机上运行的任务大小,目前未有采集,先只按任务数来判断)
            agents.forEach {
                if (hasTryAgents.contains(it.first.agentId)) {
                    return@forEach
                }

                if (agentMatcher.match(
                        agent = it.first,
                        runningCnt = it.second,
                        dockerBuilder = dispatchType.dockerInfo != null,
                        dockerRunningCnt = it.third
                    )
                ) {
                    val agent = it.first
                    if (startEnvAgentBuild(event, agent, dispatchType, hasTryAgents)) {
                        logger.info(
                            "[${event.projectId}|$[${event.pipelineId}|${event.buildId}|${agent.agentId}] " +
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
        return buildByAgentId(event, agent, dispatchType.workspace, dispatchType.dockerInfo)
    }

    private fun getRunningCnt(agentId: String, runningBuildsMapper: HashMap<String, Int>): Int {
        var runningCnt = runningBuildsMapper[agentId]
        if (runningCnt == null) {
            runningCnt = thirdPartyAgentBuildService.getRunningBuilds(agentId)
            runningBuildsMapper[agentId] = runningCnt
        }
        return runningCnt
    }

    private fun getDockerRunningCnt(agentId: String, dockerRunningBuildsMapper: HashMap<String, Int>): Int {
        var dockerRunningCnt = dockerRunningBuildsMapper[agentId]
        if (dockerRunningCnt == null) {
            dockerRunningCnt = thirdPartyAgentBuildService.getDockerRunningBuilds(agentId)
            dockerRunningBuildsMapper[agentId] = dockerRunningCnt
        }
        return dockerRunningCnt
    }

    interface AgentMatcher {
        fun match(runningCnt: Int, agent: ThirdPartyAgent, dockerBuilder: Boolean, dockerRunningCnt: Int): Boolean
    }

    class IdleAgent : AgentMatcher {
        override fun match(
            runningCnt: Int,
            agent: ThirdPartyAgent,
            dockerBuilder: Boolean,
            dockerRunningCnt: Int
        ): Boolean = if (dockerBuilder) dockerRunningCnt == 0 else runningCnt == 0
    }

    class AvailableAgent : AgentMatcher {
        override fun match(
            runningCnt: Int,
            agent: ThirdPartyAgent,
            dockerBuilder: Boolean,
            dockerRunningCnt: Int
        ): Boolean {
            return if (dockerBuilder) {
                agent.dockerParallelTaskCount != null &&
                        agent.dockerParallelTaskCount!! > 0 &&
                        agent.dockerParallelTaskCount!! > dockerRunningCnt
            } else {
                agent.parallelTaskCount != null &&
                        agent.parallelTaskCount!! > 0 &&
                        agent.parallelTaskCount!! > runningCnt
            }
        }
    }

    private fun sortAgent(
        event: PipelineAgentStartupEvent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<ThirdPartyAgent>,
        runningBuildsMapper: HashMap<String, Int>,
        dockerRunningBuildsMapper: HashMap<String, Int>
    ): MutableList<Triple<ThirdPartyAgent, Int/*runningCnt*/, Int/*dockerRunningCnt*/>> {
        val sortQ = mutableListOf<Triple<ThirdPartyAgent, Int/*runningCnt*/, Int/*dockerRunningCnt*/>>()
        agents.forEach {
            val runningCnt = getRunningCnt(it.agentId, runningBuildsMapper)
            val dockerRunningCnt = if (dispatchType.dockerInfo == null) {
                0
            } else {
                getDockerRunningCnt(it.agentId, dockerRunningBuildsMapper)
            }
            sortQ.add(Triple(it, runningCnt, dockerRunningCnt))
            logDebug(
                buildLogPrinter, event,
                message = "[${it.agentId}]${it.hostname}/${it.ip}, Jobs:$runningCnt, DockerJobs:$dockerRunningCnt"
            )
        }
        sortQ.sortBy { it.second + it.third }
        return sortQ
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentDispatcher::class.java)
        private val availableAgentMatcher = AvailableAgent()
        private val idleAgentMatcher = IdleAgent()
    }
}
