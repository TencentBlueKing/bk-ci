package com.tencent.devops.remotedev.interfaces

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType

interface ServiceRemoteDevInterface {
    fun workspaceExpandDiskCallback(
        taskId: String,
        workspaceName: String,
        operator: String
    )

    fun createWinWorkspaceByVm(
        userId: String,
        oldWorkspaceName: String?,
        projectId: String?,
        ownerType: WorkspaceOwnerType?,
        uid: String
    ): Result<Boolean>

    fun makeImageCallback(
        taskId: String,
        workspaceName: String,
        operator: String,
        imageId: String
    )
}
