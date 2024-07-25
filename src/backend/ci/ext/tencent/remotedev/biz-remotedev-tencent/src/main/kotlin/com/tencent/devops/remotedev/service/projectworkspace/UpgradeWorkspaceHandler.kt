package com.tencent.devops.remotedev.service.projectworkspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UpgradeWorkspaceHandler @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val notifyControl: NotifyControl,
    private val deliverControl: DeliverControl,
    private val workspaceCommon: WorkspaceCommon,
    private val dispatcher: RemoteDevDispatcher,
    private val deleteControl: DeleteControl,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val windowsResourceConfigService: WindowsResourceConfigService
) {

    @ActionAuditRecord(
        actionId = ActionId.CGS_EDIT_TYPE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_EDIT_TYPE_CONTENT
    )
    fun upgradeWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        rebuildReq: WorkspaceUpgradeReq
    ): WorkspaceResponse {
        logger.info("$userId upgrade project $projectId workspace $workspaceName|$rebuildReq")
        if (!permissionService.hasOwnerPermission(
                userId = userId,
                workspaceName = workspaceName,
                projectId = projectId
            ) && !permissionService.hasUserManager(userId, projectId)
        ) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to upgrade $workspaceName")
            )
        }

        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        RedisCallLimit(
            redisOperation = redisOperation,
            lockKey = "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds = expiredTimeInSeconds
        ).tryLock().use {
            if (workspaceCommon.notOk2doNextAction(workspace)) {
                logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    params = arrayOf(
                        workspace.workspaceName,
                        "status is already ${workspace.status}, can't upgrade now"
                    )
                )
            }
            if (workspace.ownerType == WorkspaceOwnerType.PROJECT) {
                val workspaceNames = workspaceDao.fetchUserWorkspaceName(
                    dslContext = dslContext,
                    projectId = workspace.projectId,
                    ownerType = WorkspaceOwnerType.PROJECT
                )
                windowsResourceConfigService.createCheckSpecLimit(
                    windowsType = rebuildReq.machineType,
                    projectId = workspace.projectId,
                    workspaceNames = workspaceNames
                )
            }
            windowsResourceConfigService.createCheckWhenWinNotAlready(
                zoneId = checkNotNull(workspace.zoneId),
                winConfigId = checkNotNull(workspace.winConfigId),
                newNum = 1,
                ownerType = workspace.ownerType
            )
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.UPGRADE,
                actionMessage = "start upgrade"
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.UPGRADING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.UPGRADE,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status,
                    WorkspaceStatus.UPGRADING
                )
            )

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.UPGRADE,
                    workspaceName = workspaceName,
                    mountType = WorkspaceMountType.START,
                    machineType = rebuildReq.machineType,
                    gameId = null,
                    projectId = projectId
                )
            )

            notifyControl.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_UPGRADE,
                status = true,
                action = WorkspaceAction.UPGRADING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START,
                ownerType = WorkspaceOwnerType.PROJECT,
                projectId = projectId
            )

            return WorkspaceResponse(
                workspaceName = workspaceName,
                workspaceHost = "",
                status = WorkspaceAction.UPGRADING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START
            )
        }
    }

    fun checkAndUpgradeVm(
        workspaceName: String
    ) {
        val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: run {
            logger.info("checkAndUpgradeVm not find workspace $workspaceName")
            return
        }
        val bakWorkspaceName = ws.bakWorkspaceName
        if (bakWorkspaceName == null) {
            logger.info("checkAndUpgradeVm not need upgrade workspace $workspaceName")
            return
        }
        // 同步原有拥有者分配者信息
        val shareInfos = workspaceSharedDao.fetchWorkspaceSharedInfo(dslContext, workspaceName = bakWorkspaceName)
        deliverControl.assignUser2Workspace(
            userId = ws.createUserId,
            workspaceName = ws.workspaceName,
            assigns = shareInfos.map {
                ProjectWorkspaceAssign(
                    it.sharedUser, it.type, null
                )
            },
            checkPermission = false
        )
        // 别名等信息同步
        val old = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = bakWorkspaceName)
        if (old != null) {
            workspaceDao.modifyWorkspaceProperty(
                dslContext = dslContext,
                projectId = ws.projectId,
                workspaceName = workspaceName,
                workspaceProperty = WorkspaceProperty(
                    old.displayName, old.remark, old.labels
                )
            )

            // 删除旧云桌面
            if (old.status.checkUnused()) {
                deleteControl.deleteWorkspace4System(ws.createUserId, bakWorkspaceName)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeWorkspaceHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }
}
