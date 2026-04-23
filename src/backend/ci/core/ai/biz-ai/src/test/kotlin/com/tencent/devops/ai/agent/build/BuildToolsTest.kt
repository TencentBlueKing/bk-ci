package com.tencent.devops.ai.agent.build

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.Pipeline
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildToolsTest : BkCiAbstractTest() {

    @Test
    fun `getPipelineStatus should return raw pipeline payload`() {
        val pipelineResource = client.mockGet(ServicePipelineResource::class)
        every {
            pipelineResource.status(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                channelCode = ChannelCode.BS
            )
        } returns Result(
            Pipeline(
                projectId = "demo-project",
                pipelineId = "p-1",
                pipelineName = "demo-pipeline",
                taskCount = 7,
                buildCount = 12,
                canManualStartup = true,
                latestBuildEstimatedExecutionSeconds = 30L,
                deploymentTime = 1L,
                updateTime = 2L,
                pipelineVersion = 3,
                currentTimestamp = 4L,
                hasPermission = true,
                hasCollect = false,
                updater = "tester",
                creator = "tester",
                latestBuildStatus = BuildStatus.RUNNING,
                latestBuildNum = 15,
                latestBuildId = "b-1"
            )
        )

        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val json = JsonUtil.getObjectMapper(false).readTree(
            tools.getPipelineStatus(projectId = "demo-project", pipelineId = "p-1")
        )

        assertEquals(7, json["data"]["taskCount"].asInt())
        assertEquals(12L, json["data"]["buildCount"].asLong())
        assertEquals("demo-pipeline", json["data"]["pipelineName"].asText())
        assertFalse(json["data"].has("failedStages"))
        assertFalse(json["data"].has("stageSummary"))
    }

    @Test
    fun `getBuildLogs should return truncated content for oversized single line`() {
        val logResource = client.mockGet(ServiceLogResource::class)
        every {
            logResource.getInitLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                subTag = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true,
                reverse = false
            )
        } returns Result(
            QueryLogs(
                buildId = "b-1",
                finished = true,
                hasMore = false,
                logs = mutableListOf(logLine(1L, "x".repeat(20_100)))
            )
        )

        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val json = JsonUtil.getObjectMapper(false).readTree(
            tools.getBuildLogs(
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                tag = "e-1"
            )
        )

        assertEquals("1-1", json["lineRange"].asText())
        assertTrue(json["content"].asText().startsWith("[1] "))
        assertTrue(json["content"].asText().contains("...(日志内容过长，已截断"))
        assertTrue(json["hasMore"].asBoolean())
    }

    @Test
    fun `getBuildLogs should not force hasMore when fetch cap reached on final page`() {
        val logResource = client.mockGet(ServiceLogResource::class)
        every {
            logResource.getInitLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                subTag = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true,
                reverse = false
            )
        } returns Result(
            QueryLogs(
                buildId = "b-1",
                finished = false,
                hasMore = true,
                logs = mutableListOf(logLine(1L))
            )
        )
        every {
            logResource.getAfterLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 1L,
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true
            )
        } returns Result(queryAfterLogs(start = 1L, hasMore = true, finished = false))
        every {
            logResource.getAfterLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 2L,
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true
            )
        } returns Result(queryAfterLogs(start = 2L, hasMore = true, finished = false))
        every {
            logResource.getAfterLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 3L,
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true
            )
        } returns Result(queryAfterLogs(start = 3L, hasMore = true, finished = false))
        every {
            logResource.getAfterLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 4L,
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true
            )
        } returns Result(queryAfterLogs(start = 4L, hasMore = true, finished = false))
        every {
            logResource.getAfterLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 5L,
                debug = false,
                logType = null,
                tag = "e-1",
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = false,
                checkPermissionFlag = true
            )
        } returns Result(queryAfterLogs(start = 5L, hasMore = false, finished = true))

        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val json = JsonUtil.getObjectMapper(false).readTree(
            tools.getBuildLogs(
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                tag = "e-1"
            )
        )

        assertFalse(json["hasMore"].asBoolean())
        assertTrue(json["finished"].asBoolean())
        assertEquals(6, json["fetchedPages"].asInt())
        assertEquals("1-6", json["lineRange"].asText())
    }

    @Test
    fun `appendLogs should cap merged lines at max limit`() {
        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val target = (1L..9_999L).map { logLine(it) }.toMutableList()
        val extraLogs = (10_000L..12_000L).map { logLine(it) }

        tools.invokePrivate<Unit>("appendLogs", target, extraLogs)

        assertEquals(10_000, target.size)
        assertEquals(10_000L, target.last().lineNo)
    }

    private fun queryAfterLogs(start: Long, hasMore: Boolean, finished: Boolean): QueryLogs {
        return QueryLogs(
            buildId = "b-1",
            finished = finished,
            hasMore = hasMore,
            logs = mutableListOf(
                logLine(start),
                logLine(start + 1)
            )
        )
    }

    private fun logLine(lineNo: Long, message: String = "line-$lineNo"): LogLine {
        return LogLine(
            lineNo = lineNo,
            timestamp = lineNo,
            message = message,
            containerHashId = null,
            stepId = null
        )
    }
}
