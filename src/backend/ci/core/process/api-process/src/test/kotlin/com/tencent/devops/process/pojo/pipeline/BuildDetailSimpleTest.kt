package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildDetailSimpleTest {

    @Test
    fun `toBuildDetailSimple should expose failed elements with context and full element payload`() {
        val failedElement = LinuxScriptElement(
            name = "failed-script",
            id = "e-1",
            status = BuildStatus.FAILED.name,
            stepId = "step-1",
            scriptType = BuildScriptType.SHELL,
            script = "exit 1",
            continueNoneZero = false,
            additionalOptions = ElementAdditionalOptions(enable = true)
        ).apply {
            errorType = "USER"
            errorCode = 1001
            errorMsg = "boom"
        }
        val successElement = LinuxScriptElement(
            name = "success-script",
            id = "e-2",
            status = BuildStatus.SUCCEED.name,
            stepId = "step-2",
            scriptType = BuildScriptType.SHELL,
            script = "echo ok",
            continueNoneZero = false,
            additionalOptions = ElementAdditionalOptions(enable = true)
        )
        val detail = ModelDetail(
            id = "b-1",
            pipelineId = "p-1",
            pipelineName = "demo-pipeline",
            userId = "tester",
            triggerUser = "tester",
            trigger = "MANUAL",
            startTime = 1L,
            endTime = 2L,
            status = BuildStatus.FAILED.name,
            model = Model(
                name = "demo-pipeline",
                desc = null,
                stages = listOf(
                    Stage(
                        id = "s-1",
                        name = "build-stage",
                        stageIdForUser = "1",
                        status = BuildStatus.FAILED.name,
                        containers = listOf(
                            NormalContainer(
                                id = "c-1",
                                containerId = "c-1",
                                containerHashId = "hash-1",
                                name = "job-1",
                                jobId = "job-1",
                                status = BuildStatus.FAILED.name,
                                elements = listOf(failedElement, successElement)
                            )
                        )
                    )
                )
            ),
            currentTimestamp = 3L,
            buildNum = 4,
            cancelUserId = null,
            curVersion = 1,
            latestVersion = 1,
            latestBuildNum = 4,
            lastModifyUser = "tester",
            executeTime = 10L,
            triggerReviewers = null,
            debug = false
        )

        val json = JsonUtil.getObjectMapper(false).valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            detail.toBuildDetailSimple()
        )

        assertTrue(json.has("failedElements"))
        assertFalse(json.has("stages"))
        assertFalse(json.has("containers"))
        assertFalse(json.has("activeElements"))
        assertFalse(json.has("elementPreview"))
        assertFalse(json.has("failedStages"))
        assertEquals(
            "build-stage [FAILED] containers=1, elements=2, failed=1, active=0, failedElementIds=e-1",
            json["stageSummary"][0].asText()
        )
        assertEquals(1, json["failedElements"].size())
        assertEquals("s-1", json["failedElements"][0]["stageId"].asText())
        assertEquals("job-1", json["failedElements"][0]["jobId"].asText())
        assertEquals("build-stage", json["failedElements"][0]["stageName"].asText())
        assertEquals("exit 1", json["failedElements"][0]["element"]["script"].asText())
    }
}
