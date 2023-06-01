package com.tencent.devops.dispatch.windows.listener

import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.windows.constant.ErrorCodeEnum
import com.tencent.devops.dispatch.windows.enums.WindowsJobStatus
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_LANDUN_ENV
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.windows.service.DevCloudWindowsService
import com.tencent.devops.dispatch.windows.service.WindowsBuildHistoryService
import com.tencent.devops.model.dispatch.windows.tables.records.TBuildHistoryRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import java.net.SocketTimeoutException
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WindowsBuildListener @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val devCloudWindowsService: DevCloudWindowsService,
    private val windowsBuildHistoryService: WindowsBuildHistoryService,
    private val profile: Profile,
    private val redisOperation: RedisOperation
) : BuildListener {
    companion object {
        private val logger = LoggerFactory.getLogger(WindowsBuildListener::class.java)
        private const val LOCK_SHUTDOWN = "dispatcher:locker:windows:shutdown"
        // 未找到空闲的windows构建资源，等待20秒后重试。
        const val BK_NO_FREE_WINDOWS_BUILD_RESOURCE = "bkNoFreeWindowsBuildResource"
        // Windows资源紧缺，等待1分钟分配不到资源
        const val BK_WINDOWS_RESOURCE_SCARCITY = "bkWindowsResourceScarcity"
    }

    override fun getShutdownQueue(): String {
        return ".windows"
    }

    override fun getStartupDemoteQueue(): String {
        return ".windows.demote"
    }

    override fun getStartupQueue(): String {
        return ".windows"
    }

    override fun getVmType(): JobQuotaVmType? {
        return JobQuotaVmType.WINDOWS_DEVCLOUD
    }

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("Windows Dispatch on start up - ($dispatchMessage)")
        val projectId = dispatchMessage.projectId
        val creator = dispatchMessage.userId

        val isGitProject = projectId.startsWith("git_")
        logger.info("Project is or not git project:$isGitProject")

        var startSuccess = false
        val resourceType = if (isGitProject) {
            "DEVCLOUD"
        } else {
            "BKDEVOPS"
        }
        val devCloudWindowsInfo = devCloudWindowsService.getWindowsMachine(
            os = dispatchMessage.dispatchType?.value,
            projectId = projectId,
            pipelineId = dispatchMessage.pipelineId,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            creator = creator,
            env = generateEnvs(dispatchMessage)
        )

        if (devCloudWindowsInfo != null) {
            startSuccess = true
            windowsBuildHistoryService.saveBuildHistory(dispatchMessage, devCloudWindowsInfo, resourceType)
        }

        if (!startSuccess) {
            // 如果没有找到合适的vm机器，则等待10秒后再执行, 总共执行6次
            try {
                logRed(
                    buildLogPrinter,
                    dispatchMessage.buildId,
                    dispatchMessage.containerHashId,
                    dispatchMessage.vmSeqId,
                    I18nUtil.getCodeLanMessage(BK_NO_FREE_WINDOWS_BUILD_RESOURCE),
                    dispatchMessage.executeCount
                )
                retry(sleepTimeInMS = 20000, retryTimes = 3)
            } catch (e: BuildFailureException) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.getErrorMessage(),
                    errorMessage = I18nUtil.getCodeLanMessage(BK_WINDOWS_RESOURCE_SCARCITY)
                )
            } catch (e: Throwable) {
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.getErrorMessage(),
                    errorMessage = "Windows failed to start"
                )
            }

            logger.error("Can not found any idle vm for this build($dispatchMessage),wait for 10s")
            return
        }
        log(
            buildLogPrinter = buildLogPrinter,
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "Windows resource type：$resourceType",
            executeCount = dispatchMessage.executeCount
        )
        log(
            buildLogPrinter = buildLogPrinter,
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "Windows build machine IP：${devCloudWindowsInfo?.ip}",
            executeCount = dispatchMessage.executeCount
        )

        logger.info(
            "[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}" +
                "|${dispatchMessage.buildId}] Success to start vm(${devCloudWindowsInfo?.ip})"
        )
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("[${event.pipelineId}|${event.pipelineId}|${event.buildId}] Build shutdown with event($event)")
        // 锁到buildid级别
        val lockKey = "$LOCK_SHUTDOWN:${event.buildId}"
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = lockKey,
            expiredTimeInSeconds = 20
        )

        try {
            if (!redisLock.tryLock()) {
                logger.info("shut down lock($lockKey) fail")
                Thread.sleep(100)
                return
            }

            val buildHistoryRecords = windowsBuildHistoryService.getByBuildIdAndVmSeqId(
                buildId = event.buildId,
                vmSeqId = event.vmSeqId
            )
            logger.info(
                "${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}" +
                    "|buildHistoryRecords|$buildHistoryRecords")

            val projectId = event.projectId
            val creator = event.userId
            val isGitProject = projectId.startsWith("git_")
            logger.info(
                "${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}" +
                    "|Project is or not git project:$isGitProject"
            )

            if (buildHistoryRecords.isNullOrEmpty()) {
                logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Fail to get the vm ip")
                return
            }

            doShutdown(buildHistoryRecords, event, creator)
        } catch (e: Exception) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] :$e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun doShutdown(
        buildHistoryRecords: Result<TBuildHistoryRecord>,
        event: PipelineAgentShutdownEvent,
        creator: String
    ) {
        buildHistoryRecords.forEach { buildHistory ->
            // 关闭的时候对container进行锁操作，防止重复操作
            try {
                if (buildHistory.status == WindowsJobStatus.Done.name) {
                    return@forEach
                }
                val vmIp = buildHistory.vmIp
                logger.info(
                    "${event.projectId}|${event.pipelineId}|${event.buildId}" +
                        "|${event.vmSeqId}|Get the vm ip($vmIp))"
                )
                devCloudWindowsService.deleteWindowsMachine(
                    creator = creator,
                    taskGuid = buildHistory.taskGuid
                )

                logger.info("${event.buildId}|${event.vmSeqId}|end build|buildId|${buildHistory.id}")
                windowsBuildHistoryService.endBuild(WindowsJobStatus.Done, buildHistory.id)
            } catch (e: SocketTimeoutException) {
                logger.error(
                    "${event.projectId}|${event.pipelineId}|${event.buildId}" +
                        "|vm is ${buildHistory.vmIp}, end build.", e
                )
                windowsBuildHistoryService.endBuild(
                    WindowsJobStatus.ShutDownError,
                    buildHistory.id
                )
            } catch (e: Throwable) {
                logger.error(
                    "[${event.projectId}|${event.pipelineId}|${event.buildId}] " +
                        "shutdown error,vm is ${buildHistory.vmIp}",
                    e
                )
            }
        }
    }

    private fun generateEnvs(dispatchMessage: DispatchMessage): Map<String, Any> {
        // 拼接环境变量
        with(dispatchMessage) {
            val envs = mutableMapOf<String, Any>()
            if (customBuildEnv != null) {
                envs.putAll(customBuildEnv!!)
            }
            val landunEnv = when {
                profile.isDev() -> "dev"
                profile.isTest() -> "test"
                else -> "prod"
            }
            envs.putAll(
                mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    ENV_KEY_AGENT_ID to id,
                    ENV_KEY_AGENT_SECRET_KEY to secretKey,
                    ENV_KEY_GATEWAY to gateway,
                    ENV_KEY_LANDUN_ENV to landunEnv
                )
            )

            return envs
        }
    }
}
