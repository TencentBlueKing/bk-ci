package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.RemoteDevClientVersion
import com.tencent.devops.remotedev.service.WorkspaceService

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl (
    private val workspaceService: WorkspaceService
): ServiceRemoteDevResource {
    override fun updateClientVersion(userId: String, env: String, version: String): Result<Boolean> {
        workspaceService.updateClientVersion(userId, env, version)
        return Result(true)
    }
}
