package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineBuildVersionDiffDao
import com.tencent.devops.process.pojo.BuildVersionDiffInfo
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PipelineBuildVersionDiffService(
    val dslContext: DSLContext,
    val pipelineBuildVersionDiffDao: PipelineBuildVersionDiffDao,
    val pipelineRuntimeService: PipelineRuntimeService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildVersionDiffService::class.java)
    }

    fun list(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<BuildVersionDiffInfo> {
        return pipelineBuildVersionDiffDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    @Suppress("CyclomaticComplexMethod")
    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        try {
            with(event) {
                logger.info("Start to check build version diff|$projectId|$pipelineId|$buildId")
                val currBuildInfo = pipelineRuntimeService.getBuildInfo(
                    projectId = projectId, pipelineId = pipelineId, buildId = buildId
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )
                if (currBuildInfo.buildNum == 1 || currBuildInfo.versionChange != null || currBuildInfo.debug) {
                    return
                }
                val prevBuildInfo = pipelineRuntimeService.getBuildInfoByBuildNum(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildNum = currBuildInfo.buildNum - 1
                ) ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )
                pipelineRuntimeService.updateBuildVersionChangeFlag(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    versionChange = currBuildInfo.version != prevBuildInfo.version
                )
            }
        } catch (ex: Exception) {
            logger.warn("Failed to handle build queue event: $event", ex)
        }
    }
}
