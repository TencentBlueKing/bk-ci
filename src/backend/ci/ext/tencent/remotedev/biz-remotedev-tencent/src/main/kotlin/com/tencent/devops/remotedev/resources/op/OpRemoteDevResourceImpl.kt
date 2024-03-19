package com.tencent.devops.remotedev.resources.op

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevResource
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.ImageSpec
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.pojo.windows.WindowsPoolListFetchData
import com.tencent.devops.remotedev.service.BKBaseService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.UserRefreshService
import com.tencent.devops.remotedev.service.WhiteListService
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
    private val bkBaseService: BKBaseService
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

    override fun renewalExperienceDuration(userId: String, renewalTime: Int): Result<Boolean> {
        return Result(remoteDevSettingService.renewalExperienceDuration(userId, renewalTime))
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
        return Result(
            whiteListService.addGPUWhiteListUser(
                userId = userId,
                whiteListUser = whiteListUser,
                override = true
            )
        )
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

    @AuditEntry(actionId = ActionId.CGS_DELETE)
    override fun deleteWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(
            deleteControl.deleteWorkspace4OP(
                userId = userId,
                workspaceName = workspaceName
            )
        )
    }

    @AuditEntry(actionId = ActionId.CGS_DELETE)
    override fun batchDeleteWorkspace(
        userId: String,
        workspaceNames: Set<String>
    ): Result<Map<String, Boolean>> {
        return Result(deleteControl.batchDeleteWorkspace4OP(userId, workspaceNames))
    }

    @AuditEntry(actionId = ActionId.CGS_STOP)
    override fun stopWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(
            sleepControl.stopWorkspace(
                userId = userId, workspaceName = workspaceName, needPermission = false
            )
        )
    }

    override fun getStartCloudResourceList(
        userId: String,
        data: WindowsPoolListFetchData
    ): Result<Page<Map<String, Any>>> {
        val resourceList = workspaceCommon.syncStartCloudResourceList()
        val pageNotNull = data.page ?: 1
        val pageSizeNotNull = data.pageSize ?: 6666
        val filteredResources = resourceList.filter {
            (data.zoneId.isNullOrEmpty() || it.zoneId == data.zoneId) &&
                (data.machineType.isNullOrEmpty() || it.machineType == data.machineType) &&
                (data.ips.isNullOrEmpty() || data.ips?.contains(it.cgsIp) == true) &&
                (data.status == null || it.status == data.status) &&
                (data.lockedFlag == null || it.locked == data.lockedFlag)
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

    override fun initTaiUserInfo(userId: String, taiUsers: List<String>): Result<Boolean> {
        remoteDevSettingService.updateAllTaiUserInfo(taiUsers)
        return Result(true)
    }
}
