package com.tencent.devops.dispatch

import com.tencent.devops.dispatch.cron.ThirdPartyAgentBuildClearJob
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ThirdPartyAgentBuildClearJobTest {
    private val dao = mockk<ThirdPartyAgentBuildDao>()
    private val job = job()
    private val old = LocalDateTime.now().minusDays(60)
    private val recent = LocalDateTime.now().minusDays(10)
    private val done = PipelineTaskStatus.DONE.status

    @Test
    fun agentsWithinTheLimitAreSkipped() {
        every { dao.getClearBoundaryId(any(), "small", 100000) } returns null

        assertEquals(0L, job.clearAgent("small"))
        verify(exactly = 0) { dao.listClearCandidates(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { dao.deleteByIds(any(), any()) }
    }

    @Test
    fun oldFinishedRowsAreDeletedInIdOrderBatchByBatch() {
        every { dao.getClearBoundaryId(any(), "big", 100000) } returns 5000L
        every { dao.listClearCandidates(any(), "big", 0L, 5000L, 500) } returns
            (1L..500L).map { row(it, done, old) }
        every { dao.listClearCandidates(any(), "big", 500L, 5000L, 500) } returns
            (501L..700L).map { row(it, done, old) }
        every { dao.deleteByIds(any(), any()) } answers { secondArg<Collection<Long>>().size }

        assertEquals(700L, job.clearAgent("big"))
        // 每批从上一批的最大 ID 之后继续，不满一批即结束
        verify(exactly = 1) { dao.deleteByIds(any(), (1L..500L).toList()) }
        verify(exactly = 1) { dao.deleteByIds(any(), (501L..700L).toList()) }
        verify(exactly = 2) { dao.listClearCandidates(any(), any(), any(), any(), any()) }
    }

    @Test
    fun recentAndUnfinishedRowsAreKeptAndScanningStopsWhenTheBatchEndsWithARecentRow() {
        every { dao.getClearBoundaryId(any(), "big", 100000) } returns 5000L
        // 满一批，最后一条在 14 天内：只删其中已结束的旧记录，不再取下一批
        val rows = listOf(
            row(1, done, old),
            row(2, PipelineTaskStatus.QUEUE.status, old),
            row(3, PipelineTaskStatus.FAILURE.status, old)
        ) + (4L..500L).map { row(it, done, recent) }
        every { dao.listClearCandidates(any(), "big", 0L, 5000L, 500) } returns rows
        every { dao.deleteByIds(any(), listOf(1L, 3L)) } returns 2

        assertEquals(2L, job.clearAgent("big"))
        verify(exactly = 1) { dao.listClearCandidates(any(), any(), any(), any(), any()) }
    }

    @Test
    fun aSingleRowWithAWrongCreatedTimeDoesNotBlockTheOlderRowsAfterIt() {
        every { dao.getClearBoundaryId(any(), "big", 100000) } returns 5000L
        // 第 2 条创建时间异常偏新：跳过它，后面的旧记录照常清理
        val first = listOf(row(1, done, old), row(2, done, recent)) + (3L..500L).map { row(it, done, old) }
        every { dao.listClearCandidates(any(), "big", 0L, 5000L, 500) } returns first
        every { dao.listClearCandidates(any(), "big", 500L, 5000L, 500) } returns listOf(row(501, done, old))
        every { dao.deleteByIds(any(), any()) } answers { secondArg<Collection<Long>>().size }

        assertEquals(500L, job.clearAgent("big"))
        verify(exactly = 1) { dao.deleteByIds(any(), listOf(1L) + (3L..500L).toList()) }
    }

    @Test
    fun allAgentsAreSteppedOneByOneAndOneFailureDoesNotStopTheRest() {
        every { dao.getNextAgentId(any(), "") } returns "a"
        every { dao.getNextAgentId(any(), "a") } returns "b"
        every { dao.getNextAgentId(any(), "b") } returns "c"
        every { dao.getNextAgentId(any(), "c") } returns null
        every { dao.getClearBoundaryId(any(), "a", 100000) } throws IllegalStateException("db error")
        every { dao.getClearBoundaryId(any(), "b", 100000) } returns null
        every { dao.getClearBoundaryId(any(), "c", 100000) } returns 10L
        every { dao.listClearCandidates(any(), "c", 0L, 10L, 500) } returns listOf(row(3, done, old), row(7, done, old))
        every { dao.deleteByIds(any(), listOf(3L, 7L)) } returns 2

        assertEquals(2L, job.clearAll())
        verify(exactly = 4) { dao.getNextAgentId(any(), any()) }
        verify(exactly = 3) { dao.getClearBoundaryId(any(), any(), any()) }
    }

    @Test
    fun runStopsWhenTheTimeLimitIsReached() {
        assertEquals(0L, job(maxMinutes = 0).clearAll())
        verify(exactly = 0) { dao.getNextAgentId(any(), any()) }
    }

    @Test
    fun nonPositiveKeepNumDisablesTheJob() {
        job(keepNum = 0).clear()
        verify(exactly = 0) { dao.getNextAgentId(any(), any()) }
    }

    private fun job(keepNum: Int = 100000, maxMinutes: Long = 240) = ThirdPartyAgentBuildClearJob(
        dslContext = mockk(),
        redisOperation = mockk(),
        thirdPartyAgentBuildDao = dao,
        keepNum = keepNum,
        keepDays = 14,
        batchSize = 500,
        intervalMills = 0,
        maxMinutes = maxMinutes
    )

    private fun row(id: Long, status: Int, createdTime: LocalDateTime) = TDispatchThirdpartyAgentBuildRecord().apply {
        this.id = id
        this.status = status
        this.createdTime = createdTime
    }
}
