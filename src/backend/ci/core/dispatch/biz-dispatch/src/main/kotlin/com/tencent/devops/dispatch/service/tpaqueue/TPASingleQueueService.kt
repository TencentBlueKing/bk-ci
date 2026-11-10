package com.tencent.devops.dispatch.service.tpaqueue

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.constants.AGENT_REUSE_MUTEX_REDISPATCH
import com.tencent.devops.dispatch.constants.AGENT_REUSE_MUTEX_RESERVE_REDISPATCH
import com.tencent.devops.dispatch.constants.BK_SCHEDULING_SELECTED_AGENT
import com.tencent.devops.dispatch.constants.TRY_AGENT_DISPATCH
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.QueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.TPAMonitorEvent
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.utils.DispatchLockCleanup
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor.TryAgentResult
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.TPACommonUtil.Companion.tagError
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyRedisBuild
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.pojo.SetContextVarData
import com.tencent.devops.process.pojo.VmInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service

/**
 * 存放第三方构建机单节点相关
 * Agent 不参与排队，老逻辑和新逻辑一起用
 */
@Suppress("ComplexMethod", "NestedBlockDepth")
@Service
class TPASingleQueueService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val streamBridge: StreamBridge,
    private val client: Client,
    private val commonUtil: TPACommonUtil,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val thirdPartyAgentService: ThirdPartyAgentService
) {
    fun genAgentBuild(context: QueueContext, dataContext: QueueDataContext): Boolean {
        val data = dataContext.data
        val agent = dataContext.buildAgent ?: run {
            // 理论上不可能但是逻辑上可能所以加校验
            logger.tagError("genAgentBuild|build agent is null|${data.toLog()}")
            return false
        }

        return doAgentInQueue(data, agent, context.envId)
    }

    /**
     * 尝试把任务调度到指定 agent
     * @param deferPost 为 true 时只做占位及节点别名上下文写入，不做调度成功后的收尾（构建详情/MQ/日志），
     * 由调用方在释放环境锁后自行调用 [afterAgentInQueue]，用于缩短环境锁的持有时间；
     * 默认 false 保持原有行为，调度成功后立即收尾
     */
    fun doAgentInQueue(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        envId: Long?,
        deferPost: Boolean = false,
        beforeQueueCheck: (() -> Boolean)? = null
    ): Boolean {
        if (data.dispatchType.isEnv()) {
            commonUtil.logWithAgentUrl(
                data = data,
                messageCode = TRY_AGENT_DISPATCH,
                param = arrayOf("[${agent.agentId}]${agent.hostname}/${agent.ip}"),
                nodeHashId = agent.nodeId,
                agentHashId = agent.agentId
            )
        }
        // TODO: 共享 agent 的调度项目与所属项目可能不同，和领取端的锁 key 不一致；本次按约定不调整。
        val redisLock = ThirdPartyAgentLock(redisOperation, data.projectId, agent.agentId)
        val success = DispatchLockCleanup.run<Boolean>(
            description = "agent|${data.toLog()}|${agent.agentId}",
            unlock = { redisLock.unlock() },
            isQueued = { it }
        ) {
            if (redisLock.tryLock()) {
                (beforeQueueCheck?.invoke() != false) && agentInQueue(data, agent, envId)
            } else {
                commonUtil.logWarnI18n(
                    data, ErrorCodeEnum.BUILD_MACHINE_BUSY.errorCode.toString(),
                    suffixMsg = "(Agent is busy) - ${agent.hostname}/${agent.ip}"
                )
                false
            }
        }
        // 收尾不涉及 agent 并发判断，放在 agent 锁外执行
        if (success && !deferPost) {
            afterAgentInQueue(data, agent)
        }
        return success
    }

    /**
     * 环境策略最终准入：agent 锁内重新查询真实负载，通过后同锁写入占位。
     * 规则不匹配不是入队故障，更新快照后允许后续 AVAILABLE/null 等规则继续尝试。
     * 显式 agent 调度仍调用 doAgentInQueue，不新增容量限制。
     */
    fun tryAgentWithRule(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        envId: Long?,
        nodeRule: NodeRule?,
        deferPost: Boolean = false,
        onLoadChanged: (Pair<Int, Int>) -> Unit = {}
    ): TryAgentResult {
        var loadChanged = false
        val success = doAgentInQueue(data, agent, envId, deferPost) {
            if (nodeRule == null) {
                true // 无规则保持原有排队语义，不能统一强加 agent 容量上限。
            } else {
                val (normal, docker) = thirdPartyAgentService.countRunningAndQueueByAgents(setOf(agent.agentId))
                    .getOrDefault(agent.agentId, Pair(0, 0))
                val matched = DispatchStrategyExecutor.matchesNodeRule(
                    agent, nodeRule, data.dispatchType.dockerInfo != null, normal, docker
                )
                if (!matched) {
                    loadChanged = true
                    onLoadChanged(Pair(normal, docker))
                    commonUtil.logDebug(data, "[${agent.agentId}] load changed: jobs=$normal, dockerJobs=$docker")
                }
                matched
            }
        }
        return when {
            success -> TryAgentResult.SUCCESS
            loadChanged -> TryAgentResult.LOAD_CHANGED
            else -> TryAgentResult.REJECTED
        }
    }

    /**
     * 调度成功后的收尾：保存构建机信息到构建详情、打印调度日志、发送监控事件。
     * 这些操作都是在任务已入库之后的附属动作，不影响并发判断，因此不需要放在环境锁/agent 锁内。
     * 执行到这里时任务已写库 + 写 Redis，agent 随时可能领取，收尾异常不能再向上抛，
     * 否则外层会当作调度失败处理；各步骤相互独立，单步失败不影响后续步骤（尤其是监控事件）。
     */
    fun afterAgentInQueue(data: ThirdPartyAgentDispatchData, agent: ThirdPartyAgent) {
        // 保存构建详情
        runPostStep(data, agent, "saveAgentInfoToBuildDetail") { saveAgentInfoToBuildDetail(data, agent) }

        logger.info("${data.buildId}|START_AGENT_BY_ID|j(${data.vmSeqId})|agent=${agent.agentId}")
        runPostStep(data, agent, "logSelectedAgent") {
            commonUtil.logWithAgentUrl(
                data = data,
                messageCode = BK_SCHEDULING_SELECTED_AGENT,
                param = arrayOf(agent.hostname, agent.ip),
                nodeHashId = agent.nodeId,
                agentHashId = agent.agentId
            )
        }

        // 延迟队列轮训构建机状态监控
        runPostStep(data, agent, "dispatchAgentMonitor") { dispatchAgentMonitor(data) }
    }

    private inline fun runPostStep(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        step: String,
        action: () -> Unit
    ) {
        try {
            action()
        } catch (ignore: Exception) {
            logger.tagError("afterAgentInQueue|${data.toLog()}|agent=${agent.agentId}|$step|error", ignore)
        }
    }

    /**
     * 检查 agent 可用性（升级中/复用互斥），通过后写库 + 写 Redis，并在 agent 锁内写入节点别名上下文。
     * 收尾逻辑见 [afterAgentInQueue]
     */
    private fun agentInQueue(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        envId: Long?
    ): Boolean {
        if (thirdPartyAgentBuildRedisUtils.isThirdPartyAgentUpgrading(data.projectId, agent.agentId)) {
            commonUtil.logWarnI18n(
                data, ErrorCodeEnum.BUILD_MACHINE_UPGRADE_IN_PROGRESS.errorCode.toString(),
                suffixMsg = " - ${agent.hostname}/${agent.ip}"
            )
            return false
        }

        // #10082 对于复用的机器和被复用的，需要加锁校验看看这台机器能不能使用
        val lockKey = AgentReuseMutex.genAgentReuseMutexLockKey(data.projectId, agent.agentId)
        if (data.dispatchType.hasReuseMutex()) {
            val lock = RedisLockByValue(
                redisOperation = redisOperation,
                lockKey = lockKey,
                lockValue = data.buildId,
                expiredTimeInSeconds = AgentReuseMutex.AGENT_LOCK_TIMEOUT
            )
            // 没有拿到锁说明现在这台机被复用互斥占用不能选
            if (!lock.tryLock()) {
                logAgentReuse(data, agent, AGENT_REUSE_MUTEX_REDISPATCH, null)
                return false
            }
            try {
                // # 10082 设置复用需要的关键字 jobs.<job_id>.container.agent_id，jobId需要为根节点id
                // 只用给env类型的根节点设置，因为id类型的在引擎 AgentReuseMutexCmd 直接写入了
                val dispatch = data.dispatchType
                if (dispatch.isEnv() && dispatch.reusedInfo != null && dispatch.reusedInfo!!.jobId == null) {
                    client.get(ServiceVarResource::class).setContextVar(
                        SetContextVarData(
                            projectId = data.projectId,
                            pipelineId = data.pipelineId,
                            buildId = data.buildId,
                            // 根节点一定会有 jobId，引擎侧检查
                            contextName = AgentReuseMutex.genAgentContextKey(data.jobId!!),
                            contextVal = agent.agentId,
                            readOnly = true,
                            rewriteReadOnly = true
                        )
                    )
                    // 写 linkTip，方便被阻塞的打印日志
                    redisOperation.set(
                        key = AgentReuseMutex.genAgentReuseMutexLinkTipKey(data.buildId),
                        value = "${data.pipelineId}_Job[${data.vmSeqId}|${data.jobId}]",
                        expiredInSecond = AgentReuseMutex.AGENT_LOCK_TIMEOUT
                    )
                }
            } catch (e: Exception) {
                logger.tagError("agentInQueue|${data.toLog()}|setContextVar|error", e)
            }
        } else {
            val lockedBuildId = redisOperation.get(lockKey)
            if (!lockedBuildId.isNullOrBlank() && lockedBuildId != data.buildId) {
                // 没有复用逻辑的需要检查下如果这个机器剩一个可调度空间且有复用锁那么不能进行调度
                // 判断当前复用锁有没有任务已经在跑，如果已经在跑那么不管，如果没有跑那么要留一个给复用调度
                val checkRes = if (data.dispatchType.dockerInfo != null) {
                    val (hasRun, cnt) =
                        thirdPartyAgentService.checkRunningAndSize(agent.agentId, lockedBuildId, true)
                    if (hasRun) {
                        ((agent.dockerParallelTaskCount ?: 4) - cnt) <= 0
                    } else {
                        ((agent.dockerParallelTaskCount ?: 4) - cnt) <= 1
                    }
                } else {
                    val (hasRun, cnt) =
                        thirdPartyAgentService.checkRunningAndSize(agent.agentId, lockedBuildId, false)
                    if (hasRun) {
                        ((agent.parallelTaskCount ?: 4) - cnt) <= 0
                    } else {
                        ((agent.parallelTaskCount ?: 4) - cnt) <= 1
                    }
                }
                if (checkRes) {
                    logAgentReuse(data, agent, AGENT_REUSE_MUTEX_RESERVE_REDISPATCH, lockedBuildId)
                    return false
                }
            }
        }

        // #5806 入库失败就不再写Redis
        inQueue(data, agent, envId)

        // 同项目 agent 领取任务也使用这把锁；共享 agent 的锁归属差异见上方 TODO（内部已 catch）。
        setNodeAliasContextVar(data, agent)

        // 收尾（构建详情、日志、监控等）统一在 afterAgentInQueue 中处理
        return true
    }

    private fun dispatchAgentMonitor(
        data: ThirdPartyAgentDispatchData
    ) {
        if (data.jobTimeoutMinutes == null) {
            logger.error("dispatchAgentMonitor|${data.toLog()}|jobTimeoutMinutes is null!")
            return
        }
        val event = TPAMonitorEvent(
            delayMills = 20 * 1000,
            timeoutMin = data.jobTimeoutMinutes.toLong(),
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            userId = data.userId,
            buildId = data.buildId,
            vmSeqId = data.vmSeqId,
            containerHashId = data.containerHashId,
            executeCount = data.executeCount,
            stepId = null
        )
        event.sendTo(streamBridge)
    }

    private fun logAgentReuse(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        messageCode: String,
        lockBuildId: String?
    ) {
        val lockedBuildId = if (lockBuildId == null) {
            val lockKey = AgentReuseMutex.genAgentReuseMutexLockKey(data.projectId, agent.agentId)
            redisOperation.get(lockKey)
        } else {
            lockBuildId
        }

        val params = arrayOf("${agent.agentId}|${agent.hostname}/${agent.ip}", lockedBuildId ?: "")
        if (lockedBuildId.isNullOrBlank()) {
            commonUtil.logI18n(data, messageCode, params)
            return
        }
        var linkTip = redisOperation.get(AgentReuseMutex.genAgentReuseMutexLinkTipKey(lockedBuildId))
        if (linkTip.isNullOrBlank()) {
            commonUtil.logI18n(data, messageCode, params)
            return
        }
        val pipelineId = linkTip.substringBefore("_")
        linkTip = linkTip.substringAfter("_")
        commonUtil.logWithBuildUrl(data, messageCode, params, pipelineId, lockedBuildId, linkTip)
    }

    private fun inQueue(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        envId: Long?
    ) {
        thirdPartyAgentService.queueBuild(
            agent = agent,
            dispatchData = data,
            envId = envId
        )

        thirdPartyAgentBuildRedisUtils.setThirdPartyBuild(
            agent.secretKey,
            ThirdPartyRedisBuild(
                projectId = data.projectId,
                pipelineId = data.pipelineId,
                buildId = data.buildId,
                agentId = agent.agentId,
                vmSeqId = data.vmSeqId,
                vmName = agent.hostname,
                channelCode = data.channelCode,
                atoms = data.atoms
            )
        )
    }

    /**
     * 添加上下文关键字 jobs.<job_id>.container.node_alias，涉及两次跨服务调用，失败只打日志不影响调度
     */
    private fun setNodeAliasContextVar(data: ThirdPartyAgentDispatchData, agent: ThirdPartyAgent) {
        if (data.jobId.isNullOrBlank()) {
            return
        }
        try {
            val detail = client.get(ServiceThirdPartyAgentResource::class).getAgentDetail(
                userId = data.userId,
                projectId = data.projectId,
                agentHashId = agent.agentId
            ).data
            if (detail == null) {
                logger.warn("inQueue|${data.toLog()}|setContextVar|getAgentDetail ${agent.agentId} is null")
                return
            }
            client.get(ServiceVarResource::class).setContextVar(
                SetContextVarData(
                    projectId = data.projectId,
                    pipelineId = data.pipelineId,
                    buildId = data.buildId,
                    contextName = "jobs.${data.jobId}.container.node_alias",
                    contextVal = detail.displayName,
                    readOnly = true,
                    rewriteReadOnly = true
                )
            )
        } catch (e: Exception) {
            logger.tagError("inQueue|${data.toLog()}|setContextVar|error", e)
        }
    }

    private fun saveAgentInfoToBuildDetail(data: ThirdPartyAgentDispatchData, agent: ThirdPartyAgent) {
        client.get(ServiceBuildResource::class).saveBuildVmInfo(
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            buildId = data.buildId,
            vmSeqId = data.vmSeqId,
            vmInfo = VmInfo(ip = agent.ip, name = agent.ip)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TPASingleQueueService::class.java)
    }
}