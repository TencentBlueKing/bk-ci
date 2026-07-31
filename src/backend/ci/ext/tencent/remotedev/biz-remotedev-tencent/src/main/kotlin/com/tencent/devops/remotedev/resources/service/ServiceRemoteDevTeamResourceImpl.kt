package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevTeamResource
import com.tencent.devops.remotedev.pojo.TeamInfo
import com.tencent.devops.remotedev.service.TeamService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRemoteDevTeamResourceImpl @Autowired constructor(
    private val teamService: TeamService
) : ServiceRemoteDevTeamResource {
    override fun getWorkspaceTeamInfo(
        userId: String,
        projectId: String,
        workspaceName: String?,
        envId: String?
    ): Result<TeamInfo?> {
        return Result(teamService.getTeamInfo(workspaceName, envId))
    }

    override fun enableWorkspaceTeam(
        userId: String,
        projectId: String,
        workspaceName: String?,
        envId: String?,
        enable: Boolean
    ): Result<TeamInfo?> {
        return Result(teamService.enableTeam(workspaceName, envId, enable))
    }
}