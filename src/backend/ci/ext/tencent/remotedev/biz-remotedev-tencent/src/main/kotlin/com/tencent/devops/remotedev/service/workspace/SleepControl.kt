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

package com.tencent.devops.remotedev.service.workspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.auth.api.TencentResourceTypeId
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class SleepControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val dispatcher: SampleEventDispatcher,
    private val workspaceCommon: WorkspaceCommon,
    private val notifyControl: NotifyControl
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SleepControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @ActionAuditRecord(
        actionId = TencentActionId.CGS_STOP,
        instance = AuditInstanceRecord(
            resourceType = TencentResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = TencentActionAuditContent.CGS_STOP_CONTENT
    )
    fun stopWorkspace(userId: String, workspaceName: String, needPermission: Boolean = true): Boolean {
        logger.info("$userId stop workspace $workspaceName")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 审计
        ActionAuditContext.current()
            .addAttribute(TencentActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId
        if (needPermission) {
            permissionService.checkOwnerPermission(userId, workspaceName, workspace.projectId, workspace.ownerType)
        }
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).tryLock().use {

            // 校验状态以及处理异常的情况
            checkWorkspaceStatus(workspace, userId)

            // 创建操作历史记录
            createOperationHistoryRecord(workspace, userId)

            // 更新工作区状态
            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.SLEEPING
            )

            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
            val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)

            // 发送处理事件
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType,
                    appName = gameId.first
                )
            )

            // 发送给用户
            notifyControl.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_SLEEP,
                status = true,
                action = WorkspaceAction.SLEEPING,
                systemType = workspace.workspaceSystemType,
                workspaceMountType = workspace.workspaceMountType,
                ownerType = workspace.ownerType,
                projectId = workspace.projectId
            )
            return true
        }
    }

    private fun checkWorkspaceStatus(workspace: WorkspaceRecord, userId: String) {

        if (workspace.status.checkSleeping()) {
            logger.info("${workspace.workspaceName} has been stopped, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't stop again")
            )
        }

        if (workspaceCommon.notOk2doNextAction(workspace)) {
            logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't stop now")
            )
        }

        // 处理异常的情况
        workspaceCommon.checkAndFixExceptionWS(
            status = workspace.status,
            userId = userId,
            workspaceName = workspace.workspaceName,
            mountType = workspace.workspaceMountType
        )
    }

    private fun createOperationHistoryRecord(workspace: WorkspaceRecord, userId: String) {

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspace.workspaceName,
            operator = userId,
            action = WorkspaceAction.SLEEP,
            actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
        )

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspace.workspaceName,
            operator = userId,
            action = WorkspaceAction.SLEEP,
            actionMessage = String.format(
                workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                workspace.status.name,
                WorkspaceStatus.SLEEPING.name
            )
        )
    }

    fun doStopWS(status: Boolean, operator: String, workspaceName: String, errorMsg: String? = null) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        if (workspace.status.checkSleeping()) return
        if (status) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.STOPPED,
                    dslContext = transactionContext
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = operator,
                    action = WorkspaceAction.SLEEP,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        workspace.status.name,
                        WorkspaceStatus.STOPPED.name
                    )
                )
            }
        } else {
            workspaceDao.updateWorkspaceStatus(
                workspaceName = workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = operator,
                action = WorkspaceAction.SLEEP,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        workspaceCommon.statisticalData(workspace, operator)

        notifyControl.dispatchWebsocketPushEvent(
            userId = operator,
            workspaceName = workspaceName,
            workspaceHost = null,
            errorMsg = errorMsg,
            type = WebSocketActionType.WORKSPACE_SLEEP,
            status = status,
            action = WorkspaceAction.SLEEP,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }
}
