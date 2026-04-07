package com.tencent.devops.dispatch.utils

import com.tencent.devops.dispatch.pojo.DispatchStrategyConfig
import com.tencent.devops.dispatch.pojo.LabelSelector
import com.tencent.devops.dispatch.pojo.enums.NodeRule
import com.tencent.devops.dispatch.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import org.slf4j.LoggerFactory

/**
 * 调度策略执行器，纯逻辑类，不依赖 Spring，不做任何 IO 操作。
 * 所有数据由调用方传入，方便单元测试和复用。
 *
 * @param input 调度所需的全部数据
 * @param logAction 流水线日志回调，在策略匹配的关键节点输出过程日志
 */
class DispatchStrategyExecutor(
    private val input: StrategyInput,
    private val logAction: ((String) -> Unit)? = null
) {

    data class StrategyInput(
        val allAgents: List<ThirdPartyAgent>,
        val preBuildAgentIds: Set<String>,
        val agentRunningCounts: Map<String, Int>,
        val dockerRunningCounts: Map<String, Int>,
        /** agentId -> 该 agent 拥有的 tagValueId 集合 */
        val agentTagValues: Map<String, Set<Long>>,
        val isDockerBuilder: Boolean
    )

    private val hasTryAgents = mutableSetOf<String>()

    /**
     * 按策略优先级依次尝试匹配 agent。
     *
     * @param strategies 按 priority 升序排列的策略列表（仅传入 enabled 的）
     * @param tryAgent 尝试将任务分配给该 agent 的回调，
     *        返回 true 表示成功（如 agentInQueue / genAgentBuild）
     * @return 成功分配的 agent，或 null
     */
    fun execute(
        strategies: List<DispatchStrategyConfig>,
        tryAgent: (ThirdPartyAgent) -> Boolean
    ): ThirdPartyAgent? {
        pipelineLog("[Strategy] Start matching, ${strategies.size} strategies, " +
            "${input.allAgents.size} agents, preBuild=${input.preBuildAgentIds.size}")

        for ((index, strategy) in strategies.withIndex()) {
            if (!strategy.enabled) continue

            pipelineLog("[Strategy Lv.${index + 1}] " +
                "\"${strategy.strategyName}\" scope=${strategy.scope} rule=${strategy.nodeRule}" +
                if (!strategy.labelSelector.isNullOrEmpty()) " labels=${strategy.labelSelector.size}" else "")

            val candidates = getCandidates(strategy.scope)
            if (candidates.isEmpty()) {
                pipelineLog("[Strategy Lv.${index + 1}] No candidates for scope=${strategy.scope}, skip")
                continue
            }

            val filtered = filterByLabels(candidates, strategy.labelSelector)
            if (filtered.isEmpty()) {
                pipelineLog("[Strategy Lv.${index + 1}] All ${candidates.size} agents filtered out by labels, skip")
                continue
            }
            if (!strategy.labelSelector.isNullOrEmpty() && filtered.size < candidates.size) {
                pipelineLog("[Strategy Lv.${index + 1}] Label filter: ${candidates.size} -> ${filtered.size} agents")
            }

            val sorted = sortByLoad(filtered)
            val matched = matchAndTry(sorted, strategy.nodeRule, tryAgent, index + 1)
            if (matched != null) {
                pipelineLog("[Strategy Lv.${index + 1}] Matched agent " +
                    "[${matched.agentId}]${matched.hostname}/${matched.ip}")
                logger.info(
                    "DispatchStrategyExecutor|matched|strategy=${strategy.strategyName}" +
                        "|agent=${matched.agentId}|scope=${strategy.scope}|rule=${strategy.nodeRule}"
                )
                return matched
            }
            pipelineLog("[Strategy Lv.${index + 1}] No agent matched for rule=${strategy.nodeRule}")
        }

        pipelineLog("[Strategy] All strategies exhausted, no agent available")
        return null
    }

    private fun getCandidates(scope: StrategyScope): List<ThirdPartyAgent> {
        return when (scope) {
            StrategyScope.PRE_BUILD -> {
                val idSet = input.preBuildAgentIds
                input.allAgents.filter { it.agentId in idSet }
            }
            StrategyScope.ALL -> input.allAgents
        }
    }

    private fun filterByLabels(
        agents: List<ThirdPartyAgent>,
        labelSelector: List<LabelSelector>?
    ): List<ThirdPartyAgent> {
        if (labelSelector.isNullOrEmpty()) return agents
        return agents.filter { agent ->
            val agentTags = input.agentTagValues[agent.agentId] ?: emptySet()
            labelSelector.all { selector ->
                selector.tagValueIds.any { it in agentTags }
            }
        }
    }

    private fun sortByLoad(agents: List<ThirdPartyAgent>): List<AgentWithLoad> {
        return agents.map { agent ->
            AgentWithLoad(
                agent = agent,
                runningCnt = input.agentRunningCounts[agent.agentId] ?: 0,
                dockerRunningCnt = input.dockerRunningCounts[agent.agentId] ?: 0
            )
        }.sortedBy {
            if (input.isDockerBuilder) it.dockerRunningCnt else it.runningCnt
        }
    }

    private fun matchAndTry(
        agents: List<AgentWithLoad>,
        nodeRule: NodeRule,
        tryAgent: (ThirdPartyAgent) -> Boolean,
        level: Int
    ): ThirdPartyAgent? {
        for (al in agents) {
            if (al.agent.agentId in hasTryAgents) continue

            val matched = when (nodeRule) {
                NodeRule.IDLE -> isIdle(al)
                NodeRule.AVAILABLE -> isAvailable(al)
            }

            val a = al.agent
            val loadDesc = if (input.isDockerBuilder) {
                "dockerJobs=${al.dockerRunningCnt}/${a.dockerParallelTaskCount ?: 0}"
            } else {
                "jobs=${al.runningCnt}/${a.parallelTaskCount ?: 0}"
            }

            if (!matched) {
                pipelineLog("[Strategy Lv.$level] [${a.agentId}]${a.hostname}/${a.ip} " +
                    "$loadDesc -> ${nodeRule.name} not satisfied")
                continue
            }

            pipelineLog("[Strategy Lv.$level] [${a.agentId}]${a.hostname}/${a.ip} " +
                "$loadDesc -> ${nodeRule.name} matched, trying to dispatch")

            hasTryAgents.add(al.agent.agentId)
            if (tryAgent(al.agent)) {
                return al.agent
            }

            pipelineLog("[Strategy Lv.$level] [${a.agentId}] dispatch failed, try next")
        }
        return null
    }

    private fun isIdle(al: AgentWithLoad): Boolean {
        return if (input.isDockerBuilder) al.dockerRunningCnt == 0 else al.runningCnt == 0
    }

    private fun isAvailable(al: AgentWithLoad): Boolean {
        return if (input.isDockerBuilder) {
            val limit = al.agent.dockerParallelTaskCount ?: 0
            limit == 0 || limit > al.dockerRunningCnt
        } else {
            val limit = al.agent.parallelTaskCount ?: 0
            limit == 0 || limit > al.runningCnt
        }
    }

    private fun pipelineLog(message: String) {
        logAction?.invoke(message)
    }

    data class AgentWithLoad(
        val agent: ThirdPartyAgent,
        val runningCnt: Int,
        val dockerRunningCnt: Int
    )

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchStrategyExecutor::class.java)
    }
}
