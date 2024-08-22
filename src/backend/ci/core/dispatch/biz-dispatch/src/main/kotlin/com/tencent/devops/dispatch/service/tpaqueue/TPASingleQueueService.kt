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
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.TPACommonUtil.Companion.tagError
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyRedisBuild
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.pojo.SetContextVarData
import com.tencent.devops.process.pojo.VmInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 存放第三方构建机单节点相关
 * Agent 不参与排队，老逻辑和新逻辑一起用
 */
@Suppress("ComplexMethod", "NestedBlockDepth")
@Service
class TPASingleQueueService @Autowired constructor(
    private val redisOperation: RedisOperation,
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

    fun doAgentInQueue(
        data: ThirdPartyAgentDispatchData,
        agent: ThirdPartyAgent,
        envId: Long?
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
        val redisLock = ThirdPartyAgentLock(redisOperation, data.projectId, agent.agentId)
        try {
            if (redisLock.tryLock()) {
                return agentInQueue(data, agent, envId)
            } else {
                commonUtil.logWarnI18n(
                    data, ErrorCodeEnum.BUILD_MACHINE_BUSY.errorCode.toString(),
                    suffixMsg = "(Agent is busy) - ${agent.hostname}/${agent.ip}"
                )
                return false
            }
        } finally {
            redisLock.unlock()
        }
    }

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
            if (!lockedBuildId.isNullOrBlank()) {
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

        // 保存构建详情
        saveAgentInfoToBuildDetail(data, agent)

        logger.info("${data.buildId}|START_AGENT_BY_ID|j(${data.vmSeqId})|agent=${agent.agentId}")
        commonUtil.logI18n(data, BK_SCHEDULING_SELECTED_AGENT, arrayOf(agent.hostname, agent.ip))
        return true
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

        // 添加上下文关键字 jobs.<job_id>.container.node_alias
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