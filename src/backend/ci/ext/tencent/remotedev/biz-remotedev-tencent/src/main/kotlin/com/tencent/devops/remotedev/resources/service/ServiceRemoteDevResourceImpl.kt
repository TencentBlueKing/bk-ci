package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.listener.event.CdsWebhookEvent
import com.tencent.devops.remotedev.pojo.IWhiteList
import com.tencent.devops.remotedev.pojo.OperateCvmData
import com.tencent.devops.remotedev.pojo.OperateCvmDataType
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.UserNotifyInfo
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WhiteListType
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceCloneReq
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceRegistration
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.async.AsyncNotify
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.expert.DeleteDiskData
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.gitproxy.TGitBindRemotedevData
import com.tencent.devops.remotedev.pojo.image.DeleteImageResp
import com.tencent.devops.remotedev.pojo.image.ListImagesData
import com.tencent.devops.remotedev.pojo.image.ListImagesResp
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.itsm.BKItsmCreateTicketReq
import com.tencent.devops.remotedev.pojo.itsm.BKItsmCreateTicketRespData
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.EnableRemotedevData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.RemotedevProjectNew
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
import com.tencent.devops.remotedev.pojo.record.FetchMetaDataParam
import com.tencent.devops.remotedev.pojo.record.ThumbnailEncryptedTicketResp
import com.tencent.devops.remotedev.pojo.record.UserWorkspaceRecordPermissionInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordTicketType
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmData
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmResp
import com.tencent.devops.remotedev.pojo.remotedev.SyncVmData
import com.tencent.devops.remotedev.pojo.remotedev.SyncVmResp
import com.tencent.devops.remotedev.pojo.remotedev.TaskResp
import com.tencent.devops.remotedev.pojo.remotedev.VmDiskInfo
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyFetchInfo
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyInfo
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyResp
import com.tencent.devops.remotedev.pojo.windows.QuotaInApiRes
import com.tencent.devops.remotedev.resources.op.OpProjectWorkspaceResourceImpl
import com.tencent.devops.remotedev.service.BKItsmService
import com.tencent.devops.remotedev.service.CoffeeAIService
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.ProjectStrategyService
import com.tencent.devops.remotedev.service.RemotedevProjectService
import com.tencent.devops.remotedev.service.StartWorkspaceService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceHookService
import com.tencent.devops.remotedev.service.WorkspaceLoginService
import com.tencent.devops.remotedev.service.WorkspaceRecordService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.devcloud.DevcloudService
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.gitproxy.TGitService
import com.tencent.devops.remotedev.service.projectworkspace.CloneWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.MakeWorkspaceImageHandler
import com.tencent.devops.remotedev.service.projectworkspace.RebuildWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.RestartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StartWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.StopWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.UpgradeWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.image.ImageManageService
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import java.net.URLDecoder
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl(
    private val dispatcher: SampleEventDispatcher,
    private val permissionService: PermissionService,
    private val workspaceService: WorkspaceService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val createControl: CreateControl,
    private val deleteControl: DeleteControl,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val notifyControl: NotifyControl,
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
    private val upgradeWorkspaceHandler: UpgradeWorkspaceHandler,
    private val cloneWorkspaceHandler: CloneWorkspaceHandler,
    private val workspaceHookService: WorkspaceHookService,
    private val remotedevProjectService: RemotedevProjectService,
    private val bkItsmService: BKItsmService,
    private val projectStrategyService: ProjectStrategyService,
    private val tGitBindService: TGitService,
    private val gitTransfer: RemoteDevGitTransfer,
    private val coffeeAIService: CoffeeAIService
) : ServiceRemoteDevResource {
    companion object {
        private val logger = LoggerFactory.getLogger(OpProjectWorkspaceResourceImpl::class.java)
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
        envId: String?,
        businessLineName: String?,
        ownerName: String?,
        workspaceName: String?
    ): Result<List<WeSecProjectWorkspace>> {
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                projectId = projectId,
                ip = ip,
                envId = envId,
                businessLineName = businessLineName,
                ownerName = ownerName,
                hasDepartmentsInfo = null,
                hasCurrentUser = true,
                workspaceName = workspaceName
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

    override fun getRemotedevProjectsNew(
        projectId: String?,
        page: Int,
        pageSize: Int
    ): Result<List<RemotedevProjectNew>> {
        return Result(workspaceService.getWorkspaceProjectNew(projectId, page, pageSize))
    }

    override fun checkWorkspaceProject(projectId: String, ip: String): Result<Boolean> {
        return Result(desktopWorkspaceService.checkWorkspaceProject(projectId, ip))
    }

    override fun checkUserIpPermission(user: String, ip: String): Result<Boolean> {
        return Result(desktopWorkspaceService.checkUserIpPermission(user, ip))
    }

    override fun assignWorkspace(
        operator: String,
        owner: String?,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        createControl.assignWorkspace(operator, data, owner)
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
                UserNotifyInfo::operator.name to notifyData.operator,
                "messageContent" to notifyData.data,
                "messageStartTime" to notifyData.messageStartTime.toString(),
                "messageEndTime" to notifyData.messageEndTime.toString(),
                UserNotifyInfo::body.name to notifyData.data,
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
                needPermission = true
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
                params = arrayOf("We're sorry but you don't have permission to get $workspaceName info")
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

    override fun workspaceClone(
        userId: String,
        projectId: String,
        workspaceName: String,
        req: WorkspaceCloneReq
    ): Result<Boolean> {
        cloneWorkspaceHandler.cloneWorkspace(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName,
            rebuildReq = req
        )
        return Result(true)
    }

    override fun workspaceCloneTask(
        userId: String,
        projectId: String,
        workspaceName: String,
        req: WorkspaceCloneReq
    ): Result<TaskResp> {
        return Result(
            cloneWorkspaceHandler.cloneWorkspaceWithTask(
                userId = userId,
                projectId = projectId,
                workspaceName = workspaceName,
                rebuildReq = req
            )
        )
    }

    override fun deleteProjectWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        val record = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (record == null || !record.ownerType.projectUse() || record.projectId != projectId) {
            logger.warn("delete project workspace with invalid workspace type: $userId|$projectId|$workspaceName")
            return Result(false)
        }

        return Result(
            deleteControl.deleteWorkspace(
                userId = userId,
                workspaceName = workspaceName,
                needPermission = !permissionService.hasUserManager(userId, projectId)
            )
        )
    }

    @Deprecated("未来fetch_expert_sup_record_any使用会把这个接口废弃")
    override fun fetchExpertSupRecord(
        userId: String,
        workspaceName: String,
        createLaterTimestamp: Long
    ): Result<List<SupRecordData>> {
        return Result(expertSupportService.fetchSupRecord(workspaceName, createLaterTimestamp))
    }

    override fun fetchExpertSupRecordAny(id: Long): Result<SupRecordData?> {
        return Result(expertSupportService.fetchSupRecordAny(id))
    }

    override fun getProjectWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String
    ): Result<WeSecProjectWorkspace?> {
        val workspace = workspaceService.getWorkspaceRecord(workspaceName = workspaceName)
        if (workspace == null || !workspace.ownerType.projectUse() || workspace.projectId != projectId) {
            logger.warn("get project workspace with invalid workspace type: $userId|$projectId|$workspaceName")
            return Result(null)
        }

        if (!permissionService.hasManagerOrViewerPermission(userId, workspace.projectId, workspace.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("We're sorry but you don't have permission to get $workspaceName info")
            )
        }
        return Result(
            workspaceService.getWorkspaceList4WeSec(
                workspaceName = workspace.workspaceName,
                notStatus = null,
                hasCurrentUser = true,
                userId = userId
            ).firstOrNull()
        )
    }

    override fun getWindowsQuota(
        userId: String,
        type: QuotaType?,
        zoneType: WindowsResourceZoneConfigType,
        specifyTaints: String?
    ): Result<Map<String, Map<String, Int>>> {
        return Result(
            windowsResourceConfigService.allWindowsQuota(
                searchCustom = false,
                quotaType = type,
                zoneType = zoneType,
                withProjectLimit = null,
                specifyTaints = specifyTaints
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

    @Deprecated("老的下掉，要被新的代替")
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

    override fun restartWorkspace(userId: String, workspaceName: String, force: Boolean?): Result<Boolean> {
        restartWorkspaceHandler.restartWorkspace(userId, workspaceName, force)
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
        ip: String,
        mediaGary: Boolean?
    ): Result<CheckWorkspaceRecordData> {
        val (enable, address) = workspaceRecordService.checkRecordAndAddress(
            userId = userId,
            appId = appId,
            ip = ip,
            mediaGary = mediaGary
        )
        return Result(CheckWorkspaceRecordData(enable, address))
    }

    override fun checkUserViewWorkspacePermission(userId: String, workspaceName: String): Result<Boolean> {
        return Result(workspaceRecordService.checkWorkspaceUserApproval(workspaceName = workspaceName, userId = userId))
    }

    override fun expandDisk(
        userId: String,
        workspaceName: String,
        size: String,
        pvcId: String?
    ): Result<ExpandDiskValidateResp?> {
        val data = expertSupportService.expandDisk(
            workspaceName = workspaceName,
            userId = userId,
            size = size,
            pvcId = pvcId
        ) ?: return Result(null)
        return Result(
            ExpandDiskValidateResp(
                valid = data.valid,
                message = data.message,
                taskId = data.taskId
            )
        )
    }

    override fun createDisk(
        userId: String,
        workspaceName: String,
        size: String,
        forceRestart: Boolean?
    ): Result<CreateDiskResp> {
        return Result(
            expertSupportService.createDisk(
                workspaceName = workspaceName,
                userId = userId,
                size = size,
                forceRestart = forceRestart
            )
        )
    }

    override fun fetchDiskList(userId: String, workspaceName: String): Result<List<VmDiskInfo>> {
        return Result(expertSupportService.fetchDiskList(userId, workspaceName))
    }

    override fun deleteDisk(userId: String, data: DeleteDiskData): Result<CreateDiskResp> {
        return Result(
            expertSupportService.deleteDisk(
                userId = userId,
                data = data
            )
        )
    }

    override fun upgradeWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        upgradeReq: WorkspaceUpgradeReq
    ): Result<Boolean> {
        upgradeWorkspaceHandler.upgradeWorkspace(userId, projectId, workspaceName, upgradeReq)
        return Result(true)
    }

    override fun removeUserPermission(userId: String, removeUser: String): Result<Boolean> {
        workspaceCommon.removeUserWorkspaceShare(operator = userId, userId = removeUser)
        return Result(true)
    }

    override fun reloadEnvHook(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>?) {
        workspaceHookService.hookLoad(
            userId = userId,
            projectId = projectId,
            envHashId = envHashId,
            nodeHashIds = nodeHashIds
        )
    }

    override fun deleteEnvHook(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>?) {
        workspaceHookService.hookDelete(
            userId = userId,
            projectId = projectId,
            envHashId = envHashId,
            nodeHashIds = nodeHashIds
        )
    }

    override fun getWorkspaceListNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<ProjectWorkspace>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(workspaceService.getProjectWorkspaceList(userId, projectId, page, pageSize, search))
    }

    override fun getUserWorkspaceRecordPermission(
        userId: String,
        workspaceName: String
    ): Result<UserWorkspaceRecordPermissionInfo> {
        return Result(workspaceRecordService.getUserWorkspaceRecordPermission(userId, workspaceName))
    }

    override fun updateUserWorkspaceRecordPermission(userId: String, workspaceName: String): Result<Boolean> {
        workspaceRecordService.updateApprovalRecordViewPermission(userId, workspaceName)
        return Result(true)
    }

    override fun getViewRecordMetadata(data: FetchMetaDataParam): Result<Page<WorkspaceRecordMetadata>> {
        return Result(
            workspaceRecordService.getWorkspaceRecordMetadata(
                projectId = data.projectId,
                userId = data.userId,
                workspaceName = data.workspaceName,
                page = data.page,
                pageSize = data.pageSize,
                startTime = data.startTime,
                stopTime = data.stopTime
            )
        )
    }

    override fun getWorkspaceTimeline(
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WorkspaceOpHistory>> {
        return Result(
            workspaceService.getWorkspaceTimeline(
                userId = userId,
                workspaceName = workspaceName,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getWorkspaceRecordTicket(
        userId: String,
        workspaceName: String,
        token: String
    ): Result<String> {
        return Result(
            workspaceRecordService.getWorkspaceRecordTicket(
                workspaceName = workspaceName,
                token = token,
                type = WorkspaceRecordTicketType.RECORD
            )
        )
    }

    override fun getThumbnailEncryptedTicket(
        userId: String,
        workspaceName: String?,
        envId: String?,
        expiredSeconds: Long?
    ): Result<ThumbnailEncryptedTicketResp> {
        return Result(
            workspaceRecordService.getThumbnailEncryptedTicket(
                workspaceName = workspaceName,
                envId = envId,
                expiredSeconds = expiredSeconds
            )
        )
    }

    override fun getTaskStatus(userId: String, taskId: String): Result<WorkspaceTaskStatus?> {
        return Result(expertSupportService.getTaskStatus(userId, taskId))
    }

    override fun enableProjectRemotedev(userId: String, data: EnableRemotedevData): Result<Boolean> {
        return Result(
            remotedevProjectService.enableRemotedevWithPermission(
                userId = userId,
                projectId = data.projectId,
                enable = data.enable,
                quota = data.quota ?: 1000,
                rewriteManages = data.managers
            )
        )
    }

    override fun fetchImages(userId: String, data: ListImagesData): Result<ListImagesResp?> {
        return Result(imageManageService.fetchImages(userId, data))
    }

    override fun deleteImage(
        userId: String,
        projectId: String,
        imageId: String,
        delaySeconds: Int?
    ): Result<DeleteImageResp> {
        return Result(
            imageManageService.deleteImage(
                userId = userId,
                projectId = projectId,
                imageId = imageId,
                delaySeconds = delaySeconds
            )
        )
    }

    override fun createItsmTicket(
        userId: String,
        createReq: BKItsmCreateTicketReq
    ): Result<BKItsmCreateTicketRespData> {
        return Result(
            bkItsmService.createDirectTicket(
                createReq = createReq,
                errorParam = userId
            )
        )
    }

    override fun getProjectStrategy(userId: String, data: ProjectStrategyFetchInfo): Result<ProjectStrategyResp> {
        return Result(projectStrategyService.getStrategy(data))
    }

    override fun updateProjectStrategy(userId: String, data: ProjectStrategyInfo): Result<Boolean> {
        projectStrategyService.createOrUpdateStrategy(data)
        return Result(true)
    }

    override fun syncVm(userId: String, data: SyncVmData): Result<SyncVmResp?> {
        return Result(expertSupportService.syncVm(userId, data))
    }

    override fun createCvm(userId: String, data: CreateCvmData): Result<CreateCvmResp?> {
        return Result(expertSupportService.createCvm(data))
    }

    override fun whitelist(
        userId: String,
        type: WhiteListType,
        delete: Boolean,
        body: Map<String, String>
    ): Result<Boolean> {
        return Result(whiteListService.apiSetWhiteList(userId, type, delete, body))
    }

    override fun whitelistGet(
        userId: String,
        type: WhiteListType,
        body: Map<String, String>
    ): Result<List<IWhiteList>> {
        return Result(whiteListService.apiGetWhiteList(userId, type, body))
    }

    override fun tgitGetUserOauth(userId: String, redirectUrl: String): Result<AuthorizeResult> {
        // 权限校验？
        return gitTransfer.load(RemoteDevGitType.T_GIT).isOAuth(
            userId = userId,
            redirectUrlType = RedirectUrlTypeEnum.SPEC,
            redirectUrl = redirectUrl,
            refreshToken = null
        )
    }

    override fun tgitGetProjectList(
        userId: String,
        tGitId: Long
    ): Result<List<String>> {
        return Result(tGitBindService.fetchProject(tGitId))
    }

    override fun tgitBindRemotedevProject(
        userId: String,
        data: TGitBindRemotedevData
    ): Result<Map<String, Boolean>> {
        return Result(tGitBindService.bindTGitProject(userId, data.tgitId, data.tgitUrl, data.projectIds))
    }

    override fun cdsWebhookEvent(
        userId: String,
        type: String,
        workspaceName: String?,
        envId: String?
    ): Result<Boolean> {
        if (workspaceName.isNullOrEmpty() && envId.isNullOrEmpty()) return Result(false)
        val eventType = CdsWebhookEvent.Type.fromWebhook(type) ?: return Result(false)
        dispatcher.dispatch(
            CdsWebhookEvent(
                userId = userId,
                type = eventType,
                envId = envId ?: "",
                workspaceName = workspaceName
            )
        )
        return Result(true)
    }

    override fun openClawOn(userId: String): Result<WorkspaceRegistration?> {
        return Result(coffeeAIService.openClawOn(userId))
    }
}
