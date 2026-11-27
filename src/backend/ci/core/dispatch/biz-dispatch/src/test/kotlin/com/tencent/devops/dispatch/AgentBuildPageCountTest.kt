package com.tencent.devops.dispatch

import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildView
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineReq
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineSearchReq
import com.tencent.devops.dispatch.service.ThirdPartyAgentBuildService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 节点/环境构建记录页面：滚动加载第二页起、明细翻页时不再计数（withCount = false）。
 */
class AgentBuildPageCountTest {
    private val dao = mockk<ThirdPartyAgentBuildDao>()
    private val service = ThirdPartyAgentBuildService(mockk(), mockk(), dao)

    init {
        every {
            dao.fetchAgentBuildPipelineJob(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns emptyList()
        every {
            dao.countAgentBuildPipelineJob(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Triple(3L, 5L, 8L)
        every {
            dao.listAgentBuildGroupsByPipeline(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns emptyList()
        every {
            dao.countAgentBuildGroupsByPipeline(any(), any(), any(), any(), any(), any(), any(), any())
        } returns 4L
    }

    @Test
    fun listCountsOnlyWhenAsked() {
        val first = service.fetchBuildPipelineView("u", "p", null, listReq(withCount = null))
        // Job 视图的列表总数即 Job 数
        assertEquals(listOf(3L, 5L, 8L), listOf(first.pipelineCount, first.jobCount, first.buildCount))
        assertEquals(5L, first.result.count)

        val next = service.fetchBuildPipelineView("u", "p", null, listReq(withCount = false))
        assertEquals(listOf(-1L, -1L, -1L), listOf(next.pipelineCount, next.jobCount, next.buildCount))
        assertEquals(-1L, next.result.count)
        assertEquals(-1, next.result.totalPages)
        verify(exactly = 1) {
            dao.countAgentBuildPipelineJob(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun detailPagingSkipsTheCountWhenAsked() {
        val counted = service.fetchAgentBuildsByPipeline("u", "p", null, "pipe", searchReq(withCount = true))
        assertEquals(4L, counted.count)
        val skipped = service.fetchAgentBuildsByPipeline("u", "p", null, "pipe", searchReq(withCount = false))
        assertEquals(-1L, skipped.count)
        assertEquals(-1, skipped.totalPages)
        verify(exactly = 1) {
            dao.countAgentBuildGroupsByPipeline(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun listReq(withCount: Boolean?) = TPAPipelineReq(
        agentId = "a", envId = null, page = 2, pageSize = 20, startTime = null, endTime = null, pipelineId = null,
        jobId = null, creator = null, taskStatusList = null, view = TPAPipelineBuildView.JOB, withCount = withCount
    )

    private fun searchReq(withCount: Boolean?) = TPAPipelineSearchReq(
        agentId = "a", envId = null, page = 2, pageSize = 20, startTime = null, endTime = null, pipelineId = "pipe",
        jobId = null, creator = null, taskStatusList = null, withCount = withCount
    )
}
