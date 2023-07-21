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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

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
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val redisCache: RedisCacheService,
    private val workspaceCommon: WorkspaceCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SleepControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    fun stopWorkspace(userId: String, workspaceName: String, needPermission: Boolean = true): Boolean {
        logger.info("$userId stop workspace $workspaceName")
        if (needPermission) {
            permissionService.checkPermission(userId, workspaceName)
        }
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {

            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )

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
                    workspaceName = workspace.name,
                    mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                )
            )

            // 发送给用户
            workspaceCommon.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_SLEEP,
                status = true,
                action = WorkspaceAction.SLEEPING,
                systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
            )
            return true
        }
    }

    private fun checkWorkspaceStatus(workspace: TWorkspaceRecord, userId: String) {
        val status = WorkspaceStatus.values()[workspace.status]

        if (status.checkSleeping()) {
            logger.info("${workspace.name} has been stopped, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't stop again")
            )
        }

        if (workspaceCommon.notOk2doNextAction(workspace)) {
            logger.info("${workspace.name} is $status, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't stop now")
            )
        }

        // 处理异常的情况
        workspaceCommon.checkAndFixExceptionWS(
            status,
            userId,
            workspace.name,
            WorkspaceMountType.valueOf(workspace.workspaceMountType)
        )
    }

    private fun createOperationHistoryRecord(workspace: TWorkspaceRecord, userId: String) {
        val name = workspace.name
        val status = WorkspaceStatus.values()[workspace.status]

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = name,
            operator = userId,
            action = WorkspaceAction.SLEEP,
            actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
        )

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = name,
            operator = userId,
            action = WorkspaceAction.SLEEP,
            actionMessage = String.format(
                workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                status.name,
                WorkspaceStatus.SLEEPING.name
            )
        )
    }

    fun heartBeatStopWS(workspaceName: String, opHistory: OpHistoryCopyWriting): Boolean {

        val workspace = workspaceDao.fetchAnyWorkspace(dslContext = dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 校验状态
        val status = WorkspaceStatus.values()[workspace.status]
        if (status.checkSleeping()) {
            logger.info("$workspace has been stopped, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't stop again")
            )
        }

        if (!workspaceCommon.checkProjectRouter(workspace.creator, workspaceName)) return false

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:${workspace.id}",
            expiredTimeInSeconds
        ).lock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.name,
                operator = Constansts.ADMIN_NAME,
                action = WorkspaceAction.SLEEP,
                actionMessage = workspaceCommon.getOpHistory(opHistory)
            )

            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = workspaceCommon.getSystemOperator(workspace.creator, workspace.workspaceMountType),
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.name,
                    mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                )
            )

            // 发送给用户
            workspaceCommon.dispatchWebsocketPushEvent(
                userId = Constansts.ADMIN_NAME,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_SLEEP,
                status = true,
                action = WorkspaceAction.SLEEPING,
                systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
            )
            return true
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
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkSleeping()) return
        if (status) {
            // 清缓存
            redisCache.deleteWorkspaceDetail(workspaceName)
            // 清心跳
            redisHeartBeat.deleteWorkspaceHeartbeat(operator, workspaceName)
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.SLEEP,
                    dslContext = transactionContext
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = operator,
                    action = WorkspaceAction.SLEEP,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        oldStatus.name,
                        WorkspaceStatus.SLEEP.name
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
                    oldStatus.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceCommon.updateLastHistory(transactionContext, workspaceName, operator)
            remoteDevBillingDao.endBilling(transactionContext, workspaceName)
        }

        workspaceCommon.dispatchWebsocketPushEvent(
            userId = operator,
            workspaceName = workspaceName,
            workspaceHost = null,
            errorMsg = errorMsg,
            type = WebSocketActionType.WORKSPACE_SLEEP,
            status = status,
            action = WorkspaceAction.SLEEP,
            systemType = WorkspaceSystemType.valueOf(workspace.systemType),
            workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
        )
    }
}
