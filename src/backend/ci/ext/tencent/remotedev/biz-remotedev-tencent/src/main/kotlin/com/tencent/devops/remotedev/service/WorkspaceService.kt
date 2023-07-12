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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.WorkspaceNotifyTemplateEnum
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.RemoteDevCommonConfig
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceProxyDetail
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.pojo.event.RemoteDevReminderEvent
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DEFAULT_MAX_HAVING_COUNT
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DEFAULT_MAX_RUNNING_COUNT
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DESTRUCTION_RETENTION_TIME
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DISCOUNT_TIME_KEY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OFFICIAL_DEVFILE_KEY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OP_HISTORY_KEY_PREFIX
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.utils.DevfileUtil
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import com.tencent.devops.scm.utils.code.git.GitUtils
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
class WorkspaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val remoteDevGitTransfer: RemoteDevGitTransfer,
    private val permissionService: PermissionService,
    private val sshService: SshPublicKeysService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val redisCache: RedisCacheService,
    private val bkTicketServie: BkTicketService,
    private val whiteListService: WhiteListService,
    private val profile: Profile,
    private val commonConfig: RemoteDevCommonConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val defaultPageSize = 20
        private const val DEFAULT_WAIT_TIME = 60
        private const val BLANK_TEMPLATE_YAML_NAME = "BLANK"
        private const val BLANK_TEMPLATE_ID = 1
        private const val DISCOUNT_TIME = 10000
    }

    // 处理创建工作空间逻辑
    fun createWorkspace(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): WorkspaceResponse {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")
        checkUserCreate(userId)
        val gitTransferService = remoteDevGitTransfer.loadByGitUrl(workspaceCreate.repositoryUrl)
        val pathWithNamespace = GitUtils.getDomainAndRepoName(workspaceCreate.repositoryUrl).second
        val projectName = pathWithNamespace.substring(pathWithNamespace.lastIndexOf("/") + 1)
        val yaml = if (workspaceCreate.useOfficialDevfile != true) {
            kotlin.runCatching {
                permissionService.checkOauthIllegal(userId) {
                    gitTransferService.getFileContent(
                        userId = userId,
                        pathWithNamespace = pathWithNamespace,
                        filePath = workspaceCreate.devFilePath!!,
                        ref = workspaceCreate.branch
                    )
                }
            }.getOrElse {
                logger.warn("get yaml failed ${it.message}")
                if (it is ErrorCodeException) throw it
                else throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                    params = arrayOf("获取 devfile 异常 ${it.message}")
                )
            }
        } else {
            // 防止污传,如果是基于BLANK模板创建的则用BLANK作为devFilePath
            workspaceCreate.devFilePath = if (workspaceCreate.wsTemplateId == BLANK_TEMPLATE_ID) {
                BLANK_TEMPLATE_YAML_NAME
            } else null
            redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY) ?: ""
        }

        if (yaml.isBlank()) {
            logger.warn(
                "create workspace get devfile blank,return." +
                    "|useOfficialDevfile=${workspaceCreate.useOfficialDevfile}"
            )
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                params = arrayOf("devfile 为空，请确认。")
            )
        }

        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId)
        }.onFailure { logger.warn("get $userId info error|${it.message}") }.getOrElse { null }?.data

        val devfile = DevfileUtil.parseDevfile(yaml).apply {
            gitEmail = kotlin.runCatching {
                permissionService.checkOauthIllegal(userId) {
                    gitTransferService.getUserEmail(
                        userId = userId
                    )
                }
            }.getOrElse {
                logger.warn("get user $userId info failed ${it.message}")
                if (it is ErrorCodeException) throw it
                else throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.USERINFO_ERROR.errorCode,
                    params = arrayOf("get user($userId) info from git failed")
                )
            }

            dotfileRepo = remoteDevSettingDao.fetchAnySetting(dslContext, userId).dotfileRepo
        }

        if (devfile.checkWorkspaceSystemType() == WorkspaceSystemType.WINDOWS_GPU) {
            whiteListService.checkRunsOnOs(
                key = RedisKeys.REDIS_RUNS_ON_OS_KEY,
                runsOnKey = WorkspaceSystemType.WINDOWS_GPU.name,
                currentOs = workspaceCreate.currentOS
            )

            whiteListService.numberLimit(
                key = RedisKeys.REDIS_WHITE_LIST_GPU_KEY,
                id = userId,
                value = workspaceDao.countUserWorkspace(
                    dslContext = dslContext,
                    userId = userId,
                    unionShared = false,
                    status = setOf(WorkspaceStatus.RUNNING, WorkspaceStatus.PREPARING, WorkspaceStatus.STARTING),
                    systemType = WorkspaceSystemType.WINDOWS_GPU
                )
            )
        }

        val mountType = checkMountType(userId, devfile.checkWorkspaceMountType())
        logger.info("createWorkspace|mountType|$mountType")
        val bizId = MDC.get(TraceTag.BIZID)
        val workspaceName = generateWorkspaceName(userId)
        val workspace = with(workspaceCreate) {
            Workspace(
                workspaceId = null,
                workspaceName = workspaceName,
                projectId = projectId,
                displayName = null,
                repositoryUrl = repositoryUrl,
                branch = branch,
                devFilePath = devFilePath,
                yaml = yaml,
                wsTemplateId = wsTemplateId,
                status = null,
                lastStatusUpdateTime = null,
                sleepingTime = null,
                createUserId = userId,
                workPath = Constansts.prefixWorkPath.plus(projectName),
                workspaceFolder = devfile.workspaceFolder ?: "",
                hostName = "",
                workspaceMountType = mountType,
                workspaceSystemType = devfile.checkWorkspaceSystemType()
            )
        }

        workspaceDao.createWorkspace(
            userId = userId,
            workspace = workspace,
            workspaceStatus = WorkspaceStatus.PREPARING,
            dslContext = dslContext,
            userInfo = userInfo
        )

        // 替换部分devfile内容，兼容使用老remoting的情况
        if (!isImageInDefaultList(
                devfile.runsOn?.container?.image,
                redisCache.getSetMembers(RedisKeys.REDIS_DEFAULT_IMAGES_KEY) ?: emptySet()
            )
        ) {
//            devfile.runsOn?.container?.image = if (mountType == WorkspaceMountType.BCS) {
//                "${commonConfig.bcsWorkspaceImageRegistryHost}/remote/${workspace.workspaceName}"
//            }else{
//                "${commonConfig.workspaceImageRegistryHost}/remote/${workspace.workspaceName}"
//            }
            devfile.runsOn?.container?.image =
                "${commonConfig.workspaceImageRegistryHost}/remote/${workspace.workspaceName}"
        }

        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = workspace.workspaceName,
                repositoryUrl = workspace.repositoryUrl,
                branch = workspace.branch,
                devFilePath = workspace.devFilePath,
                devFile = devfile,
                gitOAuth = gitTransferService.getAndCheckOauthToken(userId),
                settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId).envsForVariable,
                bkTicket = bkTicket,
                projectId = projectId,
                mountType = mountType
            )
        )

        // 发送给用户
        dispatchWebsocketPushEvent(
            userId = userId,
            workspaceName = workspaceName,
            workspaceHost = null,
            errorMsg = null,
            type = WebSocketActionType.WORKSPACE_CREATE,
            status = true,
            action = WorkspaceAction.PREPARING,
            systemType = workspace.workspaceSystemType, workspaceMountType = workspace.workspaceMountType
        )

        return WorkspaceResponse(
            workspaceName = workspaceName,
            status = WorkspaceAction.PREPARING,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType
        )
    }

    fun checkMountType(userId: String, devfileMountType: WorkspaceMountType): WorkspaceMountType {
        logger.info("checkMountType|userId|$userId|devfileMountType|$devfileMountType")
        if (devfileMountType == WorkspaceMountType.START) {
            return devfileMountType
        }
        val mountType = remoteDevSettingDao.fetchAnySetting(dslContext, userId).userSetting.mountType
        logger.info("checkMountType|userId|$userId|devfileMountType|$devfileMountType|mountType|$mountType")

        // 简化判断逻辑，优先处理 WorkspaceMountType.BCS 的情况
        if (devfileMountType == WorkspaceMountType.DEVCLOUD && mountType == WorkspaceMountType.BCS) {
            return WorkspaceMountType.BCS
        }

        // 处理其他情况时，均返回 WorkspaceMountType.DEVCLOUD
        return WorkspaceMountType.DEVCLOUD
    }

    // k8s创建workspace后回调的方法
    fun afterCreateWorkspace(event: RemoteDevUpdateEvent) {
        val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = event.workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(event.workspaceName)
            )
        if (event.status) {
            val pathWithNamespace = GitUtils.getDomainAndRepoName(ws.url).second
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    status = WorkspaceStatus.RUNNING,
                    hostName = event.environmentHost
                )
                remoteDevBillingDao.newBilling(transactionContext, event.workspaceName, event.userId)
                workspaceHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    startUserId = event.userId,
                    lastSleepTimeCost = 0
                )
                arrayOf(WorkspaceAction.CREATE to String.format(
                    getOpHistory(OpHistoryCopyWriting.CREATE),
                    pathWithNamespace,
                    ws.branch,
                    ws.name
                ),
                    WorkspaceAction.START to getOpHistory(OpHistoryCopyWriting.FIRST_START))
                    .forEach { (action, actionMessage) ->
                        workspaceOpHistoryDao.createWorkspaceHistory(
                            dslContext = transactionContext,
                            workspaceName = event.workspaceName,
                            operator = event.userId,
                            action = action,
                            actionMessage = actionMessage
                        )
                    }
            }

            getOrSaveWorkspaceDetail(event.workspaceName, event.mountType)
            val systemType = WorkspaceSystemType.valueOf(ws.systemType)
            if (systemType.needHeartbeat()) {
                redisHeartBeat.refreshHeartbeat(event.workspaceName)
            }

            if (systemType.needReminderUser()) {
                val duration = remoteDevSettingService.startCloudExperienceDuration(event.userId)
                val limit = redisCache.get(RedisKeys.REDIS_NOTICE_AHEAD_OF_TIME)?.toLong() ?: 60
                dispatcher.dispatch(
                    RemoteDevReminderEvent(
                        userId = event.userId,
                        workspaceName = event.workspaceName,
                        delayMills = (duration * 60 - limit).times(60).toInt()
                            .coerceAtLeast(60) * 1000
                    )
                )
            }

            kotlin.runCatching {
                bkTicketServie.updateBkTicket(event.userId, event.bkTicket, event.environmentHost, event.mountType)
            }

            // websocket 通知成功
        } else {
            // 创建失败
            // websocket 通知失败
            logger.warn("create workspace ${event.workspaceName} failed")
            workspaceDao.deleteWorkspace(event.workspaceName, dslContext)
        }

        dispatchWebsocketPushEvent(
            userId = event.userId,
            workspaceName = event.workspaceName,
            workspaceHost = event.environmentHost,
            errorMsg = event.errorMsg,
            type = WebSocketActionType.WORKSPACE_CREATE,
            status = event.status,
            action = WorkspaceAction.START,
            systemType = WorkspaceSystemType.valueOf(ws.systemType),
            workspaceMountType = WorkspaceMountType.valueOf(ws.workspaceMountType)
        )
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

                notOk2doNextAction(workspace) -> {
                    logger.info("${workspace.name} is $status, return error.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                        params = arrayOf(workspace.name, "status is already $status, can't start now")
                    )
                }

                else -> {
                    checkUserCreate(userId, true)
                    /*处理异常的情况*/
                    checkAndFixExceptionWS(
                        status,
                        userId,
                        workspaceName,
                        WorkspaceMountType.valueOf(workspace.workspaceMountType)
                    )
                    checkWorkspaceAvailability(userId, workspace)
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
                    dispatchWebsocketPushEvent(
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
            actionMessage = getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
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
                getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                status.name,
                WorkspaceStatus.STARTING.name
            )
        )
    }

    private fun checkWorkspaceAvailability(
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

    private fun checkAndFixExceptionWS(
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

    private fun doStartWS(
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
                        getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        oldStatus.name,
                        WorkspaceStatus.RUNNING.name
                    )
                )
            }

            getOrSaveWorkspaceDetail(workspaceName, WorkspaceMountType.valueOf(workspace.workspaceMountType))
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
                    getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    oldStatus.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        // 分发到WS
        dispatchWebsocketPushEvent(
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
            dispatchWebsocketPushEvent(
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

        if (notOk2doNextAction(workspace)) {
            logger.info("${workspace.name} is $status, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't stop now")
            )
        }

        // 处理异常的情况
        checkAndFixExceptionWS(
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
            actionMessage = getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
        )

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = name,
            operator = userId,
            action = WorkspaceAction.SLEEP,
            actionMessage = String.format(
                getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                status.name,
                WorkspaceStatus.SLEEPING.name
            )
        )
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
            dispatchWebsocketPushEvent(
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

    private fun checkWorkspaceStatusForDelete(workspace: TWorkspaceRecord, userId: String): Boolean {
        val status = WorkspaceStatus.values()[workspace.status]

        if (status.checkDeleted()) {
            logger.info("${workspace.name} has been deleted, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't delete again")
            )
        }

        if (notOk2doNextAction(workspace)) {
            logger.info("${workspace.name} is $status, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                params = arrayOf(workspace.name, "status is already $status, can't delete now")
            )
        }

        var deleteImmediately = false
        kotlin.runCatching {
            checkAndFixExceptionWS(
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
                actionMessage = getOpHistory(OpHistoryCopyWriting.DELETE)
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
                    getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    status.name,
                    WorkspaceStatus.DELETING.name
                )
            )
        }
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

    // 修改workspace备注名称
    fun editWorkspace(userId: String, workspaceName: String, displayName: String): Boolean {
        logger.info("$userId edit workspace $workspaceName|$displayName")
        permissionService.checkPermission(userId, workspaceName)
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:editWorkspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceDao.updateWorkspaceDisplayName(
                dslContext = transactionContext,
                workspaceName = workspaceName,
                displayName = displayName
            )
        }
        return true
    }

    fun shareWorkspace(userId: String, workspaceName: String, sharedUser: String): Boolean {
        logger.info("$userId share workspace $workspaceName|$sharedUser")
        permissionService.checkPermission(userId, workspaceName)
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:shareWorkspace:${workspaceName}_$sharedUser",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
            if (userId != workspace.creator) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                    params = arrayOf("only workspace creator can share")
                )
            }
            val shareInfo = WorkspaceShared(workspaceName, userId, sharedUser)
            if (workspaceSharedDao.existWorkspaceSharedInfo(shareInfo, dslContext)) {
                logger.info("$workspaceName has already shared to $sharedUser")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_SHARE_FAIL.errorCode,
                    params = arrayOf("$workspaceName has already shared to $sharedUser")
                )
            }

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceSharedDao.createWorkspaceSharedInfo(userId, shareInfo, transactionContext)
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = userId,
                    action = WorkspaceAction.SHARE,
                    actionMessage = String.format(
                        getOpHistory(OpHistoryCopyWriting.SHARE),
                        sharedUser
                    )
                )
            }
            return true
        }
    }

    fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Page<Workspace> {
        logger.info("$userId get user workspace list")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val count = workspaceDao.countUserWorkspace(dslContext, userId)
        val result = workspaceDao.limitFetchUserWorkspace(
            dslContext = dslContext,
            userId = userId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        ) ?: emptyList()

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                var status = WorkspaceStatus.values()[it.status]
                run {
                    if (status.notOk2doNextAction() && Duration.between(
                            it.lastStatusUpdateTime ?: LocalDateTime.now(),
                            LocalDateTime.now()
                        ).seconds > DEFAULT_WAIT_TIME
                    ) {
                        status = fixUnexpectedStatus(
                            userId = userId,
                            workspaceName = it.name,
                            status = status,
                            mountType = WorkspaceMountType.valueOf(it.workspaceMountType)
                        )
                    }
                }
                Workspace(
                    workspaceId = it.id,
                    workspaceName = it.name,
                    projectId = it.projectId,
                    displayName = it.displayName,
                    repositoryUrl = it.url,
                    branch = it.branch,
                    devFilePath = it.yamlPath,
                    yaml = it.yaml,
                    wsTemplateId = it.templateId,
                    status = status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime.timestamp(),
                    sleepingTime = if (status.checkSleeping()) it.lastStatusUpdateTime.timestamp() else null,
                    createUserId = it.creator,
                    workPath = it.workPath,
                    workspaceFolder = it.workspaceFolder,
                    hostName = it.hostName,
                    workspaceMountType = WorkspaceMountType.valueOf(it.workspaceMountType),
                    workspaceSystemType = WorkspaceSystemType.valueOf(it.systemType)
                )
            }
        )
    }

    private fun fixUnexpectedStatus(
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
                doStopWS(true, userId, workspaceName)
                return WorkspaceStatus.SLEEP
            }

            workspaceInfo.status == EnvStatusEnum.deleted -> {
                doDeleteWS(true, userId, workspaceName, workspaceInfo.environmentIP)
                return WorkspaceStatus.DELETED
            }

            workspaceInfo.status == EnvStatusEnum.running && workspaceInfo.started != false -> {
                doStartWS(true, userId, workspaceName, workspaceInfo.environmentHost)
                return WorkspaceStatus.RUNNING
            }

            else -> logger.warn(
                "wait workspace change over $DEFAULT_WAIT_TIME second |" +
                    "$workspaceName|${workspaceInfo.status}"
            )
        }
        return status
    }

    fun getWorkspaceUserDetail(userId: String): WorkspaceUserDetail {
        logger.info("$userId get his all workspace ")
        val workspaces = workspaceDao.fetchWorkspace(dslContext, userId) ?: emptyList()
        val status = workspaces.map { WorkspaceStatus.values()[it.status] }
        val now = LocalDateTime.now()

        // 查出所有正在运行ws的最新历史记录
        val latestHistory = workspaceHistoryDao.fetchLatestHistory(
            dslContext,
            workspaces.asSequence()
                .filter { WorkspaceStatus.values()[it.status].checkRunning() }.map { it.name }.toSet()
        ).associateBy { it.workspaceName }

        // 查出所有已休眠状态ws的最新历史记录
        val latestSleepHistory = workspaceHistoryDao.fetchLatestHistory(
            dslContext,
            workspaces.asSequence()
                .filter { WorkspaceStatus.values()[it.status].checkSleeping() }.map { it.name }.toSet()
        ).associateBy { it.workspaceName }
        val usageTime = workspaces.sumOf {
            it.usageTime + if (WorkspaceStatus.values()[it.status].checkRunning()) {
                // 如果正在运行，需要加上目前距离该次启动的时间
                Duration.between(latestHistory[it.name]?.startTime ?: now, now).seconds
            } else 0
        }
        val sleepingTime = workspaces.sumOf {
            it.sleepingTime + if (WorkspaceStatus.values()[it.status].checkSleeping()) {
                // 如果正在休眠，需要加上目前距离上次结束的时间
                Duration.between(latestSleepHistory[it.name]?.endTime ?: now, now).seconds
            } else 0
        }

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toLong() ?: 10000
        return WorkspaceUserDetail(
            runningCount = status.count { it.checkRunning() },
            sleepingCount = status.count { it.checkSleeping() },
            deleteCount = status.count { it.checkDeleted() },
            chargeableTime = endBilling.second +
                (notEndBillingTime + endBilling.first - discountTime * 60).coerceAtLeast(0),
            usageTime = usageTime,
            sleepingTime = sleepingTime,
            discountTime = discountTime,
            cpu = workspaces.sumOf {
                if (it.status == WorkspaceStatus.RUNNING.ordinal) {
                    it.cpu
                } else {
                    0
                }
            },
            memory = workspaces.sumOf {
                if (it.status == WorkspaceStatus.RUNNING.ordinal) {
                    it.memory
                } else {
                    0
                }
            },
            disk = workspaces.sumOf {
                if (it.status in
                    setOf(WorkspaceStatus.RUNNING.ordinal, WorkspaceStatus.SLEEP.ordinal)
                ) {
                    it.disk
                } else {
                    0
                }
            }
        )
    }

    fun getWorkspaceDetail(userId: String, workspaceName: String, checkPermission: Boolean = true): WorkspaceDetail? {
        logger.info("$userId get workspace from id $workspaceName")
        if (checkPermission) {
            permissionService.checkPermission(userId, workspaceName)
        }
        val now = LocalDateTime.now()
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: return null

        val workspaceStatus = WorkspaceStatus.values()[workspace.status]

        val lastHistory = workspaceHistoryDao.fetchAnyHistory(dslContext, workspaceName)

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: DISCOUNT_TIME

        val usageTime = workspace.usageTime + if (workspaceStatus.checkRunning()) {
            // 如果正在运行，需要加上目前距离该次启动的时间
            Duration.between(lastHistory?.startTime ?: now, now).seconds
        } else 0

        val sleepingTime = workspace.sleepingTime + if (workspaceStatus.checkSleeping()) {
            // 如果正在休眠，需要加上目前距离上次结束的时间
            Duration.between(lastHistory?.endTime ?: now, now).seconds
        } else 0

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)
        return with(workspace) {
            WorkspaceDetail(
                workspaceId = id,
                workspaceName = name,
                displayName = displayName,
                status = workspaceStatus,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = endBilling.second +
                    (notEndBillingTime + endBilling.first - discountTime * 60).coerceAtLeast(0),
                usageTime = usageTime,
                sleepingTime = sleepingTime,
                cpu = cpu,
                memory = memory,
                disk = disk,
                yaml = yaml,
                systemType = WorkspaceSystemType.valueOf(systemType),
                workspaceMountType = WorkspaceMountType.valueOf(workspaceMountType)
            )
        }
    }

    fun startCloudWorkspaceDetail(userId: String, workspaceName: String): WorkspaceStartCloudDetail {
        logger.info("$userId get startCloud workspace from workspaceName $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        checkWorkspaceAvailability(userId, workspace)
        val detail = redisCache.getWorkspaceDetail(workspaceName)
        if (detail == null || !WorkspaceStatus.values()[workspace.status].checkRunning()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                params = arrayOf(workspaceName)
            )
        }
        return WorkspaceStartCloudDetail(detail.environmentIP, detail.curLaunchId!!)
    }

    fun getWorkspaceTimeline(
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Page<WorkspaceOpHistory> {
        logger.info("$userId get workspace time line from id $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: defaultPageSize
        val count = workspaceOpHistoryDao.countOpHistory(dslContext, workspaceName)
        val result = workspaceOpHistoryDao.limitFetchOpHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                WorkspaceOpHistory(
                    createdTime = it.createdTime.timestamp(),
                    operator = it.operator,
                    action = WorkspaceAction.values()[it.action],
                    actionMessage = it.actionMsg
                )
            }
        )
    }

    fun getWorkspaceProxyDetail(workspaceName: String): WorkspaceProxyDetail {
        return redisCache.getWorkspaceDetail(workspaceName)?.let {
            WorkspaceProxyDetail(
                workspaceName = workspaceName,
                podIp = it.environmentIP,
                sshKey = it.sshKey,
                environmentHost = it.environmentHost
            )
        } ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode
        )
    }

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

    // 更新用户运行中的空间的detail缓存信息
    fun updateUserWorkspaceDetailCache(userId: String) {
        workspaceDao.fetchWorkspace(
            dslContext, userId = userId, status = WorkspaceStatus.RUNNING
        )?.parallelStream()?.forEach {
            MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            val sshKey = sshService.getSshPublicKeys4Ws(setOf(userId))
            val workspaceInfo =
                client.get(ServiceRemoteDevResource::class)
                    .getWorkspaceInfo(userId, it.name, WorkspaceMountType.valueOf(it.workspaceMountType)).data!!
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
                it.name,
                cache
            )
        }
    }

    fun preCiAgent(agentId: String, workspaceName: String): Boolean {
        logger.info("update preCiAgent id|$workspaceName|$agentId")
        return workspaceDao.updatePreCiAgentId(dslContext, agentId, workspaceName)
    }

    fun checkDevfile(
        userId: String,
        pathWithNamespace: String,
        branch: String,
        gitType: RemoteDevGitType
    ): List<String> {
        logger.info("$userId get devfile list from git. $pathWithNamespace|$branch")
        return permissionService.checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getFileNameTree(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                path = Constansts.devFileDirectoryName, // 根目录
                ref = branch,
                recursive = false // 不递归
            ).map { Constansts.devFileDirectoryName + "/" + it }
        }
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

        if (!checkProjectRouter(workspace.creator, workspaceName)) return false

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:${workspace.id}",
            expiredTimeInSeconds
        ).lock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.name,
                operator = ADMIN_NAME,
                action = WorkspaceAction.SLEEP,
                actionMessage = getOpHistory(opHistory)
            )

            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = getSystemOperator(workspace.creator, workspace.workspaceMountType),
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.name,
                    mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                )
            )

            // 发送给用户
            dispatchWebsocketPushEvent(
                userId = ADMIN_NAME,
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

    private fun checkProjectRouter(
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

    fun getUnavailableWorkspace(): List<String> {
        val now = LocalDateTime.now()
        return workspaceDao.fetchWorkspace(
            dslContext, status = WorkspaceStatus.RUNNING, mountType = WorkspaceMountType.START
        )?.asSequence()
            ?.filter {
                val usageTime = it.usageTime + Duration.between(it.lastStatusUpdateTime, now).seconds
                remoteDevSettingService.startCloudExperienceDuration(it.creator) * 60 * 60 < usageTime
            }?.map { it.name }?.toList() ?: emptyList()
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
                val retentionTime = redisCache.get(REDIS_DESTRUCTION_RETENTION_TIME)?.toInt() ?: 3
                if (Duration.between(it.lastStatusUpdateTime, now).toDays() >= retentionTime) {
                    kotlin.runCatching { heartBeatDeleteWS(it) }.onFailure { i ->
                        logger.warn("deleteInactivityWorkspace fail|${i.message}", i)
                    }
                }
            }
    }

    // 提前7天邮件提醒，云环境即将自动回收
    fun sendInactivityWorkspaceNotify() {
        logger.info("sendInactivityWorkspaceNotify")
        val workspaceMap = workspaceDao.getTimeOutInactivityWorkspace(
            Constansts.timeoutDays - Constansts.sendNotifyDays, dslContext
        ).groupBy { it.creator }
        logger.info("sendInactivityWorkspaceNotify|workspaceMap|$workspaceMap")
        // 遍历workspaceMap，按 creator 分批发送邮件
        workspaceMap.forEach { (creator, workspaces) ->
            val request = SendNotifyMessageTemplateRequest(
                templateCode = WorkspaceNotifyTemplateEnum.REMOTEDEV_WORKSPACE_RECYCLE_TEMPLATE.templateCode,
                receivers = mutableSetOf(creator),
                cc = mutableSetOf(creator),
                titleParams = null,
                bodyParams = mapOf(
                    "userId" to creator,
                    "workspaceName" to workspaces.joinToString(separator = "\n") { it.name }
                ),
                notifyType = mutableSetOf(NotifyType.EMAIL.name)
            )
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        }
    }

    private fun getSystemOperator(workspaceOwner: String, mountType: String): String =
        when (mountType) {
            WorkspaceMountType.START.name -> workspaceOwner
            else -> ADMIN_NAME
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

        if (!checkProjectRouter(workspace.creator, workspace.name)) return false
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
                actionMessage = getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
            )
            val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = getSystemOperator(workspace.creator, workspace.workspaceMountType),
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.name,
                    mountType = WorkspaceMountType.valueOf(workspace.workspaceMountType)
                )
            )

            dispatchWebsocketPushEvent(
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

    private fun doDeleteWS(
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
                        getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
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
                    getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    oldStatus.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            updateLastHistory(transactionContext, workspaceName, operator)
            remoteDevBillingDao.endBilling(transactionContext, workspaceName)
        }
        dispatchWebsocketPushEvent(
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

    private fun doStopWS(status: Boolean, operator: String, workspaceName: String, errorMsg: String? = null) {
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
                        getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
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
                    getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    oldStatus.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            updateLastHistory(transactionContext, workspaceName, operator)
            remoteDevBillingDao.endBilling(transactionContext, workspaceName)
        }

        dispatchWebsocketPushEvent(
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

    private fun updateLastHistory(
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

    fun initBilling(freeTime: Int? = null) {
        remoteDevBillingDao.monthlyInit(
            dslContext,
            (freeTime ?: redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: DISCOUNT_TIME) * 60
        )
    }

    private fun getWebSocketUsers(operator: String, workspaceName: String): Set<String> {
        return if (operator == ADMIN_NAME) {
            workspaceDao.fetchWorkspaceUser(dslContext, workspaceName).toSet()
        } else setOf(operator)
    }

    private fun getOpHistory(key: OpHistoryCopyWriting) =
        redisCache.get(REDIS_OP_HISTORY_KEY_PREFIX + key.name)?.ifBlank {
            key.default
        } ?: key.default

    private fun generateWorkspaceName(userId: String): String {
        val subUserId = if (userId.length > Constansts.subUserIdLimitLen) {
            userId.substring(0 until Constansts.subUserIdLimitLen)
        } else {
            userId
        }
        return "${subUserId.replace("_", "-")}-${UUIDUtil.generate().takeLast(Constansts.workspaceNameSuffixLimitLen)}"
    }

    /**
     * workspace 正在变更状态时，不能新建任务去执行。但如果超过 60s 便不做该限制。 以免因下游某服务节点故障状态未闭环回传导致问题。
     * 如果已经销毁，直接返回false
     */
    private fun notOk2doNextAction(workspace: TWorkspaceRecord): Boolean {
        return (
            WorkspaceStatus.values()[workspace.status].notOk2doNextAction() && Duration.between(
                workspace.lastStatusUpdateTime ?: LocalDateTime.now(),
                LocalDateTime.now()
            ).seconds < DEFAULT_WAIT_TIME
            ) || WorkspaceStatus.values()[workspace.status].checkDeleted()
    }

    fun getWorkspaceHost(workspaceName: String): String {
        val url = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)?.url
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        return GitUtils.getDomainAndRepoName(url).first
    }

    fun getDevfile(): String {
        return redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY) ?: ""
    }

    fun checkUserCreate(userId: String, runningOnly: Boolean = false): Boolean {
        val setting = remoteDevSettingDao.fetchSingleUserWsCount(dslContext, userId)
        val maxRunningCount = setting.first ?: redisCache.get(REDIS_DEFAULT_MAX_RUNNING_COUNT)?.toInt() ?: 1
        if (!runningOnly) {
            val maxHavingCount = setting.second ?: redisCache.get(REDIS_DEFAULT_MAX_HAVING_COUNT)?.toInt() ?: 3
            workspaceDao.countUserWorkspace(dslContext, userId, unionShared = false).let {
                if (it >= maxHavingCount) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_MAX_HAVING.errorCode,
                        params = arrayOf(it.toString(), maxHavingCount.toString())
                    )
                }
            }
        }
        workspaceDao.countUserWorkspace(
            dslContext,
            userId,
            unionShared = false,
            status = setOf(WorkspaceStatus.RUNNING, WorkspaceStatus.PREPARING, WorkspaceStatus.STARTING)
        ).let {
            if (it >= maxRunningCount) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_MAX_RUNNING.errorCode,
                    params = arrayOf(it.toString(), maxRunningCount.toString())
                )
            }
        }
        return true
    }

    // 判断用户定义的镜像是否在默认镜像白名单列表中
    fun isImageInDefaultList(image: String?, whitelist: Set<String>): Boolean {
        if (image.isNullOrBlank()) return false
        whitelist.forEach { cidr ->
            if (image.contains(cidr)) return true
        }
        return false
    }
}
