package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.utils.TestTool
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DependOnStageCmdTest {

    private val pipelineContainerService: PipelineContainerService = mockk()
    private val buildLogPrinter = BuildLogPrinter(mockk<Client>(), mockk())
    private val cmd = DependOnStageCmd(
        pipelineContainerService = pipelineContainerService,
        buildLogPrinter = buildLogPrinter
    )

    @Test
    fun `canExecute only when stage ready to run`() {
        val runningContext = genStageContext(stageStatus = BuildStatus.RUNNING)
        Assertions.assertFalse(cmd.canExecute(runningContext))

        val readyContext = genStageContext(stageStatus = BuildStatus.QUEUE)
        Assertions.assertTrue(cmd.canExecute(readyContext))
    }

    @Test
    fun `execute resolve dependOn with runtime variable and persist`() {
        justRun { pipelineContainerService.batchUpdate(any(), any()) }
        justRun {
            buildLogPrinter.addLine(
                buildId = any(),
                message = any(),
                tag = any(),
                containerHashId = any(),
                executeCount = any(),
                jobId = any(),
                stepId = any()
            )
        }
        val jobA = TestTool.genVmBuildContainer(
            vmSeqId = 1,
            status = BuildStatus.QUEUE
        ).copy(jobId = "job_a")
        val jobBOption = JobControlOption(
            enable = true,
            dependOnType = DependOnType.NAME,
            dependOnName = "\${DEP_JOB}"
        )
        val jobB = TestTool.genVmBuildContainer(
            vmSeqId = 2,
            status = BuildStatus.QUEUE,
            jobControlOption = jobBOption
        ).copy(jobId = "job_b")
        val context = genStageContext(
            stageStatus = BuildStatus.QUEUE,
            containers = listOf(jobA, jobB),
            variables = mapOf("DEP_JOB" to "job_a")
        )

        cmd.execute(context)

        Assertions.assertEquals(mapOf("1" to "job_a"), jobBOption.dependOnContainerId2JobIds)
        Assertions.assertEquals(CmdFlowState.CONTINUE, context.cmdFlowState)
        verify(exactly = 1) { pipelineContainerService.batchUpdate(any(), any()) }
    }

    @Test
    fun `execute fail stage when runtime dependOn cycle`() {
        justRun {
            buildLogPrinter.addErrorLine(
                buildId = any(),
                message = any(),
                tag = any(),
                jobId = any(),
                executeCount = any(),
                stepId = any()
            )
        }
        val jobAOption = JobControlOption(
            enable = true,
            dependOnType = DependOnType.NAME,
            dependOnName = "\${NEXT}"
        )
        val jobBOption = JobControlOption(
            enable = true,
            dependOnType = DependOnType.NAME,
            dependOnName = "\${PREV}"
        )
        val jobA = TestTool.genVmBuildContainer(
            vmSeqId = 1,
            status = BuildStatus.QUEUE,
            jobControlOption = jobAOption
        ).copy(jobId = "job_a")
        val jobB = TestTool.genVmBuildContainer(
            vmSeqId = 2,
            status = BuildStatus.QUEUE,
            jobControlOption = jobBOption
        ).copy(jobId = "job_b")
        val context = genStageContext(
            stageStatus = BuildStatus.QUEUE,
            containers = listOf(jobA, jobB),
            variables = mapOf("NEXT" to "job_b", "PREV" to "job_a")
        )

        cmd.execute(context)

        Assertions.assertEquals(BuildStatus.FAILED, context.buildStatus)
        Assertions.assertEquals(CmdFlowState.FINALLY, context.cmdFlowState)
        verify(exactly = 0) { pipelineContainerService.batchUpdate(any(), any()) }
    }

    private fun genStageContext(
        stageStatus: BuildStatus,
        containers: List<PipelineBuildContainer> = listOf(
            TestTool.genVmBuildContainer(status = stageStatus)
        ),
        variables: Map<String, String> = emptyMap()
    ): StageContext {
        val stage = PipelineBuildStage(
            projectId = TestTool.projectId,
            pipelineId = TestTool.pipelineId,
            buildId = TestTool.buildId,
            stageId = TestTool.stageId,
            seq = 1,
            status = stageStatus,
            controlOption = null
        )
        return StageContext(
            stage = stage,
            containers = containers,
            buildStatus = stageStatus,
            event = PipelineBuildStageEvent(
                source = "test",
                projectId = TestTool.projectId,
                pipelineId = TestTool.pipelineId,
                userId = "user",
                buildId = TestTool.buildId,
                stageId = TestTool.stageId,
                actionType = ActionType.START
            ),
            latestSummary = "init",
            watcher = Watcher("DependOnStageCmdTest"),
            variables = variables,
            debug = false
        )
    }
}
