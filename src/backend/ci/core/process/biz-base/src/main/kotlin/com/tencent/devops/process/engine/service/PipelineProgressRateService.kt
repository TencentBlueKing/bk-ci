package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.JobHeartbeatRequest
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressDetail
import com.tencent.devops.common.pipeline.pojo.progress.BuildTaskProgressDetailValidator
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.pojo.BuildStageProgressInfo
import com.tencent.devops.process.pojo.BuildTaskProgressInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.ws.rs.core.Response

@Service
@Suppress("LongParameterList")
class PipelineProgressRateService constructor(
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildRecordService: ContainerBuildRecordService,
    private val buildRecordTaskDao: BuildRecordTaskDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val dslContext: DSLContext
) {
    fun reportProgressRate(
        projectId: String,
        buildId: String,
        executeCount: Int = 1,
        jobHeartbeatRequest: JobHeartbeatRequest?
    ) {
        val task2ProgressRate = jobHeartbeatRequest?.task2ProgressRate.orEmpty()
        val task2ProgressDetail = jobHeartbeatRequest?.task2ProgressDetail.orEmpty()
        logger.info(
            "report progress rate:$projectId|$buildId|$executeCount|" +
                "rateSize=${task2ProgressRate.size}|detailSize=${task2ProgressDetail.size}"
        )
        if (task2ProgressRate.isEmpty() && task2ProgressDetail.isEmpty()) return
        val pipelineId = pipelineBuildDao.getBuildInfo(
            dslContext = dslContext, projectId = projectId, buildId = buildId
        )?.pipelineId ?: run {
            logger.error("no build info found for $buildId")
            return
        }
        (task2ProgressRate.keys + task2ProgressDetail.keys).forEach { taskId ->
            val progressDetail = task2ProgressDetail[taskId]?.let {
                runCatching { BuildTaskProgressDetailValidator.normalize(it) }.getOrNull()
            }
            val progressRate = progressDetail?.progress?.value ?: task2ProgressRate[taskId] ?: return@forEach
            val taskVar = mutableMapOf<String, Any>(progressRatePlaceholder to progressRate)
            if (progressDetail != null) {
                taskVar[progressDetailPlaceholder] = progressDetail
            }
            try {
                taskBuildRecordService.updateTaskRecord(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    taskId = taskId,
                    executeCount = executeCount,
                    taskVar = taskVar,
                    buildStatus = null,
                    operation = "reportProgressRate"
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "report progress rate failed|projectId=$projectId|buildId=$buildId|" +
                        "taskId=$taskId|executeCount=$executeCount",
                    ignored
                )
            }
        }
    }

    @Suppress("ImplicitDefaultLocale")
    fun calculateStageProgressRate(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String
    ): BuildStageProgressInfo {
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            buildId = buildId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        val executeCount = buildInfo.executeCount ?: 1
        val stageTasks = buildRecordTaskDao.getLatestNormalRecords(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
            stageId = stageId,
            matrixContainerIds = emptyList()
        )
        val completedTasks = stageTasks.filter {
            BuildStatus.parse(it.status).isSuccess()
        }
        val runningTasks = stageTasks.filter {
            BuildStatus.parse(it.status).isRunning()
        }
        if (stageTasks.isEmpty()) {
            return BuildStageProgressInfo(stageProgressRete = 0.0, taskProgressList = emptyList())
        }
        val runningTaskTotalProgressRate = runningTasks.sumOf { getProgressRate(it.taskVar) }
        val stageProgressRate = (completedTasks.size + runningTaskTotalProgressRate) / stageTasks.size
        val taskProgressList = runningTasks.map {
            val taskName = pipelineTaskService.getBuildTask(projectId, buildId, it.taskId)?.taskName
            BuildTaskProgressInfo(
                taskProgressRete = getProgressRate(it.taskVar),
                taskName = taskName,
                jobExecutionOrder = getJobExecutionOrder(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    executeCount = executeCount,
                    stageId = stageId,
                    containerId = it.containerId
                ),
                progressDetail = getProgressDetail(it.taskVar)?.withDefaultTitles(taskName)
            )
        }.sortedBy { it.jobExecutionOrder }
        return BuildStageProgressInfo(
            stageProgressRete = String.format("%.4f", stageProgressRate).toDouble(),
            taskProgressList = taskProgressList
        )
    }

    fun getTaskProgressDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int? = null
    ): BuildTaskProgressInfo {
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            buildId = buildId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        val targetExecuteCount = executeCount ?: buildInfo.executeCount
        val taskRecord = buildRecordTaskDao.getLatestNormalRecords(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            executeCount = targetExecuteCount,
            matrixContainerIds = emptyList()
        ).firstOrNull { it.taskId == taskId }
        val taskName = pipelineTaskService.getBuildTask(projectId, buildId, taskId)?.taskName
        return BuildTaskProgressInfo(
            taskProgressRete = taskRecord?.taskVar?.let(::getProgressRate) ?: 0.0,
            taskName = taskName,
            progressDetail = taskRecord?.taskVar?.let(::getProgressDetail)?.withDefaultTitles(taskName)
        )
    }

    private fun getJobExecutionOrder(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        stageId: String,
        containerId: String
    ): String {
        val stageOrder = stageId.replace("stage-", "").toInt() - 1
        val jobOrder = buildRecordService.getContainerOrderInStage(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            stageId = stageId,
            containerId = containerId
        ) + 1
        return "$stageOrder-$jobOrder"
    }

    private fun getProgressRate(taskVar: Map<String, Any>): Double {
        return getProgressDetail(taskVar)?.progress?.value
            ?: taskVar[progressRatePlaceholder]?.toString()?.toDoubleOrNull()
            ?: 0.0
    }

    private fun getProgressDetail(taskVar: Map<String, Any>): BuildTaskProgressDetail? {
        val progressDetail = taskVar[progressDetailPlaceholder] ?: return null
        return when (progressDetail) {
            is BuildTaskProgressDetail -> progressDetail
            is String -> JsonUtil.toOrNull(progressDetail, BuildTaskProgressDetail::class.java)
            else -> JsonUtil.toOrNull(
                JsonUtil.toJson(progressDetail, formatted = false),
                BuildTaskProgressDetail::class.java
            )
        }?.let { BuildTaskProgressDetailValidator.normalize(it) }
    }

    private fun BuildTaskProgressDetail.withDefaultTitles(taskName: String?): BuildTaskProgressDetail {
        val titlePrefix = taskName?.takeIf { it.isNotBlank() } ?: "步骤"
        val subtaskGroup = subtasks
        val progressTimeline = timeline
        return copy(
            progress = progress.copy(title = progress.title ?: "${titlePrefix}进度"),
            subtasks = subtaskGroup?.copy(title = subtaskGroup.title ?: "子任务进度"),
            timeline = progressTimeline?.copy(title = progressTimeline.title ?: "${titlePrefix}阶段时间线")
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineProgressRateService::class.java)
        private const val progressRatePlaceholder = "progressRate"
        private const val progressDetailPlaceholder = "progressDetail"
    }
}
