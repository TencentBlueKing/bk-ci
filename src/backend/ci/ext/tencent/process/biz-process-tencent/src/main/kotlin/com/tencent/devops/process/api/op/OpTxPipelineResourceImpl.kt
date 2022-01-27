package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.dao.TencentPipelineBuildDao
import com.tencent.devops.process.dao.TxPipelineInfoDao
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTxPipelineResourceImpl @Autowired constructor(
    val pipelineInfoDao: TxPipelineInfoDao,
    val tencentPipelineBuildDao: TencentPipelineBuildDao,
    val pipelineBuildFacadeService: PipelineBuildFacadeService,
    val dslContext: DSLContext,
    val client: Client
) : OpTxPipelineResource {

    private val logger = LoggerFactory.getLogger(OpTxPipelineResourceImpl::class.java)

    override fun updatePipelineCreator(pipelineId: String, creator: String): Result<Boolean> {
        // 校验用户是否为tx在职用户
        client.get(ServiceTxUserResource::class).get(creator)

        pipelineInfoDao.updateCreator(dslContext, pipelineId, creator)

        return Result(true)
    }

    override fun fixPipelineCheckOut(stageTimeoutDays: Long?, buildTimeoutDays: Long?): Result<Int> {
        var count = 0
        stageTimeoutDays?.let {
            val stages = tencentPipelineBuildDao.listCheckOutErrorStage(dslContext, stageTimeoutDays)
            stages.forEach { buildStage ->
                logger.info("cancel build with stage(buildId=${buildStage.buildId}, stageId=${buildStage.stageId}), " +
                    "status=${buildStage.status}, checkOut=${buildStage.checkOut}")
                try {
                    pipelineBuildFacadeService.buildManualShutdown(
                        userId = "SYSTEM",
                        projectId = buildStage.projectId,
                        pipelineId = buildStage.pipelineId,
                        buildId = buildStage.buildId,
                        channelCode = ChannelCode.GIT,
                        checkPermission = false
                    )
                    count++
                } catch (ignore: Throwable) {
                    logger.warn("cancel stage failed: ", ignore)
                }
            }
        }
        buildTimeoutDays?.let {
            val builds = tencentPipelineBuildDao.listRunningErrorBuild(dslContext, buildTimeoutDays)
            builds.forEach { build ->
                logger.info("cancel build with stage(buildId=${build.buildId}, " +
                    "status=${build.status}")
                try {
                    pipelineBuildFacadeService.buildManualShutdown(
                        userId = "SYSTEM",
                        projectId = build.projectId,
                        pipelineId = build.pipelineId,
                        buildId = build.buildId,
                        channelCode = ChannelCode.GIT,
                        checkPermission = false
                    )
                    count++
                } catch (ignore: Throwable) {
                    logger.warn("cancel build failed: ", ignore)
                }
            }
        }
        return Result(count)
    }
}
