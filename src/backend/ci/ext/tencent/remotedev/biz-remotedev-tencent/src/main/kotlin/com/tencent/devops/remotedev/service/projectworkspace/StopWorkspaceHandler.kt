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
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.SshPublicKeysService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StopWorkspaceHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val permissionService: PermissionService,
    private val sshService: SshPublicKeysService,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceCommon: WorkspaceCommon,
    private val notifyControl: NotifyControl
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StopWorkspaceHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_STOP,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_STOP_CONTENT
    )
    fun stopWorkspace(userId: String, projectId: String, workspaceName: String): WorkspaceResponse {
        logger.info("$userId stop project workspace $workspaceName")
        permissionService.checkUserManager(userId, projectId)
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).tryLock().use {
            if (workspaceCommon.notOk2doNextAction(workspace)) {
                logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    params = arrayOf(
                        workspace.workspaceName,
                        "status is already ${workspace.status}, can't stop now"
                    )
                )
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.STOP,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.STOP,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status,
                    WorkspaceStatus.STOPPING.name
                )
            )

            // 更新工作区状态
            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.STOPPING
            )

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.STOP,
                    sshKeys = sshService.getSshPublicKeys4Ws(
                        workspaceDao.fetchWorkspaceUser(
                            dslContext,
                            workspaceName
                        ).toSet()
                    ),
                    workspaceName = workspaceName,
                    settingEnvs = remoteDevSettingDao.fetchOneSetting(dslContext, userId).envsForVariable,
                    bkTicket = "",
                    mountType = WorkspaceMountType.START
                )
            )

            notifyControl.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_STOP,
                status = true,
                action = WorkspaceAction.STOPPING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START,
                ownerType = WorkspaceOwnerType.PROJECT,
                projectId = projectId
            )

            return WorkspaceResponse(
                workspaceName = workspaceName,
                workspaceHost = "",
                status = WorkspaceAction.STOPPING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START
            )
        }
    }

    fun stopWorkspaceCallback(event: RemoteDevUpdateEvent) {
        val workspace = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = event.workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(event.workspaceName)
        )
        if (event.status) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = event.workspaceName,
                    status = WorkspaceStatus.STOPPED,
                    dslContext = transactionContext
                )

                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    operator = event.userId,
                    action = WorkspaceAction.START,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        workspace.status.name,
                        WorkspaceStatus.STOPPED.name
                    )
                )
            }
        } else {
            // 启动失败,记录为EXCEPTION
            logger.warn("stop workspace ${event.workspaceName} failed")
            workspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = event.workspaceName,
                operator = event.userId,
                action = WorkspaceAction.STOP,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }
        workspaceCommon.statisticalData(workspace, event.userId)

        // 分发到WS
        notifyControl.dispatchWebsocketPushEvent(
            userId = event.userId,
            workspaceName = event.workspaceName,
            workspaceHost = "",
            errorMsg = event.errorMsg,
            type = WebSocketActionType.WORKSPACE_STOP,
            status = event.status,
            action = WorkspaceAction.STOP,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }
}
