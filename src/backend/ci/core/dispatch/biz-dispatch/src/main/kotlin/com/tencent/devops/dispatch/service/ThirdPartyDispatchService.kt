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
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.agent.AgentDispatchType
import com.tencent.devops.common.pipeline.type.agent.CreateAgentIdDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.AGENT_REUSE_MUTEX_WAIT_REUSED_ENV
import com.tencent.devops.dispatch.constants.BK_AGENT_IS_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_NODE_DISABLE
import com.tencent.devops.dispatch.constants.BK_ENV_WORKER_ERROR_IGNORE
import com.tencent.devops.dispatch.constants.BK_ENV_WORKER_ERROR_IGNORE_UNLOCK
import com.tencent.devops.dispatch.constants.BK_NO_AGENT_AVAILABLE
import com.tencent.devops.dispatch.constants.BK_QUEUE_TIMEOUT_MINUTES
import com.tencent.devops.dispatch.constants.BK_THIRD_JOB_ENV_CURR
import com.tencent.devops.dispatch.constants.BK_THIRD_JOB_NODE_CURR
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.tpaqueue.TPAQueueService
import com.tencent.devops.dispatch.service.tpaqueue.TPASingleQueueService
import com.tencent.devops.dispatch.utils.DispatchLockCleanup
import com.tencent.devops.dispatch.utils.DispatchLogBuffer
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor
import com.tencent.devops.dispatch.utils.ThirdPartyAgentJobEnvLock
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.ThirdPartyAgentEnvLock
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.AllCreateNodeEnv
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.thirdpartyagent.EnvNodeAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    // 环境锁最长等待时间（毫秒）。节点别名上下文 RPC 仍在锁内，等待过久会长期占用 MQ 消费线程；
    // 拿不到锁时交给 MQ 延迟重试更合适。热点环境抢不到锁（BK_ENV_BUSY）偏多时可调小，默认 2000（原为写死 5000）
    @Value("\${dispatch.thirdPartyEnv.lockWaitMills:2000}")
    private val envLockWaitMills: Long = 2000
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

    // 按构建项目灰度，只有显式 true 才启用；不缓存开关，删除配置即可回退。
    // 现有 DISPATCH_REDIS_QUEUE_GRAY_PROJECT_PIPELINE 新队列仍优先，不改变队列路由。
    internal fun useFineGrainedEnvLock(projectId: String): Boolean = try {
        redisOperation.hget(DISPATCH_ENV_FINE_LOCK_PROJECTS, projectId) == "true"
    } catch (e: Exception) {
        logger.warn("read fine-grained dispatch switch failed|$projectId|fallback to env lock", e)
        false
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
                    dispatchType.copy(
                        displayName = agentId,
                        agentType = AgentDispatchType.REUSE_JOB_ID,
                        reusedInfo = null
                    )
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
                        agentType = AgentDispatchType.REUSE_JOB_ID,
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

            is CreateAgentIdDispatchType -> {
                val originDispatchType = dispatchMessage.event.dispatchType as CreateAgentIdDispatchType
                buildByAgentId(
                    dispatchMessage = dispatchMessage,
                    dispatchType = ThirdPartyAgentIDDispatchType(
                        displayName = originDispatchType.value,
                        workspace = originDispatchType.workspace,
                        agentType = AgentDispatchType.ID,
                        dockerInfo = originDispatchType.dockerInfo,
                        reusedInfo = null
                    ),
                    envId = AllCreateNodeEnv.hashIdToId(originDispatchType.envHashId)
                )
            }

            else -> {
                throw InvalidParamException("Unknown agent type - ${dispatchMessage.event.dispatchType}")
            }
        }
    }

    private fun buildByAgentId(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentIDDispatchType,
        envId: Long? = null
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

        if (!agentInQueue(dispatchMessage, dispatchType, agent = agentResult.data!!, envId = envId)) {
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
                            ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName,
                        userId = dispatchMessage.event.userId
                    )
            } else {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvNameWithId(
                        projectId = dispatchMessage.event.projectId,
                        envName = dispatchType.envProjectId.takeIf { !it.isNullOrBlank() }
                            ?.let { "$it@${dispatchType.envName}" } ?: dispatchType.envName,
                        userId = dispatchMessage.event.userId
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
            (agentsResult.data as Pair<Long, List<EnvNodeAgent>>)
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
                // 这里要解开下复用锁，如果有的话，因为上一次失败的调度是上了锁的
                if (data.dispatchType.hasReuseMutex()) {
                    val lockKey = AgentReuseMutex.genAgentReuseMutexLockKey(data.projectId, it)
                    val lock = RedisLockByValue(
                        redisOperation = redisOperation,
                        lockKey = lockKey,
                        lockValue = data.buildId,
                        expiredTimeInSeconds = AgentReuseMutex.AGENT_LOCK_TIMEOUT
                    )
                    if (lock.unlock()) {
                        commonUtil.logWithAgentUrl(
                            data = data,
                            messageCode = BK_ENV_WORKER_ERROR_IGNORE_UNLOCK,
                            param = arrayOf(it),
                            nodeHashId = a?.nodeId,
                            agentHashId = a?.agentId
                        )
                    }
                }
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

    /**
     * 环境调度时，在环境锁外提前准备的数据。
     * 这些数据都是配置或历史数据，与"当前环境并发计数"无关，提前查询不会造成超调度，
     * 放在锁外可以显著缩短环境锁的持有时间。
     */
    private data class PickupPrepared(
        // 最近构建过的 agent，用于 PRE_BUILD 策略
        val preBuildAgentIds: Set<String>,
        // 环境调度策略
        val strategies: List<DispatchStrategyConfig>,
        // agentId -> 该 agent 拥有的标签键值
        val agentTagValues: Map<String, Map<Long, Set<String>>>,
        // 标签 key 的 id -> 名称
        val tagKeys: Map<Long, String>
    )

    /**
     * 从环境中挑选 agent 并调度。
     *
     * 环境锁的作用是保证 "读取并发计数 -> 判断 -> 写库占位" 的原子性，防止并发超调度，
     * 节点别名仍需在释放 agent 锁前写入，其他收尾操作移到锁外：
     * 1. 锁外预检：job 全集群/单节点并发已满则直接重试，不必排队抢锁
     * 2. 锁外准备：调度策略/标签（RPC）、最近构建机（SQL）
     * 3. 锁内：并发复检 + 批量查负载 + 按策略选机 + 写库占位及节点别名上下文
     * 4. 锁外收尾：构建详情、监控事件、排队耗时统计
     */
    private fun screenEnvNode(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: List<ThirdPartyAgent>,
        envId: Long
    ) {
        // 1. 锁外预检：job 全集群/单节点并发若已满则直接重试，不必排队抢锁。
        // 预检到锁内复检之间，并发计数可能增也可能减；这里只在"已满"时提前拒绝，代价最多是偶尔多重试一轮，
        // 不会误放行——真正的放行判断仍由锁内复检保证。没有配置 jobEnv 并发限制时这两个方法会直接返回。
        // printLog = false：预检通过时不打印任何日志，避免与锁内复检重复；
        // 已满时仍会打印并发已满的构建日志后抛重试（此时不再进锁复检，日志只有这一条）
        checkAllNodeConcurrency(envId, dispatchMessage, printLog = false)
        checkSingleNodeConcurrency(dispatchMessage, envId, agents, printLog = false)

        // 2. 锁外准备与并发竞争无关的数据（策略/标签 RPC、最近构建机 SQL）。
        // 放在预检之后，避免并发已满时白做查询；覆盖原始 agents，供锁内按相同集合复检后选机
        val prepared = preparePickup(dispatchMessage, dispatchType, agents, envId)

        // 只在此次选机调用内缓冲日志，finally 在环境/Job/agent 锁全部释放后发送，异常路径也不会漏日志。
        val result = DispatchLogBuffer.withBuffer(dispatchMessage.event.buildId, buildLogPrinter) {
            if (useFineGrainedEnvLock(dispatchMessage.event.projectId)) {
                screenEnvNodeWithFineGrainedLock(dispatchMessage, dispatchType, agents, envId, prepared)
            } else {
                screenEnvNodeWithEnvLock(dispatchMessage, dispatchType, agents, envId, prepared)
            }
        }
        val (locked, pickupTried, pickedAgent) = result

        // 4. 锁外收尾：任务已入库占位，后续操作不影响并发判断
        val agent = pickedAgent
        if (agent != null) {
            tpaSingleQueueService.afterAgentInQueue(
                data = ThirdPartyAgentDispatchData(dispatchMessage, dispatchType),
                agent = agent
            )
            // 错误结束的在最外边有处理了，这里只管正常逻辑的
            commonUtil.updateQueueTime(
                event = dispatchMessage.event,
                createTime = dispatchMessage.event.dispatchQueueStartTimeMilliSecond ?: return,
                endTime = LocalDateTime.now().timestampmilli()
            )
            return
        }

        if (!locked) {
            logWarn(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_ENV_BUSY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
        } else if (pickupTried && dispatchMessage.event.retryTime == 1) {
            // 与原 pickupAgent 内的逻辑保持一致：只有真正按策略选过机、但所有策略都没匹配到可用 agent 时，
            // 才在第一次重试时提示；可用 agent 列表为空、没有进入选机的情况不打印
            // 之后与原逻辑一样继续打印 BK_AGENT_IS_BUSY 并抛重试
            logWarn(
                dispatchMessage.event,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_NO_AGENT_AVAILABLE,
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

    private data class EnvPickupResult(
        val locked: Boolean,
        val pickupTried: Boolean = false,
        val agent: ThirdPartyAgent? = null
    )

    /** 默认路径仍按环境所属项目 + 数字 envId 互斥，兼容名称/hashId、共享环境。 */
    private fun screenEnvNodeWithEnvLock(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: List<ThirdPartyAgent>,
        envId: Long,
        prepared: PickupPrepared
    ): EnvPickupResult {
        val projectId = dispatchType.envProjectId?.takeIf { it.isNotBlank() } ?: dispatchMessage.event.projectId
        val lock = ThirdPartyAgentEnvLock(redisOperation, projectId, envId.toString())
        return DispatchLockCleanup.run<EnvPickupResult>(
            description = "env|$projectId|$envId|${dispatchMessage.event.buildId}",
            unlock = { lock.unlock() },
            isQueued = { it.agent != null }
        ) {
            if (!lock.tryLock(timeout = envLockWaitMills)) return@run EnvPickupResult(false)
            pickupWithJobLock(dispatchMessage, dispatchType, agents, envId, prepared)
        }
    }

    /**
     * 灰度路径不拿环境锁，不同 Job 可以并行选机。
     * 批量负载只作为锁外选机快照，最终 nodeRule 必须在目标 agent 锁内重新查询，不能靠缓存放行。
     * 暂不引入跨请求计数缓存/占位表，沿用数据库真实计数与重试记录口径。
     */
    private fun screenEnvNodeWithFineGrainedLock(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: List<ThirdPartyAgent>,
        envId: Long,
        prepared: PickupPrepared
    ): EnvPickupResult {
        val counts = thirdPartyAgentBuildService.countRunningAndQueueByAgents(agents.map { it.agentId }.toSet())
        return pickupWithJobLock(dispatchMessage, dispatchType, agents, envId, prepared, counts)
    }

    private fun pickupWithJobLock(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: List<ThirdPartyAgent>,
        envId: Long,
        prepared: PickupPrepared,
        counts: Map<String, Pair<Int, Int>>? = null
    ): EnvPickupResult {
        val event = dispatchMessage.event
        val pick = {
            checkAllNodeConcurrency(envId, dispatchMessage)
            // 必须使用原始 agents：在给定集合内取最新记录，不能缩小为候选节点，否则会重新计入旧重试。
            val activeAgents = checkSingleNodeConcurrency(dispatchMessage, envId, agents)
            EnvPickupResult(
                locked = true,
                pickupTried = activeAgents.isNotEmpty(),
                agent = if (activeAgents.isEmpty()) null else pickupAgent(
                    activeAgents, dispatchMessage, dispatchType, envId, prepared, counts
                )
            )
        }
        // 沿用 jobId 缺失时不校验 Job 配额的行为；物理节点规则仍会在 agent 锁内复检。
        val jobId = event.jobId?.takeIf { it.isNotBlank() } ?: return pick()
        val lock = ThirdPartyAgentJobEnvLock(redisOperation, event.projectId, event.pipelineId, jobId, envId)
        return DispatchLockCleanup.run<EnvPickupResult>(
            description = "job|${event.projectId}|${event.pipelineId}|$jobId|$envId|${event.buildId}",
            unlock = { lock.unlock() },
            isQueued = { it.agent != null }
        ) {
            // 热点 Job 拿不到配额锁直接 MQ 重试，不持有环境锁自旋等待。
            if (!lock.tryLock()) EnvPickupResult(false) else pick()
        }
    }

    /**
     * 锁外准备调度所需的配置/历史数据，详见 [PickupPrepared]
     */
    private fun preparePickup(
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        agents: List<ThirdPartyAgent>,
        envId: Long
    ): PickupPrepared {
        val nodeIdToAgentId = mutableMapOf<Long, String>()
        agents.forEach { agent ->
            agent.nodeId?.let { nodeIdToAgentId[HashUtil.decodeIdToLong(it)] = agent.agentId }
        }
        // 共享环境使用环境所属项目的策略/标签；历史构建机与 Job 配额仍属于构建项目。
        val strategyResult = thirdPartyAgentBuildService.getEnvStrategiesWithTags(
            projectId = dispatchType.envProjectId?.takeIf { it.isNotBlank() } ?: dispatchMessage.event.projectId,
            envId = envId,
            nodeIds = nodeIdToAgentId.keys
        )
        // 不含 PRE_BUILD 策略时，不扫描最近构建历史。
        val preBuildAgentIds = if (strategyResult.strategies.any { it.enabled && it.scope == StrategyScope.PRE_BUILD }) {
            thirdPartyAgentBuildService.getPreBuildAgentIds(
                projectId = dispatchMessage.event.projectId,
                pipelineId = dispatchMessage.event.pipelineId,
                vmSeqId = dispatchMessage.event.vmSeqId,
                size = agents.size.coerceAtLeast(1)
            ).toSet()
        } else emptySet()
        val agentTagValues = mutableMapOf<String, Map<Long, Set<String>>>()
        strategyResult.nodeTagValues.forEach { (nodeId, kv) ->
            nodeIdToAgentId[nodeId]?.let { agentId -> agentTagValues[agentId] = kv }
        }
        return PickupPrepared(
            preBuildAgentIds = preBuildAgentIds,
            strategies = strategyResult.strategies,
            agentTagValues = agentTagValues,
            tagKeys = strategyResult.tagKeys
        )
    }

    /**
     * @param printLog 是否打印"配置了并发但 jobId 为空"的告警。锁外预检传 false，避免与锁内复检重复打印；
     * 并发已满时的构建日志不受该参数控制，因为紧接着就会抛重试，不会重复
     */
    private fun checkSingleNodeConcurrency(
        dispatchMessage: DispatchMessage,
        envId: Long,
        activeAgents: List<ThirdPartyAgent>,
        printLog: Boolean = true
    ): List<ThirdPartyAgent> {
        if (dispatchMessage.event.singleNodeConcurrency == null) {
            return activeAgents
        }
        if (dispatchMessage.event.jobId.isNullOrBlank()) {
            if (printLog) logger.warn(
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
            // 无记录按 0 计数；并发配置为 0 时排队，不能对缺失计数使用 !!。
            if ((m[it.agentId] ?: 0) < dispatchMessage.event.singleNodeConcurrency!!) {
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

    /**
     * @param printLog 含义同 [checkSingleNodeConcurrency]
     */
    private fun checkAllNodeConcurrency(
        envId: Long,
        dispatchMessage: DispatchMessage,
        printLog: Boolean = true
    ) {
        if (dispatchMessage.event.allNodeConcurrency == null) {
            return
        }
        if (!dispatchMessage.event.jobId.isNullOrBlank()) {
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
        } else if (printLog) {
            logger.warn(
                "buildByEnvId|{} has allNodeConcurrency {} but env {}|job {} null",
                dispatchMessage.event.buildId,
                dispatchMessage.event.allNodeConcurrency,
                envId,
                dispatchMessage.event.jobId
            )
        }
    }

    /**
     * 在环境锁内按策略挑选 agent 并占位（写库 + 写 Redis）。
     * 策略/标签/最近构建机等数据由 [preparePickup] 在锁外提前准备好，这里只查询必须实时的 agent 负载。
     * 调度成功后的收尾由调用方在锁外执行 [TPASingleQueueService.afterAgentInQueue]。
     * @return 调度成功的 agent，所有策略都没有匹配到时返回 null
     */
    private fun pickupAgent(
        activeAgents: List<ThirdPartyAgent>,
        dispatchMessage: DispatchMessage,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        envId: Long,
        prepared: PickupPrepared,
        counts: Map<String, Pair<Int, Int>>? = null
    ): ThirdPartyAgent? {
        // 默认路径锁内批查；细粒度路径复用锁外快照。最终准入都在目标 agent 锁内复检。
        val agentCounts = counts ?: thirdPartyAgentBuildService.countRunningAndQueueByAgents(
            activeAgents.map { it.agentId }.toSet()
        )
        val runningBuildsMapper = HashMap<String, Int>()
        val dockerRunningBuildsMapper = HashMap<String, Int>()
        activeAgents.forEach {
            // 查询结果中没有的 agent 说明没有任务，按 0 处理
            val (normalCnt, dockerCnt) = agentCounts[it.agentId] ?: Pair(0, 0)
            runningBuildsMapper[it.agentId] = normalCnt
            if (dispatchType.dockerInfo != null) {
                dockerRunningBuildsMapper[it.agentId] = dockerCnt
            }
            logDebug(
                dispatchMessage.event,
                "[${it.agentId}]${it.hostname}/${it.ip}, Jobs:${runningBuildsMapper[it.agentId]}" +
                        ", DockerJobs:${dockerRunningBuildsMapper[it.agentId] ?: 0}"
            )
        }

        val executor = DispatchStrategyExecutor(
            input = DispatchStrategyExecutor.StrategyInput(
                allAgents = activeAgents,
                preBuildAgentIds = prepared.preBuildAgentIds,
                agentRunningCounts = runningBuildsMapper,
                dockerRunningCounts = dockerRunningBuildsMapper,
                agentTagValues = prepared.agentTagValues,
                tagKeys = prepared.tagKeys,
                isDockerBuilder = dispatchType.dockerInfo != null
            ),
            logAction = { msg -> logDebug(dispatchMessage.event, msg) }
        )
        return executor.executeWithRule(prepared.strategies) { agent, nodeRule ->
            // deferPost = true：收尾放到所有调度锁释放之后。
            tpaSingleQueueService.tryAgentWithRule(
                data = ThirdPartyAgentDispatchData(
                    dispatchMessage = dispatchMessage,
                    dispatchType = dispatchType
                ),
                agent = agent,
                envId = envId,
                nodeRule = nodeRule,
                deferPost = true,
                onLoadChanged = { (normal, docker) ->
                    runningBuildsMapper[agent.agentId] = normal
                    dockerRunningBuildsMapper[agent.agentId] = docker
                }
            )
        }
    }

    private fun log(event: PipelineAgentStartupEvent, logMessage: String) {
        if (DispatchLogBuffer.append(
                event.buildId, event.vmSeqId, event.containerHashId, event.executeCount, event.jobId,
                logMessage
            )
        ) return
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
        if (DispatchLogBuffer.append(
                event.buildId, event.vmSeqId, event.containerHashId, event.executeCount, event.jobId,
                logMessage, yellow = true
            )
        ) return
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
        if (DispatchLogBuffer.append(
                event.buildId, event.vmSeqId, event.containerHashId, event.executeCount, event.jobId,
                message, logType = LogType.DEBUG
            )
        ) return
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
        thirdPartyAgentBuildService.finishBuild(
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            buildResult = event.buildResult,
            executeCount = event.executeCount,
            timeInterval = event.jobTimeInterval
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDispatchService::class.java)
        const val DISPATCH_ENV_FINE_LOCK_PROJECTS = "DISPATCH_REDIS_ENV_FINE_LOCK_PROJECTS"
        const val DISPATCH_QUEUE_GRAY_PROJECT_PIPELINE = "DISPATCH_REDIS_QUEUE_GRAY_PROJECT_PIPELINE"
    }
}
