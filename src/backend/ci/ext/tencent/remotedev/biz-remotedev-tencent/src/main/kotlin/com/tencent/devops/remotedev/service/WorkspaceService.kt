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

import com.tencent.devops.common.api.constant.HTTP_401
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
import com.tencent.devops.project.api.service.ServiceProjectTagResource
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
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
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
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DEFAULT_MAX_HAVING_COUNT
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DEFAULT_MAX_RUNNING_COUNT
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DISCOUNT_TIME_KEY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OFFICIAL_DEVFILE_KEY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OP_HISTORY_KEY_PREFIX
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_REMOTEDEV_GRAY_VERSION
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_REMOTEDEV_PROD_VERSION
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.utils.DevfileUtil
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.pojo.WebSocketActionType
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.utils.code.git.GitUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
    private val webSocketDispatcher: WebSocketDispatcher,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val commonService: CommonService,
    private val redisCache: RedisCacheService,
    private val profile: Profile
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private const val ADMIN_NAME = "system"
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val defaultPageSize = 20
        private const val DEFAULT_WAIT_TIME = 60
        private const val BLANK_TEMPLATE_YAML_NAME = "BLANK"
        private const val BLANK_TEMPLATE_ID = 1
    }

    fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        gitType: RemoteDevGitType
    ): List<RemoteDevRepository> {
        logger.info("$userId get user git repository|$search|$page|$pageSize")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: defaultPageSize
        return checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getProjectList(
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
        gitType: RemoteDevGitType
    ): List<String> {
        logger.info("$userId get git repository branch list|$pathWithNamespace")
        return checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getProjectBranches(
                userId = userId,
                pathWithNamespace = pathWithNamespace
            ) ?: emptyList()
        }
    }

    fun createWorkspace(userId: String, workspaceCreate: WorkspaceCreate): WorkspaceResponse {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")
        checkUserCreate(userId)
        val gitTransferService = remoteDevGitTransfer.loadByGitUrl(workspaceCreate.repositoryUrl)
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
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                    defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage
                        .format("获取 devfile 异常 ${it.message}"),
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
                defaultMessage = ErrorCodeEnum.DEVFILE_ERROR.formatErrorMessage.format("devfile 为空，请确认。"),
                params = arrayOf("devfile 为空，请确认。")
            )
        }

        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId)
        }.onFailure { logger.warn("get $userId info error|${it.message}") }.getOrElse { null }?.data

        val devfile = DevfileUtil.parseDevfile(yaml).apply {
            gitEmail = kotlin.runCatching {
                gitTransferService.getUserEmail(
                    userId = userId
                )
            }.getOrElse {
                logger.warn("get user $userId info failed ${it.message}")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.USERINFO_ERROR.errorCode,
                    defaultMessage = ErrorCodeEnum.USERINFO_ERROR.formatErrorMessage
                        .format("get user($userId) info from git failed"),
                    params = arrayOf("get user($userId) info from git failed")
                )
            }

            dotfileRepo = remoteDevSettingDao.fetchAnySetting(dslContext, userId).dotfileRepo
        }

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
                workspaceFolder = devfile.workspaceFolder ?: "",
                hostName = ""
            )
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
                gitOAuth = gitTransferService.getAndCheckOauthToken(userId),
                settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId).envsForVariable
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
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(event.workspaceName),
                    params = arrayOf(event.workspaceName)
                )
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
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                    params = arrayOf(workspaceName)
                )
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
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                        .format(workspace.name, "status is already $status, can't start now"),
                    params = arrayOf(workspace.name, "status is already $status, can't start now")
                )
            }
            checkUserCreate(userId, true)
            /*处理异常的情况*/
            checkAndFixExceptionWS(status, userId, workspaceName)
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
                    settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId).envsForVariable
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

    private fun checkAndFixExceptionWS(
        status: WorkspaceStatus,
        userId: String,
        workspaceName: String
    ) {
        if (status.checkException()) {
            when (val fix = fixUnexpectedStatus(userId, workspaceName, status)) {
                WorkspaceStatus.EXCEPTION -> {
                    logger.info("$workspaceName is EXCEPTION and not repaired, return error.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_ERROR.errorCode,
                        defaultMessage = ErrorCodeEnum.WORKSPACE_ERROR.formatErrorMessage
                    )
                }
                else -> {
                    logger.info("$workspaceName is $status to $fix , return info.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_ERROR_FIX.errorCode,
                        defaultMessage = ErrorCodeEnum.WORKSPACE_ERROR_FIX.formatErrorMessage
                            .format(fix.name),
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
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
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

                val lastSleepTimeCost = if (lastHistory != null) {
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
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                    params = arrayOf(workspaceName)
                )
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.checkSleeping()) {
                logger.info("${workspace.name} has been stopped, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                        .format(workspace.name, "status is already $status, can't stop again"),
                    params = arrayOf(workspace.name, "status is already $status, can't stop again")
                )
            }

            if (notOk2doNextAction(workspace)) {
                logger.info("${workspace.name} is $status, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                        .format(workspace.name, "status is already $status, can't stop now"),
                    params = arrayOf(workspace.name, "status is already $status, can't stop now")
                )
            }

            /*处理异常的情况*/
            checkAndFixExceptionWS(status, userId, workspaceName)
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
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                    params = arrayOf(workspaceName)
                )
            // 校验状态
            val status = WorkspaceStatus.values()[workspace.status]
            if (status.checkDeleted()) {
                logger.info("${workspace.name} has been deleted, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                        .format(workspace.name, "status is already $status, can't delete again"),
                    params = arrayOf(workspace.name, "status is already $status, can't delete again")
                )
            }

            if (notOk2doNextAction(workspace)) {
                logger.info("${workspace.name} is $status, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                        .format(workspace.name, "status is already $status, can't delete now"),
                    params = arrayOf(workspace.name, "status is already $status, can't delete now")
                )
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
        doDeleteWS(event.status, event.userId, event.workspaceName, event.environmentIp, event.errorMsg)
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
                    defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                    params = arrayOf(workspaceName)
                )
            if (userId != workspace.creator) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                    defaultMessage = ErrorCodeEnum.FORBIDDEN.formatErrorMessage
                        .format("only workspace creator can share"),
                    params = arrayOf("only workspace creator can share")
                )
            }
            val shareInfo = WorkspaceShared(workspaceName, userId, sharedUser)
            if (workspaceSharedDao.existWorkspaceSharedInfo(shareInfo, dslContext)) {
                logger.info("$workspaceName has already shared to $sharedUser")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_SHARE_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_SHARE_FAIL.formatErrorMessage
                        .format("$workspaceName has already shared to $sharedUser"),
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
                            it.lastStatusUpdateTime,
                            LocalDateTime.now()
                        ).seconds > DEFAULT_WAIT_TIME
                    ) {
                        status = fixUnexpectedStatus(userId, it.name, status)
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
                    workspaceFolder = it.workspaceFolder,
                    hostName = it.hostName
                )
            }
        )
    }

    private fun fixUnexpectedStatus(
        userId: String,
        workspaceName: String,
        status: WorkspaceStatus
    ): WorkspaceStatus {
        val workspaceInfo = kotlin.runCatching {
            client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(userId, workspaceName).data!!
        }.getOrElse { ignore ->
            logger.warn(
                "get workspace info error $workspaceName|${ignore.message}"
            )
            workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION)
            return WorkspaceStatus.EXCEPTION
        }
        logger.info("fixUnexpectedStatus|$workspaceName|$status|$workspaceInfo")
        when (workspaceInfo.status) {
            EnvStatusEnum.stopped -> {
                doStopWS(true, userId, workspaceName)
                return WorkspaceStatus.SLEEP
            }
            EnvStatusEnum.deleted -> {
                doDeleteWS(true, userId, workspaceName, workspaceInfo.environmentIP)
                return WorkspaceStatus.DELETED
            }
            EnvStatusEnum.running -> {
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
                Duration.between(latestSleepHistory[it.name]?.endTime, now).seconds
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

    fun getWorkspaceDetail(userId: String, workspaceName: String): WorkspaceDetail? {
        logger.info("$userId get workspace from id $workspaceName")
        permissionService.checkPermission(userId, workspaceName)
        val now = LocalDateTime.now()
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: return null

        val workspaceStatus = WorkspaceStatus.values()[workspace.status]

        val lastHistory = workspaceHistoryDao.fetchAnyHistory(dslContext, workspaceName) ?: return null

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: 10560

        val usageTime = workspace.usageTime + if (workspaceStatus.checkRunning()) {
            // 如果正在运行，需要加上目前距离该次启动的时间
            Duration.between(lastHistory.startTime, now).seconds
        } else 0

        val sleepingTime = workspace.sleepingTime + if (workspaceStatus.checkSleeping()) {
            // 如果正在休眠，需要加上目前距离上次结束的时间
            Duration.between(lastHistory.endTime, now).seconds
        } else 0

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)
        return with(workspace) {
            WorkspaceDetail(
                workspaceId = id,
                workspaceName = name,
                status = workspaceStatus,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = endBilling.second +
                    (notEndBillingTime + endBilling.first - discountTime * 60).coerceAtLeast(0),
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
            sshKey = sshKey,
            environmentHost = workspaceInfo.data?.environmentHost ?: ""
        )
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
        return checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getFileNameTree(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                path = Constansts.devFileDirectoryName, // 根目录
                ref = branch,
                recursive = false // 不递归
            ).map { Constansts.devFileDirectoryName + "/" + it }
        }
    }

    fun heartBeatStopWS(workspaceName: String): Boolean {

        val workspace = workspaceDao.fetchAnyWorkspace(dslContext = dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                params = arrayOf(workspaceName)
            )
        // 校验状态
        val status = WorkspaceStatus.values()[workspace.status]
        if (status.checkSleeping()) {
            logger.info("$workspace has been stopped, return error.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                    .format(workspace.name, "status is already $status, can't stop again"),
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
                        workspaceName = workspaceName,
                        status = WorkspaceAction.SLEEPING
                    ),
                    projectId = "",
                    userIds = getWebSocketUsers(ADMIN_NAME, workspaceName),
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
            fixUnexpectedStatus(ADMIN_NAME, it.name, WorkspaceStatus.values()[it.status])
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
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                defaultMessage = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.formatErrorMessage
                    .format(workspace.name, "status is already $status, can't delete again"),
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
                defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                params = arrayOf(workspaceName)
            )
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        if (oldStatus.checkDeleted()) return
        if (status) {
            // 删除环境管理第三方构建机记录
            val projectId = remoteDevSettingDao.fetchAnySetting(dslContext, workspace.creator).projectId
            if (client.get(ServiceNodeResource::class)
                    .deleteThirdPartyNode(workspace.creator, projectId, workspace.preciAgentId ?: "").data == false
            ) {
                logger.warn(
                    "delete workspace $workspaceName, but third party agent delete failed." +
                        "|${workspace.creator}|$projectId|$nodeIp|${workspace.preciAgentId}"
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
                updateLastHistory(transactionContext, workspaceName, operator)
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
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
                params = arrayOf(workspaceName)
            )
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
                updateLastHistory(transactionContext, workspaceName, operator)
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

    private fun updateLastHistory(
        transactionContext: DSLContext,
        workspaceName: String,
        operator: String
    ) {
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
    }

    fun initBilling(freeTime: Int? = null) {
        remoteDevBillingDao.monthlyInit(
            dslContext,
            (freeTime ?: redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: 10560) * 60
        )
    }

    private fun getWebSocketUsers(operator: String, workspaceName: String): Set<String> {
        return if (operator == ADMIN_NAME) {
            workspaceDao.fetchWorkspaceUser(dslContext, workspaceName).toSet()
        } else setOf(operator)
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
                    errorCode = ErrorCodeEnum.OAUTH_ILLEGAL.errorCode,
                    defaultMessage = ErrorCodeEnum.OAUTH_ILLEGAL.formatErrorMessage.format(userId),
                    params = arrayOf(userId)
                )
            }
        }.getOrThrow()
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
                workspace.lastStatusUpdateTime,
                LocalDateTime.now()
            ).seconds < DEFAULT_WAIT_TIME
            ) || WorkspaceStatus.values()[workspace.status].checkDeleted()
    }

    fun getWorkspaceHost(workspaceName: String): String {
        val url = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)?.url
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName),
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
                        defaultMessage = ErrorCodeEnum.WORKSPACE_MAX_HAVING.formatErrorMessage
                            .format(it, maxHavingCount),
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
                    defaultMessage = ErrorCodeEnum.WORKSPACE_MAX_RUNNING.formatErrorMessage
                        .format(it, maxRunningCount),
                    params = arrayOf(it.toString(), maxRunningCount.toString())
                )
            }
        }
        return true
    }

    fun updateBkTicket(userId: String, bkTicket: String, hostName: String): Boolean {
        logger.info("updateBkTicket|userId|$userId|bkTicket|$bkTicket|hostName|$hostName")
        if (bkTicket.isEmpty() || hostName.isEmpty()) {
            return false
        }
        val url = "https://$hostName/_remoting/api/token/updateBkTicket"
        val params = mutableMapOf<String, Any?>()
        params["ticket"] = bkTicket
        params["user"] = userId
        val request = Request.Builder()
            .url(commonService.getProxyUrl(url))
            .header("Cookie", "X-DEVOPS-BK-TICKET=$bkTicket")
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), JsonUtil.toJson(params)))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body!!.string()
            logger.info("updateBkTicket|response code|${response.code}|content|$data")
            if (!response.isSuccessful) {
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = ErrorCodeEnum.UPDATE_BK_TICKET_FAIL.errorCode,
                    defaultMessage = ErrorCodeEnum.UPDATE_BK_TICKET_FAIL.formatErrorMessage
                )
            }

            val dataMap = JsonUtil.toMap(data)
            val status = dataMap["status"]
            return (status == 0)
        }
    }

    // 校验是否有当前环境客户端的最新稳定版
    fun checkUpdate(userId: String): String {
        logger.info("checkUpdate|userId|$userId")
        // 先查询该用户信息获取是否灰度用户标记
        val grayFlag = remoteDevSettingDao.fetchAnyOpUserSetting(dslContext, userId)?.grayFlag ?: false
        // 根据灰度标识读取不同redis key对应的版本
        var redisKey = REDIS_REMOTEDEV_PROD_VERSION
        if (grayFlag) {
            redisKey = REDIS_REMOTEDEV_GRAY_VERSION
        }
        return redisCache.get(redisKey)?.ifBlank {
            ""
        } ?: ""
    }

    // 客户端版本升级后调用接口更新记录的版本信息
    fun updateClientVersion(userId: String, env: String, version: String) {
        logger.info("updateClientVersion|userId|$userId|env|$env|version|$version")
        var redisKey = REDIS_REMOTEDEV_PROD_VERSION
        if (env == "gray") {
            redisKey = REDIS_REMOTEDEV_GRAY_VERSION
        }
        redisOperation.set(
            key = redisKey,
            value = version,
            expired = false
        )
    }
}
