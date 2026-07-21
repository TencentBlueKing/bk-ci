package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevInternalResource
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.service.WorkspaceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRemoteDevInternalResourceImpl @Autowired constructor(
    private val workspaceService: WorkspaceService
) : ServiceRemoteDevInternalResource {
    override fun getWorkspaceDetail(
        workspaceName: String
    ): Result<WorkspaceStartCloudDetail?> {
        return Result(
            workspaceService.getWorkspaceDetailNoPermission(
                workspaceName = workspaceName
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceRemoteDevInternalResourceImpl::class.java)
    }
}