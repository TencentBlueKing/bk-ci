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
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.BkTicketService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.SshPublicKeysService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
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
class StartControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val sshService: SshPublicKeysService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val bkTicketServie: BkTicketService,
    private val workspaceCommon: WorkspaceCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StartControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    fun startWorkspace(userId: String, bkTicket: String, workspaceName: String): WorkspaceResponse {
        logger.info("$userId start workspace $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
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
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            when {
                status.checkRunning() -> {
                    logger.info("${workspace.name} is running.")
                    remoteDevBillingDao.newBilling(dslContext, workspaceName, userId)
                    val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                        .getWorkspaceInfo(
                            userId, workspaceName,
                            WorkspaceMountType.valueOf(workspace.workspaceMountType)
                        )
                    bkTicketServie.updateBkTicket(
                        userId,
                        bkTicket,
                        workspaceInfo.data?.environmentHost,
                        WorkspaceMountType.valueOf(workspace.workspaceMountType)
                    )

                    return WorkspaceResponse(
                        workspaceName = workspaceName,
                        workspaceHost = workspaceInfo.data?.environmentHost ?: "",
                        status = WorkspaceAction.START,
                        systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                        workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                    )
                }

                workspaceCommon.notOk2doNextAction(workspace) -> {
                    logger.info("${workspace.name} is $status, return error.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                        params = arrayOf(workspace.name, "status is already $status, can't start now")
                    )
                }

                else -> {
                    permissionService.checkUserCreate(userId, true)
                    /*处理异常的情况*/
                    workspaceCommon.checkAndFixExceptionWS(
                        status,
                        userId,
                        workspaceName,
                        WorkspaceMountType.valueOf(workspace.workspaceMountType)
                    )
                    workspaceCommon.checkWorkspaceAvailability(userId, workspace)
                    createWorkspaceHistoryForStart(userId, workspaceName)
                    updateWorkspaceStatus(workspace.name, status, userId)
                    val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
                    dispatcher.dispatch(
                        WorkspaceOperateEvent(
                            userId = userId,
                            traceId = bizId,
                            type = UpdateEventType.START,
                            sshKeys = sshService.getSshPublicKeys4Ws(
                                workspaceDao.fetchWorkspaceUser(
                                    dslContext,
                                    workspaceName
                                ).toSet()
                            ),
                            workspaceName = workspace.name,
                            settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId).envsForVariable,
                            bkTicket = bkTicket,
                            mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                        )
                    )

                    // 发送给用户
                    workspaceCommon.dispatchWebsocketPushEvent(
                        userId = userId,
                        workspaceName = workspaceName,
                        workspaceHost = null,
                        errorMsg = null,
                        type = WebSocketActionType.WORKSPACE_START,
                        status = true,
                        action = WorkspaceAction.STARTING,
                        systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                        workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                    )
                    return WorkspaceResponse(
                        workspaceName = workspace.name,
                        workspaceHost = "",
                        status = WorkspaceAction.STARTING,
                        systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                        workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                    )
                }
            }
        }
    }

    private fun createWorkspaceHistoryForStart(userId: String, workspaceName: String) {
        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            operator = userId,
            action = WorkspaceAction.START,
            actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
        )
    }

    private fun updateWorkspaceStatus(workspaceName: String, status: WorkspaceStatus, userId: String) {
        workspaceDao.updateWorkspaceStatus(
            dslContext = dslContext,
            workspaceName = workspaceName,
            status = WorkspaceStatus.STARTING
        )

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            operator = userId,
            action = WorkspaceAction.START,
            actionMessage = String.format(
                workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                status.name,
                WorkspaceStatus.STARTING.name
            )
        )
    }


    fun afterStartWorkspace(event: RemoteDevUpdateEvent) {
        if (!event.status) {
            // 调devcloud接口查询是否已经启动成功，如果成功还是走成功的逻辑.
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, event.workspaceName, event.mountType).data!!
            when {
                workspaceInfo.status == EnvStatusEnum.running && workspaceInfo.started != false -> event.status = true
                else -> logger.warn(
                    "start workspace callback with error|" +
                        "${event.workspaceName}|${workspaceInfo.status}"
                )
            }
        }
        doStartWS(event.status, event.userId, event.workspaceName, event.environmentHost, event.errorMsg)
        if (event.status) {
            bkTicketServie.updateBkTicket(event.userId, event.bkTicket, event.environmentHost, event.mountType)
        }
    }

    fun doStartWS(
        status: Boolean,
        operator: String,
        workspaceName: String,
        environmentHost: String?,
        errorMsg: String? = null
    ) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkRunning()) return
        if (status) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.RUNNING,
                    dslContext = transactionContext
                )

                remoteDevBillingDao.newBilling(transactionContext, workspaceName, operator)

                val lastHistory = workspaceHistoryDao.fetchAnyHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName
                )

                val lastSleepTimeCost = if (lastHistory?.endTime != null) {
                    Duration.between(lastHistory.endTime, LocalDateTime.now()).seconds.toInt().also {
                        workspaceDao.updateWorkspaceSleepingTime(
                            workspaceName = workspaceName,
                            sleepTime = it,
                            dslContext = transactionContext
                        )
                    }
                } else 0
                workspaceHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    startUserId = operator,
                    lastSleepTimeCost = lastSleepTimeCost
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = operator,
                    action = WorkspaceAction.START,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        oldStatus.name,
                        WorkspaceStatus.RUNNING.name
                    )
                )
            }

            workspaceCommon.getOrSaveWorkspaceDetail(
                workspaceName,
                WorkspaceMountType.valueOf(workspace.workspaceMountType)
            )
            if (WorkspaceSystemType.valueOf(workspace.systemType).needHeartbeat()) {
                redisHeartBeat.refreshHeartbeat(workspaceName)
            }
        } else {
            // 启动失败,记录为EXCEPTION
            logger.warn("start workspace $workspaceName failed")
            workspaceDao.updateWorkspaceStatus(
                workspaceName = workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = operator,
                action = WorkspaceAction.START,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    oldStatus.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        // 分发到WS
        workspaceCommon.dispatchWebsocketPushEvent(
            userId = operator,
            workspaceName = workspaceName,
            workspaceHost = environmentHost,
            errorMsg = errorMsg,
            type = WebSocketActionType.WORKSPACE_START,
            status = status,
            action = WorkspaceAction.START,
            systemType = WorkspaceSystemType.valueOf(workspace.systemType),
            workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
        )
    }
}
