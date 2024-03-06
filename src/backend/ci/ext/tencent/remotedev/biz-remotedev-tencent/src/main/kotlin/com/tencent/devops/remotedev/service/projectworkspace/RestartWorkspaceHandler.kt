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
import com.tencent.devops.common.api.util.DateTimeUtil
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
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.SshPublicKeysService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.WINDOWS_GPU_RESTART_NOTIFY
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.util.Date
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class RestartWorkspaceHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val permissionService: PermissionService,
    private val sshService: SshPublicKeysService,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val notifyControl: NotifyControl
) {
    private val executor = Executors.newCachedThreadPool()

    companion object {
        private val logger = LoggerFactory.getLogger(RestartWorkspaceHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_RESTART,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_RESTART_CONTENT
    )
    fun restartWorkspace(userId: String, projectId: String, workspaceName: String): WorkspaceResponse {
        logger.info("$userId restart project workspace $workspaceName")
        if (!permissionService.hasOwnerPermission(
                userId = userId,
                workspaceName = workspaceName,
                projectId = projectId
            ) && !permissionService.hasUserManager(userId, projectId)
        ) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to restart $workspaceName")
            )
        }

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
                        "status is already ${workspace.status}, can't restart now"
                    )
                )
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.RESTART,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.RESTARTING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.RESTART,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status,
                    WorkspaceStatus.RESTARTING
                )
            )

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.RESTART,
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
                type = WebSocketActionType.WORKSPACE_RESTART,
                status = true,
                action = WorkspaceAction.RESTARTING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START,
                ownerType = WorkspaceOwnerType.PROJECT,
                projectId = projectId
            )

            return WorkspaceResponse(
                workspaceName = workspaceName,
                workspaceHost = "",
                status = WorkspaceAction.RESTARTING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START
            )
        }
    }

    fun restartWorkspaceCallback(event: RemoteDevUpdateEvent) {
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
                    status = WorkspaceStatus.RUNNING,
                    dslContext = transactionContext
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    operator = event.userId,
                    action = WorkspaceAction.RESTART,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        WorkspaceStatus.RESTARTING,
                        WorkspaceStatus.RUNNING.name
                    )
                )
                notifyControl.notify4User(
                    userIds = permissionService.getWorkspaceOwner(workspace.workspaceName).toMutableSet(),
                    notifyTemplateCode = WINDOWS_GPU_RESTART_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.CLIENT_PUSH),
                    bodyParams = mutableMapOf(
                        "workspaceName" to workspace.workspaceName,
                        "projectId" to workspace.projectId,
                        "cgsId" to (workspace.hostName ?: workspace.workspaceName),
                        "displayName" to workspace.displayName,
                        "time" to DateTimeUtil.formatDate(Date())
                    )
                )
            }
            // 重装成功后做异步设置(L盘挂载)
            val ip = event.environmentIp?.substringAfter(".")
            ip?.let { it ->
                executor.execute {
                    workspaceCommon.makeDiskMount(it, event.userId)
                }
            }
        } else {
            // 启动失败,记录为EXCEPTION
            logger.warn("restart workspace ${event.workspaceName} failed")
            workspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = event.workspaceName,
                operator = event.userId,
                action = WorkspaceAction.RESTART,
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
            type = WebSocketActionType.WORKSPACE_RESTART,
            status = event.status,
            action = WorkspaceAction.RESTART,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }
}
