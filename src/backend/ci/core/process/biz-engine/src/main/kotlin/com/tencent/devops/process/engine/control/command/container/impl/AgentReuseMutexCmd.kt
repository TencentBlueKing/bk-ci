package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.AgentReuseMutex
import com.tencent.devops.common.pipeline.container.AgentReuseMutexType
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ContainerMutexStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.redis.RedisLockByValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.MutexControl
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.EngineConfigService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.service.BuildVariableService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class AgentReuseMutexCmd @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineUrlBean: PipelineUrlBean,
    private val buildVariableService: BuildVariableService,
    private val engineConfigService: EngineConfigService
) : ContainerCmd {
    override fun canExecute(commandContext: ContainerContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish() &&
            commandContext.container.controlOption.agentReuseMutex != null
    }

    override fun execute(commandContext: ContainerContext) {
        // 终止或者结束事件不做互斥判断
        if (!commandContext.event.actionType.isEnd()) {
            doExecute(commandContext)
        }
    }

    /**
     * 互斥情况存在三种
     * 1、依赖某个AgentId，直接往下执行即可
     * 2、Root节点，即没有复用节点的节点，根据类型先拿取锁
     * 3、Reuse节点，但没有复用节点，可能是因为和复用节点同级，或存在和复用节点先后顺序不明确的，
     * 这种先拿取复用的JobId看有没有，没有就按root节点的逻辑走
     */
    private fun doExecute(commandContext: ContainerContext) {
        var mutex = commandContext.container.controlOption.agentReuseMutex!!
        if (!mutex.type.needEngineLock()) {
            commandContext.cmdFlowState = CmdFlowState.CONTINUE
            return
        }
        // 如果有依赖Job且不是依赖类型的可以先拿一下上下文看看有没有已经写入了，如果写入了可以直接跳过
        if (mutex.reUseJobId != null &&
            commandContext.variables.containsKey(AgentReuseMutex.genAgentContextKey(mutex.reUseJobId!!))
        ) {
            commandContext.cmdFlowState = CmdFlowState.CONTINUE
            return
        }
        // 极端情况上下文没有写入，且agentId还是空，理论上不会有，逻辑上出现了就失败
        if (mutex.agentOrEnvId.isNullOrBlank()) {
            return agentIdNullError(commandContext, mutex, null)
        }
        // 对存在变量的Agent做替换
        mutex = decorateMutex(commandContext, mutex, commandContext.variables) ?: return
        when (mutex.type) {
            AgentReuseMutexType.AGENT_ID, AgentReuseMutexType.AGENT_NAME -> {
                acquireMutex(commandContext, mutex)
            }

            else -> {
                commandContext.cmdFlowState = CmdFlowState.CONTINUE
            }
        }
    }

    private fun decorateMutex(
        commandContext: ContainerContext,
        mutex: AgentReuseMutex,
        variables: Map<String, String>
    ): AgentReuseMutex? {
        if (!mutex.runtimeAgentOrEnvId.isNullOrBlank()) {
            return mutex
        }

        // 超时时间限制，0表示排队不等待直接超时
        val timeOut = MutexControl.parseTimeoutVar(mutex.timeout, mutex.timeoutVar, variables)
        // 排队任务数量限制，0表示不排队
        val queue = mutex.queue.coerceAtLeast(0).coerceAtMost(engineConfigService.getMutexMaxQueue())
        // 替换环境变量
        var runtimeAgentOrEnvId = if (!mutex.agentOrEnvId.isNullOrBlank()) {
            EnvUtils.parseEnv(mutex.agentOrEnvId, variables)
        } else {
            mutex.agentOrEnvId
        }

        // 针对Name类型的请求agentId，放在初始化中统一处理，防止事件重放后使用id挑选name
        if (mutex.type == AgentReuseMutexType.AGENT_NAME) {
            runtimeAgentOrEnvId = try {
                client.get(ServiceThirdPartyAgentResource::class).getAgentByDisplayName(
                    commandContext.event.projectId,
                    runtimeAgentOrEnvId!!
                ).data?.agentId ?: run {
                    agentIdNullError(commandContext, mutex, null)
                    return null
                }
            } catch (e: Exception) {
                agentIdNullError(commandContext, mutex, e)
                return null
            }
        }

        return if (
            runtimeAgentOrEnvId != mutex.agentOrEnvId ||
            timeOut != mutex.timeout ||
            queue != mutex.queue
        ) {
            mutex.copy(runtimeAgentOrEnvId = runtimeAgentOrEnvId, timeout = timeOut, queue = queue)
        } else {
            mutex.runtimeAgentOrEnvId = runtimeAgentOrEnvId // 初始化过
            mutex
        }
    }

    private fun acquireMutex(commandContext: ContainerContext, mutex: AgentReuseMutex) {
        if (commandContext.buildStatus.isReadyToRun()) { // 锁定之后进入RUNNING态，不再检查锁

            val mutexResult = doAcquireMutex(commandContext.container, mutex)
            val event = commandContext.event

            when (mutexResult) {
                ContainerMutexStatus.CANCELED -> {
                    LOG.info("ENGINE|${event.buildId}|${event.source}|AGENT_REUSE_MUTEX_CANCEL")
                    // job互斥失败处理
                    commandContext.buildStatus = BuildStatus.FAILED
                    commandContext.latestSummary = "agent_reuse_mutex_cancel"
                    commandContext.cmdFlowState = CmdFlowState.FINALLY
                }

                ContainerMutexStatus.WAITING -> {
                    commandContext.latestSummary = "agent_reuse_mutex_delay"
                    commandContext.cmdFlowState = CmdFlowState.LOOP // 循环消息命令 延时10秒钟
                }

                ContainerMutexStatus.FIRST_LOG -> { // 增加可视化的互斥状态打印，注：这里进行了Job状态流转！
                    commandContext.latestSummary = "agent_reuse_mutex_print"
                    commandContext.cmdFlowState = CmdFlowState.LOOP
                }

                else -> { // 正常运行
                    commandContext.cmdFlowState = CmdFlowState.CONTINUE // 检查通过，继续向下执行
                }
            }
        } else if (commandContext.container.status.isFinish()) { // 对于存在重放的结束消息做闭环
            val event = commandContext.event

            LOG.info(
                "AGENT_REUSE|ENGINE|${event.buildId}|${event.source}|status=${commandContext.container.status}" +
                    "|concurrent_container_event"
            )

            releaseContainerMutex(
                projectId = commandContext.event.projectId,
                pipelineId = commandContext.event.pipelineId,
                buildId = commandContext.event.buildId,
                containerId = commandContext.event.containerId,
                runtimeAgentOrEnvId = mutex.runtimeAgentOrEnvId!!,
                executeCount = commandContext.container.executeCount
            )
            // 原在ContainerControl 处的逻辑移到这
            commandContext.cmdFlowState = CmdFlowState.BREAK
        }
    }

    private fun doAcquireMutex(container: PipelineBuildContainer, mutex: AgentReuseMutex): ContainerMutexStatus {
        // 对于AgentId这种需要写入上下文，防止最后没有被执行导致兜底无法解锁，排队中也可能被取消导致无法退出队列
        // 只要不是依赖节点，根节点和同级依赖节点都要写入，防止出现被同级env节点获取到节点锁但没写入依赖id节点就无法获取到AgentId
        buildVariableService.setVariable(
            projectId = container.projectId,
            pipelineId = container.pipelineId,
            buildId = container.buildId,
            varName = AgentReuseMutex.genAgentContextKey(mutex.reUseJobId ?: mutex.jobId),
            varValue = mutex.runtimeAgentOrEnvId!!,
            readOnly = true,
            rewriteReadOnly = true
        )

        val res = tryAgentLockOrQueue(container, mutex)
        return if (res) {
            LOG.info("ENGINE|${container.buildId}|AGENT_REUSE_LOCK_SUCCESS|${mutex.type}")
            // 抢到锁则可以继续运行，并退出队列
            quitMutexQueue(
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                buildId = container.buildId,
                executeCount = container.executeCount,
                containerId = container.containerId,
                runtimeAgentOrEnvId = mutex.runtimeAgentOrEnvId!!
            )
            ContainerMutexStatus.READY
        } else {
            // 首先判断队列的等待情况
            val queueResult = checkMutexQueue(container, mutex)
            if (ContainerMutexStatus.CANCELED == queueResult) {
                // 排队失败说明超时或者超出队列则取消运行，退出队列
                releaseContainerMutex(
                    projectId = container.projectId,
                    pipelineId = container.pipelineId,
                    buildId = container.buildId,
                    containerId = container.containerId,
                    runtimeAgentOrEnvId = mutex.runtimeAgentOrEnvId!!,
                    executeCount = container.executeCount
                )
            }
            queueResult
        }
    }

    private fun tryAgentLockOrQueue(container: PipelineBuildContainer, mutex: AgentReuseMutex): Boolean {
        val lockKey = AgentReuseMutex.genAgentReuseMutexLockKey(container.projectId, mutex.runtimeAgentOrEnvId!!)
        val lockValue = container.buildId
        // 过期时间按整个流水线过期时间为准，7天
        val expireSec = AgentReuseMutex.AGENT_LOCK_TIMEOUT
        val containerMutexLock = RedisLockByValue(
            redisOperation = redisOperation,
            lockKey = lockKey,
            lockValue = lockValue,
            expiredTimeInSeconds = expireSec
        )
        // 获取到锁的内容
        val lv = redisOperation.get(lockKey)
        if (lv != null && lv == lockValue) {
            return true
        }

        val queueKey = AgentReuseMutex.genAgentReuseMutexQueueKey(container.projectId, mutex.runtimeAgentOrEnvId!!)
        // 获取队列中的开始时间，为空的时候则为当前时间
        val startTime = redisOperation.hget(queueKey, lockValue)?.toLong() ?: LocalDateTime.now().timestamp()
        var minTime: Long? = null
        val queueValues = redisOperation.hvalues(queueKey)
        if (queueValues != null && queueValues.size > 0) {
            val queueLongValues = queueValues.map { it.toLong() }
            minTime = queueLongValues.minOrNull()
        }

        val lockResult = if (minTime != null) {
            if (startTime == minTime) {
                // 最小值和container入队列时间一致的时候，可以开始抢锁
                containerMutexLock.tryLock()
            } else {
                // 不是最早的队列，则不能抢锁，直接返回
                false
            }
        } else {
            // 没有排队最小值的时候，则开始抢锁
            containerMutexLock.tryLock()
        }

        if (lockResult) {
            mutex.linkTip?.let {
                redisOperation.set(
                    key = AgentReuseMutex.genAgentReuseMutexLinkTipKey(container.buildId),
                    value = mutex.linkTip!!,
                    expiredInSecond = expireSec
                )
            }
            logAgentMutex(
                container, mutex, null,
                I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_GET_LOCKED,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + " ${expireSec}s"
            )
        }

        return lockResult
    }

    private fun quitMutexQueue(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String,
        runtimeAgentOrEnvId: String
    ) {
        val queueKey = AgentReuseMutex.genAgentReuseMutexQueueKey(projectId, runtimeAgentOrEnvId)
        redisOperation.hdelete(queueKey, buildId)
        containerBuildRecordService.updateContainerRecord(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            containerId = containerId, executeCount = executeCount,
            containerVar = emptyMap(), buildStatus = null,
            timestamps = mapOf(
                BuildTimestampType.JOB_AGENT_REUSE_MUTEX_QUEUE to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            )
        )
    }

    @Suppress("LongMethod")
    private fun checkMutexQueue(container: PipelineBuildContainer, mutex: AgentReuseMutex): ContainerMutexStatus {
        val lockKey = AgentReuseMutex.genAgentReuseMutexLockKey(container.projectId, mutex.runtimeAgentOrEnvId!!)
        val lockedBuildId = redisOperation.get(lockKey)
        // 多判断一次防止因为锁并发竞争失败导致需要排队的情况
        if (lockedBuildId == container.buildId) {
            return ContainerMutexStatus.READY
        }
        // 当没有启用互斥组排队
        if (!mutex.queueEnable) {
            logAgentMutex(
                container, mutex, lockedBuildId,
                I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_QUEUE_DISABLED,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                isError = true
            )
            return ContainerMutexStatus.CANCELED
        }
        val queueKey = AgentReuseMutex.genAgentReuseMutexQueueKey(container.projectId, mutex.runtimeAgentOrEnvId!!)
        val exist = redisOperation.hhaskey(queueKey, container.buildId)
        val queueSize = redisOperation.hsize(queueKey)
        // 也已经在队列中,判断是否已经超时
        return if (exist) {
            val startTime =
                redisOperation.hget(queueKey, container.buildId)?.toLong() ?: LocalDateTime.now().timestamp()
            val currentTime = LocalDateTime.now().timestamp()
            val timeDiff = currentTime - startTime
            // 排队等待时间为0的时候，立即超时, 退出队列，并失败, 没有就继续在队列中,timeOut时间为分钟
            if (mutex.timeout == 0 || timeDiff > TimeUnit.MINUTES.toSeconds(mutex.timeout.toLong())) {
                val desc = "${
                if (mutex.timeoutVar.isNullOrBlank()) {
                    "[${mutex.timeout} minutes]"
                } else " timeoutVar[${mutex.timeoutVar}] setup to [${mutex.timeout} minutes]"
                } "
                logAgentMutex(
                    container, mutex, lockedBuildId,
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_QUEUE_TIMEOUT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + " $desc",
                    isError = true
                )
                quitMutexQueue(
                    projectId = container.projectId,
                    pipelineId = container.pipelineId,
                    buildId = container.buildId,
                    executeCount = container.executeCount,
                    containerId = container.containerId,
                    runtimeAgentOrEnvId = mutex.runtimeAgentOrEnvId!!
                )
                ContainerMutexStatus.CANCELED
            } else {
                val timeDiffMod = timeDiff % TimeUnit.MINUTES.toSeconds(1L) // 余数 1分钟内
                // 在一分钟内的小于[SECOND_TO_PRINT]秒的才打印
                if (timeDiffMod <= MutexControl.SECOND_TO_PRINT) {
                    logAgentMutex(
                        container, mutex, lockedBuildId,
                        I18nUtil.getCodeLanMessage(
                            messageCode = ProcessMessageCode.BK_CURRENT_NUMBER_OF_QUEUES,
                            language = I18nUtil.getDefaultLocaleLanguage(),
                            params = arrayOf(queueSize.toString(), timeDiff.toString())
                        )
                    )
                }
                ContainerMutexStatus.WAITING
            }
        } else {
            // 此处存在并发问题，假设capacity只有1个, 两个并发都会同时满足queueSize = 0,导入入队，但问题不大，暂不解决
            // 排队队列为0的时候，不做排队
            // 还没有在队列中，则判断队列的数量,如果超过了则排队失败,没有则进入队列.
            if (mutex.queue == 0 || queueSize >= mutex.queue) {
                logAgentMutex(
                    container, mutex, lockedBuildId,
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_QUEUE_FULL,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    isError = true
                )
                ContainerMutexStatus.CANCELED
            } else {
                logAgentMutex(
                    container, mutex, lockedBuildId,
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_ENQUEUE,
                        params = arrayOf("${queueSize + 1}"),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
                // 则进入队列,并返回成功
                enterMutexQueue(
                    projectId = container.projectId,
                    pipelineId = container.pipelineId,
                    buildId = container.buildId,
                    executeCount = container.executeCount,
                    containerId = container.containerId,
                    runtimeAgentOrEnvId = mutex.runtimeAgentOrEnvId!!
                )
                mutex.linkTip?.let {
                    redisOperation.set(
                        key = AgentReuseMutex.genAgentReuseMutexLinkTipKey(container.buildId),
                        value = mutex.linkTip!!,
                        expiredInSecond = AgentReuseMutex.AGENT_LOCK_TIMEOUT
                    )
                }
                ContainerMutexStatus.FIRST_LOG
            }
        }
    }

    private fun enterMutexQueue(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String,
        runtimeAgentOrEnvId: String
    ) {
        val queueKey = AgentReuseMutex.genAgentReuseMutexQueueKey(projectId, runtimeAgentOrEnvId)
        redisOperation.hset(queueKey, buildId, LocalDateTime.now().timestamp().toString())
        containerBuildRecordService.updateContainerRecord(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId,
            containerId = containerId, executeCount = executeCount,
            containerVar = emptyMap(), buildStatus = null,
            timestamps = mapOf(
                BuildTimestampType.JOB_MUTEX_QUEUE to
                    BuildRecordTimeStamp(LocalDateTime.now().timestampmilli(), null)
            )
        )
    }

    private fun releaseContainerMutex(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        runtimeAgentOrEnvId: String,
        executeCount: Int?
    ) {
        LOG.info(
            "[$buildId]|AGENT_REUSE_RELEASE_MUTEX_LOCK|project=$projectId|$runtimeAgentOrEnvId"
        )
        // 删除tip
        redisOperation.delete(AgentReuseMutex.genAgentReuseMutexLinkTipKey(buildId))
        quitMutexQueue(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount ?: 1,
            containerId = containerId,
            runtimeAgentOrEnvId = runtimeAgentOrEnvId
        )

        // 完善日志
        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_RELEASE_LOCK,
                language = I18nUtil.getDefaultLocaleLanguage()
            ) + " Mutex[$runtimeAgentOrEnvId]",
            tag = VMUtils.genStartVMTaskId(containerId),
            jobId = null,
            executeCount = executeCount ?: 1,
            stepId = null
        )
    }

    private fun logAgentMutex(
        container: PipelineBuildContainer,
        mutexGroup: AgentReuseMutex,
        lockedBuildId: String?,
        msg: String,
        isError: Boolean = false
    ) {

        val message = I18nUtil.getCodeLanMessage(
            messageCode = ProcessMessageCode.BK_AGENT_REUSE_MUTEX,
            params = arrayOf(mutexGroup.runtimeAgentOrEnvId!!)
        ) + if (!lockedBuildId.isNullOrBlank()) {
            // #5454 拿出占用锁定的信息
            redisOperation.get(AgentReuseMutex.genAgentReuseMutexLinkTipKey(lockedBuildId))?.let { s ->
                val endIndex = s.indexOf("_")
                val pipelineId = s.substring(0, endIndex)
                val linkTip = s.substring(endIndex + 1)
                val link = pipelineUrlBean.genBuildDetailUrl(
                    projectCode = container.projectId,
                    pipelineId = pipelineId,
                    buildId = lockedBuildId,
                    position = null,
                    stageId = null,
                    needShortUrl = false
                )
                if (lockedBuildId != container.buildId) {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_LOCKED,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + ": $linkTip<a target='_blank' href='$link'>" +
                        I18nUtil.getCodeLanMessage(
                            messageCode = ProcessMessageCode.BK_CLICK,
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ) + "</a> | $msg"
                } else {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_CURRENT,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + ": $linkTip| $msg"
                }
            } ?: msg
        } else {
            msg
        }

        if (isError) {
            buildLogPrinter.addErrorLine(
                buildId = container.buildId,
                message = message,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = null,
                executeCount = container.executeCount,
                stepId = null
            )
        } else {
            buildLogPrinter.addYellowLine(
                buildId = container.buildId,
                message = message,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = null,
                executeCount = container.executeCount,
                stepId = null
            )
        }
    }

    private fun agentIdNullError(
        commandContext: ContainerContext,
        mutex: AgentReuseMutex,
        error: Exception?
    ) {
        logAgentMutex(
            commandContext.container, mutex, commandContext.container.buildId,
            I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_AGENT_REUSE_MUTEX_AGENT_ID_NULL,
                language = I18nUtil.getDefaultLocaleLanguage()
            ),
            true
        )
        if (error != null) {
            LOG.error(
                "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}|AGENT_REUSE_MUTEX_CANCEL", error
            )
        } else {
            LOG.warn(
                "ENGINE|${commandContext.event.buildId}|${commandContext.event.source}|AGENT_REUSE_MUTEX_CANCEL"
            )
        }
        // job互斥失败处理
        commandContext.buildStatus = BuildStatus.FAILED
        commandContext.latestSummary = "agent_reuse_mutex_cancel"
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AgentReuseMutexCmd::class.java)
    }
}
