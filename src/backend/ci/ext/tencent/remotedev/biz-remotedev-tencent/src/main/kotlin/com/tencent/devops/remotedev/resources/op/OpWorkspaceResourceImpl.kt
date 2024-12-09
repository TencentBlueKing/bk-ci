package com.tencent.devops.remotedev.resources.op

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpWorkspaceResource
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSharedOpUse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.service.WorkspaceRecordService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpWorkspaceResourceImpl @Autowired constructor(
    private val workspaceService: WorkspaceService,
    private val workspaceCommon: WorkspaceCommon,
    private val createControl: CreateControl,
    private val workspaceRecordService: WorkspaceRecordService
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

    override fun devxEnvNodeInit(userId: String, workspaceName: String): Result<Boolean> {
        val ws = workspaceService.getWorkspaceDetail(
            userId = userId,
            workspaceName = workspaceName
        ) ?: return Result(false)
        val ip = ws.ip?.substringAfter(".") ?: run {
            logger.info("workspace not find ip|$workspaceName")
            return Result(false)
        }
        workspaceCommon.devxEnvNodeInit(
            userId = userId,
            projectId = ws.projectId,
            workspaceName = ws.workspaceName,
            ip = ip,
            size = ws.machineType ?: ""
        )
        if (ws.ownerType != WorkspaceOwnerType.PROJECT_PUBLIC) {
            workspaceService.changeWorkspaceOwnerType(
                ws.workspaceName,
                ws.ownerType,
                WorkspaceOwnerType.PROJECT_PUBLIC
            )
        }
        return Result(true)
    }

    override fun devxEnvNodeDel(userId: String, workspaceName: String): Result<Boolean> {
        workspaceCommon.devxEnvNodeDel(userId, workspaceName)
        return Result(true)
    }

    override fun createWorkspaceRecordTicket(userId: String, workspaceNames: Set<String>): Result<Boolean> {
        workspaceNames.forEach {
            workspaceRecordService.saveWorkspaceRecordTicket(it)
        }
        return Result(true)
    }
}
