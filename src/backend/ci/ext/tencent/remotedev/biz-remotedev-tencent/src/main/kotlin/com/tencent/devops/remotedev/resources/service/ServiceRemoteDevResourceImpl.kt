package com.tencent.devops.remotedev.resources.service

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.pojo.OperateCvmData
import com.tencent.devops.remotedev.pojo.OperateCvmDataType
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.async.AsyncNotify
import com.tencent.devops.remotedev.pojo.async.AsyncPipelineEvent
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
import com.tencent.devops.remotedev.pojo.windows.QuotaInApiRes
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.resources.op.OpProjectWorkspaceResourceImpl
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.StartWorkspaceService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceLoginService
import com.tencent.devops.remotedev.service.WorkspaceRecordService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.devcloud.DevcloudService
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.projectworkspace.MakeWorkspaceImageHandler
import com.tencent.devops.remotedev.service.projectworkspace.RebuildWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.RestartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StopWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.UpgradeWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.image.ImageManageService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.net.URLDecoder
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge

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
    private val streamBridge: StreamBridge,
    private val expertSupportService: ExpertSupportService,
    private val devcloudService: DevcloudService,
    private val deliverControl: DeliverControl,
    private val imageManageService: ImageManageService,
    private val whiteListService: WhiteListService,
    private val tGitService: GitProxyTGitService,
    private val rebuildWorkspaceHandler: RebuildWorkspaceHandler,
    private val startWorkspaceHandler: StartWorkspaceHandler,
    private val stopWorkspaceHandler: StopWorkspaceHandler,
    private val restartWorkspaceHandler: RestartWorkspaceHandler,
    private val makeWorkspaceImageHandler: MakeWorkspaceImageHandler,
    private val workspaceRecordService: WorkspaceRecordService,
    private val upgradeWorkspaceHandler: UpgradeWorkspaceHandler
) : ServiceRemoteDevResource {
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

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun desktopTokenCheck(token: String, dToken: String): Result<UserOnePassword> {
        logger.info("Checking desktop token $dToken")
        return Result(permissionService.checkAndGetUser1Password(dToken))
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
        val res = workspaceService.getWorkspaceList4WeSec(
            projectId = null,
            ip = ip,
            hasDepartmentsInfo = true,
            hasCurrentUser = true
        )
        // 理论上一个IP最多只会有一条，如果查出了两条记录可能会出现越界数据，不能返回，需要抛错
        if (res.size > 1) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REMOTEDEV_CLIENT_IP_DUPLICATE_ERROR.errorCode,
                params = arrayOf(ip)
            )
        }
        return Result(res.randomOrNull())
    }

    override fun getRemotedevProjects(projectId: String?): Result<List<RemotedevProject>> {
        return Result(workspaceService.getWorkspaceProject(projectId))
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
        zoneType: WindowsResourceZoneConfigType?,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        val projectId = checkNotNull(data.projectId)
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
                ),
                zoneType = zoneType
            )
            Thread.sleep(500)
        }
        // 启动流水线完成剩下的分配工作
        if (data.repoId.isNullOrBlank() || data.localDriver.isNullOrBlank()) {
            return Result(true)
        }
        try {
            val infoS = redisOperation.get(PIPELINE_CONFIG_INFO) ?: return Result(true)
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
            AsyncExecute.dispatch(
                streamBridge, AsyncPipelineEvent(
                    userId = info.userId ?: operator,
                    projectId = info.projectId,
                    pipelineId = info.pipelineId,
                    values = newParam
                )
            )
        } catch (e: Exception) {
            logger.warn("execute assignWorkspace pipeline error", e)
        }
        return Result(true)
    }

    override fun notifyWorkspaceInfo(operator: String, notifyData: WorkspaceNotifyData): Result<Boolean> {
        logger.info("notify workspace|notifyData|$notifyData")
        AsyncExecute.dispatch(
            streamBridge, AsyncNotify(
                operator = operator,
                notifyData = notifyData
            )
        )
        return Result(true)
    }

    override fun notifyDesktopCheckIp(ip: String, notifyData: WorkspaceDesktopNotifyData): Result<Boolean> {
        val ok = startWorkspaceService.checkIpUsers(ip, notifyData.userIdList)
        if (!ok) {
            return Result(false)
        }
        notifyControl.notify4User(
            userIds = notifyData.userIdList,
            notifyType = setOf(notifyData.dataType),
            bodyParams = mutableMapOf(
                "operator" to notifyData.operator,
                "messageContent" to notifyData.data,
                "messageStartTime" to notifyData.messageStartTime.toString(),
                "messageEndTime" to notifyData.messageEndTime.toString(),
                "clientMsg" to notifyData.data,
                "notifyTemplateCode" to notifyData.notifyTemplateCode
            )
        )
        return Result(true)
    }

    override fun getWindowsResourceList(): Result<List<WindowsResourceTypeConfig>> {
        return Result(windowsResourceConfigService.getAllType(true, null))
    }

    override fun createPersonalWorkspace(
        userId: String,
        zoneType: WindowsResourceZoneConfigType?,
        data: WindowsWorkspaceCreate
    ): Result<Boolean> {
        return Result(
            createControl.devcloudCreateWorkspace(
                userId = userId,
                workspaceCreate = data,
                projectId = null,
                zoneType = zoneType
            )
        )
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
        val workspace = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (workspace == null || workspace.ownerType != WorkspaceOwnerType.PERSONAL) {
            logger.warn("get personal workspace with invalid workspace type: $userId|$workspaceName")
            return Result(null)
        }

        if (!permissionService.hasManagerOrViewerPermission(userId, workspace.projectId, workspace.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to get $workspaceName info")
            )
        }
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                workspaceName = workspace.workspaceName,
                notStatus = null
            ).firstOrNull()
        )
    }

    override fun createProjectWorkspace(
        userId: String,
        projectId: String,
        zoneType: WindowsResourceZoneConfigType?,
        data: WindowsWorkspaceCreate
    ): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        return Result(
            createControl.devcloudCreateWorkspace(
                userId = userId,
                workspaceCreate = data,
                projectId = projectId,
                zoneType = zoneType
            )
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

    override fun fetchExpertSupRecord(
        userId: String,
        workspaceName: String,
        createLaterTimestamp: Long
    ): Result<List<SupRecordData>> {
        return Result(expertSupportService.fetchSupRecord(workspaceName, createLaterTimestamp))
    }

    override fun getProjectWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String
    ): Result<WeSecProjectWorkspace?> {
        val workspace = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (workspace == null || workspace.ownerType != WorkspaceOwnerType.PROJECT || workspace.projectId != projectId) {
            logger.warn("get project workspace with invalid workspace type: $userId|$projectId|$workspaceName")
            return Result(null)
        }

        if (!permissionService.hasManagerOrViewerPermission(userId, workspace.projectId, workspace.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to get $workspaceName info")
            )
        }
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                workspaceName = workspace.workspaceName,
                notStatus = null,
                hasCurrentUser = true
            ).firstOrNull()
        )
    }

    override fun getWindowsQuota(userId: String, type: QuotaType): Result<Map<String, Map<String, Int>>> {
        return Result(windowsResourceConfigService.allWindowsQuota(
            searchCustom = false,
            quotaType = type,
            withProjectLimit = null
        )
        )
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

    override fun fetchCvmList(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Result<Page<DevcloudCVMData>?> {
        return Result(devcloudService.fetchCVMList(userId, projectId, page, pageSize))
    }

    override fun assignUser(
        userId: String,
        projectId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean> {
        deliverControl.assignUser2Workspace(userId, workspaceName, assigns)
        return Result(true)
    }

    override fun getWorkspaceImageList(projectId: String?, imageId: String?): Result<Map<String, Any>> {
        // 获取基础镜像
        val baseImages = imageManageService.getVmStandardImages().map { JsonUtil.toMap(it) }

        // 获取项目特定镜像（如果有）
        val projectImageMap = if (!projectId.isNullOrBlank()) {
            val projectImages = imageManageService.getProjectImageList(projectId, imageId).map { JsonUtil.toMap(it) }
            mapOf(projectId to projectImages)
        } else {
            emptyMap()
        }

        // 合并基础镜像和项目镜像
        val allImages = projectImageMap + mapOf("base" to baseImages)

        return Result(allImages)
    }

    override fun modifyWorkspaceDisplayName(userId: String, ip: String, displayName: String): Result<Boolean> {
        return Result(workspaceService.modifyWorkspaceDisplayName(userId, ip, displayName))
    }

    override fun reBuildWorkspace(
        userId: String,
        workspaceName: String,
        rebuildReq: WorkspaceRebuildReq
    ): Result<Boolean> {
        rebuildWorkspaceHandler.rebuildWorkspace(
            userId = userId,
            workspaceName = workspaceName,
            rebuildReq = rebuildReq
        )
        return Result(true)
    }

    override fun startWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        startWorkspaceHandler.startWorkspace(userId, workspaceName)
        return Result(true)
    }

    override fun stopWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        stopWorkspaceHandler.stopWorkspace(userId, workspaceName)
        return Result(true)
    }

    override fun restartWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        restartWorkspaceHandler.restartWorkspace(userId, workspaceName)
        return Result(true)
    }

    override fun makeImageByVm(
        userId: String,
        workspaceName: String,
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean> {
        makeWorkspaceImageHandler.makeWorkspaceImage(
            userId = userId,
            workspaceName = workspaceName,
            makeImageReq = makeImageReq
        )
        return Result(true)
    }

    override fun modifyWorkspaceProperty(
        userId: String,
        workspaceName: String?,
        ip: String?,
        workspaceProperty: WorkspaceProperty
    ): Result<Boolean> {
        return Result(
            workspaceService.modifyWorkspaceProperty(
                userId = userId,
                workspaceName = workspaceName,
                ip = ip,
                workspaceProperty = workspaceProperty
            )
        )
    }

    override fun workspaceExpandDiskCallback(taskId: String, workspaceName: String, operator: String) {
        expertSupportService.expandDiskCallback(taskId, workspaceName, operator)
    }

    override fun deleteProjectImage(userId: String, projectId: String, imageId: String): Result<Boolean> {
        return Result(imageManageService.deleteProjectImage(userId, projectId, imageId))
    }

    override fun opCvm(data: OperateCvmData): Result<Boolean> {
        if (!tGitService.checkProjectExist(data.projectId)) {
            return Result(false)
        }
        tGitService.addOrRemoveAclIp(
            projectId = data.projectId,
            ips = data.ipList,
            remove = when (data.opType) {
                OperateCvmDataType.ADD -> false
                OperateCvmDataType.DELETE -> true
            },
            tgitId = null
        )
        return Result(true)
    }

    override fun enableWorkspaceRecord(
        userId: String,
        projectId: String,
        workspaceName: String,
        enable: Boolean
    ): Result<Boolean> {
        permissionService.checkUserProjectManager(userId, projectId)
        workspaceRecordService.enableRecord(
            workspaceName = workspaceName,
            enableUser = if (enable) {
                userId
            } else {
                null
            }
        )
        return Result(true)
    }

    override fun checkWorkspaceEnableAddress(
        userId: String,
        appId: Long,
        ip: String
    ): Result<CheckWorkspaceRecordData> {
        val (enable, address) = workspaceRecordService.checkRecordAndAddress(
            appId = appId,
            ip = ip
        )
        return Result(CheckWorkspaceRecordData(enable, address))
    }

    override fun checkUserViewWorkspacePermission(userId: String, workspaceName: String): Result<Boolean> {
        return Result(workspaceRecordService.checkWorkspaceUserApproval(workspaceName = workspaceName, userId = userId))
    }

    override fun expandDisk(userId: String, workspaceName: String, size: String): Result<ExpandDiskValidateResp?> {
        val data = expertSupportService.expandDisk(
            workspaceName = workspaceName,
            userId = userId,
            size = size
        ) ?: return Result(null)

        return Result(
            ExpandDiskValidateResp(
                valid = data.valid,
                message = data.message
            )
        )
    }

    override fun upgradeWorkspace(userId: String, projectId: String, workspaceName: String, upgradeReq: WorkspaceUpgradeReq): Result<Boolean> {
        upgradeWorkspaceHandler.upgradeWorkspace(userId, projectId, workspaceName, upgradeReq)
        return Result(true)
    }
}
