package com.tencent.devops.remotedev.service.projectworkspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.common.QuotaType
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
    private val dispatcher: SampleEventDispatcher,
    private val deleteControl: DeleteControl,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val workspaceWindowsDao: WorkspaceWindowsDao
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
        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )

        if (!permissionService.hasOwnerPermission(
                userId = userId,
                workspaceName = workspaceName,
                projectId = projectId,
                ownerType = workspace.ownerType
            ) && !permissionService.hasUserManager(userId, projectId)
        ) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to upgrade $workspaceName")
            )
        }
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
            createCheckWhenUpgrade(
                old = workspace,
                machineType = rebuildReq.machineType
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

            val (appName, _) = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.UPGRADE,
                    workspaceName = workspaceName,
                    mountType = WorkspaceMountType.START,
                    machineType = rebuildReq.machineType,
                    appName = appName,
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
                ownerType = workspace.ownerType,
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
        if (bakWorkspaceName == null || !bakWorkspaceName.startsWith("upgrade")) {
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
        val old = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = bakWorkspaceName)
        if (old != null) {
            workspaceDao.modifyWorkspaceProperty(
                dslContext = dslContext,
                projectId = ws.projectId,
                workspaceName = workspaceName,
                workspaceProperty = WorkspaceProperty(
                    old.displayName, old.remark, old.labels
                )
            )
            if (old.nodeHashId != null) {
                workspaceWindowsDao.updateNodeHashId(dslContext, old.nodeHashId, workspaceName)
            }

            // 删除旧云桌面
            if (old.status.checkUnused()) {
                deleteControl.deleteWorkspace4System(ws.createUserId, bakWorkspaceName)
            }
        }
    }

    private fun createCheckWhenUpgrade(
        old: WorkspaceRecordWithWindows,
        machineType: String
    ) {
        val zoneId = checkNotNull(old.zoneId)
        val winConfigId = checkNotNull(old.winConfigId)
        val windowsConfig = windowsResourceConfigService.getTypeConfig(winConfigId)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(winConfigId.toString())
            )

        if (windowsConfig.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(windowsConfig.size)
            )
        }
        val windowsZone = windowsResourceConfigService.getZoneConfig(zoneId.replace(Regex("\\d+"), ""))
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(zoneId)
            )
        if (windowsZone.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(zoneId)
            )
        }

        if (old.ownerType.projectUse() && old.winConfigId != windowsConfig.id?.toInt()) {
            val workspaceNames = workspaceDao.fetchProjectWorkspaceName(
                dslContext = dslContext,
                projectId = old.projectId
            )
            windowsResourceConfigService.createCheckSpecLimit(
                windowsType = machineType,
                projectId = old.projectId,
                workspaceNames = workspaceNames,
                createCount = 1
            )
        }
        windowsResourceConfigService.createCheckWhenWinNotAlready(
            windowsZone = windowsZone,
            windowsConfig = windowsConfig,
            newNum = 1,
            quotaType = QuotaType.parse(windowsZone.type)
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeWorkspaceHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }
}
