package com.tencent.devops.dispatch.macos.listener

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.dispatch.macos.enums.MacVMStatus
import com.tencent.devops.dispatcher.macos.service.MacVmService
import com.tencent.devops.dispatch.macos.service.MacVmTypeService
import com.tencent.devops.model.dispatch.macos.tables.records.TVirtualMachineRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.macos.constant.DispatcherMacOSMessageCode
import com.tencent.devops.dispatch.macos.constant.ErrorCodeEnum
import com.tencent.devops.dispatch.macos.service.BuildHistoryService
import com.tencent.devops.dispatch.macos.service.BuildTaskService
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import com.tencent.devops.dispatch.macos.service.MacosVMRedisService
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date

@Component
class MacOSBuildListener @Autowired constructor(
    private val client: Client,
    private val macVmService: MacVmService,
    private val buildHistoryService: BuildHistoryService,
    private val macVmTypeService: MacVmTypeService,
    private val buildTaskService: BuildTaskService,
    private val redisOperation: RedisOperation,
    private val profile: Profile,
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

        val isdevCloudProjectGray = devCloudMacosService.isDevCloudGrayProject(projectId)

        var startSuccess: Boolean = false
        var startIp: String = ""
        var startVmId: Int = 0
        var resourceType = ""
        if (isdevCloudProjectGray || isGitProject) {
            resourceType = "DEVCLOUD"

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
        } else {

            var resourceType = "BKDEVOPS"
            // 寻找合适的vm类型列表
            var vmTypeList = macVmTypeService.searchVmTypes(
                systemVersion = systemVersion,
                xcodeVersion = xcodeVersion
            )
            // 当没有合适的vm类型时，选择合适的xcode版本
            if (vmTypeList == null) {
                vmTypeList = macVmTypeService.searchVmTypes(
                    systemVersion = null,
                    xcodeVersion = xcodeVersion
                )
            }
            // 当没有合适的vm类型时，选择合适的系统版本
            if (vmTypeList == null) {
                vmTypeList = macVmTypeService.searchVmTypes(
                    systemVersion = systemVersion,
                    xcodeVersion = null
                )
            }
            // 当没有任何合适的vm类型时，找出最早的空闲机器
            if (vmTypeList == null) {
                vmTypeList = macVmTypeService.searchVmTypes(
                    systemVersion = null,
                    xcodeVersion = null
                )
            }

            logger.info("vmTypeList: ($vmTypeList)")
            val vmTypeIdList = if (vmTypeList != null && vmTypeList.isNotEmpty()) {
                vmTypeList.map { vmType ->
                    vmType.id
                }
            } else {
                null
            }
            logger.info("vmTypeIdList: ($vmTypeIdList)")
            // 寻找合适的vm列表
            val vmInfoList = macVmService.searchVMs(MacVMStatus.Idle, vmTypeIdList)
            logger.info("vmInfoList: ($vmInfoList)")
            val vmInfoIdList = if (vmInfoList.isNotEmpty()) {
                vmInfoList.map { vmInfo ->
                    vmInfo.id
                }
            } else {
                null
            }
            logger.info("vmInfoIdList: ($vmInfoIdList)")

            // 启动的时候需要锁住build的shutdown锁，防止和关闭后再启动
            val lockKey = "$LOCK_SHUTDOWN:${dispatchMessage.buildId}"
            val redisLock = RedisLock(
                redisOperation,
                lockKey,
                20
            )
            var vm: TVirtualMachineRecord?
            try {
                redisLock.lock()
                // 判断流水线是否还在运行，如果已经停止则不在运行
                // 只有detail的信息是在shutdown事件发出之前就写入的，所以这里去builddetail的信息。
                // 为了兼容gitci的权限，这里把渠道号都改成GIT,以便去掉用户权限验证
                val record = client.get(ServiceBuildResource::class).getBuildDetailStatusWithoutPermission(
                    dispatchMessage.userId,
                    dispatchMessage.projectId,
                    dispatchMessage.pipelineId,
                    dispatchMessage.buildId,
                    ChannelCode.BS
                )
                if (record.isNotOk() || record.data == null) {
                    logger.warn("The build($dispatchMessage) fail to check if pipeline is running because of ${record.message}")
                    throw BuildFailureException(ErrorType.SYSTEM, DispatcherMacOSMessageCode.ERROR_NO_PIPELINE, "无法获取流水线状态", "无法获取流水线状态")
                }
                val status = BuildStatus.parse(record.data)
                if (!BuildStatus.isRunning(status)) {
                    logger.warn("The build($dispatchMessage) is not running")
                    throw BuildFailureException(ErrorType.SYSTEM, DispatcherMacOSMessageCode.ERROR_PIPELINE_NOT_RUNNING, "流水线已经不再运行", "无法获取流水线状态")
                }
                vm = macVmService.startVMFromIdleList(dispatchMessage, vmInfoIdList)
            } catch (e: Throwable) {
                logger.error(e.toString())
                throw e
            } finally {
                redisLock.unlock()
            }
            if (vm != null) {
                startIp = vm.ip
                startVmId = vm.id
                startSuccess = true
            }
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
                onAlert(dispatchMessage, "MacOS资源紧缺，等待1分钟分配不到资源", resourceType)
                throw BuildFailureException(
                    errorType = ErrorCodeEnum.NO_IDLE_MACOS_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_IDLE_MACOS_ERROR.errorCode,
                    formatErrorMessage = ErrorCodeEnum.NO_IDLE_MACOS_ERROR.formatErrorMessage,
                    errorMessage = "MacOS资源紧缺，等待1分钟分配不到资源"
                )
            } catch (t: Throwable) {
                onAlert(dispatchMessage, "MacOS启动失败 - (${t.message})", resourceType)
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

        logger.info("[${dispatchMessage.projectId}|${dispatchMessage.pipelineId}|${dispatchMessage.buildId}] Success to start vm($startIp|$startVmId)")
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        onStartup(dispatchMessage)
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("[${event.pipelineId}|${event.pipelineId}|${event.buildId}] Build shutdown with event($event)")
        // 如果是某个job关闭，则锁到job，如果是整条流水线shutdown，则锁到buildid级别
        val lockKey =
            if (event.vmSeqId == null) "$LOCK_SHUTDOWN:${event.buildId}" else "$LOCK_SHUTDOWN:${event.buildId}:${event.vmSeqId}"
        val redisLock = RedisLock(
            redisOperation,
            lockKey,
            20
        )
        try {
            if (redisLock.tryLock()) {
                val buildTaskRecords = buildTaskService.getByBuildIdAndVmSeqId(
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    executeCount = event.executeCount
                )
                logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] buildTaskRecords: ${buildTaskRecords.size}")

                val projectId = event.projectId
                val creator = event.userId
                val isdevCloudProjectGray = devCloudMacosService.isDevCloudGrayProject(projectId)
                val isGitProject = projectId.startsWith("git_")
                logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] Project is or not git project:$isGitProject")

                if (buildTaskRecords.isNotEmpty) {
                    buildTaskRecords.forEach { buildTask ->
                        // 关闭的时候对container进行锁操作，防止重复操作
                        try {
                            val vmIp = buildTask.vmIp
                            val vmId = buildTask.vmId
                            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.vmSeqId}] Get the vm ip($vmIp),vm id($vmId)")
                            macosVMRedisService.deleteRedisBuild(vmIp)
                            if ((isdevCloudProjectGray || isGitProject) && buildTask.resourceType == "DEVCLOUD") {
                                devCloudMacosService.deleteVM(
                                    creator = creator,
                                    projectId = projectId,
                                    pipelineId = buildTask.pipelineId,
                                    buildId = buildTask.buildId,
                                    vmSeqId = buildTask.vmSeqId,
                                    vmId = vmId

                                )
                            } else {
                                macVmService.reStartVm(vmIp)
                            }
                            logger.info("[${event.buildId}]|[${event.vmSeqId}] end build. buildId: ${buildTask.id}")
                            buildHistoryService.endBuild(MacJobStatus.Done, buildTask.buildHistoryId, buildTask.id)
                        } catch (e: Exception) {
                            val vmIp = buildTask.vmIp
                            logger.error(
                                "[${event.projectId}|${event.pipelineId}|${event.buildId}] shutdown error,vm is $vmIp",
                                e
                            )

                            if (e is SocketTimeoutException) {
                                logger.error("[${event.projectId}|${event.pipelineId}|${event.buildId}] vm is $vmIp, end build.")
                                buildHistoryService.endBuild(
                                    MacJobStatus.ShutDownError,
                                    buildTask.buildHistoryId,
                                    buildTask.id
                                )
                            }
                        }
                    }
                } else {
                    logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Fail to get the vm ip")
                }
            }
        } catch (e: Exception) {
            logger.error("[${event.projectId}|${event.pipelineId}|${event.buildId}] :$e")
        } finally {
            redisLock.unlock()
        }
    }

    @Synchronized
    fun setAlertUsers(users: Set<String>) {
        logger.info("Set alert users: $users")
        users.forEach {
            redisOperation.addSetValue(MACOS_DISPATCHER_ALERT_USER_CACHE, it)
        }
        alertUserLastUpdate = 0
    }

    @Synchronized
    fun rmAlertUsers(users: Set<String>) {
        logger.info("RM alert users: $users")
        users.forEach {
            redisOperation.removeSetMember(MACOS_DISPATCHER_ALERT_USER_CACHE, it)
        }
        alertUserLastUpdate = 0
    }

    @Synchronized
    fun getAlertUsers(): Set<String> {
        if (expire()) {
            alertUsers.clear()
            alertUsers.addAll(redisOperation.getSetMembers(MACOS_DISPATCHER_ALERT_USER_CACHE) ?: emptySet())
            alertUserLastUpdate = System.currentTimeMillis()
        }
        return alertUsers
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
            "detailUrl" to "http://${getGateway()}/console/pipeline/${dispatchMessage.projectId}/${dispatchMessage.pipelineId}/detail/${dispatchMessage.buildId}"
        )
        super.onAlert(getAlertUsers(), "MacOS构建机启动失败", parseMessageTemplate(getEmailBody(), map))
    }

    private val alertUsers = HashSet<String>()
    private var alertUserLastUpdate = 0L

    private fun expire(): Boolean {
        return System.currentTimeMillis() - alertUserLastUpdate >= (ALERT_CACHE_EXPIRE * 1000)
    }

    private fun getGateway(): String {
        return when {
            profile.isDev() -> "dev.devops.oa.com"
            profile.isTest() -> "test.devops.oa.com"
            else -> "devops.oa.com"
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

    companion object {
        private val logger = LoggerFactory.getLogger(MacOSBuildListener::class.java)
        private const val MACOS_DISPATCHER_ALERT_USER_CACHE = "dispatcher:macos:alert:user:cache"
        private const val ALERT_CACHE_EXPIRE = 30 // expire in 30 seconds
        private const val LOCK_SHUTDOWN = "dispatcher:locker:macos:shutdown"
    }
}
