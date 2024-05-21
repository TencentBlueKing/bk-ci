package com.tencent.devops.remotedev.resources.service

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.async.AsyncPipelineEvent
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.windows.QuotaInApiRes
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.resources.op.OpProjectWorkspaceResourceImpl
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.StartWorkspaceService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceLoginService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.net.URLDecoder
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl(
    private val permissionService: PermissionService,
    private val workspaceService: WorkspaceService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val createControl: CreateControl,
    private val deleteControl: DeleteControl,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val notifyControl: NotifyControl,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val workspaceLoginService: WorkspaceLoginService,
    private val startWorkspaceService: StartWorkspaceService,
    private val rabbitTemplate: RabbitTemplate,
    private val expertSupportService: ExpertSupportService,
    private val whiteListService: WhiteListService
) : ServiceRemoteDevResource {
    private val executor = Executors.newCachedThreadPool()

    companion object {
        private val logger = LoggerFactory.getLogger(OpProjectWorkspaceResourceImpl::class.java)
        private const val PIPELINE_CONFIG_INFO = "remotedev:assignWorkspace.pipelineinfo"
    }

    override fun validateUserTicket(userId: String, isOffshore: Boolean, ticket: String): Result<Boolean> {
        val data = permissionService.checkAndGetUser1Password(URLDecoder.decode(ticket, "UTF-8"))
        val result = (data.userId == userId)
        if (!result) {
            return Result(false)
        }
        try {
            workspaceLoginService.addUserLogin(data.userId, data.workspaceName)
        } catch (e: Exception) {
            logger.error("validateUserTicket error", e)
        }
        return Result(true)
    }

    override fun getProjectWorkspace(
        projectId: String?,
        ip: String?,
        businessLineName: String?,
        ownerName: String?
    ): Result<List<WeSecProjectWorkspace>> {
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                projectId = projectId,
                ip = ip,
                businessLineName = businessLineName,
                ownerName = ownerName,
                hasDepartmentsInfo = null,
                hasCurrentUser = true
            )
        )
    }

    override fun getProjectWorkspaceIp(ip: String): Result<WeSecProjectWorkspace?> {
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                projectId = null,
                ip = ip,
                hasDepartmentsInfo = true,
                hasCurrentUser = true
            ).randomOrNull()
        )
    }

    override fun getRemotedevProjects(projectId: String?): Result<List<RemotedevProject>> {
        return Result(workspaceService.getWorkspaceProject(projectId))
    }

    override fun queryProjectRemoteDevCvm(projectId: String?): Result<List<RemotedevCvmData>> {
        return Result(workspaceService.getRemotedevCvm(projectId))
    }

    override fun checkWorkspaceProject(projectId: String, ip: String): Result<Boolean> {
        return Result(desktopWorkspaceService.checkWorkspaceProject(projectId, ip))
    }

    override fun checkUserIpPermission(user: String, ip: String): Result<Boolean> {
        return Result(desktopWorkspaceService.checkUserIpPermission(user, ip))
    }

    override fun createWinWorkspaceByVm(
        userId: String,
        oldWorkspaceName: String?,
        projectId: String?,
        ownerType: WorkspaceOwnerType?,
        uid: String
    ): Result<Boolean> {
        val res = createControl.createWinWorkspaceByVm(
            userId = userId,
            oldWorkspaceName = oldWorkspaceName,
            projectCode = projectId,
            ownerType = ownerType,
            uid = uid
        )
        return Result(res)
    }

    override fun assignWorkspace(
        operator: String,
        owner: String?,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        val projectId = checkNotNull(data.projectId)
        workspaceCommon.syncStartCloudResourceList()
        val cgsData = workspaceCommon.getCgsData(data.cgsIds, data.ips) ?: return Result(false)
        // 增加可以分配的配额
        if (!data.ips.isNullOrEmpty() || !data.cgsIds.isNullOrEmpty()) {
            client.get(ServiceTxProjectResource::class).updateRemotedev(
                userId = operator,
                projectCode = projectId,
                addcloudDesktopNum = (data.ips?.size ?: 0) + (data.cgsIds?.size ?: 0),
                enable = null
            )
        }
        cgsData.forEach { cgs ->
            if (cgs.status != Constansts.CGS_AVAIABLE_STATUS) return@forEach
            // 先校验该cgsId是否已被申领分配并运行中
            if (workspaceCommon.checkCgsRunning(cgs.cgsId)) return@forEach
            // 审计
            ActionAuditContext.current()
                .addInstanceInfo(
                    cgs.cgsId,
                    cgs.cgsId,
                    null,
                    null
                )
                .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, data.projectId)
                .scopeId = data.projectId
            // 再根据机型和地域获取硬件资源配置
            val windowsResourceConfigId = windowsResourceConfigService.getTypeConfig(
                machineType = cgs.machineType
            ) ?: return Result(false)
            // 调用CreateControl.asyncCreateWorkspace发起创建
            createControl.projectCreateWorkspace(
                pmUserId = owner ?: operator,
                projectId = projectId,
                cgsId = cgs.cgsId,
                workspaceCreate = WindowsWorkspaceCreate(
                    windowsType = windowsResourceConfigId.size,
                    windowsZone = cgs.zoneId.replace(Regex("\\d+"), ""),
                    baseImageId = 0,
                    count = 1,
                    assignOwners = owner?.let { listOf(owner) } ?: emptyList()
                )
            )
            Thread.sleep(500)
        }
        // 启动流水线完成剩下的分配工作
        if (data.repoId.isNullOrBlank() || data.localDriver.isNullOrBlank()) {
            return Result(true)
        }
        executor.execute {
            try {
                val infoS = redisOperation.get(PIPELINE_CONFIG_INFO) ?: return@execute
                val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)

                val cgsIps = data.cgsIds?.map {
                    val hostIdSub = it.split(".")
                    hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")
                }?.toSet()
                val resIps = mutableSetOf<String>()
                resIps.addAll(cgsIps ?: emptySet())
                resIps.addAll(data.ips ?: emptySet())

                val newParam = mutableMapOf<String, String>()
                info.buildParam.forEach { (k, v) ->
                    when (v) {
                        "job_ip_list" -> newParam[k] = resIps.joinToString(separator = " ")
                        "repoId" -> newParam[k] = data.repoId ?: ""
                        "localDriver" -> newParam[k] = data.localDriver ?: ""
                        else -> newParam[k] = v
                    }
                }
                client.get(ServiceBuildResource::class).manualStartupNew(
                    userId = info.userId ?: operator,
                    projectId = info.projectId,
                    pipelineId = info.pipelineId,
                    values = newParam,
                    channelCode = ChannelCode.BS,
                    buildNo = null,
                    startType = StartType.SERVICE
                )
            } catch (e: Exception) {
                logger.warn("execute assignWorkspace pipeline error", e)
            }
        }
        return Result(true)
    }

    override fun notifyWorkspaceInfo(operator: String, notifyData: WorkspaceNotifyData): Result<Boolean> {
        notifyControl.notifyWorkspaceInfo(
            userId = operator,
            notifyData = notifyData
        )
        return Result(true)
    }

    override fun getWindowsResourceList(): Result<List<WindowsResourceTypeConfig>> {
        return Result(windowsResourceConfigService.getAllType(true, null))
    }

    override fun createPersonalWorkspace(userId: String, data: WindowsWorkspaceCreate): Result<Boolean> {
        return Result(createControl.devcloudCreateWorkspace(userId = userId, workspaceCreate = data, projectId = null))
    }

    override fun deletePersonalWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        val record = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (record == null || record.ownerType != WorkspaceOwnerType.PERSONAL) {
            logger.warn("delete personal workspace with invalid workspace type: $userId|$workspaceName")
            return Result(false)
        }
        return Result(
            deleteControl.deleteWorkspace(
                userId = userId,
                workspaceName = workspaceName,
                needPermission = true,
                checkDeleteImmediately = true
            )
        )
    }

    override fun getPersonalWorkspace(userId: String, workspaceName: String): Result<WeSecProjectWorkspace?> {
        val record = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (record == null || record.ownerType != WorkspaceOwnerType.PERSONAL) {
            logger.warn("get personal workspace with invalid workspace type: $userId|$workspaceName")
            return Result(null)
        }
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                workspaceName = workspaceName,
                notStatus = null
            ).firstOrNull()
        )
    }

    override fun createProjectWorkspace(
        userId: String,
        projectId: String,
        data: WindowsWorkspaceCreate
    ): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        return Result(
            createControl.devcloudCreateWorkspace(userId = userId, workspaceCreate = data, projectId = projectId)
        )
    }

    override fun deleteProjectWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        val record = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (record == null || record.ownerType != WorkspaceOwnerType.PROJECT || record.projectId != projectId) {
            logger.warn("delete project workspace with invalid workspace type: $userId|$projectId|$workspaceName")
            return Result(false)
        }

        return Result(
            deleteControl.deleteWorkspace(
                userId = userId,
                workspaceName = workspaceName,
                needPermission = !permissionService.hasUserManager(userId, projectId),
                checkDeleteImmediately = true
            )
        )
    }

    override fun getProjectWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String
    ): Result<WeSecProjectWorkspace?> {
        val record = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (record == null || record.ownerType != WorkspaceOwnerType.PROJECT || record.projectId != projectId) {
            logger.warn("get project workspace with invalid workspace type: $userId|$projectId|$workspaceName")
            return Result(null)
        }
        permissionService.checkViewerPermission(userId, workspaceName, projectId)
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                workspaceName = workspaceName,
                notStatus = null
            ).firstOrNull()
        )
    }

    override fun getWindowsQuota(userId: String, type: QuotaType): Result<Map<String, Map<String, Int>>> {
        return Result(windowsResourceConfigService.allWindowsQuota(userId, false, type))
    }

    override fun updateUsageLimit(
        userId: String,
        projectId: String?,
        machineType: String?,
        count: Int,
        available: Boolean?
    ): Result<QuotaInApiRes> {
        val mix = if (available == true) {
            val using = workspaceService.getWorkspaceList4WeSec(
                projectId = projectId,
                notStatus = listOf(WorkspaceStatus.DELETED),
                ownerName = if (projectId == null) userId else null
            )
            using.associate {
                it.machineType to using.count { c -> c.machineType == it.machineType }
            }
        } else null
        logger.info("update usage limit for $userId|$projectId|$machineType|$count|$mix")
        val spec = windowsResourceConfigService.getAllType(withUnavailable = true, onlySpecModel = true)
            .associate { it.size to 0 }
        val res = when {
            machineType != null -> {
                checkNotNull(projectId)
                QuotaInApiRes(
                    project = windowsResourceConfigService.updateAndGetProjectTotalQuota(
                        userId = userId,
                        projectId = projectId,
                        quota = 0
                    ),
                    quotas = spec.plus(
                        windowsResourceConfigService.updateAndGetAllSpec(
                            projectId = projectId,
                            machineType = machineType,
                            count = count
                        )
                    )
                )
            }

            projectId != null -> {
                QuotaInApiRes(
                    project = windowsResourceConfigService.updateAndGetProjectTotalQuota(
                        userId = userId,
                        projectId = projectId,
                        quota = count
                    ),
                    quotas = spec.plus(
                        windowsResourceConfigService.updateAndGetAllSpec(
                            projectId = projectId,
                            machineType = null,
                            count = 0
                        )
                    )
                )
            }

            else -> {
                QuotaInApiRes(user = whiteListService.updateAndGetWindowsLimit(userId, count))
            }
        }
        // 对mix做计算
        return Result(
            res.copy(
                user = res.user?.let { it - (mix?.values?.sum() ?: 0) },
                project = res.project?.let { it - (mix?.values?.sum() ?: 0) },
                quotas = res.quotas?.mapValues { it.value - (mix?.get(it.key) ?: 0) }
            )
        )
    }
}
