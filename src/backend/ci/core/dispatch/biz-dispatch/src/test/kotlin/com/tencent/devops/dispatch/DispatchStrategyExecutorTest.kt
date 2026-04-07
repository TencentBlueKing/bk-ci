package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.dispatch.pojo.DispatchStrategyConfig
import com.tencent.devops.dispatch.pojo.LabelSelector
import com.tencent.devops.dispatch.pojo.enums.DefaultStrategyCode
import com.tencent.devops.dispatch.pojo.enums.NodeRule
import com.tencent.devops.dispatch.pojo.enums.StrategyScope
import com.tencent.devops.dispatch.pojo.enums.StrategyType
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor
import com.tencent.devops.dispatch.utils.DispatchStrategyExecutor.StrategyInput
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DispatchStrategyExecutor 单元测试")
class DispatchStrategyExecutorTest {

    private fun buildAgent(
        id: String,
        parallelTaskCount: Int? = 4,
        dockerParallelTaskCount: Int? = 4
    ) = ThirdPartyAgent(
        agentId = id,
        projectId = "proj",
        nodeId = "node_$id",
        status = AgentStatus.IMPORT_OK,
        hostname = "host_$id",
        os = "LINUX",
        ip = "10.0.0.$id",
        secretKey = "key",
        createUser = "test",
        createTime = System.currentTimeMillis(),
        parallelTaskCount = parallelTaskCount,
        dockerParallelTaskCount = dockerParallelTaskCount,
        masterVersion = null
    )

    private fun buildStrategy(
        scope: StrategyScope,
        nodeRule: NodeRule,
        enabled: Boolean = true,
        priority: Int = 0,
        labelSelector: List<LabelSelector>? = null,
        name: String = "${scope}_$nodeRule"
    ) = DispatchStrategyConfig(
        id = null,
        projectId = "proj",
        envId = 1L,
        strategyType = StrategyType.CUSTOM,
        defaultStrategyCode = null,
        strategyName = name,
        scope = scope,
        nodeRule = nodeRule,
        labelSelector = labelSelector,
        enabled = enabled,
        priority = priority,
        createdUser = "test",
        updatedUser = "test"
    )

    @Nested
    @DisplayName("基本策略匹配")
    inner class BasicMatching {

        @Test
        @DisplayName("空 agent 列表应返回 null")
        fun emptyAgents() {
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = emptyList(),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = emptyMap(),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                DispatchStrategyConfig.buildDefaults("proj", 1L, "user")
            ) { true }
            assertNull(result)
        }

        @Test
        @DisplayName("空策略列表应返回 null")
        fun emptyStrategies() {
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(buildAgent("1")),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(emptyList()) { true }
            assertNull(result)
        }

        @Test
        @DisplayName("全部策略 disabled 应返回 null")
        fun allDisabled() {
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(buildAgent("1")),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val strategies = listOf(
                buildStrategy(StrategyScope.ALL, NodeRule.IDLE, enabled = false)
            )
            val result = executor.execute(strategies) { true }
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("IDLE 规则")
    inner class IdleRule {

        @Test
        @DisplayName("空闲 agent 应被匹配")
        fun matchIdleAgent() {
            val agent = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }
            assertNotNull(result)
            assertEquals("1", result!!.agentId)
        }

        @Test
        @DisplayName("非空闲 agent 不应被 IDLE 匹配")
        fun skipBusyAgent() {
            val agent = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 3),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }
            assertNull(result)
        }

        @Test
        @DisplayName("Docker 模式下 IDLE 按 dockerRunningCnt 判断")
        fun dockerIdleCheck() {
            val agent = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 5),
                    dockerRunningCounts = mapOf("1" to 0),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = true
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }
            assertNotNull(result)
        }
    }

    @Nested
    @DisplayName("AVAILABLE 规则")
    inner class AvailableRule {

        @Test
        @DisplayName("未达并发上限的 agent 应被匹配")
        fun matchAvailableAgent() {
            val agent = buildAgent("1", parallelTaskCount = 4)
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 2),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))
            ) { true }
            assertNotNull(result)
        }

        @Test
        @DisplayName("已达并发上限的 agent 不应被匹配")
        fun skipFullAgent() {
            val agent = buildAgent("1", parallelTaskCount = 2)
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 2),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))
            ) { true }
            assertNull(result)
        }

        @Test
        @DisplayName("parallelTaskCount=0 表示无限制，应始终匹配")
        fun unlimitedParallel() {
            val agent = buildAgent("1", parallelTaskCount = 0)
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 999),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))
            ) { true }
            assertNotNull(result)
        }

        @Test
        @DisplayName("Docker 模式下 AVAILABLE 按 dockerParallelTaskCount 判断")
        fun dockerAvailableCheck() {
            val agent = buildAgent("1", dockerParallelTaskCount = 3)
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(agent),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 10),
                    dockerRunningCounts = mapOf("1" to 3),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = true
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))
            ) { true }
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Scope 过滤")
    inner class ScopeFiltering {

        @Test
        @DisplayName("PRE_BUILD scope 只选取 preBuildAgentIds 中的 agent")
        fun preBuildScope() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE))
            ) { true }
            assertNotNull(result)
            assertEquals("1", result!!.agentId)
        }

        @Test
        @DisplayName("ALL scope 选取全部 agent")
        fun allScope() {
            val agents = listOf(buildAgent("1"), buildAgent("2"), buildAgent("3"))
            val tried = mutableListOf<String>()
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = agents,
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0, "3" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { agent ->
                tried.add(agent.agentId)
                false
            }
            assertEquals(3, tried.size)
        }

        @Test
        @DisplayName("PRE_BUILD scope 无匹配 preBuildAgent 时应跳过")
        fun preBuildEmpty() {
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(buildAgent("1")),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE))
            ) { true }
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("标签过滤")
    inner class LabelFiltering {

        @Test
        @DisplayName("匹配标签的 agent 应通过过滤")
        fun matchLabels() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = mapOf(
                        "1" to setOf(100L, 200L),
                        "2" to setOf(300L)
                    ),
                    isDockerBuilder = false
                )
            )
            val strategy = buildStrategy(
                StrategyScope.ALL, NodeRule.IDLE,
                labelSelector = listOf(LabelSelector(tagKeyId = 1, op = "IN", tagValueIds = setOf(100L)))
            )
            val result = executor.execute(listOf(strategy)) { true }
            assertNotNull(result)
            assertEquals("1", result!!.agentId)
        }

        @Test
        @DisplayName("不匹配标签的 agent 应被过滤掉")
        fun noMatchLabels() {
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = mapOf("1" to setOf(300L)),
                    isDockerBuilder = false
                )
            )
            val strategy = buildStrategy(
                StrategyScope.ALL, NodeRule.IDLE,
                labelSelector = listOf(LabelSelector(tagKeyId = 1, op = "IN", tagValueIds = setOf(100L)))
            )
            val result = executor.execute(listOf(strategy)) { true }
            assertNull(result)
        }

        @Test
        @DisplayName("多个标签条件使用 AND 语义")
        fun multiLabelAnd() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = mapOf(
                        "1" to setOf(100L, 200L),
                        "2" to setOf(100L)
                    ),
                    isDockerBuilder = false
                )
            )
            val strategy = buildStrategy(
                StrategyScope.ALL, NodeRule.IDLE,
                labelSelector = listOf(
                    LabelSelector(tagKeyId = 1, op = "IN", tagValueIds = setOf(100L)),
                    LabelSelector(tagKeyId = 2, op = "IN", tagValueIds = setOf(200L))
                )
            )
            val result = executor.execute(listOf(strategy)) { true }
            assertNotNull(result)
            assertEquals("1", result!!.agentId)
        }

        @Test
        @DisplayName("无标签选择器时不过滤")
        fun nullLabelSelector() {
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val strategy = buildStrategy(StrategyScope.ALL, NodeRule.IDLE, labelSelector = null)
            val result = executor.execute(listOf(strategy)) { true }
            assertNotNull(result)
        }
    }

    @Nested
    @DisplayName("负载排序")
    inner class LoadSorting {

        @Test
        @DisplayName("应优先选择负载最低的 agent")
        fun sortByRunningCount() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val a3 = buildAgent("3")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2, a3),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 5, "2" to 1, "3" to 3),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))
            ) { true }
            assertNotNull(result)
            assertEquals("2", result!!.agentId)
        }

        @Test
        @DisplayName("Docker 模式按 dockerRunningCnt 排序")
        fun sortByDockerRunningCount() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 1, "2" to 10),
                    dockerRunningCounts = mapOf("1" to 3, "2" to 0),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = true
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }
            assertNotNull(result)
            assertEquals("2", result!!.agentId)
        }
    }

    @Nested
    @DisplayName("策略优先级与降级")
    inner class PriorityAndFallback {

        @Test
        @DisplayName("按 priority 顺序执行策略，首个匹配即返回")
        fun priorityOrder() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 2, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val strategies = listOf(
                buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE, priority = 0),
                buildStrategy(StrategyScope.ALL, NodeRule.IDLE, priority = 1)
            )
            val result = executor.execute(strategies) { true }
            assertNotNull(result)
            assertEquals("2", result!!.agentId)
        }

        @Test
        @DisplayName("默认 4 条策略应按 Lv1→Lv4 降级")
        fun defaultFourStrategies() {
            val preBuild = buildAgent("1")
            val other = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(preBuild, other),
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 2, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val defaults = DispatchStrategyConfig.buildDefaults("proj", 1L, "user")
            val result = executor.execute(defaults) { true }
            assertNotNull(result)
            // Lv1 (PRE_BUILD+IDLE): agent1 运行中 → 不匹配
            // Lv2 (PRE_BUILD+AVAILABLE): agent1 (running=2 < parallel=4) → 匹配
            assertEquals("1", result!!.agentId)
        }

        @Test
        @DisplayName("前面策略全部不匹配时应降级到后面策略")
        fun fallbackToLowerPriority() {
            val a1 = buildAgent("1", parallelTaskCount = 1)
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 1, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val defaults = DispatchStrategyConfig.buildDefaults("proj", 1L, "user")
            val result = executor.execute(defaults) { true }
            assertNotNull(result)
            // Lv1 (PRE_BUILD+IDLE): agent1 busy → 跳过
            // Lv2 (PRE_BUILD+AVAILABLE): agent1 (1 >= 1) → 已满 → 跳过
            // Lv3 (ALL+IDLE): agent2 (running=0) → 匹配
            assertEquals("2", result!!.agentId)
        }
    }

    @Nested
    @DisplayName("hasTryAgents 去重")
    inner class DeduplicationAcrossStrategies {

        @Test
        @DisplayName("tryAgent 失败的 agent 在后续策略中不应重试")
        fun failedAgentNotRetried() {
            val a1 = buildAgent("1")
            val tried = mutableListOf<String>()
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val strategies = listOf(
                buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE, priority = 0),
                buildStrategy(StrategyScope.ALL, NodeRule.IDLE, priority = 1)
            )
            val result = executor.execute(strategies) { agent ->
                tried.add(agent.agentId)
                false
            }
            assertNull(result)
            assertEquals(1, tried.size, "agent1 应只被尝试一次")
        }

        @Test
        @DisplayName("已成功的 agent 不影响其他 agent 的尝试")
        fun successDoesNotBlockOthers() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { agent -> agent.agentId == "2" }
            assertNotNull(result)
            assertEquals("2", result!!.agentId)
        }
    }

    @Nested
    @DisplayName("tryAgent 回调")
    inner class TryAgentCallback {

        @Test
        @DisplayName("tryAgent 返回 false 时应尝试下一个 agent")
        fun tryNextOnFailure() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { agent -> agent.agentId == "2" }
            assertNotNull(result)
            assertEquals("2", result!!.agentId)
        }

        @Test
        @DisplayName("所有 agent 的 tryAgent 都返回 false 时应返回 null")
        fun allFail() {
            val agents = (1..5).map { buildAgent(it.toString()) }
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = agents,
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = agents.associate { it.agentId to 0 },
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { false }
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("综合场景")
    inner class IntegrationScenarios {

        @Test
        @DisplayName("自定义策略 + 标签 + PRE_BUILD scope 综合")
        fun customStrategyWithLabelsAndPreBuild() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val a3 = buildAgent("3")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2, a3),
                    preBuildAgentIds = setOf("1", "2"),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0, "3" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = mapOf(
                        "1" to setOf(10L),
                        "2" to setOf(10L, 20L),
                        "3" to setOf(20L)
                    ),
                    isDockerBuilder = false
                )
            )
            val strategy = buildStrategy(
                StrategyScope.PRE_BUILD, NodeRule.IDLE,
                labelSelector = listOf(LabelSelector(tagKeyId = 1, op = "IN", tagValueIds = setOf(20L)))
            )
            val result = executor.execute(listOf(strategy)) { true }
            assertNotNull(result)
            // PRE_BUILD → a1, a2; label 20L → a2; idle → a2
            assertEquals("2", result!!.agentId)
        }

        @Test
        @DisplayName("多策略降级：自定义策略不匹配后降级到默认策略")
        fun customFallbackToDefault() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = mapOf(
                        "1" to setOf(10L),
                        "2" to setOf(20L)
                    ),
                    isDockerBuilder = false
                )
            )
            val strategies = listOf(
                buildStrategy(
                    StrategyScope.ALL, NodeRule.IDLE, priority = 0,
                    labelSelector = listOf(LabelSelector(tagKeyId = 1, op = "IN", tagValueIds = setOf(999L)))
                ),
                buildStrategy(StrategyScope.ALL, NodeRule.IDLE, priority = 1)
            )
            val result = executor.execute(strategies) { true }
            assertNotNull(result)
            // 第一个策略标签 999L 无匹配，降级到第二个策略（无标签），匹配 a1
            assertTrue(result!!.agentId in setOf("1", "2"))
        }

        @Test
        @DisplayName("Docker 模式下完整 4 级降级")
        fun dockerFullFallback() {
            val a1 = buildAgent("1", dockerParallelTaskCount = 2)
            val a2 = buildAgent("2", dockerParallelTaskCount = 2)
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = setOf("1"),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = mapOf("1" to 2, "2" to 1),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = true
                )
            )
            val defaults = DispatchStrategyConfig.buildDefaults("proj", 1L, "user")
            val result = executor.execute(defaults) { true }
            assertNotNull(result)
            // Lv1 (PRE_BUILD+IDLE): a1 dockerRunning=2 → 不空闲
            // Lv2 (PRE_BUILD+AVAILABLE): a1 dockerRunning=2 >= limit=2 → 已满
            // Lv3 (ALL+IDLE): a1 busy, a2 busy → 都不空闲
            // Lv4 (ALL+AVAILABLE): a2 dockerRunning=1 < limit=2 → 匹配
            assertEquals("2", result!!.agentId)
        }

        @Test
        @DisplayName("全部策略全部 agent 都不满足时返回 null")
        fun totalMismatch() {
            val a1 = buildAgent("1", parallelTaskCount = 1)
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 1),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val defaults = DispatchStrategyConfig.buildDefaults("proj", 1L, "user")
            val result = executor.execute(defaults) { true }
            // Lv1: PRE_BUILD 为空; Lv2: PRE_BUILD 为空; Lv3: IDLE→busy; Lv4: AVAILABLE→1>=1 已满
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("流水线日志输出")
    inner class PipelineLogging {

        @Test
        @DisplayName("匹配成功时应输出策略匹配过程日志")
        fun logsOnMatch() {
            val logs = mutableListOf<String>()
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                input = StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                ),
                logAction = { logs.add(it) }
            )
            executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }

            assertTrue(logs.any { "Start matching" in it })
            assertTrue(logs.any { "Lv.1" in it && "IDLE matched" in it })
            assertTrue(logs.any { "Matched agent" in it })
        }

        @Test
        @DisplayName("匹配失败时应输出 exhausted 日志")
        fun logsOnExhausted() {
            val logs = mutableListOf<String>()
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                input = StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 3),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                ),
                logAction = { logs.add(it) }
            )
            executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }

            assertTrue(logs.any { "All strategies exhausted" in it })
        }

        @Test
        @DisplayName("标签过滤淘汰时应输出 label filter 日志")
        fun logsOnLabelFilter() {
            val logs = mutableListOf<String>()
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val executor = DispatchStrategyExecutor(
                input = StrategyInput(
                    allAgents = listOf(a1, a2),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0, "2" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = mapOf("1" to setOf(10L)),
                    isDockerBuilder = false
                ),
                logAction = { logs.add(it) }
            )
            val strategy = buildStrategy(
                StrategyScope.ALL, NodeRule.IDLE,
                labelSelector = listOf(LabelSelector(tagKeyId = 1, op = "IN", tagValueIds = setOf(10L)))
            )
            executor.execute(listOf(strategy)) { true }

            assertTrue(logs.any { "Label filter" in it && "2 -> 1" in it })
        }

        @Test
        @DisplayName("scope 无候选时应输出 No candidates 日志")
        fun logsOnNoCandidates() {
            val logs = mutableListOf<String>()
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                input = StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                ),
                logAction = { logs.add(it) }
            )
            executor.execute(
                listOf(buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE))
            ) { true }

            assertTrue(logs.any { "No candidates" in it })
        }

        @Test
        @DisplayName("不传 logAction 时不应报错")
        fun noLogActionSafe() {
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                )
            )
            val result = executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { true }
            assertNotNull(result)
        }

        @Test
        @DisplayName("tryAgent 失败时应输出 dispatch failed 日志")
        fun logsOnDispatchFailed() {
            val logs = mutableListOf<String>()
            val a1 = buildAgent("1")
            val executor = DispatchStrategyExecutor(
                input = StrategyInput(
                    allAgents = listOf(a1),
                    preBuildAgentIds = emptySet(),
                    agentRunningCounts = mapOf("1" to 0),
                    dockerRunningCounts = emptyMap(),
                    agentTagValues = emptyMap(),
                    isDockerBuilder = false
                ),
                logAction = { logs.add(it) }
            )
            executor.execute(
                listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))
            ) { false }

            assertTrue(logs.any { "dispatch failed" in it })
        }
    }
}
