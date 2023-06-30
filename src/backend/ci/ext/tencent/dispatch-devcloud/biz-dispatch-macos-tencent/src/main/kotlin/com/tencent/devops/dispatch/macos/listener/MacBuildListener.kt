package com.tencent.devops.dispatch.macos.listener

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDelete
import com.tencent.devops.dispatch.macos.service.BuildHistoryService
import com.tencent.devops.dispatch.macos.service.BuildTaskService
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import com.tencent.devops.dispatch.macos.service.MacosVMRedisService
import com.tencent.devops.dispatch.macos.util.MacOSThreadPoolUtils
import com.tencent.devops.dispatch.macos.util.ThreadPoolName
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.RejectedExecutionException

@Component
class MacBuildListener @Autowired constructor(
    private val client: Client,
    private val buildHistoryService: BuildHistoryService,
    private val buildTaskService: BuildTaskService,
    private val redisOperation: RedisOperation,
    private val devCloudMacosService: DevCloudMacosService,
    private val macosVMRedisService: MacosVMRedisService,
    private val buildLogPrinter: BuildLogPrinter
) : BuildListener {

    override fun getShutdownQueue(): String {
        return ".macos"
    }

    override fun getStartupDemoteQueue(): String {
        return ".macos.demote"
    }

    override fun getStartupQueue(): String {
        return ".macos"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.MACOS_DEVCLOUD
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        try {
            MacOSThreadPoolUtils.instance.getThreadPool(ThreadPoolName.STARTUP).execute {
                doStartup(dispatchMessage)
            }
        } catch (e: RejectedExecutionException) {
            // 构建任务被线程池拒绝，重新回队列
            logger.info("${dispatchMessage.buildId}|${dispatchMessage.vmSeqId}|${dispatchMessage.executeCount} " +
                            "build task rejected. Retry")
            retry(sleepTimeInMS = 5000, retryTimes = 120, pipelineEvent = dispatchMessage.event)
        }
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        try {
            MacOSThreadPoolUtils.instance.getThreadPool(ThreadPoolName.DEMOTE_STARTUP).execute {
                doStartup(dispatchMessage)
            }
        } catch (e: RejectedExecutionException) {
            // 构建任务被线程池拒绝，重新回队列
            logger.info("${dispatchMessage.buildId}|${dispatchMessage.vmSeqId}|${dispatchMessage.executeCount} " +
                            "build task rejected. Retry")
            retry(sleepTimeInMS = 5000, retryTimes = 120, pipelineEvent = dispatchMessage.event)
        }
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("MacOS shutdown with event($event)")
        // 如果是某个job关闭，则锁到job，如果是整条流水线shutdown，则锁到buildid级别
        val lockKey =
            if (event.vmSeqId == null)
                "$LOCK_SHUTDOWN:${event.buildId}" else "$LOCK_SHUTDOWN:${event.buildId}:${event.vmSeqId}"
        val redisLock = RedisLock(
            redisOperation,
            lockKey,
            30
        )
        try {
            if (!redisLock.tryLock()) {
                return
            }

            val buildTaskRecords = buildTaskService.getByBuildIdAndVmSeqId(
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                executeCount = event.executeCount
            )

            // task记录为空，可能createVM还在进行中，等待createVM结束后回收机器
            if (buildTaskRecords.isEmpty()) {
                logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Recycling the macos failed.")
                retry(sleepTimeInMS = 60000, retryTimes = 10, pipelineEvent = event)
                return
            }

            doShutdown(buildTaskRecords, event, event.userId, event.projectId)
        } catch (e: Exception) {
            logger.error("[${event.projectId}|${event.pipelineId}|${event.buildId}] :$e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun doStartup(dispatchMessage: DispatchMessage) {
        logger.info("MacOS Dispatch on start up - ($dispatchMessage)")
        try {
            val buildHistoryId = buildHistoryService.saveBuildHistory(dispatchMessage)
            // 保存构建任务失败，直接返回，对此次构建任务不做处理
            if (buildHistoryId < 0) {
                return
            }

            val devCloudMacosVmInfo = devCloudMacosService.creatVM(dispatchMessage)
            devCloudMacosVmInfo?.let {
                devCloudMacosService.saveVM(it)
                buildHistoryService.saveBuildTask(it.ip, it.id, buildHistoryId, dispatchMessage)
                macosVMRedisService.saveRedisBuild(dispatchMessage, it.ip)

                logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] " +
                                "Success to start vm(${it.ip}|${it.id})")

                log(
                    buildLogPrinter = buildLogPrinter,
                    buildId = dispatchMessage.buildId,
                    containerHashId = dispatchMessage.containerHashId,
                    vmSeqId = dispatchMessage.vmSeqId,
                    message = "DevCloud MacOS IP：${it.ip}",
                    executeCount = dispatchMessage.executeCount
                )
            } ?: run {
                // 如果没有找到合适的vm机器，则等待5秒后再执行, 总共执行120次（10min）
                logRed(
                    buildLogPrinter,
                    dispatchMessage.buildId,
                    dispatchMessage.containerHashId,
                    dispatchMessage.vmSeqId,
                    "No idle macOS resources found, wait 10 seconds and try again",
                    dispatchMessage.executeCount
                )

                logger.error("Can not found any idle vm for this build($dispatchMessage),wait for 5s")
                retry(sleepTimeInMS = 5000, retryTimes = 120, pipelineEvent = dispatchMessage.event)
            }
        } catch (e: BuildFailureException) {
            handleStartupException(
                t = e,
                errorCode = e.errorCode,
                errorMessage = e.formatErrorMessage,
                errorType = e.errorType,
                dispatchMessage = dispatchMessage
            )
        } catch (t: Throwable) {
            handleStartupException(
                t = t,
                errorCode = DispatchSdkErrorCode.SDK_SYSTEM_ERROR,
                errorMessage = "Fail to handle the start up message",
                errorType = ErrorType.SYSTEM,
                dispatchMessage = dispatchMessage
            )
        }
    }

    private fun handleStartupException(
        t: Throwable,
        errorCode: Int,
        errorType: ErrorType,
        errorMessage: String,
        dispatchMessage: DispatchMessage
    ) {
        logger.error("Fail to handle the start up message: $dispatchMessage)", t)
        val dispatchService = SpringContextUtil.getBean(DispatchService::class.java)
        dispatchService.logRed(
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "${I18nUtil.getCodeLanMessage("${CommonMessageCode.BK_FAILED_START_BUILD_MACHINE}")} " +
                "- ${t.message}",
            executeCount = dispatchMessage.executeCount
        )

        client.get(ServiceBuildResource::class).setVMStatus(
            projectId = dispatchMessage.projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            status = BuildStatus.FAILED,
            errorType = errorType,
            errorCode = errorCode,
            errorMsg = errorMessage
        )
    }

    private fun doShutdown(
        buildTaskRecords: Result<TBuildTaskRecord>,
        event: PipelineAgentShutdownEvent,
        creator: String,
        projectId: String
    ) {
        buildTaskRecords.forEach { buildTask ->
            try {
                val vmIp = buildTask.vmIp
                val vmId = buildTask.vmId
                logger.info("${event.buildId}|${event.vmSeqId} Shutdown MacOS ip($vmIp), id($vmId)")
                macosVMRedisService.deleteRedisBuild(vmIp)
                devCloudMacosService.deleteVM(
                    creator = creator,
                    devCloudMacosVmDelete = DevCloudMacosVmDelete(
                        project = projectId,
                        pipelineId = buildTask.pipelineId,
                        buildId = buildTask.buildId,
                        vmSeqId = buildTask.vmSeqId,
                        id = vmId.toString()
                    )
                )

                logger.info("${event.buildId}|${event.vmSeqId} end build. buildId: ${buildTask.id}")
                buildHistoryService.endBuild(
                    MacJobStatus.Done,
                    buildTask.buildHistoryId,
                    buildTask.id
                )
            } catch (e: Exception) {
                val vmIp = buildTask.vmIp
                logger.error("${event.buildId}|${event.vmSeqId} Shutdown error,vm is $vmIp", e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MacBuildListener::class.java)
        private const val LOCK_SHUTDOWN = "dispatcher:locker:macos:shutdown"
    }
}
