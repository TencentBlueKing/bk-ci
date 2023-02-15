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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceProxyDetail
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.utils.DevfileUtil
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.pojo.WebSocketActionType
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.utils.code.git.GitUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service
class WorkspaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val gitTransferService: GitTransferService,
    private val permissionService: PermissionService,
    private val sshService: SshPublicKeysService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val commonService: CommonService
) {

    private val redisCache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, String>() {
                override fun load(key: String): String {
                    return redisOperation.get(key) ?: ""
                }
            }
        )

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private const val REDIS_CALL_LIMIT_KEY = "remotedev:callLimit"
        private const val REDIS_DISCOUNT_TIME_KEY = "remotedev:discountTime"
        private const val REDIS_OFFICIAL_DEVFILE_KEY = "remotedev:devfile"
        private const val REDIS_OP_HISTORY_KEY_PREFIX = "remotedev:opHistory:"
        private const val ADMIN_NAME = "system"
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val defaultPageSize = 20
        private const val DEFAULT_WAIT_TIME = 60
    }

    fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): List<RemoteDevRepository> {
        logger.info("$userId get user git repository|$search|$page|$pageSize")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: defaultPageSize
        return checkOauthIllegal(userId) {
            gitTransferService.getProjectList(
                userId = userId,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                search = search,
                owned = false,
                minAccessLevel = GitAccessLevelEnum.DEVELOPER
            )
        }
    }

    fun getRepositoryBranch(
        userId: String,
        pathWithNamespace: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): List<String> {
        logger.info("$userId get git repository branch list|$pathWithNamespace|$search|$page|$pageSize")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: defaultPageSize
        return checkOauthIllegal(userId) {
            gitTransferService.getProjectBranches(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                search = search
            ) ?: emptyList()
        }
    }

    fun createWorkspace(userId: String, workspaceCreate: WorkspaceCreate): WorkspaceResponse {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")

        val pathWithNamespace = GitUtils.getDomainAndRepoName(workspaceCreate.repositoryUrl).second
        val projectName = pathWithNamespace.substring(pathWithNamespace.lastIndexOf("/") + 1)
        val yaml = if (workspaceCreate.useOfficialDevfile != true) {
            kotlin.runCatching {
                gitTransferService.getFileContent(
                    userId = userId,
                    pathWithNamespace = pathWithNamespace,
                    filePath = workspaceCreate.devFilePath!!,
                    ref = workspaceCreate.branch
                )
            }.getOrElse {
                logger.warn("get yaml failed ${it.message}")
                throw CustomException(Response.Status.BAD_REQUEST, "获取 devfile 异常 ${it.message}")
            }
        } else {
            // 防止污传
            workspaceCreate.devFilePath = null
            redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY)
        }

        if (yaml.isBlank()) {
            logger.warn(
                "create workspace get devfile blank,return." +
                    "|useOfficialDevfile=${workspaceCreate.useOfficialDevfile}"
            )
            throw CustomException(Response.Status.BAD_REQUEST, "devfile 为空，请确认。")
        }

        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId)
        }.onFailure { logger.warn("get $userId info error|${it.message}") }.getOrElse { null }?.data

        val bizId = MDC.get(TraceTag.BIZID)
        val workspaceName = generateWorkspaceName(userId)
        val workspace = with(workspaceCreate) {
            Workspace(
                workspaceId = null,
                workspaceName = workspaceName,
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
                hostName = ""
            )
        }

        val devfile = DevfileUtil.parseDevfile(yaml).apply {
            gitEmail = kotlin.runCatching {
                gitTransferService.getUserInfo(
                    userId = userId
                )
            }.getOrElse {
                logger.warn("get user $userId info failed ${it.message}")
                throw CustomException(Response.Status.BAD_REQUEST, "获取 user $userId info 异常 ${it.message}")
            }.email
        }

        workspaceDao.createWorkspace(
            userId = userId,
            workspace = workspace,
            workspaceStatus = WorkspaceStatus.PREPARING,
            dslContext = dslContext,
            userInfo = userInfo
        )

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
                gitOAuth = gitTransferService.getAndCheckOauthToken(userId).accessToken,
                settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId)?.envsForVariable ?: emptyMap()
            )
        )

        // 发送给用户
        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = WebSocketActionType.WORKSPACE_CREATE,
                status = true,
                anyMessage = WorkspaceResponse(
                    workspaceName = workspaceName,
                    status = WorkspaceAction.PREPARING
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

        return WorkspaceResponse(
            workspaceName = workspaceName,
            status = WorkspaceAction.PREPARING
        )
    }

    // k8s创建workspace后回调的方法
    fun afterCreateWorkspace(event: RemoteDevUpdateEvent) {
        if (event.status) {
            val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = event.workspaceName)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace ${event.workspaceName} not find")
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
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    operator = event.userId,
                    action = WorkspaceAction.CREATE,
                    actionMessage = String.format(
                        getOpHistory(OpHistoryCopyWriting.CREATE),
                        pathWithNamespace,
                        ws.branch,
                        ws.name
                    )
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    operator = event.userId,
                    action = WorkspaceAction.START,
                    actionMessage = getOpHistory(OpHistoryCopyWriting.FIRST_START)
                )
            }

            redisHeartBeat.refreshHeartbeat(event.workspaceName)

            // websocket 通知成功
        } else {
            // 创建失败
            // websocket 通知失败
            logger.warn("create workspace ${event.workspaceName} failed")
            workspaceDao.deleteWorkspace(event.workspaceName, dslContext)
        }

        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = WebSocketActionType.WORKSPACE_CREATE,
                status = event.status,
                anyMessage = WorkspaceResponse(
                    workspaceHost = event.environmentHost ?: "",
                    workspaceName = event.workspaceName,
                    status = WorkspaceAction.START,
                    errorMsg = event.errorMsg
                ),
                projectId = "",
                userIds = getWebSocketUsers(event.userId, event.workspaceName),
                redisOperation = redisOperation,
                page = WorkspacePageBuild.buildPage(event.workspaceName),
                notifyPost = NotifyPost(
                    module = "remotedev",
                    level = NotityLevel.LOW_LEVEL.getLevel(),
                    message = "",
                    dealUrl = null,
                    code = 200,
                    webSocketType = "IFRAME",
                    page = WorkspacePageBuild.buildPage(event.workspaceName)
                )
            )
        )
    }

    fun startWorkspace(userId: String, workspaceName: String): WorkspaceResponse {
        logger.info("$userId start workspace $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.checkRunning()) {
                logger.info("${workspace.name} is running.")
                remoteDevBillingDao.newBilling(dslContext, workspaceName, userId)
                val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                    .getWorkspaceInfo(userId, workspaceName)

                return WorkspaceResponse(
                    workspaceName = workspaceName,
                    workspaceHost = workspaceInfo.data?.environmentHost ?: "",
                    status = WorkspaceAction.START
                )
            }

            if (notOk2doNextAction(workspace)) {
                logger.info("${workspace.name} is $status, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "${workspace.name} is $status , can't start now.")
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.START,
                actionMessage = getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
            )

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
                    WorkspaceStatus.values()[workspace.status].name,
                    WorkspaceStatus.STARTING.name
                )
            )

            val bizId = MDC.get(TraceTag.BIZID)

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
                    settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId)?.envsForVariable ?: emptyMap()
                )
            )

            // 发送给用户
            webSocketDispatcher.dispatch(
                WorkspaceWebsocketPush(
                    type = WebSocketActionType.WORKSPACE_START,
                    status = true,
                    anyMessage = WorkspaceResponse(
                        workspaceName = workspaceName,
                        status = WorkspaceAction.STARTING
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

            return WorkspaceResponse(
                workspaceName = workspace.name,
                workspaceHost = "",
                status = WorkspaceAction.STARTING
            )
        }
    }

    fun afterStartWorkspace(event: RemoteDevUpdateEvent) {
        if (!event.status) {
            // 调devcloud接口查询是否已经启动成功，如果成功还是走成功的逻辑.
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, event.workspaceName).data!!
            when (workspaceInfo.status) {
                EnvStatusEnum.running -> event.status = true
                else -> logger.warn(
                    "start workspace callback with error|" +
                        "${event.workspaceName}|${workspaceInfo.status}"
                )
            }
        }

        doStartWS(event.status, event.userId, event.workspaceName, event.environmentHost, event.errorMsg)
    }

    private fun doStartWS(
        status: Boolean,
        operator: String,
        workspaceName: String,
        environmentHost: String?,
        errorMsg: String? = null
    ) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkRunning()) return
        if (status) {
            val history = workspaceHistoryDao.fetchHistory(dslContext, workspaceName).firstOrNull()
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
                if (lastHistory != null) {
                    workspaceDao.updateWorkspaceSleepingTime(
                        workspaceName = workspaceName,
                        sleepTime = Duration.between(lastHistory.endTime, LocalDateTime.now()).seconds.toInt(),
                        dslContext = transactionContext
                    )
                }
                workspaceHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    startUserId = operator,
                    lastSleepTimeCost = if (history != null) {
                        Duration.between(history.endTime, LocalDateTime.now()).seconds.toInt()
                    } else 0
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

            redisHeartBeat.refreshHeartbeat(workspaceName)
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

        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = WebSocketActionType.WORKSPACE_START,
                status = status,
                anyMessage = WorkspaceResponse(
                    workspaceHost = environmentHost ?: "",
                    workspaceName = workspaceName,
                    status = WorkspaceAction.START,
                    errorMsg = errorMsg
                ),
                projectId = "",
                userIds = getWebSocketUsers(operator, workspaceName),
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

    fun stopWorkspace(userId: String, workspaceName: String): Boolean {
        logger.info("$userId stop workspace $workspaceName")

        permissionService.checkPermission(userId, workspaceName)
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.checkSleeping()) {
                logger.info("${workspace.name} has been stopped, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "${workspace.name} has been stopped")
            }

            if (notOk2doNextAction(workspace)) {
                logger.info("${workspace.name} is $status, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "${workspace.name} is $status , can't stop now.")
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.SLEEP,
                actionMessage = getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.SLEEPING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.SLEEP,
                actionMessage = String.format(
                    getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    WorkspaceStatus.values()[workspace.status].name,
                    WorkspaceStatus.SLEEPING.name
                )
            )

            val bizId = MDC.get(TraceTag.BIZID)

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.name
                )
            )

            // 发送给用户
            webSocketDispatcher.dispatch(
                WorkspaceWebsocketPush(
                    type = WebSocketActionType.WORKSPACE_SLEEP,
                    status = true,
                    anyMessage = WorkspaceResponse(
                        workspaceName = workspaceName,
                        status = WorkspaceAction.SLEEPING
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

            return true
        }
    }

    fun afterStopWorkspace(event: RemoteDevUpdateEvent) {
        if (!event.status) {
            // 调devcloud接口查询是否已经启动成功，如果成功还是走成功的逻辑.
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, event.workspaceName).data!!
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

    fun deleteWorkspace(userId: String, workspaceName: String): Boolean {
        logger.info("$userId delete workspace $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.checkDeleted()) {
                logger.info("${workspace.name} has been deleted, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "${workspace.name} has been deleted")
            }

            if (notOk2doNextAction(workspace)) {
                logger.info("${workspace.name} is $status, return error.")
                throw CustomException(Response.Status.BAD_REQUEST, "${workspace.name} is $status , can't delete now.")
            }

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspaceName,
                    operator = userId,
                    action = WorkspaceAction.DELETE,
                    actionMessage = getOpHistory(OpHistoryCopyWriting.DELETE)
                )

                workspaceDao.updateWorkspaceStatus(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.DELETING
                )

                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = userId,
                    action = WorkspaceAction.DELETING,
                    actionMessage = String.format(
                        getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        WorkspaceStatus.values()[workspace.status].name,
                        WorkspaceStatus.DELETING.name
                    )
                )
            }

            val bizId = MDC.get(TraceTag.BIZID)
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.name
                )
            )

            // 发送给用户
            webSocketDispatcher.dispatch(
                WorkspaceWebsocketPush(
                    type = WebSocketActionType.WORKSPACE_DELETE,
                    status = true,
                    anyMessage = WorkspaceResponse(
                        workspaceName = workspaceName,
                        status = WorkspaceAction.DELETING
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
            return true
        }
    }

    fun afterDeleteWorkspace(event: RemoteDevUpdateEvent) {
        if (!event.status) {
            // 调devcloud接口查询是否已经成功，如果成功还是走成功的逻辑.
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, event.workspaceName).data!!
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

    fun shareWorkspace(userId: String, workspaceName: String, sharedUser: String): Boolean {
        logger.info("$userId share workspace $workspaceName|$sharedUser")
        permissionService.checkPermission(userId, workspaceName)
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:shareWorkspace:${workspaceName}_$sharedUser",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
            if (userId != workspace.creator) throw CustomException(Response.Status.FORBIDDEN, "你没有权限操作")
            val shareInfo = WorkspaceShared(workspaceName, userId, sharedUser)
            if (workspaceSharedDao.existWorkspaceSharedInfo(shareInfo, dslContext)) {
                logger.info("$workspaceName has already shared to $sharedUser")
                throw CustomException(Response.Status.BAD_REQUEST, "$workspaceName has already shared to $sharedUser")
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
                if (status.notOk2doNextAction() && Duration.between(
                        it.lastStatusUpdateTime,
                        LocalDateTime.now()
                    ).seconds > DEFAULT_WAIT_TIME
                ) {
                    val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                        .getWorkspaceInfo(userId, it.name).data!!
                    when (workspaceInfo.status) {
                        EnvStatusEnum.stopped -> {
                            doStopWS(true, userId, it.name)
                            status = WorkspaceStatus.SLEEP
                        }
                        EnvStatusEnum.deleted -> {
                            doDeleteWS(true, userId, it.name)
                            status = WorkspaceStatus.DELETED
                        }
                        EnvStatusEnum.running -> {
                            doStartWS(true, userId, it.name, workspaceInfo.environmentHost)
                            status = WorkspaceStatus.RUNNING
                        }
                        else -> logger.warn(
                            "wait workspace change over $DEFAULT_WAIT_TIME second |" +
                                "${it.name}|${workspaceInfo.status}"
                        )
                    }
                }
                Workspace(
                    workspaceId = it.id,
                    workspaceName = it.name,
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
                    hostName = it.hostName
                )
            }
        )
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
                Duration.between(latestSleepHistory[it.name]?.endTime, now).seconds
            } else 0
        }

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it.startTime, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY).toLong()
        return WorkspaceUserDetail(
            runningCount = status.count { it.checkRunning() },
            sleepingCount = status.count { it.checkSleeping() },
            deleteCount = status.count { it.checkDeleted() },
            chargeableTime = endBilling.value2() +
                (notEndBillingTime + endBilling.value1() - discountTime).coerceAtLeast(0),
            usageTime = usageTime,
            sleepingTime = sleepingTime,
            discountTime = discountTime,
            cpu = workspaces.sumOf { it.cpu },
            memory = workspaces.sumOf { it.memory },
            disk = workspaces.sumOf { it.disk }
        )
    }

    fun getWorkspaceDetail(userId: String, workspaceName: String): WorkspaceDetail? {
        logger.info("$userId get workspace from id $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
        val now = LocalDateTime.now()
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: return null

        val workspaceStatus = WorkspaceStatus.values()[workspace.status]

        val lastHistory = workspaceHistoryDao.fetchAnyHistory(dslContext, workspaceName) ?: return null

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY).toInt()

        val usageTime = workspace.usageTime + if (workspaceStatus.checkRunning()) {
            // 如果正在运行，需要加上目前距离该次启动的时间
            Duration.between(lastHistory.startTime, now).seconds
        } else 0

        val sleepingTime = workspace.sleepingTime + if (workspaceStatus.checkSleeping()) {
            // 如果正在休眠，需要加上目前距离上次结束的时间
            Duration.between(lastHistory.endTime, now).seconds
        } else 0

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it.startTime, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)
        return with(workspace) {
            WorkspaceDetail(
                workspaceId = id,
                workspaceName = name,
                status = workspaceStatus,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = endBilling.value2() +
                    (notEndBillingTime + endBilling.value1() - discountTime).coerceAtLeast(0),
                usageTime = usageTime,
                sleepingTime = sleepingTime,
                cpu = cpu,
                memory = memory,
                disk = disk,
                yaml = yaml
            )
        }
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
        val userSet = workspaceDao.fetchWorkspaceUser(
            dslContext,
            workspaceName
        ).toSet()
        val sshKey = sshService.getSshPublicKeys4Ws(userSet)
        val workspaceInfo = client.get(ServiceRemoteDevResource::class).getWorkspaceInfo(userSet.first(), workspaceName)

        return WorkspaceProxyDetail(
            workspaceName = workspaceName,
            podIp = workspaceInfo.data?.environmentIP ?: "",
            sshKey = sshKey
        )
    }

    fun checkDevfile(userId: String, pathWithNamespace: String, branch: String): List<String> {
        logger.info("$userId get devfile list from git. $pathWithNamespace|$branch")
        return checkOauthIllegal(userId) {
            gitTransferService.getFileNameTree(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                path = Constansts.devFileDirectoryName, // 根目录
                ref = branch,
                recursive = false // 不递归
            ).map { Constansts.devFileDirectoryName + "/" + it }
        }
    }

    fun heartBeatStopWS(workSpaceName: String): Boolean {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext = dslContext, workspaceName = workSpaceName)
            ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workSpaceName not find")
        // 校验状态
        val status = WorkspaceStatus.values()[workspace.status]
        if (status.checkSleeping()) {
            logger.info("$workspace has been stopped, return error.")
            throw CustomException(Response.Status.BAD_REQUEST, "$workspace has been stopped")
        }
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:workspace:${workspace.id}",
            expiredTimeInSeconds
        ).lock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.name,
                operator = ADMIN_NAME,
                action = WorkspaceAction.SLEEP,
                actionMessage = getOpHistory(OpHistoryCopyWriting.TIMEOUT_SLEEP)
            )

            val bizId = MDC.get(TraceTag.BIZID)

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = ADMIN_NAME,
                    traceId = bizId,
                    type = UpdateEventType.STOP,
                    workspaceName = workspace.name
                )
            )

            // 发送给用户
            webSocketDispatcher.dispatch(
                WorkspaceWebsocketPush(
                    type = WebSocketActionType.WORKSPACE_SLEEP,
                    status = true,
                    anyMessage = WorkspaceResponse(
                        workspaceName = workSpaceName,
                        status = WorkspaceAction.SLEEPING
                    ),
                    projectId = "",
                    userIds = getWebSocketUsers(ADMIN_NAME, workSpaceName),
                    redisOperation = redisOperation,
                    page = WorkspacePageBuild.buildPage(workSpaceName),
                    notifyPost = NotifyPost(
                        module = "remotedev",
                        level = NotityLevel.LOW_LEVEL.getLevel(),
                        message = "",
                        dealUrl = null,
                        code = 200,
                        webSocketType = "IFRAME",
                        page = WorkspacePageBuild.buildPage(workSpaceName)
                    )
                )
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
            heartBeatDeleteWS(it)
        }
    }

    fun heartBeatDeleteWS(workspace: TWorkspaceRecord): Boolean {
        logger.info("heart beat delete workspace ${workspace.name}")
        // 校验状态
        val status = WorkspaceStatus.values()[workspace.status]
        if (status.checkDeleted()) {
            logger.info("$workspace has been deleted, return error.")
            throw CustomException(Response.Status.BAD_REQUEST, "$workspace has been deleted")
        }
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY:workspace:${workspace.name}",
            expiredTimeInSeconds
        ).lock().use {
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspace.name,
                operator = ADMIN_NAME,
                action = WorkspaceAction.DELETE,
                actionMessage = getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
            )
            val bizId = MDC.get(TraceTag.BIZID)
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = ADMIN_NAME,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.name
                )
            )

            webSocketDispatcher.dispatch(
                WorkspaceWebsocketPush(
                    type = WebSocketActionType.WORKSPACE_DELETE,
                    status = true,
                    anyMessage = WorkspaceResponse(
                        workspaceName = workspace.name,
                        status = WorkspaceAction.DELETING
                    ),
                    projectId = "",
                    userIds = getWebSocketUsers(ADMIN_NAME, workspace.name),
                    redisOperation = redisOperation,
                    page = WorkspacePageBuild.buildPage(workspace.name),
                    notifyPost = NotifyPost(
                        module = "remotedev",
                        level = NotityLevel.LOW_LEVEL.getLevel(),
                        message = "",
                        dealUrl = null,
                        code = 200,
                        webSocketType = "IFRAME",
                        page = WorkspacePageBuild.buildPage(workspace.name)
                    )
                )
            )
            return true
        }
    }

    private fun doDeleteWS(status: Boolean, operator: String, workspaceName: String, errorMsg: String? = null) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkDeleted()) return
        if (status) {
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
        remoteDevBillingDao.endBilling(dslContext, workspaceName)

        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = WebSocketActionType.WORKSPACE_DELETE,
                status = status,
                anyMessage = WorkspaceResponse(
                    workspaceName = workspaceName,
                    status = WorkspaceAction.DELETE,
                    errorMsg = errorMsg
                ),
                projectId = "",
                userIds = getWebSocketUsers(operator, workspaceName),
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

    private fun doStopWS(status: Boolean, operator: String, workspaceName: String, errorMsg: String? = null) {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw CustomException(Response.Status.NOT_FOUND, "workspace $workspaceName not find")
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkSleeping()) return
        if (status) {
            // 清心跳
            redisHeartBeat.deleteWorkspaceHeartbeat(operator, workspaceName)
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.SLEEP,
                    dslContext = transactionContext
                )
                val lastHistory = workspaceHistoryDao.fetchAnyHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName
                )
                if (lastHistory != null) {
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

        remoteDevBillingDao.endBilling(dslContext, workspaceName)

        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = WebSocketActionType.WORKSPACE_SLEEP,
                status = status,
                anyMessage = WorkspaceResponse(
                    workspaceName = workspaceName,
                    status = WorkspaceAction.SLEEP,
                    errorMsg = errorMsg
                ),
                projectId = "",
                userIds = getWebSocketUsers(operator, workspaceName),
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

    fun initBilling(freeTime: Int) {
        remoteDevBillingDao.monthlyInit(dslContext, freeTime)
    }

    private fun getWebSocketUsers(operator: String, workspaceName: String): Set<String> {
        return if (operator == ADMIN_NAME)
            workspaceDao.fetchWorkspaceUser(dslContext, workspaceName).toSet()
        else setOf(operator)
    }

    /**
     * 检查工蜂接口是否返回401，针对这种情况，抛出OAUTH_ILLEGAL 让前端跳转去重新授权
     */
    private fun <T> checkOauthIllegal(userId: String, action: () -> T): T {
        return kotlin.runCatching {
            action()
        }.onFailure {
            if (it is RemoteServiceException && it.httpStatus == HTTP_401 || it is OauthForbiddenException) {
                throw ErrorCodeException(
                    statusCode = 400,
                    errorCode = ErrorCodeEnum.OAUTH_ILLEGAL.errorCode.toString(),
                    defaultMessage = ErrorCodeEnum.OAUTH_ILLEGAL.formatErrorMessage.format(userId),
                    params = arrayOf(userId)
                )
            }
        }.getOrThrow()
    }

    private fun getOpHistory(key: OpHistoryCopyWriting) =
        redisCache.get(REDIS_OP_HISTORY_KEY_PREFIX + key.name).ifBlank {
            key.default
        }

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
     */
    private fun notOk2doNextAction(workspace: TWorkspaceRecord): Boolean {
        return WorkspaceStatus.values()[workspace.status].notOk2doNextAction() && Duration.between(
            workspace.lastStatusUpdateTime,
            LocalDateTime.now()
        ).seconds < DEFAULT_WAIT_TIME
    }

    fun getWorkspaceHost(workspaceName: String): String {
        val url = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)?.url
            ?: throw CustomException(Response.Status.NOT_FOUND, "not find workspaceName $workspaceName")
        return GitUtils.getDomainAndRepoName(url).first
    }

    fun getDevfile(): String {
        return redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY)
    }

    fun updateBkTicket(userId: String, bkTicket: String, hostName: String): Boolean {
        logger.info("updateBkTicket|userId|$userId|bkTicket|$bkTicket|hostName|$hostName")
        val url = "http://$hostName/_remoting/api/token/updateBkTicket"
        val params = mutableMapOf<String, Any?>()
        params["ticket"] = bkTicket
        params["user"] = userId
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .header("Cookie", "X-DEVOPS-BK-TICKET=$bkTicket")
            .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(params)))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            val dataMap = JsonUtil.toMap(data)
            val status = dataMap["status"]
            return (status == 0)
        }
    }
}
