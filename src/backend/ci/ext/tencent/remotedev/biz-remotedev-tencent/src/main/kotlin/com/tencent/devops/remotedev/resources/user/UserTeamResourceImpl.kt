package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserTeamResource
import com.tencent.devops.remotedev.pojo.TeamInfo
import com.tencent.devops.remotedev.service.TeamService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTeamResourceImpl @Autowired constructor(
    private val teamService: TeamService
) : UserTeamResource {
    override fun getWorkspaceTeamInfo(
        userId: String,
        projectId: String,
        workspaceName: String
    ): Result<TeamInfo?> {
        return Result(teamService.getTeamInfo(userId, projectId, workspaceName, null))
    }

    override fun enableWorkspaceTeam(
        userId: String,
        projectId: String,
        workspaceName: String,
        enable: Boolean
    ): Result<TeamInfo?> {
        return Result(teamService.enableTeam(userId, projectId, workspaceName, null, enable))
    }
}