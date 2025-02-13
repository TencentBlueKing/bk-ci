/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.remotedev.service.projectworkspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dispatch.kubernetes.service.WorkspaceOperateCommonObject
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.software.SoftwareManageService
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RebuildWorkspaceHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val permissionService: PermissionService,
    private val dispatcher: SampleEventDispatcher,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val notifyControl: NotifyControl,
    private val softwareManageService: SoftwareManageService,
    private val workspaceSharedDao: WorkspaceSharedDao
) {
    @ActionAuditRecord(
        actionId = ActionId.CGS_REBUILD_SYSTEM_DISK,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = TencentActionAuditContent.CGS_REBUILD_SYSTEM_DISK_CONTENT
    )
    fun rebuildWorkspace(
        userId: String,
        workspaceName: String,
        rebuildReq: WorkspaceRebuildReq
    ): WorkspaceResponse {
        logger.info("$userId rebuild project workspace $workspaceName")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )

        if (!permissionService.hasManagerOrOwnerPermission(
                userId = userId,
                projectId = workspace.projectId,
                workspaceName = workspace.workspaceName,
                ownerType = workspace.ownerType
            )
        ) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to rebuild $workspaceName")
            )
        }
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .setScopeId(workspace.projectId)

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).tryLock().use {
            // 异常状态的允许直接重装，进行中的不允许。
            if (workspace.status.checkException()) {
                workspaceCommon.fixUnexpectedStatus(
                    status = workspace.status,
                    userId = userId,
                    workspaceName = workspaceName,
                    mountType = workspace.workspaceMountType
                )
            }
            if (workspaceCommon.notOk2doNextAction(workspace)) {
                logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    params = arrayOf(
                        workspace.workspaceName,
                        "status is already ${workspace.status}, can't rebuild now"
                    )
                )
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.REBUILD,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.REBUILDING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.REBUILD,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status,
                    WorkspaceStatus.REBUILDING
                ) + if (rebuildReq.formatDataDisk == true) {
                    ", 格式化数据盘: " + rebuildReq.formatDataDisk.toString()
                } else {
                    ""
                } + if (rebuildReq.removeOwner == true) {
                    ", 清空拥有者: " + rebuildReq.removeOwner.toString()
                } else {
                    ""
                }
            )

            val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.REBUILD,
                    workspaceName = workspaceName,
                    mountType = WorkspaceMountType.START,
                    imageCosFile = rebuildReq.imageCosFile,
                    appName = gameId.first,
                    formatDataDisk = rebuildReq.formatDataDisk ?: false,
                    rebuildRemoveOwner = rebuildReq.removeOwner ?: false
                )
            )

            notifyControl.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_REBUILD,
                status = true,
                action = WorkspaceAction.REBUILDING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START,
                ownerType = workspace.ownerType,
                projectId = workspace.projectId
            )

            return WorkspaceResponse(
                workspaceName = workspaceName,
                workspaceHost = "",
                status = WorkspaceAction.REBUILDING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START
            )
        }
    }

    fun rebuildWorkspaceCallback(event: RemoteDevUpdateEvent) {
        val options = if (!event.taskUid.isNullOrBlank()) {
            WorkspaceOperateCommonObject.getRebuildOptions(redisOperation, event.taskUid!!)
        } else {
            logger.error("rebuildWorkspaceCallback event $event taskUid is null")
            null
        }
        val workspace = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = event.workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(event.workspaceName)
        )
        if (event.status) {
            // 成功的调用才删除参数
            logger.debug("rebuildWorkspace|getRebuildOptions|${event.workspaceName}|${event.userId}")
            // 重写IOA注册表
            if (workspace.workspaceSystemType.needSafeInitialization()) {
                softwareManageService.safeInitialization(
                    projectId = workspace.projectId,
                    userId = event.userId,
                    workspaceName = event.workspaceName
                )
            }
            if (options != null) {
                WorkspaceOperateCommonObject.deleteRebuildOptions(redisOperation, event.taskUid!!)
            }
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                var toStatus = WorkspaceStatus.RUNNING
                if (options?.removeOwner == true) {
                    workspaceSharedDao.deleteOwner(transactionContext, event.workspaceName)
                    toStatus = WorkspaceStatus.DISTRIBUTING
                }
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = event.workspaceName,
                    status = toStatus,
                    dslContext = transactionContext
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    operator = event.userId,
                    action = WorkspaceAction.REBUILD,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        WorkspaceStatus.REBUILDING,
                        toStatus.name
                    )
                )
            }
        } else {
            // 启动失败,记录为EXCEPTION
            logger.warn("rebuild workspace ${event.workspaceName} failed")
            workspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = event.workspaceName,
                operator = event.userId,
                action = WorkspaceAction.REBUILD,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        // 分发到WS
        notifyControl.dispatchWebsocketPushEvent(
            userId = event.userId,
            workspaceName = event.workspaceName,
            workspaceHost = "",
            errorMsg = event.errorMsg,
            type = WebSocketActionType.WORKSPACE_REBUILD,
            status = event.status,
            action = WorkspaceAction.REBUILD,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RebuildWorkspaceHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }
}
