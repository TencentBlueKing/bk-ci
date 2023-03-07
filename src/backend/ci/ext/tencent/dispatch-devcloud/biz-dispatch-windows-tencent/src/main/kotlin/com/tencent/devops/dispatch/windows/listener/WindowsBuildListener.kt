package com.tencent.devops.dispatch.windows.listener

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.windows.constant.ErrorCodeEnum
import com.tencent.devops.dispatch.windows.enums.WindowsJobStatus
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_LANDUN_ENV
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.windows.service.WindowsBuildHistoryService
import com.tencent.devops.dispatch.windows.service.DevCloudWindowsService
import com.tencent.devops.dispatch.windows.service.WindowsTypeService
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date

@Component
class WindowsBuildListener @Autowired constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val devCloudWindowsService: DevCloudWindowsService,
    private val windowsTypeService: WindowsTypeService,
    private val windowsBuildHistoryService: WindowsBuildHistoryService,
    private val profile: Profile,
    private val redisOperation: RedisOperation
) : BuildListener {
    companion object {
        private val logger = LoggerFactory.getLogger(WindowsBuildListener::class.java)
        private const val WINDOWS_DISPATCHER_ALERT_USER_CACHE = "dispatcher:windows:alert:user:cache"
        private const val ALERT_CACHE_EXPIRE = 30 // expire in 30 seconds
        private const val LOCK_SHUTDOWN = "dispatcher:locker:windows:shutdown"
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
                    "未找到空闲的windows构建资源，等待20秒后重试。",
                    dispatchMessage.executeCount
                )
                retry(sleepTimeInMS = 20000, retryTimes = 3)
            } catch (e: BuildFailureException) {
                onAlert(dispatchMessage, "Windows资源紧缺，等待1分钟分配不到资源", resourceType)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.NO_IDLE_WINDOWS_ERROR.formatErrorMessage,
                    errorMessage = "Windows资源紧缺，等待1分钟分配不到资源"
                )
            } catch (e: Throwable) {
                onAlert(dispatchMessage, "Windows启动失败 - (${e.message})", resourceType)
                throw e
            }

            logger.error("Can not found any idle vm for this build($dispatchMessage),wait for 10s")
            return
        }
        log(
            buildLogPrinter = buildLogPrinter,
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "Windows资源类型：$resourceType",
            executeCount = dispatchMessage.executeCount
        )
        log(
            buildLogPrinter = buildLogPrinter,
            buildId = dispatchMessage.buildId,
            containerHashId = dispatchMessage.containerHashId,
            vmSeqId = dispatchMessage.vmSeqId,
            message = "Windows构建机IP：${devCloudWindowsInfo?.ip}",
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
            redisLock.use {
                if (!redisLock.tryLock()) {
                    logger.info("shut down lock($lockKey) fail")
                    Thread.sleep(100)
                    return@use
                }

                val buildHistoryRecords = windowsBuildHistoryService.getByBuildIdAndVmSeqId(
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId
                )
                logger.info(
                    "${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}" +
                        "|buildHistoryRecords|${buildHistoryRecords}"
                )

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
        } catch (e: Exception) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] :$e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun onAlert(
        dispatchMessage: DispatchMessage,
        error: String,
        resourceType: String = "BKDEVOPS"
    ) {
        val map = mapOf(
            "projectId" to "${dispatchMessage.projectId}  ($resourceType)",
            "username" to dispatchMessage.userId,
            "error" to error,
            "detailUrl" to "http://${getGateway()}/console/pipeline/${dispatchMessage.projectId}" +
                "/${dispatchMessage.pipelineId}/detail/${dispatchMessage.buildId}"
        )
        super.onAlert(getAlertUsers(), "Windows构建机启动失败", parseMessageTemplate(getEmailBody(), map))
    }

    private fun getGateway(): String {
        return when {
            profile.isDev() -> "dev.devops.oa.com"
            profile.isTest() -> "test.devops.oa.com"
            else -> "devops.oa.com"
        }
    }

    private val alertUsers = HashSet<String>()
    private var alertUserLastUpdate = 0L

    @Synchronized
    fun getAlertUsers(): Set<String> {
        if (expire()) {
            alertUsers.clear()
            alertUsers.addAll(redisOperation.getSetMembers(WINDOWS_DISPATCHER_ALERT_USER_CACHE) ?: emptySet())
            alertUserLastUpdate = System.currentTimeMillis()
        }
        return alertUsers
    }

    private fun expire(): Boolean {
        return System.currentTimeMillis() - alertUserLastUpdate >= (ALERT_CACHE_EXPIRE * 1000)
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

    private fun getEmailBody(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = simpleDateFormat.format(Date())
        return "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
            "\t<tbody>\n" +
            "\t\t<tr>\n" +
            "\t\t\t<td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\n" +
            "\t\t\t   <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t<tbody>\n" +
            "\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t<td valign=\"top\" align=\"center\">\n" +
            "\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f9f8f6\" class=\"layout layout-table root-table\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t<tbody>\n" +
            "\t\t\t\t\t\t\t\t\t\t<tr style=\"height: 64px; background: #555;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left: 24px;\" width=\"60\" align=\"center\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<img src=\"http://file.tapd.oa.com//tfl/pictures/201807/tapd_20363462_1531467552_72.png\" width=\"52\" style=\"display: block\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left: 6px;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<img src=\"http://file.tapd.oa.com//tfl/pictures/201807/tapd_20363462_1531467605_41.png\" width=\"176\" style=\"display: block\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t</tbody>\n" +
            "\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t<td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\n" +
            "\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\n" +
            "\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t<td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">【MACOS公共构建机(NEW)启动失败告警通知】</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t<td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自BKDevOps/蓝盾DevOps平台的通知推送</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"email-information\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td class=\"table-info\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"table-title\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\"></td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<thead style=\"background: #f6f8f8;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr style=\"color: #333C48;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">所属项目</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"20%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">启动人</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"35%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\">错误信息</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<th width=\"25%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\">流水线</th>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</thead>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tbody style=\"color: #707070;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{projectId}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{username}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">#{error}</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\"><a href=\"#{detailUrl}\" style=\"color: #3c96ff\">流水线</a></td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tbody>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<!-- 空数据 -->\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<!-- <tr class=\"no-data\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 40px; color: #707070;\">敬请期待！</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr> -->\n" +
            "\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"prompt-tips\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何问题，可随时联系蓝盾助手。</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t<tr class=\"info-remark\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t<div>$date</div>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t\t<tr class=\"email-footer\">\n" +
            "\t\t\t\t\t\t\t\t\t\t<td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你是MacOS构建机管理负责人</td>\n" +
            "\t\t\t\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t\t\t\t</table>\n" +
            "\t\t\t\t\t\t\t</td>\n" +
            "\t\t\t\t\t\t</tr>\n" +
            "\t\t\t\t\t</tbody>\n" +
            "\t\t\t   </table>\n" +
            "\t\t\t</td>\n" +
            "\t\t</tr>\n" +
            "\t</tbody>\n" +
            "</table>\n"
    }

}
