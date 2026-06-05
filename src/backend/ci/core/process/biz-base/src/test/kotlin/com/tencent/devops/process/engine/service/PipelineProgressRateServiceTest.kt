package com.tencent.devops.process.engine.service

import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.JobHeartbeatRequest
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressDetail
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressSummary
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressTimeline
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressTimelineItem
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import java.time.LocalDateTime
import java.util.Locale
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PipelineProgressRateServiceTest {

    private val taskBuildRecordService: TaskBuildRecordService = mockk()
    private val pipelineTaskService: PipelineTaskService = mockk()
    private val pipelineRuntimeService: PipelineRuntimeService = mockk()
    private val buildRecordService: ContainerBuildRecordService = mockk()
    private val buildRecordTaskDao: BuildRecordTaskDao = mockk()
    private val pipelineBuildDao: PipelineBuildDao = mockk()
    private val dslContext: DSLContext = mockk()

    private val service = PipelineProgressRateService(
        taskBuildRecordService = taskBuildRecordService,
        pipelineTaskService = pipelineTaskService,
        pipelineRuntimeService = pipelineRuntimeService,
        buildRecordService = buildRecordService,
        buildRecordTaskDao = buildRecordTaskDao,
        pipelineBuildDao = pipelineBuildDao,
        dslContext = dslContext
    )

    @Test
    fun reportProgressRatePersistProgressDetailAndRate() {
        val taskVarSlot = slot<Map<String, Any>>()
        val progressDetail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 0.45678)
        )
        every { pipelineBuildDao.getBuildInfo(dslContext, PROJECT_ID, BUILD_ID) } returns buildInfo()
        every {
            taskBuildRecordService.updateTaskRecord(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                taskId = TASK_ID,
                executeCount = 2,
                taskVar = capture(taskVarSlot),
                buildStatus = null,
                operation = "reportProgressRate"
            )
        } just Runs

        service.reportProgressRate(
            projectId = PROJECT_ID,
            buildId = BUILD_ID,
            executeCount = 2,
            jobHeartbeatRequest = JobHeartbeatRequest(
                task2ProgressRate = mapOf(TASK_ID to 0.1),
                task2ProgressDetail = mapOf(TASK_ID to progressDetail)
            )
        )

        Assertions.assertEquals(0.4568, taskVarSlot.captured["progressRate"])
        Assertions.assertTrue(taskVarSlot.captured["progressDetail"] is BuildTaskProgressDetail)
    }

    @Test
    fun calculateStageProgressRatePrefersDetailAndFillsDefaultTitle() {
        val progressDetail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 0.66666),
            timeline = BuildTaskProgressTimeline(
                items = listOf(
                    BuildTaskProgressTimelineItem(
                        name = "prepare",
                        startTime = "2026-05-27T07:00:00Z",
                        duration = 1000
                    )
                )
            )
        )
        every { pipelineRuntimeService.getBuildInfo(PROJECT_ID, BUILD_ID) } returns buildInfo()
        every {
            buildRecordTaskDao.getLatestNormalRecords(
                dslContext = dslContext,
                projectId = PROJECT_ID,
                buildId = BUILD_ID,
                executeCount = 1,
                matrixContainerIds = emptyList(),
                stageId = STAGE_ID
            )
        } returns listOf(
            buildRecordTask("success-task", "container-1", BuildStatus.SUCCEED, mutableMapOf()),
            buildRecordTask(
                TASK_ID,
                "container-2",
                BuildStatus.RUNNING,
                mutableMapOf("progressRate" to 0.1, "progressDetail" to progressDetail)
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(buildTask())
        every {
            buildRecordService.getContainerOrderInStage(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                executeCount = 1,
                stageId = STAGE_ID,
                containerId = "container-2"
            )
        } returns 1

        val defaultLocale = Locale.getDefault()
        val result = try {
            Locale.setDefault(Locale.GERMANY)
            service.calculateStageProgressRate(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                stageId = STAGE_ID
            )
        } finally {
            Locale.setDefault(defaultLocale)
        }

        Assertions.assertEquals(0.8334, result.stageProgressRete)
        Assertions.assertEquals(0.6667, result.taskProgressList?.first()?.taskProgressRete)
        Assertions.assertEquals("编译进度", result.taskProgressList?.first()?.progressDetail?.progress?.title)
        Assertions.assertEquals("编译阶段时间线", result.taskProgressList?.first()?.progressDetail?.timeline?.title)
    }

    @Test
    fun calculateStageProgressRateReturnsCompletedTaskWithProgressDetailWhenNoRunningTask() {
        val progressDetail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 0.75)
        )
        every { pipelineRuntimeService.getBuildInfo(PROJECT_ID, BUILD_ID) } returns buildInfo()
        every {
            buildRecordTaskDao.getLatestNormalRecords(
                dslContext = dslContext,
                projectId = PROJECT_ID,
                buildId = BUILD_ID,
                executeCount = 1,
                matrixContainerIds = emptyList(),
                stageId = STAGE_ID
            )
        } returns listOf(
            buildRecordTask(
                taskId = TASK_ID,
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskVar = mutableMapOf("progressRate" to 0.75, "progressDetail" to progressDetail),
                endTime = LocalDateTime.of(2026, 4, 1, 10, 30)
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask().copy(status = BuildStatus.SUCCEED)
        )
        every {
            buildRecordService.getContainerOrderInStage(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                executeCount = 1,
                stageId = STAGE_ID,
                containerId = "container-1"
            )
        } returns 0

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        Assertions.assertEquals(1.0, result.stageProgressRete)
        Assertions.assertEquals(1, result.taskProgressList?.size)
        Assertions.assertEquals(0.75, result.taskProgressList?.first()?.taskProgressRete)
        Assertions.assertEquals(0.75, result.taskProgressList?.first()?.progressDetail?.progress?.value)
    }

    private fun buildInfo() = BuildInfo(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        version = 1,
        versionName = null,
        yamlVersion = null,
        buildNum = 1,
        trigger = "manual",
        status = BuildStatus.RUNNING,
        queueTime = 0,
        executeTime = 0,
        startUser = "user",
        triggerUser = "user",
        startTime = null,
        endTime = null,
        taskCount = 1,
        firstTaskId = TASK_ID,
        parentBuildId = null,
        parentTaskId = null,
        channelCode = ChannelCode.BS,
        buildParameters = null,
        errorInfoList = null,
        stageStatus = null,
        debug = false,
        webhookType = null,
        recommendVersion = null,
        buildNumAlias = null
    )

    private fun buildRecordTask(
        taskId: String,
        containerId: String,
        status: BuildStatus,
        taskVar: MutableMap<String, Any>,
        endTime: LocalDateTime? = null
    ) = BuildRecordTask(
        buildId = BUILD_ID,
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        resourceVersion = 1,
        stageId = STAGE_ID,
        containerId = containerId,
        taskId = taskId,
        taskSeq = 1,
        executeCount = 1,
        taskVar = taskVar,
        classType = "linuxScript",
        atomCode = "script",
        status = status.name,
        endTime = endTime,
        timestamps = emptyMap<BuildTimestampType, BuildRecordTimeStamp>()
    )

    private fun buildTask() = PipelineBuildTask(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        stageId = STAGE_ID,
        containerId = "container-2",
        containerHashId = "container-hash",
        containerType = "VM",
        taskSeq = 1,
        taskId = TASK_ID,
        taskName = "编译",
        taskType = "linuxScript",
        taskAtom = "script",
        status = BuildStatus.RUNNING,
        taskParams = mutableMapOf(),
        additionalOptions = null,
        starter = "user",
        approver = null,
        subProjectId = null,
        subBuildId = null
    )

    companion object {
        private const val PROJECT_ID = "project-1"
        private const val PIPELINE_ID = "pipeline-1"
        private const val BUILD_ID = "build-1"
        private const val TASK_ID = "task-1"
        private const val STAGE_ID = "stage-1"
    }
}
