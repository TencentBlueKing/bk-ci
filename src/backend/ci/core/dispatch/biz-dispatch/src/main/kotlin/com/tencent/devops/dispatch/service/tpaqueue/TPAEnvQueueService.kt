package com.tencent.devops.dispatch.service.tpaqueue

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.constants.BK_AGENT_IS_BUSY
import com.tencent.devops.dispatch.constants.BK_ENV_DISPATCH_AGENT
import com.tencent.devops.dispatch.constants.BK_ENV_NODE_DISABLE
import com.tencent.devops.dispatch.constants.BK_ENV_WORKER_ERROR_IGNORE
import com.tencent.devops.dispatch.constants.BK_NO_AGENT_AVAILABLE
import com.tencent.devops.dispatch.constants.BK_QUEUE_TIMEOUT_MINUTES
import com.tencent.devops.dispatch.constants.BK_THIRD_JOB_ENV_CURR
import com.tencent.devops.dispatch.constants.BK_THIRD_JOB_NODE_CURR
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.EnvQueueContext
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.TPACommonUtil.Companion.tagError
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.thirdpartyagent.EnvNodeAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import jakarta.ws.rs.core.Response

/**
 * 存放第三方构建机环境相关逻辑
 */
@Suppress("ComplexMethod")
@Service
class TPAEnvQueueService @Autowired constructor(
    private val client: Client,
    private val commonUtil: TPACommonUtil,
    private val thirdPartyAgentService: ThirdPartyAgentService,
    private val tpaSingleQueueService: TPASingleQueueService
) {
    fun initEnvContext(dataContext: QueueDataContext): EnvQueueContext {
        val data = dataContext.data

        val (envId, envAgents) = fetchEnvIdAndAgents(dataContext, data.genEnvWithProject()!!)
        logDisableAgents(data, envAgents)

        val agents = envAgents.filter {
            it.agent.status == AgentStatus.IMPORT_OK && (data.os == it.agent.os || data.os == VMBaseOS.ALL.name) &&
                    it.enableNode
        }.map { it.agent }
        if (agents.isNotEmpty()) {
            return EnvQueueContext(envId, agents)
        }

        commonUtil.logWarnI18n(data, BK_NO_AGENT_AVAILABLE)
        throw TPACommonUtil.queueRetry(
            errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
            errMsg = "${data.buildId}|${data.vmSeqId} " + I18nUtil.getCodeLanMessage(
                messageCode = BK_QUEUE_TIMEOUT_MINUTES,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = arrayOf("${data.queueTimeoutMinutes}")
            )
        )
    }

    fun refreshEnvContextAgents(context: EnvQueueContext, dataContext: QueueDataContext) {
        val data = dataContext.data

        val (envId, envAgents) = fetchEnvIdAndAgents(dataContext, data.genEnvWithProject()!!)
        logDisableAgents(data, envAgents)

        val agents = envAgents.filter {
            it.agent.status == AgentStatus.IMPORT_OK && (data.os == it.agent.os || data.os == VMBaseOS.ALL.name) &&
                    it.enableNode
        }.map { it.agent }
        if (agents.isNotEmpty()) {
            context.envId = envId
            context.agents = agents
            return
        }
        commonUtil.logWarnI18n(data, BK_NO_AGENT_AVAILABLE)
        throw TPACommonUtil.queueRetry(
            errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
            errMsg = "${data.buildId}|${data.vmSeqId} " + I18nUtil.getCodeLanMessage(
                messageCode = BK_QUEUE_TIMEOUT_MINUTES,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = arrayOf("${data.queueTimeoutMinutes}")
            )
        )
    }

    fun inEnvQueue(
        context: EnvQueueContext,
        dataContext: QueueDataContext
    ) {
        ignoreAgentCheck(context, dataContext)
        allNodeConcurrencyCheck(context, dataContext)
        singleNodeConcurrencyCheck(context, dataContext)
        if (context.agents.isNotEmpty() && pickupAgent(context, dataContext)) {
            afterGenAgentBuild(context, dataContext)
            return
        }

        // 没有可用构建机列表进入下一次重试
        val data = dataContext.data
        logger.info("START_AGENT|${data.toLog()}|Not Found, Retry!")
        commonUtil.logWarnI18n(data, BK_AGENT_IS_BUSY, suffixMsg = dataContext.retryLog("env agents not found"))
        throw TPACommonUtil.queueRetry(
            errorCode = ErrorCodeEnum.LOAD_BUILD_AGENT_FAIL,
            errMsg = "${data.buildId}|${data.vmSeqId} " + I18nUtil.getCodeLanMessage(
                messageCode = BK_QUEUE_TIMEOUT_MINUTES,
                language = I18nUtil.getDefaultLocaleLanguage(),
                params = arrayOf("${data.queueTimeoutMinutes}")
            )
        )
    }

    private fun logDisableAgents(data: ThirdPartyAgentDispatchData, agents: List<EnvNodeAgent>) {
        val disableIds =
            agents.filter { !it.enableNode }.associate { it.agent.agentId to it.nodeDisplayName }.ifEmpty { return }
        val disableStr = disableIds.map { "[${it.key}][${it.value}]" }.joinToString(",")
        commonUtil.logI18n(data, BK_ENV_NODE_DISABLE, arrayOf(disableStr))
    }

    private fun fetchEnvIdAndAgents(
        dataContext: QueueDataContext,
        env: String
    ): Pair<Long, List<EnvNodeAgent>> {
        val data = dataContext.data

        val agentsResult = try {
            if (data.dispatchType.idType()) {
                client.get(ServiceThirdPartyAgentResource::class).getAgentsByEnvId(data.projectId, env)
            } else {
                client.get(ServiceThirdPartyAgentResource::class).getAgentsByEnvNameWithId(data.projectId, env)
            }
        } catch (e: Exception) {
            throw TPACommonUtil.queueFailure(
                ErrorCodeEnum.GET_VM_ERROR, if (e is RemoteServiceException) {
                    e.errorMessage
                } else {
                    e.message ?: (ErrorCodeEnum.GET_VM_ERROR.getErrorMessage() + "($env)")
                }
            )
        }

        if (agentsResult.status == Response.Status.FORBIDDEN.statusCode) {
            logger.warn("fetchEnvIdAndAgents|START_AGENT_FAILED_FORBIDDEN|${data.toLog()}|err=${agentsResult.message}")
            throw TPACommonUtil.queueFailure(ErrorCodeEnum.GET_VM_ERROR, agentsResult.message ?: "")
        }

        if (agentsResult.isNotOk()) {
            logger.warn("fetchEnvIdAndAgents|START_AGENT_FAILED|${data.toLog()}|err=${agentsResult.message}")
            commonUtil.logDebugI18n(data, BK_AGENT_IS_BUSY, suffixMsg = dataContext.retryLog(agentsResult.message))
            throw TPACommonUtil.queueRetry(
                ErrorCodeEnum.GET_BUILD_AGENT_ERROR,
                suffixMsg = "(System Error) - $env: ${agentsResult.message}"
            )
        }

        if (agentsResult.data == null) {
            logger.warn("fetchEnvIdAndAgents|START_AGENT_FAILED|${data.toLog()}|err=null agents")
            commonUtil.logDebugI18n(data, BK_AGENT_IS_BUSY, suffixMsg = dataContext.retryLog("null agents"))
            throw TPACommonUtil.queueRetry(
                ErrorCodeEnum.FOUND_AGENT_ERROR,
                suffixMsg = "System Error - $env: agent is null"
            )
        }

        val (envId, agentResData) = if (data.dispatchType.idType()) {
            Pair(
                HashUtil.decodeIdToLong((data.dispatchType as ThirdPartyAgentEnvDispatchType).envName),
                (agentsResult.data as List<EnvNodeAgent>)
            )
        } else {
            (agentsResult.data as Pair<Long, List<EnvNodeAgent>>)
        }

        if (agentResData.isEmpty()) {
            logger.warn("fetchEnvIdAndAgents|START_AGENT_FAILED|${data.toLog()}|err=empty agents")
            throw TPACommonUtil.queueRetry(
                ErrorCodeEnum.VM_NODE_NULL,
                errMsg = ErrorCodeEnum.BUILD_NODE_IS_EMPTY.getErrorMessage(arrayOf(env)),
                suffixMsg = "build cluster： $env is empty)"
            )
        }

        return Pair(envId, agentResData)
    }

    private fun ignoreAgentCheck(context: EnvQueueContext, dataContext: QueueDataContext) {
        val data = dataContext.data

        if (data.ignoreEnvAgentIds.isNullOrEmpty() || !data.isEnv()) {
            return
        }

        val agentMap = context.agents.associateBy { it.agentId }
        data.ignoreEnvAgentIds.forEach {
            val a = agentMap[it]
            commonUtil.logWithAgentUrl(data, BK_ENV_WORKER_ERROR_IGNORE, arrayOf(it), a?.nodeId, a?.agentId)
        }

        val activeAgents = context.agents.filter { it.agentId !in data.ignoreEnvAgentIds }
        if (activeAgents.isEmpty()) {
            throw TPACommonUtil.queueFailureI18n(
                ErrorCodeEnum.BK_ENV_WORKER_ERROR_IGNORE_ALL_ERROR,
                param = arrayOf(data.ignoreEnvAgentIds.joinToString(","))
            )
        }

        context.agents = activeAgents
        return
    }

    private fun allNodeConcurrencyCheck(context: EnvQueueContext, dataContext: QueueDataContext) {
        val data = dataContext.data

        if (data.allNodeConcurrency == null || !data.isEnv()) {
            return
        }

        val envId = context.envId
        val jobId = data.jobId
        if (envId == null || jobId.isNullOrBlank()) {
            logger.warn(
                "allNodeConcurrencyCheck|${data.toLog()}|has ${data.allNodeConcurrency} but env $envId job $jobId null"
            )
            return
        }

        if (context.projectJobRunningAndQueueAllCount(jobId) == null) {
            context.setProjectJobRunningAndQueueAllCount(
                jobId = jobId,
                cnt = thirdPartyAgentService.countProjectJobRunningAndQueueAll(
                    pipelineId = data.pipelineId,
                    envId = envId,
                    jobId = jobId,
                    projectId = data.projectId
                )
            )
        }

        val c = context.projectJobRunningAndQueueAllCount(jobId)!!
        if (c < data.allNodeConcurrency) {
            return
        }

        commonUtil.logI18n(
            data, BK_THIRD_JOB_ENV_CURR, arrayOf(
                c.toString(), data.allNodeConcurrency.toString(), (data.queueTimeoutMinutes ?: 10).toString()
            )
        )
        throw TPACommonUtil.queueRetry(ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR)
    }

    private fun singleNodeConcurrencyCheck(context: EnvQueueContext, dataContext: QueueDataContext) {
        val data = dataContext.data

        if (data.singleNodeConcurrency == null || !data.isEnv()) {
            return
        }

        val envId = context.envId
        val jobId = data.jobId
        if (envId == null || jobId.isNullOrBlank()) {
            logger.warn(
                "${data.toLog()}|has singleNodeConcurrency ${data.singleNodeConcurrency} but env $envId job $jobId null"
            )
            return
        }

        val activeAgents = mutableListOf<ThirdPartyAgent>()

        val agentIds = context.agents.map { it.agentId }.toSet()
        // 可能存在某些条件导致前面最后选剩下的 agent 在不同的编排中不同，这里需要补缺的
        if (context.agentsJobRunningAndQueueAllMap(jobId) == null) {
            context.setAllAgentsJobRunningAndQueueAllMap(
                jobId,
                thirdPartyAgentService.countAgentsJobRunningAndQueueAll(
                    pipelineId = data.pipelineId,
                    envId = envId,
                    jobId = jobId,
                    agentIds = agentIds,
                    projectId = data.projectId
                ).toMutableMap()
            )
        } else if (context.agentsJobRunningAndQueueAllMap(jobId)!!.keys != agentIds) {
            val newMap = thirdPartyAgentService.countAgentsJobRunningAndQueueAll(
                pipelineId = data.pipelineId,
                envId = envId,
                jobId = jobId,
                agentIds = agentIds,
                projectId = data.projectId
            )
            newMap.forEach { (k, v) ->
                if (!context.agentsJobRunningAndQueueAllMap(jobId)!!.containsKey(k)) {
                    context.setAgentsJobRunningAndQueueAllMap(jobId, k, v)
                }
            }
        }
        val m = context.agentsJobRunningAndQueueAllMap(jobId)!!.toMap()

        context.agents.forEach { agent ->
            // 为空说明当前节点没有记录就是没有任务直接加，除非并发是0的情况
            val agentCount = m[agent.agentId] ?: if (data.singleNodeConcurrency > 0) {
                activeAgents.add(agent)
                return@forEach
            } else {
                commonUtil.logDebug(data, "singleNodeConcurrency: ${data.singleNodeConcurrency} == 0")
                return@forEach
            }
            if (agentCount < data.singleNodeConcurrency) {
                activeAgents.add(agent)
                return@forEach
            }
            commonUtil.logDebug(
                data,
                "singleNodeConcurrency: ${agent.agentId}:$agentCount > ${data.singleNodeConcurrency}"
            )
        }

        // 没有一个节点满足则进入排队机制
        if (activeAgents.isEmpty()) {
            commonUtil.logI18n(
                data, BK_THIRD_JOB_NODE_CURR,
                arrayOf(data.singleNodeConcurrency.toString(), (data.queueTimeoutMinutes ?: 10).toString())
            )
            throw TPACommonUtil.queueRetry(ErrorCodeEnum.GET_BUILD_RESOURCE_ERROR)
        }

        context.agents = activeAgents
        return
    }

    private fun pickupAgent(context: EnvQueueContext, dataContext: QueueDataContext): Boolean {
        val data = dataContext.data

        if (!data.dispatchType.isEnv()) {
            logger.tagError("PickupAgentCheck|not env|${data.toLog()}")
            return false
        }

        val activeAgents = context.agents
        val preBuildAgentIds = thirdPartyAgentService.getPreBuildAgentIds(
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            vmSeqId = data.vmSeqId,
            size = activeAgents.size.coerceAtLeast(1)
        ).toSet()

        val isDocker = data.dispatchType.dockerInfo != null
        val runningCounts = mutableMapOf<String, Int>()
        val dockerRunningCounts = mutableMapOf<String, Int>()
        activeAgents.forEach { agent ->
            val rc = context.agentRunningCnt[agent.agentId]
                ?: thirdPartyAgentService.getRunningBuilds(agent.agentId).also {
                    context.agentRunningCnt[agent.agentId] = it
                }
            runningCounts[agent.agentId] = rc

            if (isDocker) {
                val drc = context.dockerRunningCnt[agent.agentId]
                    ?: thirdPartyAgentService.getDockerRunningBuilds(agent.agentId).also {
                        context.dockerRunningCnt[agent.agentId] = it
                    }
                dockerRunningCounts[agent.agentId] = drc
            }

            commonUtil.logDebug(
                data,
                "[${agent.agentId}]${agent.hostname}/${agent.ip}" +
                    ", Jobs:${runningCounts[agent.agentId]}, DockerJobs:${dockerRunningCounts[agent.agentId] ?: 0}"
            )
        }

        val strategyResult = thirdPartyAgentService.getEnvStrategiesWithTags(data.projectId, context.envId)
        val nodeIdToAgentId = mutableMapOf<Long, String>()
        activeAgents.forEach { agent ->
            agent.nodeId?.let { nodeIdToAgentId[HashUtil.decodeIdToLong(it)] = agent.agentId }
        }
        val agentTagValues = mutableMapOf<String, Map<Long, Set<String>>>()
        strategyResult.nodeTagValues.forEach { (nodeId, kv) ->
            nodeIdToAgentId[nodeId]?.let { agentId -> agentTagValues[agentId] = kv }
        }
        val executor = DispatchStrategyExecutor(
            input = DispatchStrategyExecutor.StrategyInput(
                allAgents = activeAgents,
                preBuildAgentIds = preBuildAgentIds,
                agentRunningCounts = runningCounts,
                dockerRunningCounts = dockerRunningCounts,
                agentTagValues = agentTagValues,
                tagKeys = strategyResult.tagKeys,
                isDockerBuilder = isDocker
            ),
            logAction = { msg -> commonUtil.logDebug(data, msg) }
        )
        val matched = executor.execute(strategyResult.strategies) { agent ->
            dataContext.buildAgent = agent
            val ok = tpaSingleQueueService.genAgentBuild(context, dataContext)
            if (ok) {
                commonUtil.logWithAgentUrl(
                    data = data,
                    messageCode = BK_ENV_DISPATCH_AGENT,
                    param = arrayOf("[${agent.agentId}]${agent.hostname}/${agent.ip}"),
                    nodeHashId = agent.nodeId,
                    agentHashId = agent.agentId
                )
            }
            ok
        }
        if (matched != null) {
            return true
        }

        commonUtil.logWarnI18n(data, BK_NO_AGENT_AVAILABLE)
        return false
    }

    fun afterGenAgentBuild(context: EnvQueueContext, dataContext: QueueDataContext) {
        dataContext.data.jobId?.let { jobId ->
            context.setProjectJobRunningAndQueueAllCount(jobId, null)
        }

        val agentId = dataContext.buildAgent?.agentId ?: return
        dataContext.data.jobId?.let { jobId ->
            context.setAgentsJobRunningAndQueueAllMap(jobId, agentId, null)
        }
        context.agentRunningCnt.remove(agentId)
        context.dockerRunningCnt.remove(agentId)
        context.hasTryAgents.remove(agentId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TPAEnvQueueService::class.java)
    }
}