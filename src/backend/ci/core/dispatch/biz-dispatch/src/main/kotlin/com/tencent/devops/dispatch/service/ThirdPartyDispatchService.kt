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

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.AGENT_REUSE_MUTEX_WAIT_REUSED_ENV
import com.tencent.devops.dispatch.constants.BK_AGENT_IS_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_NODE_DISABLE
import com.tencent.devops.dispatch.constants.BK_ENV_WORKER_ERROR_IGNORE
import com.tencent.devops.dispatch.constants.BK_MAX_BUILD_SEARCHING_AGENT
import com.tencent.devops.dispatch.constants.BK_NO_AGENT_AVAILABLE
import com.tencent.devops.dispatch.constants.BK_QUEUE_TIMEOUT_MINUTES
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT_MOST_IDLE
import com.tencent.devops.dispatch.constants.BK_SEARCHING_AGENT_PARALLEL_AVAILABLE
import com.tencent.devops.dispatch.constants.BK_THIRD_JOB_ENV_CURR
import com.tencent.devops.dispatch.constants.BK_THIRD_JOB_NODE_CURR
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.tpaqueue.TPAQueueService
import com.tencent.devops.dispatch.service.tpaqueue.TPASingleQueueService
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.ThirdPartyAgentEnvLock
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.EnvNodeAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import jakarta.ws.rs.core.Response

@Service
@Suppress("UNUSED", "ComplexMethod", "LongMethod", "NestedBlockDepth", "MagicNumber")
class ThirdPartyDispatchService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val commonUtil: TPACommonUtil,
    private val thirdPartyAgentBuildService: ThirdPartyAgentService,
    private val tpaQueueService: TPAQueueService,
    private val tpaSingleQueueService: TPASingleQueueService
) {
    fun canDispatch(event: PipelineAgentStartupEvent) =
        event.dispatchType is ThirdPartyAgentIDDispatchType ||
                event.dispatchType is ThirdPartyAgentEnvDispatchType ||
                event.dispatchType is ThirdPartyDevCloudDispatchType

    // 按 Redis 灰度使用新排队逻辑的项目或者流水线
    // project to pipeline1;pipeline2;.....
    private fun useNewQueue(projectId: String, pipelineId: String): Boolean {
        val v = redisOperation.hget(
            DISPATCH_QUEUE_GRAY_PROJECT_PIPELINE, projectId
        ) ?: return false
        if (v.isBlank()) {
            return true
        }
        return v.split(";").toSet().contains(pipelineId)
    }

    fun startUp(dispatchMessage: DispatchMessage) {
        when (dispatchMessage.event.dispatchType) {
            is ThirdPartyAgentIDDispatchType -> {
                val dispatchType = dispatchMessage.event.dispatchType as ThirdPartyAgentIDDispatchType
                // 没有复用逻辑的直接调度
                if (!dispatchType.agentType.isReuse()) {
                    buildByAgentId(dispatchMessage, dispatchType)
                    return
                }
                // 只要是复用就先拿一下上下文，可能存在同stage但被复用的已经跑完了
                val agentId = dispatchMessage.getAgentReuseContextVar(dispatchType.displayName)

                // 是复用，但是和被复用对象在同一stage且先后顺序未知，且被复用对象还没有跑完，这里拿复用对象的资源调度
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

                // 到了这里就剩两种
                // 1、绝对复用有先后区别
                // 2、先后顺序未知，但是客观上被复用对象先跑完了，就按照绝对复用处理
                buildByAgentId(
                    dispatchMessage,
                    dispatchType.copy(displayName = agentId, agentType = AgentType.REUSE_JOB_ID, reusedInfo = null)
                )
            }

            is ThirdPartyAgentEnvDispatchType -> {
                val dispatchType = dispatchMessage.event.dispatchType as ThirdPartyAgentEnvDispatchType
                if (!dispatchType.agentType.isReuse()) {
                    if (useNewQueue(dispatchMessage.event.projectId, dispatchMessage.event.pipelineId)) {
                        tpaQueueService.queue(
                            ThirdPartyAgentDispatchData(dispatchMessage, dispatchType)
                        )
                    } else {
                        buildByEnvId(dispatchMessage, dispatchType)
                    }
                    return
                }
                // 只要是复用就先拿一下上下文，可能存在同stage但又先后的情况
                val agentId = dispatchMessage.getAgentReuseContextVar(dispatchType.envName)

                // 是复用，但是和被复用对象在同一stage且先后顺序未知
                // AgentEnv 类型的需要等到被复用对象选出节点再执行
                if (dispatchType.reusedInfo != null && agentId.isNullOrBlank()) {
                    log(
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
                        agentType = AgentType.REUSE_JOB_ID,
                        dockerInfo = dispatchType.dockerInfo,
                        reusedInfo = null
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
        dispatchMessage.event.dispatchQueueStartTimeMilliSecond = LocalDateTime.now().timestampmilli()
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

        if (!agentInQueue(dispatchMessage, dispatchType, agent = agentResult.data!!, envId = null)) {
            logDebug(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
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

        // 错误结束的在最外边有处理了，这里只管正常逻辑的
        commonUtil.updateQueueTime(
            event = dispatchMessage.event,
            createTime = dispatchMessage.event.dispatchQueueStartTimeMilliSecond ?: return,
            endTime = LocalDateTime.now().timestampmilli()
        )
    }

    private fun agentInQueue(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentDispatch,
        agent: ThirdPartyAgent,
        envId: Long?
    ): Boolean {
        return tpaSingleQueueService.doAgentInQueue(
            data = ThirdPartyAgentDispatchData(
                dispatchMessage = dispatchMessage,
                dispatchType = dispatchType
            ),
            agent = agent,
            envId = envId
        )
    }

    @Suppress("ComplexMethod", "LongMethod", "NestedBlockDepth", "MagicNumber")
    private fun buildByEnvId(dispatchMessage: DispatchMessage, dispatchType: ThirdPartyAgentEnvDispatchType) {
        dispatchMessage.event.dispatchQueueStartTimeMilliSecond = LocalDateTime.now().timestampmilli()
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
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
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
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
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
                (agentsResult.data!! as List<EnvNodeAgent>)
            )
        } else {
            (agentsResult.data as Pair<Long?, List<EnvNodeAgent>>)
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

        val disableAgentIds =
            agentResData.filter { !it.enableNode }.associate { it.agent.agentId to it.nodeDisplayName }
        if (disableAgentIds.isNotEmpty()) {
            log(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_ENV_NODE_DISABLE,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(disableAgentIds.map { "[${it.key}][${it.value}]" }.joinToString(","))
                )
            )
        }

        /**
         * 1. 现获取当前正常的agent列表
         * 2. 获取可用的agent列表
         * 3. 优先调用可用的agent执行任务
         * 4. 如果启动可用的agent失败再调用有任务的agent
         */
        val activeAgents = agentResData.filter {
            it.agent.status == AgentStatus.IMPORT_OK &&
                    (dispatchMessage.event.os == it.agent.os || dispatchMessage.event.os == VMBaseOS.ALL.name) &&
                    it.enableNode
        }.map { it.agent }
        if (activeAgents.isEmpty()) {
            logWarn(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_NO_AGENT_AVAILABLE,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
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

        var jobEnvActiveAgents = activeAgents
        if (!dispatchMessage.event.ignoreEnvAgentIds.isNullOrEmpty()) {
            val data = ThirdPartyAgentDispatchData(dispatchMessage, dispatchType)
            val agentMap = activeAgents.associateBy { it.agentId }
            dispatchMessage.event.ignoreEnvAgentIds?.forEach {
                val a = agentMap[it]
                commonUtil.logWithAgentUrl(data, BK_ENV_WORKER_ERROR_IGNORE, arrayOf(it), a?.nodeId, a?.agentId)
            }
            jobEnvActiveAgents = activeAgents.filter { it.agentId !in dispatchMessage.event.ignoreEnvAgentIds!! }
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

        screenEnvNode(dispatchMessage, dispatchType, jobEnvActiveAgents, envId)
    }

    private fun screenEnvNode(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: List<ThirdPartyAgent>,
        envId: Long?
    ) {
        ThirdPartyAgentEnvLock(redisOperation, dispatchMessage.event.projectId, dispatchType.envName).use { redisLock ->
            val lock = redisLock.tryLock(timeout = 5000) // # 超时尝试锁定，防止环境过热锁定时间过长，影响其他环境构建
            if (lock) {
                // 判断是否有 jobEnv 的限制，检查全集群限制
                checkAllNodeConcurrency(envId, dispatchMessage)

                // 判断是否有 jobEnv 的限制，筛选单节点的并发数
                val activeAgents = checkSingleNodeConcurrency(dispatchMessage, envId, agents)

                // 没有可用构建机列表进入下一次重试, 修复获取最近构建构建机超过10次不构建会被驱逐出最近构建机列表的BUG
                if (activeAgents.isNotEmpty() && pickupAgent(
                        activeAgents = activeAgents,
                        dispatchMessage = dispatchMessage,
                        dispatchType = dispatchType,
                        envId = envId
                    )
                ) {
                    // 错误结束的在最外边有处理了，这里只管正常逻辑的
                    commonUtil.updateQueueTime(
                        event = dispatchMessage.event,
                        createTime = dispatchMessage.event.dispatchQueueStartTimeMilliSecond ?: return,
                        endTime = LocalDateTime.now().timestampmilli()
                    )
                    return
                }
            } else {
                logWarn(
                    dispatchMessage.event,
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_ENV_BUSY,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            }

            logger.info(
                "${dispatchMessage.event.buildId}|START_AGENT|j(${dispatchMessage.event.vmSeqId})|" +
                        "dispatchType=$dispatchType|Not Found, Retry!"
            )

            logWarn(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
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

    private fun checkSingleNodeConcurrency(
        dispatchMessage: DispatchMessage,
        envId: Long?,
        activeAgents: List<ThirdPartyAgent>
    ): List<ThirdPartyAgent> {
        if (dispatchMessage.event.singleNodeConcurrency == null) {
            return activeAgents
        }
        if (envId == null || dispatchMessage.event.jobId.isNullOrBlank()) {
            logger.warn(
                "buildByEnvId|{} has singleNodeConcurrency {} but env {}|job {} null",
                dispatchMessage.event.buildId,
                dispatchMessage.event.singleNodeConcurrency,
                envId,
                dispatchMessage.event.jobId
            )
            return activeAgents
        }

        val jobEnvActiveAgents = mutableListOf<ThirdPartyAgent>()
        val m = thirdPartyAgentBuildService.countAgentsJobRunningAndQueueAll(
            pipelineId = dispatchMessage.event.pipelineId,
            envId = envId,
            jobId = dispatchMessage.event.jobId!!,
            agentIds = activeAgents.map { it.agentId }.toSet(),
            projectId = dispatchMessage.event.projectId
        )
        activeAgents.forEach {
            // 为空说明当前节点没有记录就是没有任务直接加，除非并发是0的情况
            if (m[it.agentId] == null && dispatchMessage.event.singleNodeConcurrency!! > 0) {
                jobEnvActiveAgents.add(it)
                return@forEach
            }
            if (m[it.agentId]!! < dispatchMessage.event.singleNodeConcurrency!!) {
                jobEnvActiveAgents.add(it)
                return@forEach
            }
        }
        // 没有一个节点满足则进入排队机制
        if (jobEnvActiveAgents.isEmpty()) {
            log(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_THIRD_JOB_NODE_CURR,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf(
                        dispatchMessage.event.singleNodeConcurrency!!.toString(),
                        (dispatchMessage.event.queueTimeoutMinutes ?: 10).toString()
                    )
                )
            )
            throw DispatchRetryMQException(
                errorCodeEnum = ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR,
                errorMessage = ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR.getErrorMessage()
            )
        }

        return jobEnvActiveAgents.toList()
    }

    private fun checkAllNodeConcurrency(
        envId: Long?,
        dispatchMessage: DispatchMessage
    ) {
        if (dispatchMessage.event.allNodeConcurrency == null) {
            return
        }
        if (envId != null && !dispatchMessage.event.jobId.isNullOrBlank()) {
            val c = thirdPartyAgentBuildService.countProjectJobRunningAndQueueAll(
                pipelineId = dispatchMessage.event.pipelineId,
                envId = envId,
                jobId = dispatchMessage.event.jobId!!,
                projectId = dispatchMessage.event.projectId
            )
            if (c >= dispatchMessage.event.allNodeConcurrency!!) {
                log(
                    dispatchMessage.event,
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_THIRD_JOB_ENV_CURR,
                        language = I18nUtil.getDefaultLocaleLanguage(),
                        params = arrayOf(
                            c.toString(),
                            dispatchMessage.event.allNodeConcurrency!!.toString(),
                            (dispatchMessage.event.queueTimeoutMinutes ?: 10).toString()
                        )
                    )
                )
                throw DispatchRetryMQException(
                    errorCodeEnum = ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR,
                    errorMessage = ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR.getErrorMessage()
                )
            }
        } else {
            logger.warn(
                "buildByEnvId|{} has allNodeConcurrency {} but env {}|job {} null",
                dispatchMessage.event.buildId,
                dispatchMessage.event.allNodeConcurrency,
                envId,
                dispatchMessage.event.jobId
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
            dispatchMessage.event,
            "retry: ${dispatchMessage.event.retryTime} | " +
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
            dispatchMessage.event,
            "retry: ${dispatchMessage.event.retryTime} | " +
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
            dispatchMessage.event,
            "retry: ${dispatchMessage.event.retryTime} | " +
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
            dispatchMessage.event,
            "retry: ${dispatchMessage.event.retryTime} | " +
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
            logWarn(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
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
        return agentInQueue(dispatchMessage, dispatchType, agent, envId)
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
                (agent.dockerParallelTaskCount == 0) || (agent.dockerParallelTaskCount != null &&
                        agent.dockerParallelTaskCount!! > 0 &&
                        agent.dockerParallelTaskCount!! > dockerRunningCnt)
            } else {
                (agent.parallelTaskCount == 0) || (agent.parallelTaskCount != null &&
                        agent.parallelTaskCount!! > 0 &&
                        agent.parallelTaskCount!! > runningCnt)
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
                dispatchMessage.event,
                "[${it.agentId}]${it.hostname}/${it.ip}, Jobs:$runningCnt, DockerJobs:$dockerRunningCnt"
            )
        }
        sortQ.sortBy { it.second + it.third }
        return sortQ
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

    private fun logDebug(event: PipelineAgentStartupEvent, message: String) {
        buildLogPrinter.addDebugLine(
            buildId = event.buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(event.vmSeqId),
            containerHashId = event.containerHashId,
            executeCount = event.executeCount ?: 1,
            jobId = event.jobId,
            stepId = VMUtils.genStartVMTaskId(event.vmSeqId)
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

    fun finishBuild(event: PipelineAgentShutdownEvent) {
        tpaQueueService.finishQueue(event.buildId, event.vmSeqId)
        thirdPartyAgentBuildService.finishBuild(event.buildId, event.vmSeqId, event.buildResult, event.executeCount)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDispatchService::class.java)
        private val availableAgentMatcher = AvailableAgent()
        private val idleAgentMatcher = IdleAgent()
        const val DISPATCH_QUEUE_GRAY_PROJECT_PIPELINE = "DISPATCH_REDIS_QUEUE_GRAY_PROJECT_PIPELINE"
    }
}
