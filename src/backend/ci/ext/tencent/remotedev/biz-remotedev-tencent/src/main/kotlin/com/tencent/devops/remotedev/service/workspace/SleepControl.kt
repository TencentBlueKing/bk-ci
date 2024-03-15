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
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.cron.HolidayHelper
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.BKBaseService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import java.time.LocalDateTime
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
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val redisHeartBeat: RedisHeartBeat,
    private val workspaceCommon: WorkspaceCommon,
    private val notifyControl: NotifyControl,
    private val bkBaseService: BKBaseService,
    private val holidayHelper: HolidayHelper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SleepControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_STOP,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_STOP_CONTENT
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
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId
        if (needPermission) {
            permissionService.checkOwnerPermission(userId, workspaceName, workspace.projectId)
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

            // 发送处理事件
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType
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

    fun systemStopWS(workspaceName: String, opHistory: OpHistoryCopyWriting): Boolean {

        val workspace = workspaceDao.fetchAnyWorkspace(dslContext = dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 校验状态
        if (workspace.status.checkSleeping()) {
            logger.info("$workspace has been stopped, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't stop again")
            )
        }

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:${workspace.workspaceId}",
            expiredTimeInSeconds
        ).tryLock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.workspaceName,
                operator = Constansts.ADMIN_NAME,
                action = WorkspaceAction.SLEEP,
                actionMessage = workspaceCommon.getOpHistory(opHistory)
            )

            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = workspaceCommon.getSystemOperator(workspace.createUserId, workspace.workspaceMountType),
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType
                )
            )

            // 发送给用户
            notifyControl.dispatchWebsocketPushEvent(
                userId = Constansts.ADMIN_NAME,
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

    fun heartBeatStopWS(workspaceName: String, opHistory: OpHistoryCopyWriting): Boolean {

        val workspace = workspaceDao.fetchAnyWorkspace(dslContext = dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 校验状态
        if (workspace.status.checkSleeping()) {
            logger.info("$workspace has been stopped, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't stop again")
            )
        }

        if (!workspaceCommon.checkProjectRouter(
                creator = workspace.createUserId,
                workspaceName = workspaceName,
                workspaceOwnerType = workspace.ownerType
            )
        ) return false

        return systemStopWS(workspaceName, opHistory)
    }

    fun autoSleepWhenNotLogin(onSleep: Boolean = false, readySleepWorkspace: MutableList<String> = mutableListOf()) {
        val limitDay = holidayHelper.getLastWorkingDays(7).last()
        val logins = bkBaseService.fetchOnlineIps(limitDay)
        logger.info("autoDeleteWhenSleep7Day|$limitDay|${logins.size}")
        workspaceDao.fetchWorkspace(
            dslContext = dslContext,
            status = WorkspaceStatus.RUNNING,
            systemType = WorkspaceSystemType.WINDOWS_GPU
        )?.parallelStream()?.forEach { workspace ->
            if ((workspace.lastStatusUpdateTime ?: LocalDateTime.now()) < limitDay &&
                workspace.hostName != null && workspace.hostName !in logins
            ) {
                logger.info(
                    "ready to sleep when not login 7 day " +
                            "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|${workspace.hostName}"
                )
                readySleepWorkspace.add(
                    "project=${workspace.projectId}, ip=${workspace.hostName}," +
                            " 原因=超过7天未登陆(最近登陆时间: ${logins[workspace.hostName]}" +
                            " 早于检测时间 ${limitDay.format(formatter)})"
                )
                if (onSleep) {
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspace.workspaceName,
                        operator = Constansts.ADMIN_NAME,
                        action = WorkspaceAction.DELETE,
                        actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
                    )
                    kotlin.runCatching { systemStopWS(workspace.workspaceName, OpHistoryCopyWriting.TIMEOUT_SLEEP) }
                        .onFailure { i ->
                            logger.warn("auto sleep fail|${i.message}", i)
                        }.onSuccess {
                            logger.info(
                                "sleep $it when not login 7 day " +
                                        "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|" +
                                        "${workspace.hostName}"
                            )
                            if (it) {
                                val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
                                notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                                    userIds = userIds.toMutableSet(),
                                    workspaceName = workspace.workspaceName,
                                    cc = mutableSetOf(workspace.createUserId),
                                    projectId = workspace.projectId,
                                    notifyTemplateCode = NotifyControl.NOT_LOGIN_AUTO_SLEEP_NOTIFY,
                                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                                    bodyParams = mutableMapOf(
                                        "cgsIp" to (workspace.hostName ?: ""),
                                        "projectId" to (workspace.projectId),
                                        "userId" to userIds.joinToString()
                                    )
                                )
                            }
                        }
                }
            }
        }
    }

    fun afterStopWorkspace(event: RemoteDevUpdateEvent) {
        if (!event.status) {
            // 调devcloud接口查询是否已经启动成功，如果成功还是走成功的逻辑.
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, event.workspaceName, event.mountType).data!!
            when (workspaceInfo.status) {
                EnvStatusEnum.stopped -> event.status = true
                else -> logger.warn(
                    "stop workspace callback with error|" +
                            "${event.workspaceName}|${workspaceInfo.status}"
                )
            }
        }
        doStopWS(event.status, event.userId, event.workspaceName, event.errorMsg)
    }

    fun doStopWS(status: Boolean, operator: String, workspaceName: String, errorMsg: String? = null) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        if (workspace.status.checkSleeping()) return
        if (status) {
            // 清心跳
            redisHeartBeat.deleteWorkspaceHeartbeat(operator, workspaceName)
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
