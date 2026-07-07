package com.tencent.devops.ai.agent.build

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.log.pojo.QueryLogsText
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
            logResource.getLatestLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                debug = false,
                logType = null,
                size = 500,
                tag = "e-1",
                subTag = null,
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = null,
                checkPermissionFlag = true
            )
        } returns Result(
            QueryLogsText(
                buildId = "b-1",
                finished = true,
                hasMore = false,
                startLineNo = 1L,
                endLineNo = 1L,
                content = "x".repeat(20_100)
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
        assertTrue(json["content"].asText().startsWith("x".repeat(512)))
        assertTrue(json["content"].asText().contains("...(日志内容过长，已截断"))
        assertFalse(json["hasMore"].asBoolean())
    }

    @Test
    fun `getBuildLogs should preserve latest flags and expose next actions`() {
        val logResource = client.mockGet(ServiceLogResource::class)
        every {
            logResource.getLatestLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                debug = false,
                logType = null,
                size = 500,
                tag = "e-1",
                subTag = null,
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = null,
                checkPermissionFlag = true
            )
        } returns Result(
            QueryLogsText(
                buildId = "b-1",
                finished = true,
                hasMore = true,
                startLineNo = 101L,
                endLineNo = 120L,
                content = "line-101\nline-120"
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

        assertTrue(json["hasMore"].asBoolean())
        assertTrue(json["finished"].asBoolean())
        assertEquals(1, json["fetchedPages"].asInt())
        assertEquals("101-120", json["lineRange"].asText())
        assertTrue(json["nextActions"][0].asText().contains("获取指定行号范围构建日志"))
    }

    @Test
    fun `getMiddleBuildLogs should preserve requested range semantics`() {
        val logResource = client.mockGet(ServiceLogResource::class)
        every {
            logResource.getMiddleLogs(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 1L,
                end = 6L,
                debug = false,
                logType = null,
                tag = "e-1",
                subTag = null,
                containerHashId = null,
                executeCount = null,
                jobId = null,
                stepId = null,
                archiveFlag = null,
                checkPermissionFlag = true
            )
        } returns Result(
            QueryLogsText(
                buildId = "b-1",
                finished = false,
                hasMore = true,
                startLineNo = 1L,
                endLineNo = 6L,
                content = "line-1\nline-2\nline-3\nline-4\nline-5\nline-6"
            )
        )

        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val json = JsonUtil.getObjectMapper(false).readTree(
            tools.getMiddleBuildLogs(
                projectId = "demo-project",
                pipelineId = "p-1",
                buildId = "b-1",
                start = 1L,
                end = 6L,
                tag = "e-1"
            )
        )

        assertEquals("1-6", json["lineRange"].asText())
        assertTrue(json["hasMore"].asBoolean())
        assertTrue(json["notices"][0].asText().contains("middle"))
    }
}
