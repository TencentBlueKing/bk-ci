package com.tencent.devops.dispatch

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.LabelSelector
import com.tencent.devops.environment.pojo.enums.LabelOp
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.enums.StrategyType
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
        agentId = id, projectId = "proj", nodeId = "node_$id",
        status = AgentStatus.IMPORT_OK, hostname = "host_$id", os = "LINUX",
        ip = "10.0.0.$id", secretKey = "key", createUser = "test",
        createTime = System.currentTimeMillis(),
        parallelTaskCount = parallelTaskCount,
        dockerParallelTaskCount = dockerParallelTaskCount,
        masterVersion = null
    )

    private fun buildStrategy(
        scope: StrategyScope, nodeRule: NodeRule,
        enabled: Boolean = true, priority: Int = 0,
        labelSelector: List<LabelSelector>? = null,
        name: String = "${scope}_$nodeRule"
    ) = DispatchStrategyConfig(
        id = null, projectId = "proj", envId = 1L,
        strategyType = StrategyType.CUSTOM, defaultStrategyCode = null,
        strategyName = name, scope = scope, nodeRule = nodeRule,
        labelSelector = labelSelector, enabled = enabled, priority = priority,
        createdUser = "test", updatedUser = "test"
    )

    private fun input(
        agents: List<ThirdPartyAgent>,
        preBuild: Set<String> = emptySet(),
        running: Map<String, Int> = agents.associate { it.agentId to 0 },
        dockerRunning: Map<String, Int> = emptyMap(),
        tags: Map<String, Map<Long, Set<String>>> = emptyMap(),
        tagKeys: Map<Long, String> = emptyMap(),
        docker: Boolean = false
    ) = StrategyInput(
        allAgents = agents,
        preBuildAgentIds = preBuild,
        agentRunningCounts = running,
        dockerRunningCounts = dockerRunning,
        agentTagValues = tags,
        tagKeys = tagKeys,
        isDockerBuilder = docker
    )

    // ========== 基本策略匹配 ==========

    @Nested
    @DisplayName("基本策略匹配")
    inner class BasicMatching {
        @Test fun emptyAgents() {
            val r = DispatchStrategyExecutor(input(emptyList()))
                .execute(DispatchStrategyConfig.buildDefaults("proj", 1L, "u")) { true }
            assertNull(r)
        }
        @Test fun emptyStrategies() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"))))
                .execute(emptyList()) { true }
            assertNull(r)
        }
        @Test fun allDisabled() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"))))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE, enabled = false))) { true }
            assertNull(r)
        }
    }

    // ========== IDLE / AVAILABLE 规则 ==========

    @Nested
    @DisplayName("IDLE 规则")
    inner class IdleRule {
        @Test fun matchIdle() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"))))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertEquals("1", r!!.agentId)
        }
        @Test fun skipBusy() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1")), running = mapOf("1" to 3)))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertNull(r)
        }
        @Test fun dockerIdle() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1")), running = mapOf("1" to 5),
                dockerRunning = mapOf("1" to 0), docker = true
            )).execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertNotNull(r)
        }
    }

    @Nested
    @DisplayName("AVAILABLE 规则")
    inner class AvailableRule {
        @Test fun matchAvailable() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1", parallelTaskCount = 4)), running = mapOf("1" to 2)))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))) { true }
            assertNotNull(r)
        }
        @Test fun skipFull() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1", parallelTaskCount = 2)), running = mapOf("1" to 2)))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))) { true }
            assertNull(r)
        }
        @Test fun unlimitedParallel() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1", parallelTaskCount = 0)), running = mapOf("1" to 999)))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))) { true }
            assertNotNull(r)
        }
    }

    // ========== Scope 过滤 ==========

    @Nested
    @DisplayName("Scope 过滤")
    inner class ScopeFiltering {
        @Test fun preBuildScope() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"), buildAgent("2")), preBuild = setOf("1")))
                .execute(listOf(buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE))) { true }
            assertEquals("1", r!!.agentId)
        }
        @Test fun preBuildEmpty() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"))))
                .execute(listOf(buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE))) { true }
            assertNull(r)
        }
    }

    // ========== 标签操作符（9种）==========

    @Nested
    @DisplayName("标签操作符")
    inner class LabelOperators {

        private fun execWithLabel(
            agentTags: Map<Long, Set<String>>,
            op: LabelOp,
            values: Set<String>,
            tagKeyId: Long = 1L
        ): ThirdPartyAgent? {
            val a = buildAgent("1")
            return DispatchStrategyExecutor(input(listOf(a), tags = mapOf("1" to agentTags)))
                .execute(listOf(buildStrategy(
                    StrategyScope.ALL, NodeRule.IDLE,
                    labelSelector = listOf(LabelSelector(tagKeyId = tagKeyId, op = op, values = values))
                ))) { true }
        }

        @Test fun inMatch() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("linux", "windows")), LabelOp.IN, setOf("linux")))
        }
        @Test fun inNoMatch() {
            assertNull(execWithLabel(mapOf(1L to setOf("macos")), LabelOp.IN, setOf("linux", "windows")))
        }
        @Test fun equalMatch() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("v1.0")), LabelOp.EQUAL, setOf("v1.0")))
        }
        @Test fun equalNoMatch() {
            assertNull(execWithLabel(mapOf(1L to setOf("v2.0")), LabelOp.EQUAL, setOf("v1.0")))
        }
        @Test fun gtNumeric() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("10")), LabelOp.GT, setOf("5")))
        }
        @Test fun gtNumericFail() {
            assertNull(execWithLabel(mapOf(1L to setOf("3")), LabelOp.GT, setOf("5")))
        }
        @Test fun gteEqual() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("5")), LabelOp.GTE, setOf("5")))
        }
        @Test fun ltNumeric() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("3")), LabelOp.LT, setOf("5")))
        }
        @Test fun lteFail() {
            assertNull(execWithLabel(mapOf(1L to setOf("6")), LabelOp.LTE, setOf("5")))
        }
        @Test fun startWith() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("release-1.0")), LabelOp.START_WITH, setOf("release")))
        }
        @Test fun startWithFail() {
            assertNull(execWithLabel(mapOf(1L to setOf("dev-1.0")), LabelOp.START_WITH, setOf("release")))
        }
        @Test fun endWith() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("app-linux")), LabelOp.END_WITH, setOf("linux")))
        }
        @Test fun endWithFail() {
            assertNull(execWithLabel(mapOf(1L to setOf("app-linux")), LabelOp.END_WITH, setOf("windows")))
        }
        @Test fun contains() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("my-feature-branch")), LabelOp.CONTAINS, setOf("feature")))
        }
        @Test fun containsFail() {
            assertNull(execWithLabel(mapOf(1L to setOf("my-hotfix-branch")), LabelOp.CONTAINS, setOf("feature")))
        }
        @Test fun stringCompare() {
            assertNotNull(execWithLabel(mapOf(1L to setOf("beta")), LabelOp.GT, setOf("alpha")))
        }
        @Test fun missingTagKey() {
            assertNull(execWithLabel(mapOf(2L to setOf("linux")), LabelOp.IN, setOf("linux")))
        }

        @Test
        @DisplayName("多标签条件 AND 语义")
        fun multiLabelAnd() {
            val a1 = buildAgent("1")
            val a2 = buildAgent("2")
            val tags = mapOf(
                "1" to mapOf(1L to setOf("linux"), 2L to setOf("amd64")),
                "2" to mapOf(1L to setOf("linux"))
            )
            val r = DispatchStrategyExecutor(input(listOf(a1, a2), tags = tags))
                .execute(listOf(buildStrategy(
                    StrategyScope.ALL, NodeRule.IDLE,
                    labelSelector = listOf(
                        LabelSelector(tagKeyId = 1, op = LabelOp.EQUAL, values = setOf("linux")),
                        LabelSelector(tagKeyId = 2, op = LabelOp.EQUAL, values = setOf("amd64"))
                    )
                ))) { true }
            assertEquals("1", r!!.agentId)
        }

        @Test
        @DisplayName("无标签选择器时不过滤")
        fun nullLabelSelector() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"))))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE, labelSelector = null))) { true }
            assertNotNull(r)
        }
    }

    // ========== 负载排序 ==========

    @Nested
    @DisplayName("负载排序")
    inner class LoadSorting {
        @Test fun sortByRunning() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1"), buildAgent("2"), buildAgent("3")),
                running = mapOf("1" to 5, "2" to 1, "3" to 3)
            )).execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.AVAILABLE))) { true }
            assertEquals("2", r!!.agentId)
        }
        @Test fun sortByDockerRunning() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1"), buildAgent("2")),
                running = mapOf("1" to 1, "2" to 10),
                dockerRunning = mapOf("1" to 3, "2" to 0), docker = true
            )).execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertEquals("2", r!!.agentId)
        }
    }

    // ========== 策略优先级与降级 ==========

    @Nested
    @DisplayName("策略优先级与降级")
    inner class PriorityAndFallback {
        @Test fun defaultFourStrategies() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1"), buildAgent("2")),
                preBuild = setOf("1"), running = mapOf("1" to 2, "2" to 0)
            )).execute(DispatchStrategyConfig.buildDefaults("proj", 1L, "user")) { true }
            assertEquals("1", r!!.agentId)
        }
        @Test fun fallback() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1", parallelTaskCount = 1), buildAgent("2")),
                preBuild = setOf("1"), running = mapOf("1" to 1, "2" to 0)
            )).execute(DispatchStrategyConfig.buildDefaults("proj", 1L, "user")) { true }
            assertEquals("2", r!!.agentId)
        }
        @Test fun dockerFullFallback() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1", dockerParallelTaskCount = 2), buildAgent("2", dockerParallelTaskCount = 2)),
                preBuild = setOf("1"),
                dockerRunning = mapOf("1" to 2, "2" to 1), docker = true
            )).execute(DispatchStrategyConfig.buildDefaults("proj", 1L, "user")) { true }
            assertEquals("2", r!!.agentId)
        }
        @Test fun totalMismatch() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1", parallelTaskCount = 1)), running = mapOf("1" to 1)
            )).execute(DispatchStrategyConfig.buildDefaults("proj", 1L, "user")) { true }
            assertNull(r)
        }
    }

    // ========== hasTryAgents 去重 ==========

    @Nested
    @DisplayName("hasTryAgents 去重")
    inner class Dedup {
        @Test fun failedNotRetried() {
            val tried = mutableListOf<String>()
            DispatchStrategyExecutor(input(listOf(buildAgent("1")), preBuild = setOf("1")))
                .execute(listOf(
                    buildStrategy(StrategyScope.PRE_BUILD, NodeRule.IDLE, priority = 0),
                    buildStrategy(StrategyScope.ALL, NodeRule.IDLE, priority = 1)
                )) { tried.add(it.agentId); false }
            assertEquals(1, tried.size)
        }
    }

    // ========== 综合场景 ==========

    @Nested
    @DisplayName("综合场景")
    inner class Integration {
        @Test fun labelsWithPreBuild() {
            val tags = mapOf(
                "1" to mapOf(1L to setOf("a")),
                "2" to mapOf(1L to setOf("a", "b")),
                "3" to mapOf(1L to setOf("b"))
            )
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1"), buildAgent("2"), buildAgent("3")),
                preBuild = setOf("1", "2"), tags = tags
            )).execute(listOf(buildStrategy(
                StrategyScope.PRE_BUILD, NodeRule.IDLE,
                labelSelector = listOf(LabelSelector(tagKeyId = 1, op = LabelOp.IN, values = setOf("b")))
            ))) { true }
            assertEquals("2", r!!.agentId)
        }

        @Test fun customFallbackToDefault() {
            val r = DispatchStrategyExecutor(input(
                listOf(buildAgent("1"), buildAgent("2")),
                tags = mapOf("1" to mapOf(1L to setOf("x")), "2" to mapOf(1L to setOf("y")))
            )).execute(listOf(
                buildStrategy(StrategyScope.ALL, NodeRule.IDLE, priority = 0,
                    labelSelector = listOf(LabelSelector(tagKeyId = 1, op = LabelOp.EQUAL, values = setOf("zzz")))),
                buildStrategy(StrategyScope.ALL, NodeRule.IDLE, priority = 1)
            )) { true }
            assertNotNull(r)
        }
    }

    // ========== 流水线日志 ==========

    @Nested
    @DisplayName("流水线日志")
    inner class Logging {
        @Test fun logsOnMatch() {
            val logs = mutableListOf<String>()
            DispatchStrategyExecutor(input(listOf(buildAgent("1"))), logAction = { logs.add(it) })
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertTrue(logs.any { "Start matching" in it && "env:1" in it })
            assertTrue(logs.any { "Matched agent" in it })
        }
        @Test fun logsOnExhausted() {
            val logs = mutableListOf<String>()
            DispatchStrategyExecutor(input(listOf(buildAgent("1")), running = mapOf("1" to 3)), logAction = { logs.add(it) })
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertTrue(logs.any { "All strategies exhausted" in it })
        }
        @Test fun logsLabelFilter() {
            val logs = mutableListOf<String>()
            DispatchStrategyExecutor(
                input(listOf(buildAgent("1"), buildAgent("2")), tags = mapOf("1" to mapOf(1L to setOf("ok")))),
                logAction = { logs.add(it) }
            ).execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE,
                labelSelector = listOf(LabelSelector(tagKeyId = 1, op = LabelOp.IN, values = setOf("ok")))
            ))) { true }
            assertTrue(logs.any { "Label filter" in it && "2 -> 1" in it })
        }
        @Test fun noLogActionSafe() {
            val r = DispatchStrategyExecutor(input(listOf(buildAgent("1"))))
                .execute(listOf(buildStrategy(StrategyScope.ALL, NodeRule.IDLE))) { true }
            assertNotNull(r)
        }
    }
}
