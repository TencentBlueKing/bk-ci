package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineRealtimeResource
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineStatus
import com.tencent.devops.process.service.PipelineListFacadeService
import org.jooq.DSLContext

@RestResource
class UserPipelineRealtimeResourceImpl(
    private val dslContext: DSLContext,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineInfoDao: PipelineInfoDao
) : UserPipelineRealtimeResource {
    override fun status(
        userId: String,
        projectId: String,
        pipelineIds: Set<String>
    ): Result<Map<String, PipelineStatus>> {
        require(userId.isNotBlank() && projectId.isNotBlank() && pipelineIds.size in 1..128)
        require(pipelineIds.all { it.length in 1..128 })
        val existing = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).map { it.pipelineId }.toSet()
        if (existing.isEmpty()) return Result(emptyMap())
        val allowed = pipelinePermissionService.filterPipelines(
            userId, projectId, setOf(AuthPermission.VIEW), existing.toList()
        )[AuthPermission.VIEW].orEmpty().toSet()
        if (allowed.isEmpty()) return Result(emptyMap())
        val pipelines = pipelineListFacadeService.getPipelineStatus(userId, projectId, allowed)
        val summaries = pipelineBuildSummaryDao.listSummaryByPipelineIds(dslContext, allowed, projectId)
            .associateBy { it.pipelineId }
        val buildIds = summaries.values.mapNotNull { it.latestBuildId }.filter { it.isNotBlank() }.toSet()
        val counts = if (buildIds.isEmpty()) emptyMap() else {
            pipelineBuildTaskDao.countGroupByBuildId(dslContext, projectId, buildIds).groupBy { it.value1() }
        }
        val builds = if (buildIds.isEmpty()) emptyMap() else {
            pipelineBuildDao.listBuildInfoByBuildIds(dslContext, buildIds, projectId).associateBy { it.buildId }
        }
        val now = System.currentTimeMillis()
        return Result(pipelines.associate { pipeline ->
            val summary = summaries[pipeline.pipelineId]
            val tasks = counts[summary?.latestBuildId].orEmpty()
            val build = builds[summary?.latestBuildId]
            pipeline.pipelineId to PipelineStatus(
                taskCount = pipeline.taskCount,
                buildCount = pipeline.buildCount,
                lock = pipeline.lock,
                canManualStartup = pipeline.canManualStartup,
                latestBuildStartTime = pipeline.latestBuildStartTime,
                latestBuildEndTime = pipeline.latestBuildEndTime,
                latestBuildStatus = pipeline.latestBuildStatus,
                latestBuildNum = pipeline.latestBuildNum,
                latestBuildEstimatedExecutionSeconds = pipeline.latestBuildEstimatedExecutionSeconds,
                latestBuildId = pipeline.latestBuildId,
                currentTimestamp = now,
                runningBuildCount = pipeline.runningBuildCount,
                hasCollect = pipeline.hasCollect,
                lastBuildTotalCount = tasks.sumOf { it.value3() },
                lastBuildFinishCount = tasks
                    .filter { it.value2() == BuildStatus.SUCCEED.ordinal }
                    .sumOf { it.value3() },
                trigger = build?.trigger,
                latestBuildStageStatus = build?.stageStatus
            )
        })
    }
}
