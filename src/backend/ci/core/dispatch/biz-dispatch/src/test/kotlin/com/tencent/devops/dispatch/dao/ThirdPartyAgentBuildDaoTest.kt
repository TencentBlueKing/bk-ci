package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentBuild
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ThirdPartyAgentBuildDaoTest {
    private val dao = ThirdPartyAgentBuildDao()

    @TestFactory
    fun projectScopeQueries(): List<DynamicTest> {
        val scenarios = listOf(
            ScopeScenario("all-windows", -2L, null, true),
            ScopeScenario("all-linux", -3L, null, true),
            ScopeScenario("all-macos", -4L, null, true),
            ScopeScenario("all-windows-with-agent", -2L, "agent", true),
            ScopeScenario("all-linux-with-agent", -3L, "agent", true),
            ScopeScenario("all-macos-with-agent", -4L, "agent", true),
            ScopeScenario("shared-environment", 123L, null, false),
            ScopeScenario("shared-environment-with-agent", 123L, "agent", false),
            ScopeScenario("shared-agent", null, "agent", false),
            ScopeScenario("other-negative-environment", -1L, null, false),
            ScopeScenario("other-negative-environment-with-agent", -5L, "agent", false)
        )
        return scenarios.flatMap { scenario ->
            listOf("project-a", "project-b").flatMap { projectId ->
                val sqlList = mutableListOf<String>()
                val bindingsList = mutableListOf<List<Any?>>()
                val provider = MockDataProvider { context ->
                    sqlList.add(context.sql())
                    bindingsList.add(context.bindings().toList())
                    arrayOf(emptyQueryResult(context.sql()))
                }
                val dslContext = DSL.using(MockConnection(provider), SQLDialect.MYSQL)
                queries(dslContext, projectId, scenario).map { (name, query) ->
                    DynamicTest.dynamicTest("${scenario.name}/$projectId/$name") {
                        sqlList.clear()
                        bindingsList.clear()
                        query()
                        assertEquals(1, sqlList.size, "The DAO method must execute one query")
                        val sql = sqlList.single()
                        val bindings = bindingsList.single()
                        val projectPredicate = Regex("`PROJECT_ID`\\s*=\\s*\\?", RegexOption.IGNORE_CASE)
                        assertEquals(scenario.projectScoped, projectPredicate.containsMatchIn(sql), sql)
                        assertEquals(scenario.projectScoped, projectId in bindings, sql)
                        if (scenario.envId != null) {
                            assertTrue(scenario.envId in bindings, "Environment condition must be preserved: $sql")
                        }
                        if (scenario.agentId != null) {
                            assertTrue(scenario.agentId in bindings, "Agent condition must be preserved: $sql")
                        }
                    }
                }
            }
        }
    }

    private fun queries(
        dslContext: DSLContext,
        projectId: String,
        scenario: ScopeScenario
    ): Map<String, () -> Any?> {
        val agentId = scenario.agentId
        val envId = scenario.envId
        return linkedMapOf(
            "countAgentBuildsByJob" to {
                dao.countAgentBuildsByJob(
                    dslContext, projectId, agentId, envId,
                    pipelineId = "pipeline", jobId = "job", startTime = null, endTime = null
                )
            },
            "listAgentBuildsByJob" to {
                dao.listAgentBuildsByJob(
                    dslContext, projectId, agentId, envId,
                    pipelineId = "pipeline", jobId = "job", offset = 0, limit = 10,
                    startTime = null, endTime = null
                )
            },
            "countAgentBuildPipelineJob" to {
                dao.countAgentBuildPipelineJob(
                    dslContext, projectId, agentId, envId,
                    startTime = null, endTime = null, pipelineId = null, jobId = null, creator = null, status = null
                )
            },
            "fetchAgentBuildPipeline" to {
                dao.fetchAgentBuildPipeline(
                    dslContext, projectId, agentId, envId,
                    limit = 10, offset = 0, startTime = null, endTime = null,
                    pipelineId = null, creator = null, status = null
                )
            },
            "fetchAgentBuildPipelineBuild" to {
                dao.fetchAgentBuildPipelineBuild(
                    dslContext, projectId, agentId, envId,
                    limit = 10, offset = 0, startTime = null, endTime = null,
                    pipelineId = null, creator = null, status = null
                )
            },
            "fetchAgentBuildPipelineJob" to {
                dao.fetchAgentBuildPipelineJob(
                    dslContext, projectId, agentId, envId,
                    limit = 10, offset = 0, startTime = null, endTime = null,
                    pipelineId = null, jobId = null, creator = null, status = null
                )
            },
            "fetchPipelineIdAndName" to {
                dao.fetchPipelineIdAndName(dslContext, projectId, agentId, envId, pipelineName = "pipeline")
            },
            "fetchJobIdAndName" to {
                dao.fetchJobIdAndName(dslContext, projectId, agentId, envId, jobName = "job")
            },
            "fetchCreator" to {
                dao.fetchCreator(dslContext, projectId, agentId, envId, creator = "user")
            },
            "countAgentBuildGroupsByPipeline" to {
                dao.countAgentBuildGroupsByPipeline(
                    dslContext, projectId, agentId, envId,
                    pipelineId = "pipeline", status = null, startTime = null, endTime = null
                )
            },
            "listAgentBuildGroupsByPipeline" to {
                dao.listAgentBuildGroupsByPipeline(
                    dslContext, projectId, agentId, envId,
                    pipelineId = "pipeline", offset = 0, limit = 10, status = null, startTime = null, endTime = null
                )
            },
            "countAgentBuildGroupsByBuild" to {
                dao.countAgentBuildGroupsByBuild(
                    dslContext, projectId, agentId, envId,
                    buildId = "build", executeCount = 1, status = null, startTime = null, endTime = null
                )
            },
            "listAgentBuildGroupsByBuild" to {
                dao.listAgentBuildGroupsByBuild(
                    dslContext, projectId, agentId, envId,
                    buildId = "build", executeCount = 1, offset = 0, limit = 10,
                    status = null, startTime = null, endTime = null
                )
            },
            "fetchAgentBuildsByBuildId" to {
                dao.fetchAgentBuildsByBuildId(dslContext, projectId, agentId, envId, buildIdList = setOf("build"))
            }
        )
    }

    @Test
    fun claimOnlyUpdatesBuildsStillQueuedForTheAgent() {
        val sqlList = mutableListOf<String>()
        val bindingsList = mutableListOf<List<Any?>>()
        val dslContext = DSL.using(
            MockConnection { context ->
                sqlList.add(context.sql())
                bindingsList.add(context.bindings().toList())
                arrayOf(MockResult(0))
            },
            SQLDialect.MYSQL
        )

        assertFalse(dao.claimQueueBuild(dslContext, 7L, "agent"))
        val sql = sqlList.single()
        // SET 子句也会出现 STATUS，只检查 WHERE 之后的条件
        val where = sql.split(Regex("\\swhere\\s", RegexOption.IGNORE_CASE)).last()
        for (column in listOf("ID", "AGENT_ID", "STATUS")) {
            assertTrue(Regex("`$column`\\s*=\\s*\\?", RegexOption.IGNORE_CASE).containsMatchIn(where), sql)
        }
        val bindings = bindingsList.single()
        assertTrue(7L in bindings && "agent" in bindings, sql)
        assertTrue(PipelineTaskStatus.QUEUE.status in bindings, sql)
    }

    @Test
    fun listAgentBuildsForcesTheStatusIndexOnlyWhenAsked() {
        val sqlList = mutableListOf<String>()
        val dslContext = DSL.using(
            MockConnection { context ->
                sqlList.add(context.sql())
                arrayOf(emptyQueryResult(context.sql()))
            },
            SQLDialect.MYSQL
        )
        val hint = Regex("force index\\s*\\(`IDX_AGENTID_STATUS_UPDATE`\\)", RegexOption.IGNORE_CASE)

        dao.listAgentBuilds(dslContext, "agent", "RUNNING", null, null, 0, 1001, forceStatusIndex = true)
        assertTrue(hint.containsMatchIn(sqlList.single()), sqlList.single())

        sqlList.clear()
        dao.listAgentBuilds(dslContext, "agent", "DONE", null, null, 0, 21)
        assertFalse(Regex("force index", RegexOption.IGNORE_CASE).containsMatchIn(sqlList.single()), sqlList.single())
    }

    @Test
    fun clearQueriesOnlyReadTheAgentIndexAndDeleteByPrimaryKey() {
        val sqlList = mutableListOf<String>()
        val bindingsList = mutableListOf<List<Any?>>()
        val dslContext = DSL.using(
            MockConnection { context ->
                sqlList.add(context.sql())
                bindingsList.add(context.bindings().toList())
                arrayOf(emptyQueryResult(context.sql()))
            },
            SQLDialect.MYSQL
        )
        val agentIndex = Regex("force index\\s*\\(`idx_agent_id`\\)", RegexOption.IGNORE_CASE)

        dao.getNextAgentId(dslContext, "a")
        dao.getClearBoundaryId(dslContext, "agent", 100000)
        dao.listClearCandidates(dslContext, "agent", 10L, 5000L, 1000)
        val (agents, boundary, candidates) = sqlList
        listOf(agents, boundary, candidates).forEach { assertTrue(agentIndex.containsMatchIn(it), it) }
        // 每次只定位下一台构建机
        val next = Regex("order by .*`AGENT_ID` asc limit \\?$", RegexOption.IGNORE_CASE)
        assertTrue(next.containsMatchIn(agents), agents)
        assertFalse(agents.contains("distinct", ignoreCase = true), agents)
        // 倒序跳过要保留的记录，取下一条的 ID
        val desc = Regex("order by .*`ID` desc limit", RegexOption.IGNORE_CASE)
        assertTrue(desc.containsMatchIn(boundary), boundary)
        assertTrue(bindingsList[1].any { (it as? Number)?.toLong() == 100000L }, boundary)
        // 只取判断需要的三列，按 ID 正序；状态和创建时间由调用方判断，SQL 里不过滤
        val columns = Regex("^select .*`ID`, .*`STATUS`, .*`CREATED_TIME` from", RegexOption.IGNORE_CASE)
        assertTrue(columns.containsMatchIn(candidates), candidates)
        assertFalse(candidates.contains("`STATUS` in"), candidates)
        assertTrue(Regex("order by .*`ID` asc", RegexOption.IGNORE_CASE).containsMatchIn(candidates), candidates)
        val numbers = bindingsList[2].mapNotNull { (it as? Number)?.toLong() }
        assertTrue(numbers.containsAll(listOf(10L, 5000L)), "$candidates $numbers")

        sqlList.clear()
        dao.deleteByIds(dslContext, listOf(1L, 2L))
        val delete = sqlList.single()
        val byIds = Regex("^delete from .* where .*`ID` in \\(\\?, \\?\\)$", RegexOption.IGNORE_CASE)
        assertTrue(byIds.containsMatchIn(delete), delete)
        assertEquals(0, dao.deleteByIds(dslContext, emptyList()))
        assertEquals(1, sqlList.size)
    }

    private fun emptyQueryResult(sql: String): MockResult {
        val dslContext = DSL.using(SQLDialect.MYSQL)
        if (sql.contains("PIPELINE_COUNT")) {
            val pipelineCount = DSL.field("PIPELINE_COUNT", Long::class.javaObjectType)
            val jobCount = DSL.field("JOB_COUNT", Long::class.javaObjectType)
            val buildCount = DSL.field("BUILD_COUNT", Long::class.javaObjectType)
            val result = dslContext.newResult(pipelineCount, jobCount, buildCount)
            result.add(dslContext.newRecord(pipelineCount, jobCount, buildCount).values(0L, 0L, 0L))
            return MockResult(1, result)
        }
        if (sql.startsWith("select count", ignoreCase = true)) {
            val count = DSL.field("count", Long::class.javaObjectType)
            val result = dslContext.newResult(count)
            result.add(dslContext.newRecord(count).values(0L))
            return MockResult(1, result)
        }
        return MockResult(0, dslContext.newResult(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD))
    }

    private data class ScopeScenario(
        val name: String,
        val envId: Long?,
        val agentId: String?,
        val projectScoped: Boolean
    )

    companion object {
        @JvmStatic
        @BeforeAll
        fun mockEnvironmentNames() {
            // AllCreateNodeEnv 初始化时会解析显示名称；测试不依赖 Spring 或国际化服务。
            mockkObject(I18nUtil)
            every { I18nUtil.getCodeLanMessage(any()) } answers { firstArg<String>() }
        }

        @JvmStatic
        @AfterAll
        fun restoreEnvironmentNames() {
            unmockkObject(I18nUtil)
        }
    }
}
