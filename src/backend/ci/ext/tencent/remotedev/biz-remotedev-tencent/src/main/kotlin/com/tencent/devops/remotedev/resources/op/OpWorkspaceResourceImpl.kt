package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevResource
import com.tencent.devops.remotedev.api.op.OpWorkspaceResource
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.ImageSpec
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WindowsResourceConfig
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSharedOpUse
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.service.DataTransferService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.UserRefreshService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.WorkspaceImageService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceTemplateService
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
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
