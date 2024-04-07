package com.tencent.devops.remotedev.resources.op

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpWorkspaceResource
import com.tencent.devops.remotedev.cron.WorkspaceCheckJob
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSharedOpUse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.SleepControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpWorkspaceResourceImpl @Autowired constructor(
    private val workspaceService: WorkspaceService,
    private val workspaceCommon: WorkspaceCommon,
    private val createControl: CreateControl,
    private val deleteControl: DeleteControl,
    private val sleepControl: SleepControl,
    private val jobService: WorkspaceCheckJob
) : OpWorkspaceResource {

    companion object {
        val logger = LoggerFactory.getLogger(OpWorkspaceResourceImpl::class.java)
    }

    @AuditEntry(actionId = ActionId.CGS_SHARE)
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

    @AuditEntry(actionId = ActionId.CGS_VIEW)
    override fun moveWorkspaceDetail(userId: String, workspaceName: String): Result<Boolean> {
        // 先获取工作空间信息
        val workspaceDetail = workspaceService.getWorkspaceDetail(userId, workspaceName, checkPermission = false)
            ?: return Result(false)

        workspaceCommon.updateWorkspaceDetail(workspaceName, workspaceDetail.workspaceMountType)
        return Result(true)
    }

    override fun updateStatus(
        userId: String,
        workspaceName: String,
        workspaceStatus: WorkspaceStatus
    ): Result<Boolean> {
        workspaceCommon.updateStatusAndCreateHistory(workspaceName, workspaceStatus, WorkspaceAction.SYSTEM_CHANGES)
        return Result(true)
    }

    override fun createWinWorkspaceByVm(
        userId: String,
        oldWorkspaceName: String?,
        projectId: String?,
        uid: String
    ): Result<Boolean> {
        val res = createControl.createWinWorkspaceByVm(userId, oldWorkspaceName, projectId, uid)
        return Result(res)
    }

    override fun deleteInactivityWorkspace(userId: String): Result<Boolean> {
        deleteControl.deleteLinuxInactivityWorkspace()
        /*暂时去掉个人win的控制*/
//        deleteControl.deleteWinInactivityWorkspace()
        return Result(true)
    }

    override fun autoCleanJob4Windows(userId: String, type: String?): Result<Boolean> {
        when (type) {
            "delete" -> {
                logger.info("read to delete not use workspace")
                deleteControl.autoDeleteWhenNotAssign(true)
                deleteControl.autoDeleteWhenSleep14Day(true)
            }

            "sleep" -> {
                logger.info("read to sleep not login workspace")
                sleepControl.autoSleepWhenNotLogin(true)
            }

            else -> {
                jobService.projectWinJob()
            }
        }
        return Result(true)
    }
}
