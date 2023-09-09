package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpWorkspaceResource
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSharedOpUse
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpWorkspaceResourceImpl @Autowired constructor(
    private val workspaceService: WorkspaceService,
    private val workspaceCommon: WorkspaceCommon
) : OpWorkspaceResource {

    override fun shareWorkspace(userId: String, workspaceShared: WorkspaceSharedOpUse): Result<Boolean> {
        return Result(
            workspaceService.shareWorkspace(
                workspaceShared.operator,
                workspaceShared.workspaceName,
                workspaceShared.sharedUser,
                needPermission = false
            )
        )
    }

    override fun shareWorkspace4OP(
        userId: String,
        shareWorkspace: ShareWorkspace
    ): Result<Boolean> {
        return Result(
            workspaceService.shareWorkspace4OP(
                userId = userId,
                shareWorkspace = shareWorkspace
            )
        )
    }

    override fun getShareWorkspace(userId: String, workspaceName: String?): Result<List<WorkspaceShared>> {
        return Result(workspaceService.getShareWorkspace(workspaceName))
    }

    override fun deleteShareWorkspace(userId: String, id: Long): Result<Boolean> {
        return Result(workspaceService.deleteSharedWorkspace(id))
    }

    override fun moveWorkspaceDetail(userId: String, workspaceName: String): Result<Boolean> {
        // 先获取工作空间信息
        val workspaceDetail = workspaceService.getWorkspaceDetail(userId, workspaceName, checkPermission = false)
            ?: return Result(false)

        workspaceCommon.updateWorkspaceDetail(workspaceName, workspaceDetail.workspaceMountType)
        return Result(true)
    }
}
