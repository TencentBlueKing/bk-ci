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
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.SshPublicKeysService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OP_HISTORY_KEY_PREFIX
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
@Suppress("LongMethod")
class WorkspaceCommon @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val sshService: SshPublicKeysService,
    private val client: Client,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val redisCache: RedisCacheService,
    private val profile: Profile,
    @org.springframework.context.annotation.Lazy
    private val startControl: StartControl,
    @org.springframework.context.annotation.Lazy
    private val sleepControl: SleepControl,
    @org.springframework.context.annotation.Lazy
    private val deleteControl: DeleteControl
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCommon::class.java)
        private const val DEFAULT_WAIT_TIME = 60
    }

    // 封装统一分发WS的方法
    fun dispatchWebsocketPushEvent(
        userId: String,
        workspaceName: String,
        workspaceHost: String?,
        errorMsg: String?,
        type: WebSocketActionType,
        status: Boolean?,
        action: WorkspaceAction,
        systemType: WorkspaceSystemType,
        workspaceMountType: WorkspaceMountType
    ) {
        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = type,
                status = status ?: true,
                anyMessage = WorkspaceResponse(
                    workspaceHost = workspaceHost ?: "",
                    workspaceName = workspaceName,
                    status = action,
                    errorMsg = errorMsg,
                    systemType = systemType,
                    workspaceMountType = workspaceMountType
                ),
                projectId = "",
                userIds = getWebSocketUsers(userId, workspaceName),
                redisOperation = redisOperation,
                page = WorkspacePageBuild.buildPage(workspaceName),
                notifyPost = NotifyPost(
                    module = "remotedev",
                    level = NotityLevel.LOW_LEVEL.getLevel(),
                    message = "",
                    dealUrl = null,
                    code = 200,
                    webSocketType = "IFRAME",
                    page = WorkspacePageBuild.buildPage(workspaceName)
                )
            )
        )
    }

    fun getOpHistory(key: OpHistoryCopyWriting) =
        redisCache.get(REDIS_OP_HISTORY_KEY_PREFIX + key.name)?.ifBlank {
            key.default
        } ?: key.default

    fun getOrSaveWorkspaceDetail(workspaceName: String, mountType: WorkspaceMountType): WorkSpaceCacheInfo {
        return redisCache.getWorkspaceDetail(workspaceName) ?: run {
            val userSet = workspaceDao.fetchWorkspaceUser(
                dslContext,
                workspaceName
            ).toSet()
            val sshKey = sshService.getSshPublicKeys4Ws(userSet)
            val workspaceInfo =
                client.get(ServiceRemoteDevResource::class)
                    .getWorkspaceInfo(userSet.first(), workspaceName, mountType).data!!
            val cache = WorkSpaceCacheInfo(
                sshKey,
                workspaceInfo.environmentHost,
                workspaceInfo.hostIP,
                workspaceInfo.environmentIP,
                workspaceInfo.environmentIP,
                workspaceInfo.namespace,
                workspaceInfo.curLaunchId
            )
            redisCache.saveWorkspaceDetail(
                workspaceName,
                cache
            )
            return cache
        }
    }

    fun checkAndFixExceptionWS(
        status: WorkspaceStatus,
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType
    ) {
        if (status.checkException()) {
            when (val fix = fixUnexpectedStatus(userId, workspaceName, status, mountType)) {
                WorkspaceStatus.EXCEPTION -> {
                    logger.info("$workspaceName is EXCEPTION and not repaired, return error.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_ERROR.errorCode
                    )
                }

                else -> {
                    logger.info("$workspaceName is $status to $fix , return info.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_ERROR_FIX.errorCode,
                        params = arrayOf(fix.name)
                    )
                }
            }
        }
    }

    // 尝试修复异常工作空间状态
    fun fixUnexpectedWorkspace() {
        logger.info("fixUnexpectedWorkspace")
        workspaceDao.fetchWorkspace(
            dslContext, status = WorkspaceStatus.EXCEPTION
        )?.parallelStream()?.forEach {
            MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            logger.info(
                "workspace ${it.name} is EXCEPTION, try to fix."
            )
            if (!checkProjectRouter(it.creator, it.name)) return@forEach
            fixUnexpectedStatus(
                userId = ADMIN_NAME,
                workspaceName = it.name,
                status = WorkspaceStatus.values()[it.status],
                mountType = WorkspaceMountType.valueOf(it.workspaceMountType)
            )
        }
    }

    fun fixUnexpectedStatus(
        userId: String,
        workspaceName: String,
        status: WorkspaceStatus,
        mountType: WorkspaceMountType
    ): WorkspaceStatus {
        val workspaceInfo = kotlin.runCatching {
            client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(userId, workspaceName, mountType).data!!
        }.getOrElse { ignore ->
            logger.warn(
                "get workspace info error $workspaceName|${ignore.message}"
            )
            workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION)
            return WorkspaceStatus.EXCEPTION
        }
        logger.info("fixUnexpectedStatus|$workspaceName|$status|$workspaceInfo")
        when {
            workspaceInfo.status == EnvStatusEnum.stopped -> {
                sleepControl.doStopWS(true, userId, workspaceName)
                return WorkspaceStatus.SLEEP
            }

            workspaceInfo.status == EnvStatusEnum.deleted -> {
                deleteControl.doDeleteWS(true, userId, workspaceName, workspaceInfo.environmentIP)
                return WorkspaceStatus.DELETED
            }

            workspaceInfo.status == EnvStatusEnum.running && workspaceInfo.started != false -> {
                startControl.doStartWS(true, userId, workspaceName, workspaceInfo.environmentHost)
                return WorkspaceStatus.RUNNING
            }

            else -> logger.warn(
                "wait workspace change over $DEFAULT_WAIT_TIME second |" +
                    "$workspaceName|${workspaceInfo.status}"
            )
        }
        return status
    }

    /**
     * workspace 正在变更状态时，不能新建任务去执行。但如果超过 60s 便不做该限制。 以免因下游某服务节点故障状态未闭环回传导致问题。
     * 如果已经销毁，直接返回false
     */
    fun notOk2doNextAction(workspace: TWorkspaceRecord): Boolean {
        return (
            WorkspaceStatus.values()[workspace.status].notOk2doNextAction() && Duration.between(
                workspace.lastStatusUpdateTime ?: LocalDateTime.now(),
                LocalDateTime.now()
            ).seconds < DEFAULT_WAIT_TIME
            ) || WorkspaceStatus.values()[workspace.status].checkDeleted()
    }

    fun updateLastHistory(
        transactionContext: DSLContext,
        workspaceName: String,
        operator: String
    ) {
        val lastHistory = workspaceHistoryDao.fetchAnyHistory(
            dslContext = transactionContext,
            workspaceName = workspaceName
        )
        if (lastHistory?.startTime != null) {
            workspaceDao.updateWorkspaceUsageTime(
                workspaceName = workspaceName,
                usageTime = Duration.between(
                    lastHistory.startTime, LocalDateTime.now()
                ).seconds.toInt(),
                dslContext = transactionContext
            )
            workspaceHistoryDao.updateWorkspaceHistory(
                dslContext = transactionContext,
                id = lastHistory.id,
                stopUserId = operator
            )
        } else {
            logger.error("$workspaceName get last history info null")
        }
    }

    fun checkProjectRouter(
        creator: String,
        workspaceName: String
    ): Boolean {
        if (profile.isDebug()) return true
        val projectId = remoteDevSettingDao.fetchAnySetting(dslContext, creator).projectId
            .ifBlank { null } ?: run {
            logger.info("$workspaceName creator not init setting, ignore it.")
            return false
        }

        val projectRouterTagCheck =
            client.get(ServiceProjectTagResource::class).checkProjectRouter(projectId).data
        if (!projectRouterTagCheck!!) {
            logger.info("project $projectId router tag is not this cluster")
            return false
        }
        return true
    }

    fun getSystemOperator(workspaceOwner: String, mountType: String): String =
        when (mountType) {
            WorkspaceMountType.START.name -> workspaceOwner
            else -> ADMIN_NAME
        }

    fun checkWorkspaceAvailability(
        userId: String,
        workspace: TWorkspaceRecord
    ) {
        when (workspace.workspaceMountType) {
            WorkspaceMountType.START.name -> {
                val duration = remoteDevSettingService.startCloudExperienceDuration(userId)
                if (duration * 60 * 60 < workspace.usageTime) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_UNAVAILABLE.errorCode,
                        params = arrayOf(workspace.name, duration.toString())
                    )
                }
            }
        }
    }

    private fun getWebSocketUsers(operator: String, workspaceName: String): Set<String> {
        return if (operator == ADMIN_NAME) {
            workspaceDao.fetchWorkspaceUser(dslContext, workspaceName).toSet()
        } else setOf(operator)
    }
}
