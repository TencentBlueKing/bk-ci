package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.DesktopWorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import java.net.URLDecoder

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl(
    private val permissionService: PermissionService,
    private val workspaceService: WorkspaceService,
    private val desktopWorkspaceService: DesktopWorkspaceService,
    private val createControl: CreateControl
) : ServiceRemoteDevResource {
    override fun validateUserTicket(userId: String, isOffshore: Boolean, ticket: String): Result<Boolean> {
        return Result(
            permissionService.checkAndGetUser1Password(URLDecoder.decode(ticket, "UTF-8")).userId == userId
        )
    }

    override fun getProjectWorkspace(projectId: String?, ip: String?): Result<List<WeSecProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4WeSec(projectId, ip))
    }

    override fun getRemotedevProjects(): Result<List<RemotedevProject>> {
        return Result(workspaceService.getWorkspaceProject())
    }

    override fun queryProjectRemoteDevCvm(projectId: String?): Result<List<RemotedevCvmData>> {
        return Result(workspaceService.getRemotedevCvm(projectId))
    }

    override fun checkWorkspaceProject(projectId: String, ip: String): Result<Boolean> {
        return Result(desktopWorkspaceService.checkWorkspaceProject(projectId, ip))
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
}
