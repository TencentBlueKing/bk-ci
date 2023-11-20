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
import com.tencent.devops.environment.api.ServiceNodeResource
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
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.BKCCService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
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
    private val remoteDevSettingService: RemoteDevSettingService,
    private val workspaceCommon: WorkspaceCommon,
    private val bkccService: BKCCService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeleteControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_DELETE_CONTENT
    )
    fun deleteWorkspace(
        userId: String,
        workspaceName: String,
        needPermission: Boolean = true,
        checkDeleteImmediately: Boolean? = null
    ): Boolean {
        logger.info("$userId delete workspace $workspaceName")
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
            val deleteImmediately = checkDeleteImmediately ?: checkWorkspaceStatusForDelete(workspace, userId)

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
                    userId = workspace.createUserId,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType
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
                systemType = workspace.workspaceSystemType,
                workspaceMountType = workspace.workspaceMountType,
                ownerType = workspace.ownerType,
                projectId = workspace.projectId
            )
            return true
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_DELETE_CONTENT
    )
    fun deleteWorkspace4OP(
        userId: String,
        workspaceName: String
    ): Boolean {
        logger.info("$userId delete workspace $workspaceName")
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).tryLock().use {

            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
            // 审计
            ActionAuditContext.current()
                .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
                .scopeId = workspace.projectId

            // 创建操作历史记录
            createDeleteOperationHistoryRecord(workspace, userId)

            // 如果需要立即删除，则执行删除操作
            doDeleteWS(true, userId, workspaceName, null)

            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()

            // 发送处理事件
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType
                )
            )
            return true
        }
    }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun deleteInactivityWorkspace() {
        logger.info("getTimeOutInactivityWorkspace")
        workspaceDao.getTimeOutInactivityWorkspace(
            timeOutDays = Constansts.timeoutDays,
            dslContext = dslContext,
            systemType = WorkspaceSystemType.LINUX
        ).parallelStream().forEach {
            MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            logger.info(
                "workspace ${it.workspaceName} last active is ${
                    it.updateTime
                } ready to delete"
            )
            kotlin.runCatching { heartBeatDeleteWS(it) }.onFailure { i ->
                logger.warn("deleteInactivityWorkspace fail|${i.message}", i)
            }
        }
        val now = LocalDateTime.now()
        workspaceDao.fetchNotUsageTimeWinWorkspace(dslContext, status = WorkspaceStatus.SLEEP)
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

    fun heartBeatDeleteWS(workspace: WorkspaceRecord): Boolean {
        logger.info("heart beat delete workspace ${workspace.workspaceName}")
        // 校验状态
        if (workspace.status.checkDeleted()) {
            logger.info("$workspace has been deleted, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't delete again")
            )
        }

        if (!workspaceCommon.checkProjectRouter(
                creator = workspace.createUserId,
                workspaceName = workspace.workspaceName,
                workspaceOwnerType = workspace.ownerType
            )
        ) return false
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:${workspace.workspaceName}",
            expiredTimeInSeconds
        ).tryLock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.workspaceName,
                operator = ADMIN_NAME,
                action = WorkspaceAction.DELETE,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
            )
            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = workspaceCommon.getSystemOperator(workspace.createUserId, workspace.workspaceMountType),
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType
                )
            )

            workspaceCommon.dispatchWebsocketPushEvent(
                userId = ADMIN_NAME,
                workspaceName = workspace.workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_DELETE,
                status = true,
                action = WorkspaceAction.DELETING,
                systemType = workspace.workspaceSystemType,
                workspaceMountType = workspace.workspaceMountType,
                ownerType = workspace.ownerType,
                projectId = workspace.projectId
            )
            return true
        }
    }

    fun afterDeleteWorkspace(event: RemoteDevUpdateEvent) {
        logger.debug("afterDeleteWorkspace|RemoteDevUpdateEvent{}|", event)
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
        doDeleteWS(event.status, event.userId, event.workspaceName, event.errorMsg)
    }

    fun doDeleteWS(
        status: Boolean,
        operator: String,
        workspaceName: String,
        errorMsg: String? = null
    ) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        if (workspace.status.checkDeleted()) return

        val detail = workspaceCommon.getWorkspaceDetail(workspaceName)

        val projectId = remoteDevSettingDao.fetchAnySetting(dslContext, workspace.createUserId).projectId
        if (status) {
            // 删除环境管理第三方构建机记录
            if (!workspace.preciAgentId.isNullOrBlank() && client.get(ServiceNodeResource::class)
                    .deleteThirdPartyNode(workspace.createUserId, projectId, workspace.preciAgentId!!).data == false
            ) {
                logger.warn(
                    "delete workspace $workspaceName, but third party agent delete failed." +
                        "|${workspace.createUserId}|$projectId|${detail?.environmentIP}|${workspace.preciAgentId}"
                )
            }
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
                        workspace.status.name,
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
                    workspace.status.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            remoteDevSettingService.computeWinUsageTime(userId = workspace.createUserId)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceCommon.updateLastHistory(transactionContext, workspaceName, operator)
            remoteDevBillingDao.endBilling(
                dslContext = transactionContext,
                workspaceName = workspaceName,
                computeUsageTime = workspace.ownerType == WorkspaceOwnerType.PERSONAL
            )
        }

        // 删除时给 cmdb 去掉字段方便监控检索
        val hostIdSub = detail?.environmentIP?.split(".")
        if (!hostIdSub.isNullOrEmpty() && workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")
            bkccService.updateHostMonitor(
                regionId = null,
                workspaceName = workspaceName,
                ips = setOf(ip),
                props = mapOf("devx_meta" to "")
            )

            // 删除 cmdb 的机器别名
            bkccService.updateHostName("VM-${hostIdSub.joinToString("-")}", workspaceName)
        }

        workspaceCommon.dispatchWebsocketPushEvent(
            userId = operator,
            workspaceName = workspaceName,
            workspaceHost = null,
            errorMsg = errorMsg,
            type = WebSocketActionType.WORKSPACE_DELETE,
            status = status,
            action = WorkspaceAction.DELETE,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }

    private fun checkWorkspaceStatusForDelete(workspace: WorkspaceRecord, userId: String): Boolean {

        if (workspace.status.checkDeleted()) {
            logger.info("${workspace.workspaceName} has been deleted, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't delete again")
            )
        }

        if (workspace.status.checkDeliveringFailed()) {
            logger.info("${workspace.workspaceName} is DELIVERING_FAILED, delete immediately.")
            return true
        }

        if (workspaceCommon.notOk2doNextAction(workspace)) {
            logger.info("${workspace.workspaceName} is $workspace.status, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.workspaceName, "status is already ${workspace.status}, can't delete now")
            )
        }

        var deleteImmediately = false
        kotlin.runCatching {
            workspaceCommon.checkAndFixExceptionWS(
                status = workspace.status,
                userId = userId,
                workspaceName = workspace.workspaceName,
                mountType = workspace.workspaceMountType
            )
        }.onFailure {
            if (it is ErrorCodeException && it.errorCode == ErrorCodeEnum.WORKSPACE_ERROR.errorCode) {
                deleteImmediately = true
            } else throw it
        }

        return deleteImmediately
    }

    private fun createDeleteOperationHistoryRecord(workspace: WorkspaceRecord, userId: String) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = transactionContext,
                workspaceName = workspace.workspaceName,
                operator = userId,
                action = WorkspaceAction.DELETE,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.DELETE)
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = transactionContext,
                workspaceName = workspace.workspaceName,
                status = WorkspaceStatus.DELETING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = transactionContext,
                workspaceName = workspace.workspaceName,
                operator = userId,
                action = WorkspaceAction.DELETING,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status.name,
                    WorkspaceStatus.DELETING.name
                )
            )
        }
    }
}
