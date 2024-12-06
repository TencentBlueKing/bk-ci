package com.tencent.devops.remotedev.interfaces

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent

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
        uid: String,
        bak: Boolean
    ): Result<Boolean>

    fun makeImageCallback(
        taskId: String,
        workspaceName: String,
        operator: String,
        imageId: String
    )

    fun workspaceUpdate(
        event: RemoteDevUpdateEvent
    )

    fun workspaceCreateDiskCallback(
        taskId: String,
        workspaceName: String,
        operator: String
    )
}
