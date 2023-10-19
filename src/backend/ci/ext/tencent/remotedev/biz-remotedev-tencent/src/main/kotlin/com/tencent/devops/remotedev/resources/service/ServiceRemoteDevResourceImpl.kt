package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WorkspaceService

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl(
    private val permissionService: PermissionService,
    private val workspaceService: WorkspaceService
) : ServiceRemoteDevResource {
    override fun validateUserTicket(userId: String, isOffshore: Boolean, ticket: String): Result<Boolean> {
        return Result(permissionService.checkAndGetUser1Password(ticket).userId == userId)
    }

    override fun getProjectWorkspace(projectId: String?, ip: String?): Result<List<WeSecProjectWorkspace>> {
        return Result(workspaceService.getProjectWorkspaceList4WeSec(projectId, ip))
    }

    override fun getRemotedevProjects(): Result<List<RemotedevProject>> {
        return Result(workspaceService.getWorkspaceProject())
    }
}
