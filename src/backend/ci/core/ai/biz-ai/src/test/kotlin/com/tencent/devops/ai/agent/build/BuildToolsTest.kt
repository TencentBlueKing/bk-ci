package com.tencent.devops.ai.agent.build

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.log.pojo.QueryLogsText
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineVersionResource
import com.tencent.devops.process.pojo.Pipeline
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildToolsTest : BkCiAbstractTest() {

    @Test
    fun `should clamp long running tool wait seconds to minimum`() {
        val sleepCalls = mutableListOf<Long>()
        val tools = BuildTools(client, { "tester" }) { sleepMs ->
            sleepCalls += sleepMs
        }

        val json = parseJson(
            tools.simulateLongRunningTool(waitSeconds = 1)
        )

        assertEquals(listOf(121_000L), sleepCalls)
        assertEquals("OK", json["status"].asText())
        assertEquals(1, json["requestedWaitSeconds"].asInt())
        assertEquals(121, json["actualWaitSeconds"].asInt())
    }

    @Test
    fun `should clamp long running tool wait seconds to maximum`() {
        val sleepCalls = mutableListOf<Long>()
        val tools = BuildTools(client, { "tester" }) { sleepMs ->
            sleepCalls += sleepMs
        }

        val json = parseJson(
            tools.simulateLongRunningTool(waitSeconds = 999)
        )

        assertEquals(listOf(240_000L), sleepCalls)
        assertEquals("OK", json["status"].asText())
        assertEquals(999, json["requestedWaitSeconds"].asInt())
        assertEquals(240, json["actualWaitSeconds"].asInt())
    }

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
    fun `getPipelineModelSummary should return lightweight hierarchy`() {
        val versionResource = client.mockGet(ServicePipelineVersionResource::class)
        every {
            versionResource.getVersionModel(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                version = null
            )
        } returns Result(mockPipelineVersionWithModel())

        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val json = JsonUtil.getObjectMapper(false).readTree(
            tools.getPipelineModelSummary(
                projectId = "demo-project",
                pipelineId = "p-1",
                includeElements = false
            )
        )

        assertEquals("demo-pipeline", json["data"]["pipelineName"].asText())
        assertEquals(1, json["data"]["stageCount"].asInt())
        assertEquals(1, json["data"]["containerCount"].asInt())
        assertEquals(1, json["data"]["elementCount"].asInt())
        assertFalse(json["data"]["stages"][0]["containers"][0].has("element"))
        assertNull(json["data"]["stages"][0]["containers"][0]["elements"])
    }

    @Test
    fun `getPipelineNodeDetail should report ambiguity when jobId matches multiple containers`() {
        val versionResource = client.mockGet(ServicePipelineVersionResource::class)
        every {
            versionResource.getVersionModel(
                userId = "tester",
                projectId = "demo-project",
                pipelineId = "p-1",
                version = null
            )
        } returns Result(mockPipelineVersionWithModel(duplicateJobId = true))

        val tools = BuildTools(client = client, userIdSupplier = { "tester" })
        val json = JsonUtil.getObjectMapper(false).readTree(
            tools.getPipelineNodeDetail(
                projectId = "demo-project",
                pipelineId = "p-1",
                jobId = "dup-job"
            )
        )

        assertEquals(2, json["matchedCount"].asInt())
        assertEquals("jobId", json["matchedBy"].asText())
        assertTrue(json["message"].asText().contains("多个编排节点"))
        assertEquals("job-a", json["candidates"][0]["container"]["containerName"].asText())
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

    private fun mockPipelineVersionWithModel(duplicateJobId: Boolean = false): PipelineVersionWithModel {
        val element = LinuxScriptElement(
            name = "failed-script",
            id = "e-1",
            status = BuildStatus.FAILED.name,
            stepId = "step-1",
            scriptType = BuildScriptType.SHELL,
            script = "echo hello\nexit 1",
            continueNoneZero = false,
            additionalOptions = ElementAdditionalOptions(enable = true)
        )
        val firstContainer = NormalContainer(
            id = "c-1",
            containerId = "c-1",
            containerHashId = "hash-1",
            name = "job-a",
            jobId = if (duplicateJobId) "dup-job" else "job-a",
            status = BuildStatus.FAILED.name,
            elements = listOf(element)
        )
        val secondContainer = if (duplicateJobId) {
            NormalContainer(
                id = "c-2",
                containerId = "c-2",
                containerHashId = "hash-2",
                name = "job-b",
                jobId = "dup-job",
                status = BuildStatus.SUCCEED.name,
                elements = emptyList()
            )
        } else {
            null
        }
        return PipelineVersionWithModel(
            version = 3,
            versionName = "v3",
            baseVersion = 2,
            baseVersionName = "v2",
            modelAndSetting = PipelineModelAndSetting(
                model = Model(
                    name = "demo-pipeline",
                    desc = null,
                    stages = listOf(
                        Stage(
                            id = "stage-1",
                            name = "build-stage",
                            stageIdForUser = "build",
                            status = BuildStatus.FAILED.name,
                            containers = listOfNotNull(firstContainer, secondContainer)
                        )
                    ),
                    pipelineId = "p-1"
                ),
                setting = PipelineSetting(pipelineAsCodeSettings = null)
            ),
            yamlPreview = null,
            canDebug = true,
            description = null,
            yamlSupported = true,
            yamlInvalidMsg = null,
            updater = "tester",
            updateTime = 1L,
            latestVersion = 5
        )
    }

    private fun parseJson(value: String) = JsonUtil.getObjectMapper(false).readTree(value)
}
