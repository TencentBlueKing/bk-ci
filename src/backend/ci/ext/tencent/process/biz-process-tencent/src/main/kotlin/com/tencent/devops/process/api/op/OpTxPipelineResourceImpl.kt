package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.TencentPipelineBuildDao
import com.tencent.devops.process.dao.TxPipelineInfoDao
import com.tencent.devops.process.dao.TxPipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTxPipelineResourceImpl @Autowired constructor(
    val txPipelineInfoDao: TxPipelineInfoDao,
    val txPipelineResourceDao: TxPipelineResourceDao,
    val pipelineInfoDao: PipelineInfoDao,
    val tencentPipelineBuildDao: TencentPipelineBuildDao,
    val pipelineBuildFacadeService: PipelineBuildFacadeService,
    val dslContext: DSLContext,
    val client: Client
) : OpTxPipelineResource {

    private val logger = LoggerFactory.getLogger(OpTxPipelineResourceImpl::class.java)

    override fun updatePipelineCreator(pipelineId: String, creator: String): Result<Boolean> {
        // 校验用户是否为tx在职用户
        client.get(ServiceTxUserResource::class).get(creator)

        txPipelineInfoDao.updateCreator(dslContext, pipelineId, creator)

        return Result(true)
    }

    override fun fixPipelineCheckOut(stageTimeoutDays: Long?, buildTimeoutDays: Long?): Result<Int> {
        var count = 0
        stageTimeoutDays?.let {
            val stages = tencentPipelineBuildDao.listCheckOutErrorStage(dslContext, stageTimeoutDays)
            stages.forEach { buildStage ->
                logger.info("cancel build with stage(buildId=${buildStage.buildId}, stageId=${buildStage.stageId}), " +
                    "status=${buildStage.status}, checkOut=${buildStage.checkOut}")
                if (cancelBuild(buildStage.projectId, buildStage.pipelineId, buildStage.buildId)) {
                    count++
                }
            }
        }
        buildTimeoutDays?.let {
            val builds = tencentPipelineBuildDao.listRunningErrorBuild(dslContext, buildTimeoutDays)
            builds.forEach { build ->
                logger.info("cancel build with stage(buildId=${build.buildId}, " +
                    "status=${build.status})")
                if (cancelBuild(build.projectId, build.pipelineId, build.buildId)) {
                    count++
                }
            }
        }
        return Result(count)
    }

    override fun fixResourceVersion(
        projectId: String,
        pipelineId: String,
        version: Int?
    ): Result<Int> {
        val info = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId)
            ?: throw ErrorCodeException(
            statusCode = 404,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
        )
        logger.info("fixResourceVersion|$projectId|$pipelineId|$version|info=$info")
        val deleted = if (version == null) {
            txPipelineResourceDao.deleteResourceExceptVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = info.version
            )
        } else {
            txPipelineResourceDao.deleteResourceVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
        }
        return Result(deleted)
    }

    private fun cancelBuild(projectId: String, pipelineId: String, buildId: String): Boolean {
        return try {
            val channelCode = pipelineInfoDao.getPipelineInfo(
                dslContext, projectId, pipelineId
            )?.channel?.let { ChannelCode.getChannel(it) } ?: ChannelCode.GIT
            pipelineBuildFacadeService.buildManualShutdown(
                userId = "SYSTEM",
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = channelCode,
                checkPermission = false
            )
            true
        } catch (ignore: Throwable) {
            logger.warn("cancel build failed: ", ignore)
            false
        }
    }
}
