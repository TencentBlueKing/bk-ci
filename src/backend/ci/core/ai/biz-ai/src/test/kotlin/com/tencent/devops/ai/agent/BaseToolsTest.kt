package com.tencent.devops.ai.agent

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.process.pojo.pipeline.BuildDetailContainerSimple
import com.tencent.devops.process.pojo.pipeline.BuildDetailElementSimple
import com.tencent.devops.process.pojo.pipeline.BuildDetailSimple
import com.tencent.devops.process.pojo.pipeline.BuildDetailStageSimple
import io.swagger.v3.oas.annotations.media.Schema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BaseToolsTest : BkCiAbstractTest() {

    private val tools = TestTools()

    @Test
    fun `should wrap schema fields for single object`() {
        val json = parseJson(
            tools.stringify(
                SchemaPayload(
                    name = "pipeline",
                    detail = "running"
                )
            )
        )

        assertEquals("名称", json["_fields"]["name"].asText())
        assertEquals("详情", json["_fields"]["detail"].asText())
        assertEquals("pipeline", json["data"]["name"].asText())
        assertEquals("running", json["data"]["detail"].asText())
        assertFalse(json.has("_meta"))
    }

    @Test
    fun `should wrap schema fields for collection payload`() {
        val json = parseJson(
            tools.stringify(
                listOf(
                    SchemaPayload(name = "first", detail = "ok"),
                    SchemaPayload(name = "second", detail = "done")
                )
            )
        )

        assertEquals("名称", json["_fields"]["name"].asText())
        assertEquals(2, json["data"].size())
        assertEquals("first", json["data"][0]["name"].asText())
        assertFalse(json.has("_meta"))
    }

    @Test
    fun `should expose schema fields for build detail simple payload`() {
        val json = parseJson(
            tools.stringify(
                BuildDetailSimple(
                    id = "b-1",
                    pipelineId = "p-1",
                    pipelineName = "demo-pipeline",
                    userId = "tester",
                    triggerUser = "tester",
                    trigger = "MANUAL",
                    startTime = 1L,
                    endTime = 2L,
                    status = "FAILED",
                    currentTimestamp = 3L,
                    buildNum = 4,
                    cancelUserId = null,
                    curVersion = 5,
                    latestVersion = 6,
                    latestBuildNum = 7,
                    lastModifyUser = "tester",
                    executeTime = 8L,
                    triggerReviewers = listOf("reviewer"),
                    debug = false,
                    totalStageCount = 1,
                    totalContainerCount = 1,
                    totalElementCount = 1,
                    failedElementCount = 1,
                    activeElementCount = 0,
                    stageSummary = listOf("stage-1 [FAILED]"),
                    stages = listOf(
                        BuildDetailStageSimple(
                            stageId = "s-1",
                            stageName = "stage-1",
                            stageIdForUser = "1",
                            status = "FAILED",
                            finalStage = false,
                            containerCount = 1,
                            elementCount = 1,
                            failedElementCount = 1,
                            activeElementCount = 0
                        )
                    ),
                    containers = listOf(
                        BuildDetailContainerSimple(
                            stageId = "s-1",
                            stageName = "stage-1",
                            containerId = "c-1",
                            containerName = "job-1",
                            status = "FAILED",
                            containerHashId = "hash-1",
                            jobId = "job-1",
                            startVmStatus = "FAILED",
                            matrixGroupFlag = false,
                            elementCount = 1,
                            failedElementCount = 1,
                            activeElementCount = 0
                        )
                    ),
                    failedElements = listOf(
                        BuildDetailElementSimple(
                            stageId = "s-1",
                            stageName = "stage-1",
                            containerId = "c-1",
                            containerName = "job-1",
                            containerHashId = "hash-1",
                            jobId = "job-1",
                            elementId = "e-1",
                            elementName = "plugin-1",
                            stepId = "step-1",
                            status = "FAILED",
                            classType = "linuxPaasCodeCCScript",
                            atomCode = "CodeCCCheckAtom",
                            enabled = true,
                            errorType = "USER",
                            errorCode = 1001,
                            errorMsg = "failed"
                        )
                    ),
                    activeElements = emptyList(),
                    elementPreview = emptyList(),
                    notices = listOf("notice")
                )
            )
        )

        assertEquals("构建ID", json["_fields"]["id"].asText())
        assertEquals("失败插件列表", json["_fields"]["failedElements"].asText())
        assertEquals("b-1", json["data"]["id"].asText())
    }

    @Test
    fun `should truncate plain text when input is too large`() {
        val input = "a".repeat(48_100)

        val result = tools.stringify(input)

        assertEquals(48_000, result.length)
        assertTrue(result.startsWith("a".repeat(47_992)))
        assertTrue(result.endsWith("...(已截断)"))
    }

    @Test
    fun `should keep model field when serializing payload`() {
        val json = parseJson(
            tools.stringify(
                mapOf(
                    "id" to "pipeline-1",
                    "model" to mapOf("stage" to "big-payload")
                )
            )
        )

        assertEquals("pipeline-1", json["id"].asText())
        assertEquals("big-payload", json["model"]["stage"].asText())
        assertFalse(json.has("_meta"))
    }

    @Test
    fun `should keep nested content when depth exceeds previous limit`() {
        val json = parseJson(
            tools.stringify(
                mapOf(
                    "level1" to mapOf(
                        "level2" to mapOf(
                            "level3" to mapOf(
                                "level4" to "boom"
                            )
                        )
                    )
                )
            )
        )

        assertEquals("boom", json["level1"]["level2"]["level3"]["level4"].asText())
        assertFalse(json.has("_meta"))
    }

    @Test
    fun `should keep full arrays without global array truncation`() {
        val json = parseJson(
            tools.stringify(
                mapOf(
                    "items" to (1..25).toList()
                )
            )
        )

        assertEquals(25, json["items"].size())
        assertFalse(json.has("_meta"))
    }

    @Test
    fun `should keep long string values for troubleshooting`() {
        val longText = "b".repeat(2_100)
        val json = parseJson(
            tools.stringify(
                mapOf(
                    "content" to longText
                )
            )
        )

        assertEquals(longText, json["content"].asText())
        assertFalse(json.has("_meta"))
    }

    @Test
    fun `should truncate oversized object payload directly`() {
        val payload = (1..2_000).associate { index ->
            "field$index" to "value".repeat(20)
        }

        val result = tools.stringify(payload)

        assertEquals(48_000, result.length)
        assertTrue(result.endsWith("...(已截断)"))
    }

    private fun parseJson(value: String) = JsonUtil.getObjectMapper(false).readTree(value)

    private class TestTools : BaseTools(client, { "tester" }) {
        override val logger: Logger = LoggerFactory.getLogger(TestTools::class.java)

        fun stringify(value: Any): String = toJson(value)
    }

    private data class SchemaPayload(
        @get:Schema(title = "名称")
        val name: String,
        @get:Schema(title = "详情")
        val detail: String
    )
}
