package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevResource
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.ImageSpec
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WindowsResourceConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSharedOpUse
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.service.DataTransferService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.UserRefreshService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceImageService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceTemplateService
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevResourceImpl @Autowired constructor(
    private val workspaceTemplateService: WorkspaceTemplateService,
    private val workspaceService: WorkspaceService,
    private val workspaceCommon: WorkspaceCommon,
    private val userRefreshService: UserRefreshService,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val whiteListService: WhiteListService,
    private val workspaceImageService: WorkspaceImageService,
    private val sleepControl: SleepControl,
    private val deleteControl: DeleteControl,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val dataTransferService: DataTransferService
) : OpRemoteDevResource {

    override fun addWorkspaceTemplate(userId: String, workspaceTemplate: WorkspaceTemplate): Result<Boolean> {
        return Result(workspaceTemplateService.addWorkspaceTemplate(userId, workspaceTemplate))
    }

    override fun getWorkspaceTemplateList(userId: String): Result<List<WorkspaceTemplate>> {
        return Result(workspaceTemplateService.getWorkspaceTemplateList())
    }

    override fun updateWorkspaceTemplate(
        userId: String,
        workspaceTemplateId: Long,
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean> {
        return Result(workspaceTemplateService.updateWorkspaceTemplate(workspaceTemplateId, workspaceTemplate))
    }

    override fun deleteWorkspaceTemplate(userId: String, wsTemplateId: Long): Result<Boolean> {
        return Result(workspaceTemplateService.deleteWorkspaceTemplate(wsTemplateId))
    }

    override fun initBilling(userId: String, freeTime: Int): Result<Boolean> {
        workspaceService.initBilling(freeTime)
        return Result(true)
    }

    override fun updateUserSetting(userId: String, data: List<OPUserSetting>): Result<Boolean> {
        data.forEach {
            remoteDevSettingService.updateSetting4Op(it)
        }
        return Result(true)
    }

    override fun getUserSetting(userId: String): Result<RemoteDevUserSettings> {
        return Result(remoteDevSettingService.getUserSetting(userId))
    }

    override fun getAllUserSettings(
        userId: String,
        queryUser: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<RemoteDevUserSettings>> {
        return Result(remoteDevSettingService.getAllUserSetting4Op(queryUser, page, pageSize))
    }

    override fun refreshUserInfo(userId: String): Result<Boolean> {
        return Result(userRefreshService.refreshAllUser())
    }

    override fun addWhiteListUser(userId: String, whiteListUser: String): Result<Boolean> {
        return Result(whiteListService.addWhiteListUser(userId, whiteListUser))
    }

    override fun addGPUWhiteListUser(userId: String, whiteListUser: String): Result<Boolean> {
        return Result(whiteListService.addGPUWhiteListUser(userId, whiteListUser))
    }

    override fun addImageSpec(spec: ImageSpec): Result<Boolean> {
        return Result(workspaceImageService.addImageSpecConfig(spec))
    }

    override fun deleteImageSpec(id: Int): Result<Boolean> {
        return Result(workspaceImageService.deleteImageSpecConfig(id))
    }

    override fun updateImageSpec(id: Int, spec: ImageSpec): Result<Boolean> {
        return Result(workspaceImageService.updateImageSpecConfig(id, spec))
    }

    override fun listImageSpec(): Result<List<ImageSpec>?> {
        return Result(workspaceImageService.listImageSpecConfig())
    }

    override fun deleteWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(
            deleteControl.deleteWorkspace4OP(
                userId = userId,
                workspaceName = workspaceName
            )
        )
    }

    override fun stopWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(
            sleepControl.stopWorkspace(
                userId = userId, workspaceName = workspaceName, needPermission = false
            )
        )
    }

    override fun getWindowsResourceList(userId: String): Result<List<WindowsResourceTypeConfig>> {
        return Result(windowsResourceConfigService.getAllType())
    }

    override fun addWindowsResource(userId: String, windowsResourceConfig: WindowsResourceTypeConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.addWindowsResource(windowsResourceConfig))
    }

    override fun updateWindowsResource(
        userId: String,
        id: Long,
        windowsResourceConfig: WindowsResourceTypeConfig
    ): Result<Boolean> {
        return Result(windowsResourceConfigService.updateWindowsResource(id, windowsResourceConfig))
    }

    override fun deleteWindowsResource(userId: String, id: Long): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteWindowsResource(id))
    }

    override fun addWindowsZone(userId: String, windowsResourceConfig: WindowsResourceZoneConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.addWindowsResourceZone(windowsResourceConfig))
    }

    override fun getWindowsResourceZoneList(userId: String): Result<List<WindowsResourceZoneConfig>> {
        return Result(windowsResourceConfigService.getAllZone())
    }

    override fun updateWindowsZone(
        userId: String,
        id: Long,
        windowsResourceConfig: WindowsResourceZoneConfig
    ): Result<Boolean> {
        return Result(windowsResourceConfigService.updateWindowsResourceZone(id, windowsResourceConfig))
    }

    override fun deleteWindowsZone(userId: String, id: Long): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteWindowsResourceZone(id))
    }

    override fun shareWorkspace(userId: String, workspaceShared: WorkspaceSharedOpUse): Result<Boolean> {
        return Result(
            workspaceService.shareWorkspace(
                workspaceShared.operator,
                workspaceShared.workspaceName,
                workspaceShared.sharedUser,
                needPermission = false
            )
        )
    }

    override fun shareWorkspace4OP(
        userId: String,
        shareWorkspace: ShareWorkspace
    ): Result<Boolean> {
        return Result(
            workspaceService.shareWorkspace4OP(
                userId = userId,
                shareWorkspace = shareWorkspace
            )
        )
    }

    override fun getShareWorkspace(userId: String, workspaceName: String?): Result<List<WorkspaceShared>> {
        return Result(workspaceService.getShareWorkspace(workspaceName))
    }

    override fun deleteShareWorkspace(userId: String, id: Long): Result<Boolean> {
        return Result(workspaceService.deleteSharedWorkspace(id))
    }

    override fun getProjectWorkspaceList(
        userId: String,
        projectId: String?,
        systemType: WorkspaceSystemType?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4Op(projectId, systemType, page, pageSize))
    }

    override fun getStartCloudResourceList(
        userId: String,
        zoneId: String?,
        machineType: String?,
        ip: String?,
        status: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Map<String, Any>>> {
        val resourceList = workspaceCommon.syncStartCloudResourceList()
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val filteredResources = resourceList.filter {
            (zoneId.isNullOrEmpty() || it.zoneId == zoneId) &&
                    (machineType.isNullOrEmpty() || it.machineType == machineType) &&
                (ip.isNullOrEmpty() || it.cgsIp == ip) &&
                    (status == null || it.status == status)
        }
        val start = (pageNotNull - 1) * pageSizeNotNull
        val end = (start + pageSizeNotNull).coerceAtMost(filteredResources.size)
        return if (start >= filteredResources.size) {
            Result(
                Page(
                    page = pageNotNull, pageSize = pageSizeNotNull, count = filteredResources.size.toLong(),
                    records = emptyList()
                )
            )
        } else {
            Result(
                Page(
                    page = pageNotNull, pageSize = pageSizeNotNull, count = filteredResources.size.toLong(),
                    records = filteredResources.subList(start, end).map { JsonUtil.toMap(it) }
                )
            )
        }
    }

    override fun getCgsConfig(userId: String): Result<CgsResourceConfig> {
        return Result(workspaceCommon.getCgsConfig())
    }

    override fun moveWorkspaceDetail(userId: String, workspaceName: String): Result<Boolean> {
        // 先获取工作空间信息
        val workspaceDetail = workspaceService.getWorkspaceDetail(userId, workspaceName, checkPermission = false)
            ?: return Result(false)

        workspaceCommon.updateWorkspaceDetail(workspaceName, workspaceDetail.workspaceMountType)
        return Result(true)
    }

    override fun windowsWorkspaceDaoInit(userId: String): Result<Boolean> {
        dataTransferService.windowsWorkspaceDaoInit()
        return Result(true)
    }
}
