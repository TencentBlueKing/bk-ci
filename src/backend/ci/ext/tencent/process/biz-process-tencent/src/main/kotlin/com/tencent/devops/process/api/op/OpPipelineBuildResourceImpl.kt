package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineBuildResourceImpl @Autowired constructor(
    val pipelineBuildDao: PipelineBuildDao,
    val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    val dslContext: DSLContext,
    val client: Client
) : OpPipelineBuildResource {

    private val logger = LoggerFactory.getLogger(OpPipelineBuildResourceImpl::class.java)

    override fun fixPipelineBuildStatus(
        projectId: String,
        pipelineId: String,
        statusFrom: Int,
        statusTo: Int,
        buildIds: List<String>?
    ): Result<Int> {
        logger.info(
            "OpPipelineBuildResourceImpl|fixPipelineBuildStatus" +
                "|$projectId|$pipelineId|$statusFrom|$statusTo|$buildIds"
        )
        var okCount = 0
        val oldStatus = BuildStatus.values()[statusFrom]
        val newStatus = BuildStatus.values()[statusTo]
        buildIds?.forEach {
            if (pipelineBuildDao.updateStatus(
                    dslContext = dslContext, projectId = projectId, buildId = it, oldBuildStatus = oldStatus,
                    newBuildStatus = newStatus
                )
            ) {
                okCount += 1
            }
        }

        if (oldStatus.isRunning() && newStatus.isFinish()) {
            this.fixPipelineSummaryCount(
                projectId = projectId,
                pipelineId = pipelineId,
                finishCount = okCount,
                runningCount = -okCount,
                queueCount = null
            )
        }

        if (oldStatus.isReadyToRun() && newStatus.isFinish()) {
            this.fixPipelineSummaryCount(
                projectId = projectId,
                pipelineId = pipelineId,
                finishCount = okCount,
                runningCount = null,
                queueCount = -okCount
            )
        }
        return Result(okCount)
    }

    override fun fixPipelineSummaryCount(
        projectId: String,
        pipelineId: String,
        finishCount: Int?,
        runningCount: Int?,
        queueCount: Int?
    ): Result<Int> {
        logger.info(
            "OpPipelineBuildResourceImpl|fixPipelineSummaryCount" +
                "|$projectId|$pipelineId|$finishCount|$runningCount|$queueCount"
        )
        if (!pipelineBuildSummaryDao.fixPipelineSummaryCount(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                finishCount = finishCount ?: 0,
                runningCount = runningCount,
                queueCount = queueCount
            )
        ) {
            logger.info("OpPipelineBuildResourceImpl|fixPipelineSummaryCount|update failed")
            return Result(0)
        }
        return Result(1)
    }
}
