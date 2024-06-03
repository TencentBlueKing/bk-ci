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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.AGENT_REUSE_MUTEX_REDISPATCH
import com.tencent.devops.dispatch.constants.AGENT_REUSE_MUTEX_WAIT_REUSED_ENV
import com.tencent.devops.dispatch.constants.BK_AGENT_IS_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_WORKER_ERROR_IGNORE
import com.tencent.devops.dispatch.constants.BK_MAX_BUILD_SEARCHING_AGENT
import com.tencent.devops.dispatch.constants.BK_NO_AGENT_AVAILABLE
import com.tencent.devops.dispatch.constants.BK_QUEUE_TIMEOUT_MINUTES
import com.tencent.devops.dispatch.constants.BK_SCHEDULING_SELECTED_AGENT
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT_MOST_IDLE
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT_PARALLEL_AVAILABLE
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.utils.ThirdPartyAgentEnvLock
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyRedisBuild
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.SetContextVarData
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
@Suppress("UNUSED", "ComplexMethod", "LongMethod", "NestedBlockDepth", "MagicNumber")
class ThirdPartyDispatchService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val thirdPartyAgentBuildService: ThirdPartyAgentService
) {
    fun canDispatch(event: PipelineAgentStartupEvent) =
        event.dispatchType is ThirdPartyAgentIDDispatchType ||
                event.dispatchType is ThirdPartyAgentEnvDispatchType ||
                event.dispatchType is ThirdPartyDevCloudDispatchType

    fun startUp(dispatchMessage: DispatchMessage) {
        when (dispatchMessage.event.dispatchType) {
            is ThirdPartyAgentIDDispatchType -> {
                val dispatchType = dispatchMessage.event.dispatchType as ThirdPartyAgentIDDispatchType
                if (!dispatchType.agentType.isReuse()) {
                    buildByAgentId(dispatchMessage, dispatchType)
                    return
                }
                // 只要是复用就先拿一下上下文，可能存在同stage但又先后的情况
                val agentId = dispatchMessage.getAgentReuseContextVar(dispatchType.displayName)

                // 是复用，但是和被复用对象在同一stage且先后顺序未知
                if (dispatchType.reusedInfo != null && agentId.isNullOrBlank()) {
                    dispatchType.displayName = dispatchType.reusedInfo!!.value
                    buildByAgentId(dispatchMessage, dispatchType)
                    return
                }

                if (agentId.isNullOrBlank()) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.errorType,
                        errorCode = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.errorCode,
                        formatErrorMessage = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.formatErrorMessage,
                        errorMessage = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.getErrorMessage(
                            params = arrayOf(dispatchType.displayName)
                        )
                    )
                }

                buildByAgentId(dispatchMessage, dispatchType.copy(displayName = agentId, agentType = AgentType.ID))
            }

            is ThirdPartyAgentEnvDispatchType -> {
                val dispatchType = dispatchMessage.event.dispatchType as ThirdPartyAgentEnvDispatchType
                if (!dispatchType.agentType.isReuse()) {
                    buildByEnvId(dispatchMessage, dispatchType)
                    return
                }
                // 只要是复用就先拿一下上下文，可能存在同stage但又先后的情况
                val agentId = dispatchMessage.getAgentReuseContextVar(dispatchType.envName)

                // 是复用，但是和被复用对象在同一stage且先后顺序未知
                // AgentEnv 类型的需要等到被复用对象选出节点再执行
                if (dispatchType.reusedInfo != null && agentId.isNullOrBlank()) {
                    logWarn(
                        dispatchMessage.event,
                        I18nUtil.getCodeLanMessage(
                            messageCode = AGENT_REUSE_MUTEX_WAIT_REUSED_ENV,
                            language = I18nUtil.getDefaultLocaleLanguage(),
                            params = arrayOf(dispatchType.envName)
                        )
                    )
                    throw DispatchRetryMQException(
                        errorCodeEnum = ErrorCodeEnum.BUILD_ENV_PREPARATION,
                        errorMessage = "${dispatchMessage.event.buildId}|${dispatchMessage.event.vmSeqId} " +
                                I18nUtil.getCodeLanMessage(
                                    messageCode = BK_QUEUE_TIMEOUT_MINUTES,
                                    language = I18nUtil.getDefaultLocaleLanguage(),
                                    params = arrayOf("${dispatchMessage.event.queueTimeoutMinutes}")
                                )
                    )
                }

                if (agentId.isNullOrBlank()) {
                    throw BuildFailureException(
                        errorType = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.errorType,
                        errorCode = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.errorCode,
                        formatErrorMessage = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.formatErrorMessage,
                        errorMessage = ErrorCodeEnum.AGENT_REUSE_MUTEX_AGENT_NOT_FOUND.getErrorMessage(
                            params = arrayOf(dispatchType.envName)
                        )
                    )
                }

                buildByAgentId(
                    dispatchMessage,
                    ThirdPartyAgentIDDispatchType(
                        displayName = agentId,
                        workspace = dispatchType.workspace,
                        agentType = AgentType.ID,
                        dockerInfo = dispatchType.dockerInfo,
                        reusedInfo = dispatchType.reusedInfo
                    )
                )
                return
            }

            is ThirdPartyDevCloudDispatchType -> {
                val originDispatchType = dispatchMessage.event.dispatchType as ThirdPartyDevCloudDispatchType
                buildByAgentId(
                    dispatchMessage = dispatchMessage,
                    dispatchType = ThirdPartyAgentIDDispatchType(
                        displayName = originDispatchType.displayName,
                        workspace = originDispatchType.workspace,
                        agentType = originDispatchType.agentType,
                        dockerInfo = null,
                        reusedInfo = null
                    )
                )
            }

            else -> {
                throw InvalidParamException("Unknown agent type - ${dispatchMessage.event.dispatchType}")
            }
        }
    }

    private fun buildByAgentId(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentIDDispatchType
    ) {

        val agentResult = if (dispatchType.idType()) {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentById(dispatchMessage.event.projectId, dispatchType.displayName)
        } else {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentByDisplayName(dispatchMessage.event.projectId, dispatchType.displayName)
        }

        if (agentResult.isNotOk()) {
            throw BuildFailureException(
                errorType = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.errorType,
                errorCode = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.formatErrorMessage,
                errorMessage = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "(System Error) - ${agentResult.message}"
            )
        }

        if (agentResult.agentStatus != AgentStatus.IMPORT_OK) {
            throw BuildFailureException(
                errorType = ErrorCodeEnum.VM_STATUS_ERROR.errorType,
                errorCode = ErrorCodeEnum.VM_STATUS_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.VM_STATUS_ERROR.formatErrorMessage,
                errorMessage = ErrorCodeEnum.VM_STATUS_ERROR.getErrorMessage() +
                        "- ${dispatchType.displayName}| status: (${agentResult.agentStatus?.name})"
            )
        }

        if (agentResult.data == null) {
            throw BuildFailureException(
                errorType = ErrorCodeEnum.FOUND_AGENT_ERROR.errorType,
                errorCode = ErrorCodeEnum.FOUND_AGENT_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.FOUND_AGENT_ERROR.formatErrorMessage,
                errorMessage = ErrorCodeEnum.FOUND_AGENT_ERROR.getErrorMessage() +
                        "(System Error) - $dispatchType agent is null"
            )
        }

        if (!doAgentInQueue(
                dispatchMessage = dispatchMessage,
                agent = agentResult.data!!,
                workspace = dispatchType.workspace,
                dockerInfo = dispatchType.dockerInfo,
                envId = null,
                ignoreEnvAgentIds = null,
                hasReuseMutex = dispatchType.hasReuseMutex()
            )
        ) {
            logDebug(
                buildLogPrinter = buildLogPrinter,
                dispatchMessage = dispatchMessage,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_AGENT_IS_BUSY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " - retry: ${dispatchMessage.event.retryTime + 1}"
            )

            throw DispatchRetryMQException(
                errorCodeEnum = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
                errorMessage = ErrorCodeEnum.CONSTANT_AGENTS_UPGRADING_OR_TIMED_OUT.getErrorMessage(
                    params = arrayOf(dispatchType.displayName)
                )
            )
        }
    }

    private fun doAgentInQueue(
        dispatchMessage: DispatchMessage,
        agent: ThirdPartyAgent,
        workspace: String?,
        dockerInfo: ThirdPartyAgentDockerInfo?,
        envId: Long?,
        ignoreEnvAgentIds: Set<String>?,
        hasReuseMutex: Boolean
    ): Boolean {
        val event = dispatchMessage.event
        val redisLock = ThirdPartyAgentLock(redisOperation, event.projectId, agent.agentId)
        try {
            if (redisLock.tryLock()) {
                if (thirdPartyAgentBuildRedisUtils.isThirdPartyAgentUpgrading(
                        projectId = event.projectId,
                        agentId = agent.agentId
                    )
                ) {
                    logger.warn("The agent(${agent.agentId}) of project(${event.projectId}) is upgrading")
                    logWarn(
                        dispatchMessage.event,
                        ErrorCodeEnum.BUILD_MACHINE_UPGRADE_IN_PROGRESS.getErrorMessage(
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ) + " - ${agent.hostname}/${agent.ip}"
                    )
                    return false
                }

                // #10082 对于复用的机器和被复用的，需要加锁校验看看这台机器能不能使用
                if (hasReuseMutex) {
                    val lockKey = AgentReuseMutex.genAgentReuseMutexLockKey(event.projectId, agent.agentId)
                    val lock = RedisLockByValue(
                        redisOperation = redisOperation,
                        lockKey = lockKey,
                        lockValue = event.buildId,
                        expiredTimeInSeconds = AgentReuseMutex.AGENT_LOCK_TIMEOUT
                    )
                    // 没有拿到锁说明现在这台机被复用互斥占用不能选
                    if (!lock.tryLock()) {
                        logWarn(
                            dispatchMessage.event,
                            I18nUtil.getCodeLanMessage(
                                messageCode = AGENT_REUSE_MUTEX_REDISPATCH,
                                language = I18nUtil.getDefaultLocaleLanguage(),
                                params = arrayOf(
                                    "${agent.agentId}|${agent.hostname}/${agent.ip}", redisOperation.get(lockKey) ?: ""
                                )
                            )
                        )
                        return false
                    }
                    try {
                        // # 10082 设置复用需要的关键字 jobs.<job_id>.container.agent_id，jobId需要为根节点id
                        // 只用给env类型的根节点设置，因为id类型的在引擎 AgentReuseMutexCmd 直接写入了
                        val dispatch = dispatchMessage.event.dispatchType
                        if ((dispatch is ThirdPartyAgentEnvDispatchType) &&
                            dispatch.reusedInfo != null &&
                            dispatch.reusedInfo!!.jobId == null
                        ) {
                            client.get(ServiceVarResource::class).setContextVar(
                                SetContextVarData(
                                    projectId = dispatchMessage.event.projectId,
                                    pipelineId = dispatchMessage.event.pipelineId,
                                    buildId = dispatchMessage.event.buildId,
                                    contextName = AgentReuseMutex.genAgentContextKey(dispatchMessage.event.jobId!!),
                                    contextVal = agent.agentId,
                                    readOnly = true,
                                    rewriteReadOnly = true
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error("inQueue|doAgentInQueue|error", e)
                    }
                }

                // #5806 入库失败就不再写Redis
                inQueue(
                    agent = agent,
                    dispatchMessage = dispatchMessage,
                    agentId = agent.agentId,
                    workspace = workspace,
                    dockerInfo = if (dockerInfo == null) {
                        null
                    } else {
                        ThirdPartyAgentDockerInfoDispatch(
                            agentId = dispatchMessage.id,
                            secretKey = dispatchMessage.secretKey,
                            info = dockerInfo
                        )
                    },
                    envId = envId,
                    ignoreEnvAgentIds = ignoreEnvAgentIds
                )

                // 保存构建详情
                saveAgentInfoToBuildDetail(dispatchMessage = dispatchMessage, agent = agent)

                logger.info(
                    "${event.buildId}|START_AGENT_BY_ID|j(${event.vmSeqId})|" +
                            "agent=${agent.agentId}"
                )
                log(
                    dispatchMessage.event,
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SCHEDULING_SELECTED_AGENT,
                        params = arrayOf(agent.hostname, agent.ip),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
                return true
            } else {
                logWarn(
                    dispatchMessage.event,
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
            containerHashId = event.containerHashId,
            executeCount = event.executeCount ?: 1,
            jobId = event.jobId,
            stepId = VMUtils.genStartVMTaskId(event.vmSeqId)
        )
    }

    private fun logWarn(event: PipelineAgentStartupEvent, logMessage: String) {
        buildLogPrinter.addYellowLine(
            buildId = event.buildId,
            message = logMessage,
            tag = VMUtils.genStartVMTaskId(event.vmSeqId),
            containerHashId = event.containerHashId,
            executeCount = event.executeCount ?: 1,
            jobId = event.jobId,
            stepId = null
        )
    }

    private fun inQueue(
        agent: ThirdPartyAgent,
        dispatchMessage: DispatchMessage,
        agentId: String,
        workspace: String?,
        dockerInfo: ThirdPartyAgentDockerInfoDispatch?,
        envId: Long?,
        ignoreEnvAgentIds: Set<String>?
    ) {
        thirdPartyAgentBuildService.queueBuild(
            agent = agent,
            thirdPartyAgentWorkspace = workspace ?: "",
            dispatchMessage = dispatchMessage,
            retryCount = 0,
            dockerInfo = dockerInfo,
            envId = envId,
            ignoreEnvAgentIds = ignoreEnvAgentIds
        )

        thirdPartyAgentBuildRedisUtils.setThirdPartyBuild(
            agent.secretKey,
            ThirdPartyRedisBuild(
                projectId = dispatchMessage.event.projectId,
                pipelineId = dispatchMessage.event.pipelineId,
                buildId = dispatchMessage.event.buildId,
                agentId = agentId,
                vmSeqId = dispatchMessage.event.vmSeqId,
                vmName = agent.hostname,
                channelCode = dispatchMessage.event.channelCode,
                atoms = dispatchMessage.event.atoms
            )
        )

        // 添加上下文关键字 jobs.<job_id>.container.node_alias
        if (dispatchMessage.event.jobId.isNullOrBlank()) {
            return
        }
        try {
            val detail = client.get(ServiceThirdPartyAgentResource::class).getAgentDetail(
                userId = dispatchMessage.event.userId,
                projectId = dispatchMessage.event.projectId,
                agentHashId = agentId
            ).data
            if (detail == null) {
                logger.warn("inQueue|setContextVar|getAgentDetail $agentId is null")
                return
            }
            client.get(ServiceVarResource::class).setContextVar(
                SetContextVarData(
                    projectId = dispatchMessage.event.projectId,
                    pipelineId = dispatchMessage.event.pipelineId,
                    buildId = dispatchMessage.event.buildId,
                    contextName = "jobs.${dispatchMessage.event.jobId}.container.node_alias",
                    contextVal = detail.displayName,
                    readOnly = true,
                    rewriteReadOnly = true
                )
            )
        } catch (e: Exception) {
            logger.error("inQueue|setContextVar|error", e)
        }
    }

    private fun saveAgentInfoToBuildDetail(dispatchMessage: DispatchMessage, agent: ThirdPartyAgent) {
        client.get(ServiceBuildResource::class).saveBuildVmInfo(
            projectId = dispatchMessage.event.projectId,
            pipelineId = dispatchMessage.event.pipelineId,
            buildId = dispatchMessage.event.buildId,
            vmSeqId = dispatchMessage.event.vmSeqId,
            vmInfo = VmInfo(ip = agent.ip, name = agent.ip)
        )
    }

    @Suppress("ComplexMethod", "LongMethod", "NestedBlockDepth", "MagicNumber")
    private fun buildByEnvId(dispatchMessage: DispatchMessage, dispatchType: ThirdPartyAgentEnvDispatchType) {
        val agentsResult = try {
            if (dispatchType.idType()) {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvId(
                        projectId = dispatchMessage.event.projectId,
                        envId = dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                            ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName
                    )
            } else {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvNameWithId(
                        projectId = dispatchMessage.event.projectId,
                        envName = dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                            ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName
                    )
            }
        } catch (e: Exception) {
            throw BuildFailureException(
                errorType = ErrorCodeEnum.GET_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.GET_VM_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.GET_VM_ERROR.formatErrorMessage,
                errorMessage = if (e is RemoteServiceException) {
                    e.errorMessage
                } else {
                    e.message ?: (ErrorCodeEnum.GET_VM_ERROR.getErrorMessage() + "(${dispatchType.envName})")
                }
            )
        }

        if (agentsResult.status == Response.Status.FORBIDDEN.statusCode) {
            logger.warn(
                "${dispatchMessage.event.buildId}|START_AGENT_FAILED_FORBIDDEN|" +
                        "j(${dispatchMessage.event.vmSeqId})|dispatchType=$dispatchType|err=${agentsResult.message}"
            )

            throw BuildFailureException(
                errorType = ErrorCodeEnum.GET_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.GET_VM_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.GET_VM_ERROR.formatErrorMessage,
                errorMessage = agentsResult.message ?: ""
            )
        }

        if (agentsResult.isNotOk()) {
            logger.warn(
                "${dispatchMessage.event.buildId}|START_AGENT_FAILED|" +
                        "j(${dispatchMessage.event.vmSeqId})|dispatchType=$dispatchType|err=${agentsResult.message}"
            )

            logDebug(
                buildLogPrinter = buildLogPrinter,
                dispatchMessage = dispatchMessage,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_AGENT_IS_BUSY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " - retry: ${dispatchMessage.event.retryTime + 1}"
            )

            throw DispatchRetryMQException(
                errorCodeEnum = ErrorCodeEnum.FOUND_AGENT_ERROR,
                errorMessage = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "(System Error) - ${dispatchType.envName}: ${agentsResult.message}"
            )
        }

        if (agentsResult.data == null) {
            logger.warn(
                "${dispatchMessage.event.buildId}|START_AGENT_FAILED|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|err=null agents"
            )
            logDebug(
                buildLogPrinter = buildLogPrinter,
                dispatchMessage = dispatchMessage,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_AGENT_IS_BUSY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " - retry: ${dispatchMessage.event.retryTime + 1}"
            )

            throw DispatchRetryMQException(
                errorCodeEnum = ErrorCodeEnum.FOUND_AGENT_ERROR,
                errorMessage = ErrorCodeEnum.GET_BUILD_AGENT_ERROR.getErrorMessage() +
                        "System Error) - ${dispatchType.envName}: agent is null"
            )
        }

        val (envId, agentResData) = if (dispatchType.idType()) {
            Pair(
                HashUtil.decodeIdToLong(dispatchType.envName),
                (agentsResult.data!! as List<ThirdPartyAgent>)
            )
        } else {
            (agentsResult.data as Pair<Long?, List<ThirdPartyAgent>>)
        }

        if (agentResData.isEmpty()) {
            logger.warn(
                "${dispatchMessage.event.buildId}|START_AGENT_FAILED|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|err=empty agents"
            )
            throw DispatchRetryMQException(
                errorCodeEnum = ErrorCodeEnum.VM_NODE_NULL,
                errorMessage = ErrorCodeEnum.BUILD_NODE_IS_EMPTY.getErrorMessage(
                    params = arrayOf(dispatchType.envName)
                ) + "build cluster： ${dispatchType.envName} (env(${dispatchType.envName}) is empty)"
            )
        }

        ThirdPartyAgentEnvLock(redisOperation, dispatchMessage.event.projectId, dispatchType.envName).use { redisLock ->
            val lock = redisLock.tryLock(timeout = 5000) // # 超时尝试锁定，防止环境过热锁定时间过长，影响其他环境构建
            if (lock) {
                /**
                 * 1. 现获取当前正常的agent列表
                 * 2. 获取可用的agent列表
                 * 3. 优先调用可用的agent执行任务
                 * 4. 如果启动可用的agent失败再调用有任务的agent
                 */
                val activeAgents = agentResData.filter {
                    it.status == AgentStatus.IMPORT_OK &&
                            (dispatchMessage.event.os == it.os || dispatchMessage.event.os == VMBaseOS.ALL.name)
                }

                var jobEnvActiveAgents = activeAgents
                if (!dispatchMessage.event.ignoreEnvAgentIds.isNullOrEmpty()) {
                    log(
                        buildLogPrinter,
                        dispatchMessage,
                        message = I18nUtil.getCodeLanMessage(
                            messageCode = BK_ENV_WORKER_ERROR_IGNORE,
                            params = arrayOf(dispatchMessage.event.ignoreEnvAgentIds!!.joinToString(",")),
                            language = I18nUtil.getDefaultLocaleLanguage()
                        )
                    )
                    jobEnvActiveAgents =
                        activeAgents.filter { it.agentId !in dispatchMessage.event.ignoreEnvAgentIds!! }
                    if (jobEnvActiveAgents.isEmpty()) {
                        throw BuildFailureException(
                            ErrorCodeEnum.BK_ENV_WORKER_ERROR_IGNORE_ALL_ERROR.errorType,
                            ErrorCodeEnum.BK_ENV_WORKER_ERROR_IGNORE_ALL_ERROR.errorCode,
                            ErrorCodeEnum.BK_ENV_WORKER_ERROR_IGNORE_ALL_ERROR.formatErrorMessage,
                            I18nUtil.getCodeLanMessage(
                                messageCode = ErrorCodeEnum.BK_ENV_WORKER_ERROR_IGNORE_ALL_ERROR.errorCode.toString(),
                                params = arrayOf(dispatchMessage.event.ignoreEnvAgentIds!!.joinToString(",")),
                                language = I18nUtil.getDefaultLocaleLanguage()
                            )
                        )
                    }
                }

                // 没有可用构建机列表进入下一次重试, 修复获取最近构建构建机超过10次不构建会被驱逐出最近构建机列表的BUG
                if (jobEnvActiveAgents.isNotEmpty() && pickupAgent(
                        activeAgents = jobEnvActiveAgents,
                        dispatchMessage = dispatchMessage,
                        dispatchType = dispatchType,
                        envId = envId
                    )
                ) {
                    return
                }
            } else {
                log(
                    buildLogPrinter,
                    dispatchMessage,
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = BK_ENV_BUSY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            }

            logger.info(
                "${dispatchMessage.event.buildId}|START_AGENT|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|Not Found, Retry!"
            )

            logDebug(
                buildLogPrinter = buildLogPrinter,
                dispatchMessage = dispatchMessage,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_AGENT_IS_BUSY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " - retry: ${dispatchMessage.event.retryTime + 1}"
            )

            throw DispatchRetryMQException(
                errorCodeEnum = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
                errorMessage = "${dispatchMessage.event.buildId}|${dispatchMessage.event.vmSeqId} " +
                        I18nUtil.getCodeLanMessage(
                            messageCode = BK_QUEUE_TIMEOUT_MINUTES,
                            language = I18nUtil.getDefaultLocaleLanguage(),
                            params = arrayOf("${dispatchMessage.event.queueTimeoutMinutes}")
                        )
            )
        }
    }

    private fun pickupAgent(
        activeAgents: List<ThirdPartyAgent>,
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        envId: Long?
    ): Boolean {
        val agentMaps = activeAgents.associateBy { it.agentId }

        val preBuildAgents = ArrayList<ThirdPartyAgent>(agentMaps.size)
        thirdPartyAgentBuildService.getPreBuildAgentIds(
            projectId = dispatchMessage.event.projectId,
            pipelineId = dispatchMessage.event.pipelineId,
            vmSeqId = dispatchMessage.event.vmSeqId,
            size = activeAgents.size.coerceAtLeast(1)
        ).forEach { agentId -> agentMaps[agentId]?.let { agent -> preBuildAgents.add(agent) } }

        val hasTryAgents = HashSet<String>()
        val runningBuildsMapper = HashMap<String/*AgentId*/, Int/*running builds*/>()
        // docker和二进制任务区分开，所以单独设立一个
        val dockerRunningBuildsMapper = HashMap<String/*AgentId*/, Int/*running builds*/>()

        val pbAgents = sortAgent(
            dispatchMessage = dispatchMessage,
            dispatchType = dispatchType,
            agents = preBuildAgents,
            runningBuildsMapper = runningBuildsMapper,
            dockerRunningBuildsMapper = dockerRunningBuildsMapper
        )
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
            buildLogPrinter, dispatchMessage,
            message = "retry: ${dispatchMessage.event.retryTime} | " +
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_SEARCHING_AGENT,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
        )
        if (startEmptyAgents(dispatchMessage, dispatchType, pbAgents, hasTryAgents, envId)) {
            logger.info(
                "${dispatchMessage.event.buildId}|START_AGENT|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|Get Lv.1"
            )
            return true
        }

        logDebug(
            buildLogPrinter, dispatchMessage, message = "retry: ${dispatchMessage.event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_MAX_BUILD_SEARCHING_AGENT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        /**
         * 次高优先级的agent: 最近构建机中使用过这个构建机,并且当前有构建任务,选当前正在运行任务最少的构建机(没有达到当前构建机的最大并发数)
         */
        if (startAvailableAgents(dispatchMessage, dispatchType, pbAgents, hasTryAgents, envId)) {
            logger.info(
                "${dispatchMessage.event.buildId}|START_AGENT|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|Get Lv.2"
            )
            return true
        }

        logDebug(
            buildLogPrinter, dispatchMessage, message = "retry: ${dispatchMessage.event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SEARCHING_AGENT_MOST_IDLE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        val allAgents = sortAgent(
            dispatchMessage = dispatchMessage,
            dispatchType = dispatchType,
            agents = activeAgents,
            runningBuildsMapper = runningBuildsMapper,
            dockerRunningBuildsMapper = dockerRunningBuildsMapper
        )
        /**
         * 第三优先级的agent: 当前没有任何构建机任务
         */
        if (startEmptyAgents(dispatchMessage, dispatchType, allAgents, hasTryAgents, envId)) {
            logger.info(
                "${dispatchMessage.event.buildId}|START_AGENT|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|pickup Lv.3"
            )
            return true
        }

        logDebug(
            buildLogPrinter, dispatchMessage, message = "retry: ${dispatchMessage.event.retryTime} | " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_SEARCHING_AGENT_PARALLEL_AVAILABLE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
        )
        /**
         * 第四优先级的agent: 当前有构建任务,选当前正在运行任务最少的构建机(没有达到当前构建机的最大并发数)
         */
        if (startAvailableAgents(dispatchMessage, dispatchType, allAgents, hasTryAgents, envId)
        ) {
            logger.info(
                "${dispatchMessage.event.buildId}|START_AGENT|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|Get Lv.4"
            )
            return true
        }

        if (dispatchMessage.event.retryTime == 1) {
            log(
                buildLogPrinter = buildLogPrinter,
                dispatchMessage = dispatchMessage,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_NO_AGENT_AVAILABLE,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        }
        return false
    }

    private fun startEmptyAgents(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<Triple<ThirdPartyAgent, Int, Int>>,
        hasTryAgents: HashSet<String>,
        envId: Long?
    ): Boolean {
        return startAgentsForEnvBuild(dispatchMessage, dispatchType, agents, hasTryAgents, idleAgentMatcher, envId)
    }

    private fun startAvailableAgents(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<Triple<ThirdPartyAgent, Int, Int>>,
        hasTryAgents: HashSet<String>,
        envId: Long?
    ): Boolean {
        return startAgentsForEnvBuild(dispatchMessage, dispatchType, agents, hasTryAgents, availableAgentMatcher, envId)
    }

    private fun startAgentsForEnvBuild(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: Collection<Triple<ThirdPartyAgent, Int, Int>>,
        hasTryAgents: HashSet<String>,
        agentMatcher: AgentMatcher,
        envId: Long?
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
                    if (startEnvAgentBuild(dispatchMessage, agent, dispatchType, hasTryAgents, envId)) {
                        logger.info(
                            "[${dispatchMessage.event.projectId}|$[${dispatchMessage.event.pipelineId}|" +
                                    "${dispatchMessage.event.buildId}|${agent.agentId}] Success to start the build"
                        )
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun startEnvAgentBuild(
        dispatchMessage: DispatchMessage,
        agent: ThirdPartyAgent,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        hasTryAgents: HashSet<String>,
        envId: Long?
    ): Boolean {
        if (hasTryAgents.contains(agent.agentId)) {
            return false
        }
        hasTryAgents.add(agent.agentId)
        return doAgentInQueue(
            dispatchMessage = dispatchMessage,
            agent = agent,
            workspace = dispatchType.workspace,
            dockerInfo = dispatchType.dockerInfo,
            envId = envId,
            ignoreEnvAgentIds = dispatchMessage.event.ignoreEnvAgentIds,
            hasReuseMutex = dispatchType.hasReuseMutex()
        )
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
        dispatchMessage: DispatchMessage,
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
                buildLogPrinter, dispatchMessage,
                message = "[${it.agentId}]${it.hostname}/${it.ip}, Jobs:$runningCnt, DockerJobs:$dockerRunningCnt"
            )
        }
        sortQ.sortBy { it.second + it.third }
        return sortQ
    }

    fun log(
        buildLogPrinter: BuildLogPrinter,
        dispatchMessage: DispatchMessage,
        message: String
    ) {
        buildLogPrinter.addLine(
            buildId = dispatchMessage.event.buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(dispatchMessage.event.vmSeqId),
            containerHashId = dispatchMessage.event.containerHashId,
            executeCount = dispatchMessage.event.executeCount ?: 1,
            jobId = dispatchMessage.event.jobId,
            stepId = VMUtils.genStartVMTaskId(dispatchMessage.event.vmSeqId)
        )
    }

    fun logDebug(
        buildLogPrinter: BuildLogPrinter,
        dispatchMessage: DispatchMessage,
        message: String
    ) {
        buildLogPrinter.addDebugLine(
            buildId = dispatchMessage.event.buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(dispatchMessage.event.vmSeqId),
            containerHashId = dispatchMessage.event.containerHashId,
            executeCount = dispatchMessage.event.executeCount ?: 1,
            jobId = dispatchMessage.event.jobId,
            stepId = VMUtils.genStartVMTaskId(dispatchMessage.event.vmSeqId)
        )
    }

    private fun DispatchMessage.getAgentReuseContextVar(jobId: String): String? {
        return client.get(ServiceVarResource::class).getBuildVar(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            varName = AgentReuseMutex.genAgentContextKey(jobId)
        ).data?.get(AgentReuseMutex.genAgentContextKey(jobId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDispatchService::class.java)
        private val availableAgentMatcher = AvailableAgent()
        private val idleAgentMatcher = IdleAgent()
    }
}
