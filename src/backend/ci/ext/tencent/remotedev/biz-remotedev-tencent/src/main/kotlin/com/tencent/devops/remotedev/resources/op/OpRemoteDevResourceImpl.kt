package com.tencent.devops.remotedev.resources.op

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevResource
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.windows.WindowsPoolListFetchData
import com.tencent.devops.remotedev.service.MakeMoneyService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.UserRefreshService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevResourceImpl @Autowired constructor(
    private val workspaceCommon: WorkspaceCommon,
    private val userRefreshService: UserRefreshService,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val whiteListService: WhiteListService,
    private val sleepControl: SleepControl,
    private val deleteControl: DeleteControl,
    private val workspaceDao: WorkspaceDao,
    private val dslContext: DSLContext,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val makeMoneyService: MakeMoneyService
) : OpRemoteDevResource {
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
        return Result(deleteControl.batchDeleteWindowsWorkspace4OP(userId, workspaceNames))
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
        val resourceList = workspaceCommon.realtimeStartCloudResourceList()
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
        return Result(windowsResourceConfigService.getCgsConfig())
    }

    override fun initTaiUserInfo(userId: String, taiUsers: List<String>): Result<Boolean> {
        remoteDevSettingService.updateAllTaiUserInfo(taiUsers)
        return Result(true)
    }

    override fun initWorkspaceIp(userId: String): Result<Boolean> {
        var page = 1
        while (true) {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, 100)
            val res = workspaceDao.limitFetchWorkspace(dslContext = dslContext, sqlLimit)
            if (res.isEmpty()) return Result(true)
            res.forEach {
                if (it.ip.isNullOrBlank() && it.workspaceMountType == WorkspaceMountType.START.name) {
                    workspaceDao.updateWorkspaceIp(
                        dslContext = dslContext,
                        workspaceName = it.name,
                        hostName = it.hostName,
                        ip = it.hostName.substringAfter(".")
                    )
                    Thread.sleep(10)
                }
            }
            page += 1
        }
    }

    override fun makeMoneyLastDay(userId: String): Response {
        return makeMoneyService.makeMoneyLastDay()
    }

    override fun bills(userId: String, year: Int, month: Int, push: Boolean): Response {
        return makeMoneyService.bills(year, month, push)
    }
}
