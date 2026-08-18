package com.tencent.devops.process.engine.service

import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.JobHeartbeatRequest
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressDetail
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressSummary
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskSubtaskProgressGroup
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskSubtaskProgressItem
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressStatus
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressTimeline
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressTimelineItem
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
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
            progress = BuildTaskProgressSummary(title = "", value = 0.66666),
            subtasks = BuildTaskSubtaskProgressGroup(
                title = "",
                items = listOf(
                    BuildTaskSubtaskProgressItem(
                        name = "prepare",
                        progress = 0.5,
                        status = BuildTaskProgressStatus.RUNNING
                    )
                )
            ),
            timeline = BuildTaskProgressTimeline(
                title = "",
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
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(
                taskId = "success-task",
                taskName = "已完成步骤",
                containerId = "container-1",
                status = BuildStatus.SUCCEED
            ),
            buildTask()
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
        Assertions.assertEquals(1, result.taskProgressList?.size)
        val runningTaskProgress = result.taskProgressList?.first { it.taskName == "编译" }
        Assertions.assertEquals(0.6667, runningTaskProgress?.taskProgressRete)
        Assertions.assertEquals("编译", runningTaskProgress?.progressDetail?.progress?.title)
        Assertions.assertEquals("子任务进度", runningTaskProgress?.progressDetail?.subtasks?.title)
        Assertions.assertEquals("编译", runningTaskProgress?.progressDetail?.timeline?.title)
    }

    @Test
    fun calculateStageProgressRateIncludesCompletedTasksWithProgressDetailWhenRunningTaskExists() {
        val completedProgressDetail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 1.0)
        )
        val runningProgressDetail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 0.3758)
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
                taskId = "task-completed",
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskVar = mutableMapOf("progressRate" to 1.0, "progressDetail" to completedProgressDetail),
                endTime = LocalDateTime.of(2026, 4, 1, 10, 30)
            ),
            buildRecordTask(
                taskId = TASK_ID,
                containerId = "container-2",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.1, "progressDetail" to runningProgressDetail)
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(taskId = "task-completed", taskName = "已完成步骤", containerId = "container-1"),
            buildTask()
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

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        Assertions.assertEquals(0.6879, result.stageProgressRete)
        Assertions.assertEquals(2, result.taskProgressList?.size)
        Assertions.assertEquals(listOf("已完成步骤", "编译"), result.taskProgressList?.map { it.taskName })
        Assertions.assertEquals(
            listOf(1.0, 0.3758),
            result.taskProgressList?.map { it.taskProgressRete }
        )
    }

    @Test
    fun calculateStageProgressRateSkipsCompletedTasksWithoutProgressDetail() {
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
                taskId = "task-completed",
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskVar = mutableMapOf(),
                endTime = LocalDateTime.of(2026, 4, 1, 10, 30)
            ),
            buildRecordTask(
                taskId = TASK_ID,
                containerId = "container-2",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.3758)
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(taskId = "task-completed", taskName = "已完成步骤", containerId = "container-1"),
            buildTask()
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

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        Assertions.assertEquals(0.6879, result.stageProgressRete)
        Assertions.assertEquals(1, result.taskProgressList?.size)
        Assertions.assertEquals(listOf("编译"), result.taskProgressList?.map { it.taskName })
        Assertions.assertEquals(listOf(0.3758), result.taskProgressList?.map { it.taskProgressRete })
    }

    @Test
    fun calculateStageProgressRateIncludesFailedTasksAsCompleted() {
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
                taskId = "task-failed",
                containerId = "container-1",
                status = BuildStatus.FAILED,
                taskVar = mutableMapOf(),
                endTime = LocalDateTime.of(2026, 4, 1, 10, 30)
            ),
            buildRecordTask(
                taskId = TASK_ID,
                containerId = "container-2",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.3758)
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(
                taskId = "task-failed",
                taskName = "失败步骤",
                containerId = "container-1",
                status = BuildStatus.FAILED
            ),
            buildTask()
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

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        Assertions.assertEquals(0.6879, result.stageProgressRete)
        Assertions.assertEquals(1, result.taskProgressList?.size)
        Assertions.assertEquals(listOf("编译"), result.taskProgressList?.map { it.taskName })
        Assertions.assertEquals(listOf(0.3758), result.taskProgressList?.map { it.taskProgressRete })
    }

    @Test
    fun calculateStageProgressRateSkipsRunningTasksWithoutProgressDetail() {
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
                taskId = "task-running-without-progress",
                containerId = "container-1",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf(),
                taskSeq = 1
            ),
            buildRecordTask(
                taskId = TASK_ID,
                containerId = "container-2",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.3758),
                taskSeq = 1
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(
                taskId = "task-running-without-progress",
                taskName = "未上报步骤",
                containerId = "container-1",
                status = BuildStatus.RUNNING
            ),
            buildTask()
        )
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

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        Assertions.assertEquals(0.1879, result.stageProgressRete)
        Assertions.assertEquals(1, result.taskProgressList?.size)
        Assertions.assertEquals(listOf("编译"), result.taskProgressList?.map { it.taskName })
        Assertions.assertEquals(listOf(0.3758), result.taskProgressList?.map { it.taskProgressRete })
    }

    @Test
    fun calculateStageProgressRateSortsTasksByJobAndTaskSeq() {
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
                taskId = "task-2",
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskVar = mutableMapOf("progressRate" to 0.8),
                taskSeq = 3
            ),
            buildRecordTask(
                taskId = "task-1",
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskVar = mutableMapOf("progressRate" to 0.4),
                taskSeq = 2
            ),
            buildRecordTask(
                taskId = "task-3",
                containerId = "container-2",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.5),
                taskSeq = 1
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(
                taskId = "task-1",
                taskName = "步骤1",
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskSeq = 1
            ),
            buildTask(
                taskId = "task-2",
                taskName = "步骤2",
                containerId = "container-1",
                status = BuildStatus.SUCCEED,
                taskSeq = 2
            ),
            buildTask(
                taskId = "task-3",
                taskName = "步骤3",
                containerId = "container-2",
                taskSeq = 1
            )
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
        every {
            buildRecordService.getLatestRecord(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                containerId = "container-1",
                executeCount = 1
            )
        } returns buildRecordContainer(containerId = "container-1", startVMTaskSeq = 1)
        every {
            buildRecordService.getLatestRecord(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                containerId = "container-2",
                executeCount = 1
            )
        } returns buildRecordContainer(containerId = "container-2", startVMTaskSeq = null)

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        Assertions.assertEquals(listOf("步骤1", "步骤2", "步骤3"), result.taskProgressList?.map { it.taskName })
        Assertions.assertEquals(
            listOf("0-1-1", "0-1-2", "0-2-1"),
            result.taskProgressList?.map { it.taskExecutionOrder }
        )
        Assertions.assertEquals(
            listOf("0-1", "0-1", "0-2"),
            result.taskProgressList?.map { it.jobExecutionOrder }
        )
    }

    @Test
    fun calculateStageProgressRateSortsJobOrderNumericallyNotLexicographically() {
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
                taskId = "task-job10",
                containerId = "container-job10",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.5),
                taskSeq = 1
            ),
            buildRecordTask(
                taskId = "task-job2",
                containerId = "container-job2",
                status = BuildStatus.RUNNING,
                taskVar = mutableMapOf("progressRate" to 0.5),
                taskSeq = 1
            )
        )
        every { pipelineTaskService.getAllBuildTask(PROJECT_ID, BUILD_ID) } returns listOf(
            buildTask(taskId = "task-job2", taskName = "步骤2", containerId = "container-job2", taskSeq = 1),
            buildTask(taskId = "task-job10", taskName = "步骤10", containerId = "container-job10", taskSeq = 1)
        )
        every {
            buildRecordService.getContainerOrderInStage(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                executeCount = 1,
                stageId = STAGE_ID,
                containerId = "container-job2"
            )
        } returns 1
        every {
            buildRecordService.getContainerOrderInStage(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                executeCount = 1,
                stageId = STAGE_ID,
                containerId = "container-job10"
            )
        } returns 9
        every {
            buildRecordService.getLatestRecord(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                containerId = "container-job2",
                executeCount = 1
            )
        } returns buildRecordContainer(containerId = "container-job2", startVMTaskSeq = null)
        every {
            buildRecordService.getLatestRecord(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                containerId = "container-job10",
                executeCount = 1
            )
        } returns buildRecordContainer(containerId = "container-job10", startVMTaskSeq = null)

        val result = service.calculateStageProgressRate(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            stageId = STAGE_ID
        )

        // 数值序：0-2 应排在 0-10 之前；若按字符串字典序则会得到 0-10 在前
        Assertions.assertEquals(listOf("步骤2", "步骤10"), result.taskProgressList?.map { it.taskName })
        Assertions.assertEquals(
            listOf("0-2", "0-10"),
            result.taskProgressList?.map { it.jobExecutionOrder }
        )
        Assertions.assertEquals(
            listOf("0-2-1", "0-10-1"),
            result.taskProgressList?.map { it.taskExecutionOrder }
        )
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

    @Test
    fun getTaskProgressDetailQueriesSingleRecordByTaskId() {
        val progressDetail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 0.66)
        )
        every { pipelineRuntimeService.getBuildInfo(PROJECT_ID, BUILD_ID) } returns buildInfo()
        every {
            buildRecordTaskDao.getLatestNormalRecord(
                dslContext = dslContext,
                projectId = PROJECT_ID,
                buildId = BUILD_ID,
                taskId = TASK_ID,
                executeCount = 1
            )
        } returns buildRecordTask(
            taskId = TASK_ID,
            containerId = "container-1",
            status = BuildStatus.RUNNING,
            taskVar = mutableMapOf("progressRate" to 0.1, "progressDetail" to progressDetail)
        )
        every { pipelineTaskService.getBuildTask(PROJECT_ID, BUILD_ID, TASK_ID) } returns buildTask()

        val result = service.getTaskProgressDetail(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            taskId = TASK_ID
        )

        Assertions.assertEquals("编译", result.taskName)
        Assertions.assertEquals(0.66, result.taskProgressRete)
        Assertions.assertEquals(0.66, result.progressDetail?.progress?.value)
        Assertions.assertEquals("编译", result.progressDetail?.progress?.title)
    }

    @Test
    fun getTaskProgressDetailReturnsEmptyWhenBuildArchived() {
        every { pipelineRuntimeService.getBuildInfo(PROJECT_ID, BUILD_ID) } returns null

        val result = service.getTaskProgressDetail(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            taskId = TASK_ID
        )

        Assertions.assertEquals(0.0, result.taskProgressRete)
        Assertions.assertEquals(null, result.taskName)
        Assertions.assertEquals(null, result.progressDetail)
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
        taskSeq: Int = 1,
        endTime: LocalDateTime? = null
    ) = BuildRecordTask(
        buildId = BUILD_ID,
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        resourceVersion = 1,
        stageId = STAGE_ID,
        containerId = containerId,
        taskId = taskId,
        taskSeq = taskSeq,
        executeCount = 1,
        taskVar = taskVar,
        classType = "linuxScript",
        atomCode = "script",
        status = status.name,
        endTime = endTime,
        timestamps = emptyMap<BuildTimestampType, BuildRecordTimeStamp>()
    )

    private fun buildRecordContainer(
        containerId: String,
        startVMTaskSeq: Int?
    ) = BuildRecordContainer(
        buildId = BUILD_ID,
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        resourceVersion = 1,
        stageId = STAGE_ID,
        containerId = containerId,
        executeCount = 1,
        containerVar = mutableMapOf<String, Any>().apply {
            startVMTaskSeq?.let { this[Container::startVMTaskSeq.name] = it }
        },
        containerType = "VM",
        status = BuildStatus.RUNNING.name,
        timestamps = emptyMap()
    )

    private fun buildTask(
        taskId: String = TASK_ID,
        taskName: String = "编译",
        containerId: String = "container-2",
        status: BuildStatus = BuildStatus.RUNNING,
        taskSeq: Int = 1
    ) = PipelineBuildTask(
        projectId = PROJECT_ID,
        pipelineId = PIPELINE_ID,
        buildId = BUILD_ID,
        stageId = STAGE_ID,
        containerId = containerId,
        containerHashId = "container-hash",
        containerType = "VM",
        taskSeq = taskSeq,
        taskId = taskId,
        taskName = taskName,
        taskType = "linuxScript",
        taskAtom = "script",
        status = status,
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
