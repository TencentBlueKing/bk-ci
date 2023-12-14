package com.tencent.devops.remotedev.resources.service

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.net.URLDecoder

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl(
    private val permissionService: PermissionService,
    private val workspaceService: WorkspaceService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val createControl: CreateControl,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService
) : ServiceRemoteDevResource {
    override fun validateUserTicket(userId: String, isOffshore: Boolean, ticket: String): Result<Boolean> {
        return Result(
            permissionService.checkAndGetUser1Password(URLDecoder.decode(ticket, "UTF-8")).userId == userId
        )
    }

    override fun getProjectWorkspace(projectId: String?, ip: String?): Result<List<WeSecProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4WeSec(projectId, ip))
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
        uid: String
    ): Result<Boolean> {
        val res = createControl.createWinWorkspaceByVm(userId, oldWorkspaceName, projectId, uid)
        return Result(res)
    }

    override fun assignWorkspace(
        operator: String,
        owner: String?,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        workspaceCommon.syncStartCloudResourceList()
        val cgsData = workspaceCommon.getCgsData(data.cgsIds, data.ips) ?: return Result(false)
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
            createControl.asyncCreateWorkspace(
                pmUserId = owner ?: operator,
                projectId = data.projectId,
                cgsId = cgs.cgsId,
                autoAssign = !owner.isNullOrEmpty(),
                workspaceCreate = ProjectWorkspaceCreate(
                    windowsType = windowsResourceConfigId.size,
                    windowsZone = cgs.zoneId.replace(Regex("\\d+"), ""),
                    baseImageId = 0,
                    count = 1
                )
            )
            Thread.sleep(500)
        }
        return Result(true)
    }

    override fun notifyWorkspaceInfo(notifyData: WorkspaceNotifyData): Result<Boolean> {
        TODO("Not yet implemented")
    }
}
