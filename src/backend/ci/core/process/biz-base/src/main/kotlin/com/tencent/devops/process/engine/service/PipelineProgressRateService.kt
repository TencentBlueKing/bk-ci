package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.JobHeartbeatRequest
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
        logger.info("report progress rate:$projectId|$buildId|$executeCount|$jobHeartbeatRequest")
        val task2ProgressRate = jobHeartbeatRequest?.task2ProgressRate ?: return
        if (task2ProgressRate.isEmpty()) return
        val pipelineId = pipelineBuildDao.getBuildInfo(
            dslContext = dslContext, projectId = projectId, buildId = buildId
        )?.pipelineId ?: run {
            logger.error("no build info found for $buildId")
            return
        }
        task2ProgressRate.forEach { (taskId, progressRate) ->
            taskBuildRecordService.updateTaskRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                executeCount = executeCount,
                taskVar = mapOf(progressRatePlaceholder to progressRate),
                buildStatus = null,
                operation = "reportProgressRate"
            )
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
        var runningTaskTotalProgressRate = 0.0
        if (runningTasks.isNotEmpty()) {
            runningTasks.forEach {
                if (it.taskVar[progressRatePlaceholder] != null) {
                    runningTaskTotalProgressRate += it.taskVar[progressRatePlaceholder].toString().toDouble()
                }
            }
        }
        val stageProgressRate = (completedTasks.size + runningTaskTotalProgressRate) / stageTasks.size
        val taskProgressList = runningTasks.map {
            BuildTaskProgressInfo(
                taskProgressRete = it.taskVar[progressRatePlaceholder]?.toString()?.toDouble() ?: 0.0,
                taskName = pipelineTaskService.getBuildTask(projectId, buildId, it.taskId)?.taskName,
                jobExecutionOrder = getJobExecutionOrder(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    executeCount = executeCount,
                    stageId = stageId,
                    containerId = it.containerId
                )
            )
        }.sortedBy { it.jobExecutionOrder }
        return BuildStageProgressInfo(
            stageProgressRete = String.format("%.4f", stageProgressRate).toDouble(),
            taskProgressList = taskProgressList
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

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineProgressRateService::class.java)
        private const val progressRatePlaceholder = "progressRate"
    }
}
