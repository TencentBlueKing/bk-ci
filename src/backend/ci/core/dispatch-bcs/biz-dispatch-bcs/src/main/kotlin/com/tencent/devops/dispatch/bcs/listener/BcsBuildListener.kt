package com.tencent.devops.dispatch.bcs.listener

import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.bcs.actions.BuilderAction
import com.tencent.devops.dispatch.bcs.common.LogsPrinter
import com.tencent.devops.dispatch.bcs.dao.BuildBuilderPoolNoDao
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BcsBuildListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val builderPoolNoDao: BuildBuilderPoolNoDao,
    private val builderAction: BuilderAction,
    private val logsPrinter: LogsPrinter
) : BuildListener {

    companion object {
        private val logger = LoggerFactory.getLogger(BcsBuildListener::class.java)
        private const val shutdownLockBaseKey = "dispatch_bcs_shutdown_lock_"
    }

    override fun getShutdownQueue(): String {
        return ".bcs.public"
    }

    override fun getStartupDemoteQueue(): String {
        return ".bcs.public.demote"
    }

    override fun getStartupQueue(): String {
        return ".bcs.public"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.DOCKER_BCS
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        startUp(dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        startUp(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        if (event.source == "shutdownAllVMTaskAtom") {
            // 同一个buildId的多个shutdownAllVMTaskAtom事件一定在短时间内到达，300s足够
            val shutdownLock = RedisLock(redisOperation, shutdownLockBaseKey + event.buildId, 300L)
            try {
                if (shutdownLock.tryLock()) {
                    builderAction.doShutdown(event)
                } else {
                    logger.info("shutdownAllVMTaskAtom of {} already invoked, ignore", event.buildId)
                }
            } catch (e: Exception) {
                logger.info("Fail to shutdown VM", e)
            } finally {
                shutdownLock.unlock()
            }
        } else {
            builderAction.doShutdown(event)
        }
    }

    private fun startUp(dispatchMessage: DispatchMessage) {
        logger.info("On start up - ($dispatchMessage)")
        logsPrinter.printLogs(dispatchMessage, "准备创建BCS(蓝鲸容器平台)构建机...")

        val buildBuilderPoolNo = builderPoolNoDao.getBcsBuildLastPoolNo(
            dslContext = dslContext,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            executeCount = dispatchMessage.executeCount ?: 1
        )
        logger.info("buildBuilderPoolNo: $buildBuilderPoolNo")
        if (buildBuilderPoolNo.isNotEmpty() && buildBuilderPoolNo[0].second != null) {
            retry()
        } else {
            builderAction.createOrStartBuilder(dispatchMessage, 0)
        }
    }
}
