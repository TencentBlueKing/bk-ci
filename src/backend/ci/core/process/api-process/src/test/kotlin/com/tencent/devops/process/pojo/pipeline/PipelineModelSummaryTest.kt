package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PipelineModelSummaryTest {

    @Test
    fun `toPipelineModelSummary should expose lightweight hierarchy and expand matrix containers`() {
        val versionWithModel = mockPipelineVersionWithModel()

        val summary = versionWithModel.toPipelineModelSummary(includeElements = false)

        assertEquals("demo-pipeline", summary.pipelineName)
        assertEquals(1, summary.stageCount)
        assertEquals(2, summary.containerCount)
        assertEquals(1, summary.elementCount)
        assertTrue(summary.notices[0].contains("轻量编排摘要"))
        assertNull(summary.stages[0].containers[0].elements)
        assertEquals("parent-job", summary.stages[0].containers[0].jobId)
        assertEquals("child-job", summary.stages[0].containers[1].jobId)
        assertEquals(mapOf("os" to "linux"), summary.stages[0].containers[1].matrixContext)
    }

    @Test
    fun `findPipelineNodeDetails should return single element detail with path and script preview`() {
        val versionWithModel = mockPipelineVersionWithModel()

        val matches = versionWithModel.findPipelineNodeDetails(elementId = "e-1")

        assertEquals(1, matches.size)
        val detail = matches.single()
        assertEquals("element", detail.matchedNodeType)
        assertEquals("elementId", detail.matchedBy)
        assertEquals("build-stage", detail.path.stage.stageName)
        assertEquals("child-container", detail.path.container?.containerName)
        assertEquals("failed-script", detail.path.element?.elementName)
        assertEquals("linuxScript", detail.element?.classType)
        assertEquals("linuxScript", detail.element?.atomCode)
        assertEquals("FAILED", detail.element?.status)
        assertEquals("USER", detail.element?.errorType)
        assertNotNull(detail.element?.additionalOptions)
        assertTrue(detail.element?.scriptPreview?.contains("echo hello") == true)
    }

    private fun mockPipelineVersionWithModel(): PipelineVersionWithModel {
        val element = LinuxScriptElement(
            name = "failed-script",
            id = "e-1",
            status = BuildStatus.FAILED.name,
            stepId = "step-1",
            scriptType = BuildScriptType.SHELL,
            script = "echo hello\nexit 1",
            continueNoneZero = false,
            additionalOptions = ElementAdditionalOptions(enable = true)
        ).apply {
            errorType = "USER"
            errorCode = 1001
            errorMsg = "boom"
        }
        val childContainer = NormalContainer(
            id = "2",
            containerId = "2",
            containerHashId = "hash-child",
            name = "child-container",
            jobId = "child-job",
            status = BuildStatus.FAILED.name,
            matrixContext = mapOf("os" to "linux"),
            elements = listOf(element)
        )
        val parentContainer = NormalContainer(
            id = "1",
            containerId = "1",
            containerHashId = "hash-parent",
            name = "parent-container",
            jobId = "parent-job",
            status = BuildStatus.FAILED.name,
            matrixGroupFlag = true,
            groupContainers = mutableListOf(childContainer),
            elements = emptyList()
        )
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
                            containers = listOf(parentContainer)
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
}
