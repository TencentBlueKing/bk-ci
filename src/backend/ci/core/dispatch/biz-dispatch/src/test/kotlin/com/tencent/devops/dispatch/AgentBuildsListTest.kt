package com.tencent.devops.dispatch

import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * 构建机任务列表（OpenAPI、stream 构建机详情）：活跃任务一次取出，历史多取一行推算总数，只在必要时计数。
 */
class AgentBuildsListTest {
    private val dao = mockk<ThirdPartyAgentBuildDao>()
    private val service = ThirdPartyAgentService(
        mockk(), mockk(), mockk(), mockk(), dao, mockk(), mockk(), mockk(), mockk()
    )

    @Test
    fun activeStatusIsReadOnceWithExactCountAndInMemoryPaging() {
        every { dao.listAgentBuilds(any(), "a", "RUNNING", null, null, 0, 1001, true) } returns rows(30, 20, 10)

        val page = service.listAgentBuilds("a", "RUNNING", null, page = 2, pageSize = 2)

        assertEquals(3L, page.count)
        assertEquals(listOf("b10"), page.records.map { it.buildId })
        verify(exactly = 1) { dao.listAgentBuilds(any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { dao.countAgentBuilds(any(), any(), any(), any(), any()) }
    }

    @Test
    fun idleAgentReturnsEmptyWithoutCounting() {
        every { dao.listAgentBuilds(any(), "a", "QUEUE", "p", null, 0, 1001, true) } returns emptyList()

        val page = service.listAgentBuilds("a", "QUEUE", "p", page = 1, pageSize = 20)

        assertEquals(0L, page.count)
        assertEquals(0, page.records.size)
        verify(exactly = 0) { dao.countAgentBuilds(any(), any(), any(), any(), any()) }
    }

    @Test
    fun tooManyActiveRowsFallBackToSqlPaging() {
        every { dao.listAgentBuilds(any(), "a", "RUNNING", null, null, 0, 1001, true) } returns
            rows(*LongArray(1001) { it + 1L })
        every { dao.listAgentBuilds(any(), "a", "RUNNING", null, null, 1000, 20, true) } returns rows(5)
        every { dao.countAgentBuilds(any(), "a", "RUNNING", null, null) } returns 1500L

        // 这一页在已取出的前 1000 条内：直接用，只另外计数
        val second = service.listAgentBuilds("a", "RUNNING", null, page = 2, pageSize = 20)
        assertEquals(1500L, second.count)
        assertEquals((21L..40L).map { "b$it" }, second.records.map { it.buildId })
        verify(exactly = 1) { dao.listAgentBuilds(any(), any(), any(), any(), any(), any(), any(), any()) }

        // 超出前 1000 条：数据库分页
        val beyond = service.listAgentBuilds("a", "RUNNING", null, page = 51, pageSize = 20)
        assertEquals(1500L, beyond.count)
        assertEquals(listOf("b5"), beyond.records.map { it.buildId })
    }

    @Test
    fun historyLetsTheOptimizerChooseAndOnlyCountsFullPages() {
        // 首页不满：总数直接得出
        every { dao.listAgentBuilds(any(), "a", null, null, null, 0, 3, false) } returns rows(9, 8)
        assertEquals(2L, service.listAgentBuilds("a", null, null, page = 1, pageSize = 2).count)
        verify(exactly = 0) { dao.countAgentBuilds(any(), any(), any(), any(), any()) }

        // 满页需要计数；非空末页直接得出
        every { dao.listAgentBuilds(any(), "a", "DONE", null, null, 0, 3, false) } returns rows(9, 8, 7)
        every { dao.listAgentBuilds(any(), "a", "DONE", null, null, 2, 3, false) } returns rows(7, 6, 5)
        every { dao.listAgentBuilds(any(), "a", "DONE", null, null, 4, 3, false) } returns rows(5)
        every { dao.countAgentBuilds(any(), "a", "DONE", null, null) } returns 5L
        val first = service.listAgentBuilds("a", "DONE", null, page = 1, pageSize = 2)
        val second = service.listAgentBuilds("a", "DONE", null, page = 2, pageSize = 2)
        val last = service.listAgentBuilds("a", "DONE", null, page = 3, pageSize = 2)
        assertEquals(listOf(5L, 5L, 5L), listOf(first.count, second.count, last.count))
        assertEquals(listOf("b9", "b8"), first.records.map { it.buildId })
        assertEquals(listOf("b5"), last.records.map { it.buildId })
        verify(exactly = 2) { dao.countAgentBuilds(any(), "a", "DONE", null, null) }
    }

    @Test
    fun emptyPageBeyondTheEndStillReportsTheRealCount() {
        every { dao.listAgentBuilds(any(), "a", "unknown", null, null, 40, 21, false) } returns emptyList()
        every { dao.countAgentBuilds(any(), "a", "unknown", null, null) } returns 7L
        // 未知状态与原接口一致按 DONE 处理
        val page = service.listAgentBuilds("a", "unknown", null, page = 3, pageSize = 20)
        assertEquals(7L, page.count)
        assertEquals(0, page.records.size)
    }

    private fun rows(vararg ids: Long): List<TDispatchThirdpartyAgentBuildRecord> = ids.map { id ->
        TDispatchThirdpartyAgentBuildRecord().apply {
            this.id = id
            projectId = "project"
            agentId = "a"
            pipelineId = "p"
            pipelineName = "pipeline"
            buildId = "b$id"
            buildNum = id.toInt()
            vmSeqId = "1"
            taskName = "job"
            status = PipelineTaskStatus.RUNNING.status
            createdTime = LocalDateTime.of(2026, 11, 19, 12, 0)
            updatedTime = LocalDateTime.of(2026, 11, 19, 12, 1)
            workspace = "/data"
        }
    }
}
