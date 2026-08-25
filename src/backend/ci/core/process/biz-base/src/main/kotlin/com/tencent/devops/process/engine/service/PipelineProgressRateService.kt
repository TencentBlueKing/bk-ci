package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.container.Container
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
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import java.math.BigDecimal
import java.math.RoundingMode
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
            val taskVar = mutableMapOf<String, Any>(PROGRESS_RATE_PLACEHOLDER to progressRate)
            if (progressDetail != null) {
                taskVar[PROGRESS_DETAIL_PLACEHOLDER] = progressDetail
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
        val finishedTasks = stageTasks.filter {
            val taskStatus = BuildStatus.parse(it.status)
            taskStatus.isSuccess() || taskStatus.isFailure()
        }
        val runningTasks = stageTasks.filter {
            BuildStatus.parse(it.status).isRunning()
        }
        if (stageTasks.isEmpty()) {
            return BuildStageProgressInfo(stageProgressRete = 0.0, taskProgressList = emptyList())
        }
        val runningTaskProgresses = runningTasks
            .filter { hasReportedProgress(it.taskVar) }
            .map(::toTaskProgress)
        val runningTaskTotalProgressRate = runningTaskProgresses.sumOf { it.progressRate }
        val stageProgressRate = (finishedTasks.size + runningTaskTotalProgressRate) / stageTasks.size
        val completedTaskProgresses = finishedTasks
            .filter { hasReportedProgress(it.taskVar) }
            .map(::toTaskProgress)
        val taskProgressesForList = if (runningTaskProgresses.isNotEmpty()) {
            runningTaskProgresses + completedTaskProgresses
        } else {
            completedTaskProgresses
        }
        val taskProgressList = buildTaskProgressList(
            taskProgresses = taskProgressesForList,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            stageId = stageId
        )
        return BuildStageProgressInfo(
            stageProgressRete = roundProgressRate(stageProgressRate),
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
        // 构建已归档时运行时表已无数据，进度明细属于运行态展示信息，无需报错，返回空进度即可
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            buildId = buildId
        ) ?: run {
            logger.info("build not found, likely archived, return empty progress|$projectId|$buildId")
            return BuildTaskProgressInfo(taskProgressRete = 0.0)
        }
        val targetExecuteCount = executeCount ?: buildInfo.executeCount ?: 1
        val taskRecord = buildRecordTaskDao.getLatestNormalRecord(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            taskId = taskId,
            executeCount = targetExecuteCount
        )
        val taskName = pipelineTaskService.getBuildTask(projectId, buildId, taskId)?.taskName
        return BuildTaskProgressInfo(
            taskProgressRete = taskRecord?.taskVar?.let(::getProgressRate) ?: 0.0,
            taskName = taskName,
            progressDetail = taskRecord?.taskVar?.let(::getProgressDetail)?.withDefaultTitles(taskName)
        )
    }

    private fun buildTaskProgressList(
        taskProgresses: List<RunningTaskProgress>,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        stageId: String
    ): List<BuildTaskProgressInfo> {
        if (taskProgresses.isEmpty()) {
            return emptyList()
        }
        val taskNameMap = getTaskNameMap(
            projectId = projectId,
            buildId = buildId,
            taskIds = taskProgresses.map { it.record.taskId }
        )
        // 按去重后的容器预计算每个容器的 job 执行顺序及其数值排序键，避免循环内重复查询/解析
        val jobExecutionOrderMap = taskProgresses.map { it.record.containerId }.distinct().associateWith {
            getJobExecutionOrder(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount,
                stageId = stageId,
                containerId = it
            )
        }
        val jobOrderSortKeyMap = jobExecutionOrderMap.mapValues { toJobOrderSortKey(it.value) }
        val startVMTaskSeqCache = mutableMapOf<String, Int?>()
        return taskProgresses.sortedWith(
            // 按数值比较 stageOrder/jobOrder，避免 "0-10" 因字典序排到 "0-2" 之前
            compareBy<RunningTaskProgress> { jobOrderSortKeyMap.getValue(it.record.containerId).first }
                .thenBy { jobOrderSortKeyMap.getValue(it.record.containerId).second }
                .thenBy { it.record.taskSeq }
        ).map {
            val taskName = taskNameMap[it.record.taskId]
            val jobExecutionOrder = jobExecutionOrderMap.getValue(it.record.containerId)
            val taskExecutionOrder = "$jobExecutionOrder-${
                getVisibleTaskSeq(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    executeCount = executeCount,
                    containerId = it.record.containerId,
                    rawTaskSeq = it.record.taskSeq,
                    startVMTaskSeqCache = startVMTaskSeqCache
                )
            }"
            BuildTaskProgressInfo(
                taskProgressRete = it.progressRate,
                taskName = taskName,
                jobExecutionOrder = jobExecutionOrder,
                taskExecutionOrder = taskExecutionOrder,
                progressDetail = it.progressDetail?.withDefaultTitles(taskName)
            )
        }
    }

    private fun getVisibleTaskSeq(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        containerId: String,
        rawTaskSeq: Int,
        startVMTaskSeqCache: MutableMap<String, Int?>
    ): Int {
        val startVMTaskSeq = startVMTaskSeqCache.getOrPut(containerId) {
            runCatching {
                buildRecordService.getLatestRecord(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    containerId = containerId,
                    executeCount = executeCount
                )?.containerVar?.get(Container::startVMTaskSeq.name)?.toString()?.toIntOrNull()
            }.getOrNull()
        }
        return if (startVMTaskSeq != null && startVMTaskSeq > 0 && rawTaskSeq > startVMTaskSeq) {
            rawTaskSeq - 1
        } else {
            rawTaskSeq
        }
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

    private fun toJobOrderSortKey(jobExecutionOrder: String): Pair<Int, Int> {
        val parts = jobExecutionOrder.split("-")
        val stageOrder = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val jobOrder = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return stageOrder to jobOrder
    }

    private fun toTaskProgress(record: BuildRecordTask): RunningTaskProgress {
        val progressDetail = getProgressDetail(record.taskVar)
        return RunningTaskProgress(
            record = record,
            progressRate = getProgressRate(record.taskVar, progressDetail),
            progressDetail = progressDetail
        )
    }

    private fun getTaskNameMap(
        projectId: String,
        buildId: String,
        taskIds: List<String>
    ): Map<String, String?> {
        if (taskIds.isEmpty()) {
            return emptyMap()
        }
        val taskIdSet = taskIds.toSet()
        return pipelineTaskService.getAllBuildTask(projectId, buildId)
            .filter { it.taskId in taskIdSet }
            .associate { it.taskId to it.taskName }
    }

    private fun hasReportedProgress(taskVar: Map<String, Any>): Boolean {
        return taskVar.containsKey(PROGRESS_RATE_PLACEHOLDER) || taskVar.containsKey(PROGRESS_DETAIL_PLACEHOLDER)
    }

    private fun getProgressRate(
        taskVar: Map<String, Any>,
        progressDetail: BuildTaskProgressDetail? = getProgressDetail(taskVar)
    ): Double {
        return progressDetail?.progress?.value
            ?: taskVar[PROGRESS_RATE_PLACEHOLDER]?.toString()?.toDoubleOrNull()
            ?: 0.0
    }

    private fun getProgressDetail(taskVar: Map<String, Any>): BuildTaskProgressDetail? {
        val progressDetail = taskVar[PROGRESS_DETAIL_PLACEHOLDER] ?: return null
        val parsed = when (progressDetail) {
            is BuildTaskProgressDetail -> progressDetail
            is String -> JsonUtil.toOrNull(progressDetail, BuildTaskProgressDetail::class.java)
            else -> JsonUtil.toOrNull(
                JsonUtil.toJson(progressDetail, formatted = false),
                BuildTaskProgressDetail::class.java
            )
        } ?: return null
        // 读路径遇到脏数据/历史数据/字段口径变更时降级为空，避免单条非法明细击穿整个进度查询接口
        return runCatching { BuildTaskProgressDetailValidator.normalize(parsed) }.getOrElse {
            logger.warn("normalize progress detail failed, fallback to null|detail=$progressDetail", it)
            null
        }
    }

    private fun BuildTaskProgressDetail.withDefaultTitles(taskName: String?): BuildTaskProgressDetail {
        val titlePrefix = taskName?.takeIf { it.isNotBlank() } ?: "步骤"
        val subtaskGroup = subtasks
        val progressTimeline = timeline
        return copy(
            progress = progress.copy(title = progress.title?.takeIf { it.isNotBlank() } ?: titlePrefix),
            subtasks = subtaskGroup?.copy(title = subtaskGroup.title?.takeIf { it.isNotBlank() } ?: "子任务进度"),
            timeline = progressTimeline?.copy(title = progressTimeline.title?.takeIf { it.isNotBlank() } ?: titlePrefix)
        )
    }

    private fun roundProgressRate(value: Double): Double {
        return BigDecimal.valueOf(value).setScale(PROGRESS_SCALE, RoundingMode.HALF_UP).toDouble()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineProgressRateService::class.java)
        private const val PROGRESS_RATE_PLACEHOLDER = "progressRate"
        private const val PROGRESS_DETAIL_PLACEHOLDER = "progressDetail"
        private const val PROGRESS_SCALE = 4
    }

    private data class RunningTaskProgress(
        val record: BuildRecordTask,
        val progressRate: Double,
        val progressDetail: BuildTaskProgressDetail?
    )
}
