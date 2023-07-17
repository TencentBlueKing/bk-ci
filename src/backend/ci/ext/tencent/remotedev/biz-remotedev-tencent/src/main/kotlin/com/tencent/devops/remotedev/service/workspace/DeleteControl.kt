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
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
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
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Suppress("LongMethod")
class DeleteControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val redisCache: RedisCacheService,
    private val workspaceCommon: WorkspaceCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeleteControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    fun deleteWorkspace(userId: String, workspaceName: String, needPermission: Boolean = true): Boolean {
        logger.info("$userId delete workspace $workspaceName")
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
            val deleteImmediately = checkWorkspaceStatusForDelete(workspace, userId)

            // 创建操作历史记录
            createDeleteOperationHistoryRecord(workspace, userId)

            // 如果需要立即删除，则执行删除操作
            if (deleteImmediately) {
                doDeleteWS(true, userId, workspaceName, null)
            }

            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()

            // 发送处理事件
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
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
                type = WebSocketActionType.WORKSPACE_DELETE,
                status = true,
                action = WorkspaceAction.DELETING,
                systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
            )
            return true
        }
    }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun deleteInactivityWorkspace() {
        logger.info("getTimeOutInactivityWorkspace")
        workspaceDao.getTimeOutInactivityWorkspace(
            Constansts.timeoutDays, dslContext
        ).parallelStream().forEach {
            MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            logger.info(
                "workspace ${it.name} last active is ${
                    it.updateTime
                } ready to delete"
            )
            kotlin.runCatching { heartBeatDeleteWS(it) }.onFailure { i ->
                logger.warn("deleteInactivityWorkspace fail|${i.message}", i)
            }
        }
        val now = LocalDateTime.now()
        workspaceDao.fetchWorkspace(dslContext, status = WorkspaceStatus.SLEEP, mountType = WorkspaceMountType.START)
            ?.parallelStream()?.forEach {
                MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
                val retentionTime = redisCache.get(RedisKeys.REDIS_DESTRUCTION_RETENTION_TIME)?.toInt() ?: 3
                if (Duration.between(it.lastStatusUpdateTime, now).toDays() >= retentionTime) {
                    kotlin.runCatching { heartBeatDeleteWS(it) }.onFailure { i ->
                        logger.warn("deleteInactivityWorkspace fail|${i.message}", i)
                    }
                }
            }
    }

    fun heartBeatDeleteWS(workspace: TWorkspaceRecord): Boolean {
        logger.info("heart beat delete workspace ${workspace.name}")
        // 校验状态
        val status = WorkspaceStatus.values()[workspace.status]
        if (status.checkDeleted()) {
            logger.info("$workspace has been deleted, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't delete again")
            )
        }

        if (!workspaceCommon.checkProjectRouter(workspace.creator, workspace.name)) return false
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:${workspace.name}",
            expiredTimeInSeconds
        ).lock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.name,
                operator = ADMIN_NAME,
                action = WorkspaceAction.DELETE,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
            )
            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = workspaceCommon.getSystemOperator(workspace.creator, workspace.workspaceMountType),
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.name,
                    mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                )
            )

            workspaceCommon.dispatchWebsocketPushEvent(
                userId = ADMIN_NAME,
                workspaceName = workspace.name,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_DELETE,
                status = true,
                action = WorkspaceAction.DELETING,
                systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
            )
            return true
        }
    }

    fun afterDeleteWorkspace(event: RemoteDevUpdateEvent) {
        if (!event.status) {
            // 调devcloud接口查询是否已经成功，如果成功还是走成功的逻辑.
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, event.workspaceName, event.mountType).data!!
            when (workspaceInfo.status) {
                EnvStatusEnum.deleted -> event.status = true
                else -> logger.warn(
                    "delete workspace callback with error|" +
                        "${event.workspaceName}|${workspaceInfo.status}"
                )
            }
        }
        doDeleteWS(event.status, event.userId, event.workspaceName, event.environmentIp, event.errorMsg)
    }

    fun doDeleteWS(
        status: Boolean,
        operator: String,
        workspaceName: String,
        nodeIp: String?,
        errorMsg: String? = null
    ) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkDeleted()) return
        if (status) {
            // 删除环境管理第三方构建机记录
            val projectId = remoteDevSettingDao.fetchAnySetting(dslContext, workspace.creator).projectId
            if (!workspace.preciAgentId.isNullOrBlank() && client.get(ServiceNodeResource::class)
                    .deleteThirdPartyNode(workspace.creator, projectId, workspace.preciAgentId).data == false
            ) {
                logger.warn(
                    "delete workspace $workspaceName, but third party agent delete failed." +
                        "|${workspace.creator}|$projectId|$nodeIp|${workspace.preciAgentId}"
                )
            }
            // 清缓存
            redisCache.deleteWorkspaceDetail(workspaceName)
            // 清心跳
            redisHeartBeat.deleteWorkspaceHeartbeat(operator, workspaceName)
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.DELETED,
                    dslContext = transactionContext
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = operator,
                    action = WorkspaceAction.DELETE,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        oldStatus.name,
                        WorkspaceStatus.DELETED.name
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
                action = WorkspaceAction.DELETE,
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
            type = WebSocketActionType.WORKSPACE_DELETE,
            status = status,
            action = WorkspaceAction.DELETE,
            systemType = WorkspaceSystemType.valueOf(workspace.systemType),
            workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
        )
    }

    private fun checkWorkspaceStatusForDelete(workspace: TWorkspaceRecord, userId: String): Boolean {
        val status = WorkspaceStatus.values()[workspace.status]

        if (status.checkDeleted()) {
            logger.info("${workspace.name} has been deleted, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't delete again")
            )
        }

        if (workspaceCommon.notOk2doNextAction(workspace)) {
            logger.info("${workspace.name} is $status, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't delete now")
            )
        }

        var deleteImmediately = false
        kotlin.runCatching {
            workspaceCommon.checkAndFixExceptionWS(
                status = status,
                userId = userId,
                workspaceName = workspace.name,
                mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
            )
        }.onFailure {
            if (it is ErrorCodeException && it.errorCode == ErrorCodeEnum.WORKSPACE_ERROR.errorCode) {
                deleteImmediately = true
            } else throw it
        }

        return deleteImmediately
    }

    private fun createDeleteOperationHistoryRecord(workspace: TWorkspaceRecord, userId: String) {
        val name = workspace.name
        val status = WorkspaceStatus.values()[workspace.status]

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = transactionContext,
                workspaceName = name,
                operator = userId,
                action = WorkspaceAction.DELETE,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.DELETE)
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = transactionContext,
                workspaceName = name,
                status = WorkspaceStatus.DELETING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = transactionContext,
                workspaceName = name,
                operator = userId,
                action = WorkspaceAction.DELETING,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    status.name,
                    WorkspaceStatus.DELETING.name
                )
            )
        }
    }
}
