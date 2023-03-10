package com.tencent.devops.dispatch.macos.listener

import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.macos.constant.ErrorCodeEnum
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.dispatch.macos.service.BuildHistoryService
import com.tencent.devops.dispatch.macos.service.BuildTaskService
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import com.tencent.devops.dispatch.macos.service.MacVmTypeService
import com.tencent.devops.dispatch.macos.service.MacosVMRedisService
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException

@Component
class MacBuildListener @Autowired constructor(
    private val buildHistoryService: BuildHistoryService,
    private val macVmTypeService: MacVmTypeService,
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
        logger.info("MacOS Dispatch on start up - ($dispatchMessage)")
        val macOSEvn = dispatchMessage.dispatchMessage.split(":")
        val pair = when (macOSEvn.size) {
            0 -> Pair(null, null)
            1 -> Pair(macOSEvn[0], null)
            else -> Pair(macOSEvn[0], macOSEvn[1])
        }
        var systemVersion: String? = pair.first
        val xcodeVersion: String? = pair.second
        val projectId = dispatchMessage.projectId
        val creator = dispatchMessage.userId

        val isGitProject = projectId.startsWith("git_")
        logger.info("MacOSBuildListener|onStartup|isGitProject|$isGitProject|" +
            "systemVersion|$systemVersion|xcodeVersion|$xcodeVersion")

        if (isGitProject) {
            systemVersion = macVmTypeService.getSystemVersionByVersion(systemVersion)
        }

        var startSuccess: Boolean = false
        var startIp: String = ""
        var startVmId: Int = 0
        val resourceType = "DEVCLOUD"

        val devCloudMacosVmInfo =
            if (isGitProject)
                devCloudMacosService.creatVM(
                    projectId = projectId,
                    pipelineId = dispatchMessage.pipelineId,
                    buildId = dispatchMessage.buildId,
                    vmSeqId = dispatchMessage.vmSeqId,
                    creator = creator,
                    source = "gongfeng",
                    macosVersion = systemVersion,
                    xcodeVersion = xcodeVersion
                )
            else
                devCloudMacosService.creatVM(
                    projectId = projectId,
                    pipelineId = dispatchMessage.pipelineId,
                    buildId = dispatchMessage.buildId,
                    vmSeqId = dispatchMessage.vmSeqId,
                    creator = creator,
                    source = "landun",
                    macosVersion = systemVersion,
                    xcodeVersion = xcodeVersion
                )
        if (devCloudMacosVmInfo != null) {
            devCloudMacosService.saveVM(devCloudMacosVmInfo)
            startSuccess = true
            startIp = devCloudMacosVmInfo.ip
            startVmId = devCloudMacosVmInfo.id
            buildHistoryService.saveBuildHistory(dispatchMessage, startIp, startVmId, resourceType)
            macosVMRedisService.saveRedisBuild(dispatchMessage, startIp)
        }

        if (!startSuccess) {
            // 如果没有找到合适的vm机器，则等待10秒后再执行, 总共执行6次
            try {
                logRed(
                    buildLogPrinter,
                    dispatchMessage.buildId,
                    dispatchMessage.containerHashId,
                    dispatchMessage.vmSeqId,
                    "未找到空闲的macOS构建资源，等待10秒后重试。",
                    dispatchMessage.executeCount
                )
                retry(sleepTimeInMS = 10000, retryTimes = 6)
            } catch (t: BuildFailureException) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.NO_IDLE_MACOS_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_IDLE_MACOS_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.NO_IDLE_MACOS_ERROR.formatErrorMessage,
                    errorMessage = "MacOS资源紧缺，等待1分钟分配不到资源"
                )
            } catch (t: Throwable) {
                throw t
            }

            logger.error("Can not found any idle vm for this build($dispatchMessage),wait for 10s")
            return
        }
        log(
            buildLogPrinter = buildLogPrinter,
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "macOS 资源类型：$resourceType",
            executeCount = dispatchMessage.executeCount
        )
        log(
            buildLogPrinter = buildLogPrinter,
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "macOS 构建机IP：$startIp",
            executeCount = dispatchMessage.executeCount
        )

        logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] " +
                        "Success to start vm($startIp|$startVmId)")
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("[${event.pipelineId}|${event.pipelineId}|${event.buildId}] Build shutdown with event($event)")
        // 如果是某个job关闭，则锁到job，如果是整条流水线shutdown，则锁到buildid级别
        val lockKey =
            if (event.vmSeqId == null)
                "$LOCK_SHUTDOWN:${event.buildId}" else "$LOCK_SHUTDOWN:${event.buildId}:${event.vmSeqId}"
        val redisLock = RedisLock(
            redisOperation,
            lockKey,
            20
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
            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] " +
                            "buildTaskRecords: ${buildTaskRecords.size}")

            val projectId = event.projectId
            val creator = event.userId
            val isGitProject = projectId.startsWith("git_")
            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] " +
                            "Project is or not git project:$isGitProject")

            if (buildTaskRecords.isEmpty()) {
                logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Fail to get the vm ip")
                return
            }

            doShutdown(buildTaskRecords, event, creator, projectId)
        } catch (e: Exception) {
            logger.error("[${event.projectId}|${event.pipelineId}|${event.buildId}] :$e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun doShutdown(buildTaskRecords: Result<TBuildTaskRecord>, event: PipelineAgentShutdownEvent, creator: String, projectId: String) {
        buildTaskRecords.forEach { buildTask ->
            // 关闭的时候对container进行锁操作，防止重复操作
            try {
                val vmIp = buildTask.vmIp
                val vmId = buildTask.vmId
                logger.info(
                    "[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] " +
                        "Get the vm ip($vmIp),vm id($vmId)"
                )
                macosVMRedisService.deleteRedisBuild(vmIp)
                devCloudMacosService.deleteVM(
                    creator = creator,
                    projectId = projectId,
                    pipelineId = buildTask.pipelineId,
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    vmId = vmId
                )
                logger.info("[${event.buildId}]|[${event.vmSeqId}] end build. buildId: ${buildTask.id}")
                buildHistoryService.endBuild(MacJobStatus.Done, buildTask.buildHistoryId, buildTask.id)
            } catch (e: Exception) {
                val vmIp = buildTask.vmIp
                logger.error(
                    "[${event.projectId}|${event.pipelineId}|${event.buildId}] shutdown error,vm is $vmIp",
                    e
                )

                if (e is SocketTimeoutException) {
                    logger.error(
                        "[${event.projectId}|${event.pipelineId}|${event.buildId}] " +
                            "vm is $vmIp, end build."
                    )
                    buildHistoryService.endBuild(
                        MacJobStatus.ShutDownError,
                        buildTask.buildHistoryId,
                        buildTask.id
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MacBuildListener::class.java)
        private const val LOCK_SHUTDOWN = "dispatcher:locker:macos:shutdown"
    }
}
