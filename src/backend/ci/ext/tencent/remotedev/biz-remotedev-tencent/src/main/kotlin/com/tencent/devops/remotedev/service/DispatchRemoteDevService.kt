package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.interfaces.ServiceRemoteDevInterface
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.service.expert.ExpertSupportService
import com.tencent.devops.remotedev.service.projectworkspace.MakeWorkspaceImageHandler
import com.tencent.devops.remotedev.service.workspace.CreateControl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class DispatchRemoteDevService(
    private val createControl: CreateControl,
    private val expertSupportService: ExpertSupportService,
    private val makeWorkspaceImageHandler: MakeWorkspaceImageHandler
) : ServiceRemoteDevInterface {
    companion object {
        private val logger = LoggerFactory.getLogger(DispatchRemoteDevService::class.java)
    }

    override fun workspaceExpandDiskCallback(taskId: String, workspaceName: String, operator: String) {
        expertSupportService.expandDiskCallback(taskId, workspaceName, operator)
    }

    override fun createWinWorkspaceByVm(
        userId: String,
        oldWorkspaceName: String?,
        projectId: String?,
        ownerType: WorkspaceOwnerType?,
        uid: String,
        bak: Boolean
    ): Result<Boolean> {
        val res = createControl.createWinWorkspaceByVm(
            userId = userId,
            oldWorkspaceName = oldWorkspaceName,
            projectCode = projectId,
            ownerType = ownerType,
            uid = uid,
            bak = bak
        )
        return Result(res)
    }

    override fun makeImageCallback(
        taskId: String,
        workspaceName: String,
        operator: String,
        imageId: String
    ) {
        makeWorkspaceImageHandler.makeWorkspaceImageCallback(
            taskId = taskId,
            userId = operator,
            workspaceName = workspaceName,
            imageId = imageId
        )
    }
}
