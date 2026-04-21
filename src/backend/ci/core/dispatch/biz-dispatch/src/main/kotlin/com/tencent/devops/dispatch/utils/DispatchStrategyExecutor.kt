package com.tencent.devops.dispatch.utils

import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.DispatchStrategyConfig.Companion.toString
import com.tencent.devops.environment.pojo.LabelSelector
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.enums.LabelOp
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
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
        /** agentId -> 该 agent 拥有的标签键值: Map<tagKeyId, values> */
        val agentTagValues: Map<String, Map<Long, Set<String>>>,
        val tagKeys: Map<Long, String>,
        val isDockerBuilder: Boolean
    )

    private val hasTryAgents = mutableSetOf<String>()

    fun execute(
        strategies: List<DispatchStrategyConfig>,
        tryAgent: (ThirdPartyAgent) -> Boolean
    ): ThirdPartyAgent? {
        val envId = strategies.firstOrNull()?.envId
        val tag = "[Strategy|env:$envId]"

        pipelineLog(
            "$tag Start matching, ${strategies.size} strategies, " +
                    "${input.allAgents.size} agents, preBuild=${input.preBuildAgentIds.size}"
        )

        for ((index, strategy) in strategies.withIndex()) {
            if (!strategy.enabled) continue
            val lv = "$tag[Lv.${index + 1}]"

            pipelineLog(
                "$lv \"${strategy.strategyName ?: ""}\" scope=${strategy.scope} rule=${strategy.nodeRule}" + " labels=${
                    strategy.labelSelector?.joinToString(separator = ",") {
                        "${input.tagKeys[it.tagKeyId] ?: ""}${it.op.symbol}${it.values}"
                    }
                }"
            )

            // 检测是否命中范围
            val candidates = getCandidates(strategy.scope)
            if (candidates.isEmpty()) {
                pipelineLog("$lv No candidates for scope=${strategy.scope}, skip")
                continue
            }

            val filtered = filterByLabels(lv, candidates, strategy.labelSelector)
            if (filtered.isEmpty()) {
                pipelineLog("$lv All ${candidates.size} agents filtered out by labels, skip")
                continue
            }
            if (!strategy.labelSelector.isNullOrEmpty() && filtered.size < candidates.size) {
                pipelineLog("$lv Label filter: ${candidates.size} -> ${filtered.size} agents")
            }

            val sorted = sortByLoad(filtered)
            val matched = matchAndTry(sorted, strategy.nodeRule, tryAgent, lv)
            if (matched != null) {
                pipelineLog("$lv Matched agent [${matched.agentId}]${matched.hostname}/${matched.ip}")
                logger.info(
                    "DispatchStrategyExecutor|env:$envId|matched|strategy=${strategy.strategyName ?: ""}" +
                            "|agent=${matched.agentId}|scope=${strategy.scope}|rule=${strategy.nodeRule}"
                )
                return matched
            }
            pipelineLog("$lv No agent matched for rule=${strategy.nodeRule}")
        }

        pipelineLog("$tag All strategies exhausted, no agent available")
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
        logTag: String,
        agents: List<ThirdPartyAgent>,
        labelSelector: List<LabelSelector>?
    ): List<ThirdPartyAgent> {
        if (labelSelector.isNullOrEmpty()) return agents
        return agents.filter { agent ->
            val agentTags = input.agentTagValues[agent.agentId] ?: run {
                pipelineLog("$logTag match label [${agent.agentId}] no tag skip")
                return@filter false
            }
            labelSelector.all { selector ->
                matchLabel(
                    logTag = "$logTag match label [${agent.agentId}]",
                    selector = selector,
                    agentTags = agentTags
                )
            }
        }
    }

    private fun matchLabel(
        logTag: String,
        selector: LabelSelector,
        agentTags: Map<Long, Set<String>>
    ): Boolean {
        val agentValues = agentTags[selector.tagKeyId] ?: run {
            pipelineLog("$logTag no tag key [${input.tagKeys[selector.tagKeyId]}] skip")
            return false
        }
        val expected = selector.values
        if (expected.isEmpty()) return false
        return when (selector.op) {
            LabelOp.IN -> agentValues.any { it in expected }
            LabelOp.EQUAL -> expected.any { exp -> agentValues.any { it == exp } }
            LabelOp.GT -> expected.any { exp -> agentValues.any { compareValues(it, exp) > 0 } }
            LabelOp.GTE -> expected.any { exp -> agentValues.any { compareValues(it, exp) >= 0 } }
            LabelOp.LT -> expected.any { exp -> agentValues.any { compareValues(it, exp) < 0 } }
            LabelOp.LTE -> expected.any { exp -> agentValues.any { compareValues(it, exp) <= 0 } }
            LabelOp.START_WITH -> expected.any { exp -> agentValues.any { it.startsWith(exp) } }
            LabelOp.END_WITH -> expected.any { exp -> agentValues.any { it.endsWith(exp) } }
            LabelOp.CONTAINS -> expected.any { exp -> agentValues.any { it.contains(exp) } }
        }
    }

    /**
     * 比较两个值：优先尝试数值比较，失败则回退到字符串字典序比较。
     */
    private fun compareValues(agentValue: String, expected: String): Int {
        val aNum = agentValue.toDoubleOrNull()
        val bNum = expected.toDoubleOrNull()
        if (aNum != null && bNum != null) {
            return aNum.compareTo(bNum)
        }
        return agentValue.compareTo(expected)
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
        nodeRule: NodeRule?,
        tryAgent: (ThirdPartyAgent) -> Boolean,
        lv: String
    ): ThirdPartyAgent? {
        for (al in agents) {
            if (al.agent.agentId in hasTryAgents) continue

            val matched = when (nodeRule) {
                NodeRule.IDLE -> isIdle(al)
                NodeRule.AVAILABLE -> isAvailable(al)
                // 没有规则就是默认都可以
                else -> true
            }

            val a = al.agent
            val loadDesc = if (input.isDockerBuilder) {
                "dockerJobs=${al.dockerRunningCnt}/${a.dockerParallelTaskCount ?: 0}"
            } else {
                "jobs=${al.runningCnt}/${a.parallelTaskCount ?: 0}"
            }

            if (!matched) {
                pipelineLog(
                    "$lv [${a.agentId}]${a.hostname}/${a.ip} " +
                            "$loadDesc -> ${nodeRule?.name ?: "NULL"} not satisfied"
                )
                continue
            }

            pipelineLog(
                "$lv [${a.agentId}]${a.hostname}/${a.ip} " +
                        "$loadDesc -> ${nodeRule?.name ?: "NULL"} matched, trying to dispatch"
            )

            hasTryAgents.add(al.agent.agentId)
            if (tryAgent(al.agent)) {
                return al.agent
            }

            pipelineLog("$lv [${a.agentId}] dispatch failed, try next")
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
