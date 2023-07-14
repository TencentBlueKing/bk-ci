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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Devfile
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.RemoteDevCommonConfig
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevReminderEvent
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.service.BkTicketService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OFFICIAL_DEVFILE_KEY
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.utils.DevfileUtil
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class CreateControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val remoteDevGitTransfer: RemoteDevGitTransfer,
    private val permissionService: PermissionService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val redisCache: RedisCacheService,
    private val bkTicketServie: BkTicketService,
    private val whiteListService: WhiteListService,
    private val commonConfig: RemoteDevCommonConfig,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CreateControl::class.java)
        private const val BLANK_TEMPLATE_YAML_NAME = "BLANK"
        private const val BLANK_TEMPLATE_ID = 1
    }

    // 处理创建工作空间逻辑
    fun createWorkspace(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): WorkspaceResponse {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")
        permissionService.checkUserCreate(userId)

        val workspace = if (workspaceCreate.windowsResourceConfigId != null) {
            loadWorkspaceWithUI(userId, bkTicket, projectId, workspaceCreate)
        } else loadWorkspaceWithCode(userId, bkTicket, projectId, workspaceCreate)

        // 发送给用户
        workspaceCommon.dispatchWebsocketPushEvent(
            userId = userId,
            workspaceName = workspace.workspaceName,
            workspaceHost = null,
            errorMsg = null,
            type = WebSocketActionType.WORKSPACE_CREATE,
            status = true,
            action = WorkspaceAction.PREPARING,
            systemType = workspace.workspaceSystemType, workspaceMountType = workspace.workspaceMountType
        )

        return WorkspaceResponse(
            workspaceName = workspace.workspaceName,
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
            val pathWithNamespace = kotlin.runCatching {
                GitUtils.getDomainAndRepoName(ws.url).second
            }.getOrNull()
            val opActions = kotlin.runCatching {
                arrayOf(
                    WorkspaceAction.CREATE to getOpHistoryCreate(
                        WorkspaceSystemType.valueOf(ws.systemType),
                        pathWithNamespace ?: "",
                        ws.branch ?: ""
                    ),
                    WorkspaceAction.START to workspaceCommon.getOpHistory(OpHistoryCopyWriting.FIRST_START)
                )
            }.getOrElse { emptyArray() }
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
                opActions.forEach { (action, actionMessage) ->
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceName = event.workspaceName,
                        operator = event.userId,
                        action = action,
                        actionMessage = actionMessage
                    )
                }
            }

            workspaceCommon.getOrSaveWorkspaceDetail(event.workspaceName, event.mountType)
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

        workspaceCommon.dispatchWebsocketPushEvent(
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

    private fun getOpHistoryCreate(type: WorkspaceSystemType, vararg args: Any) = when (type) {
        WorkspaceSystemType.WINDOWS_GPU -> workspaceCommon.getOpHistory(OpHistoryCopyWriting.CREATE_WINDOWS)
        WorkspaceSystemType.LINUX -> workspaceCommon.getOpHistory(OpHistoryCopyWriting.CREATE).format(args)
    }

    @Suppress("ComplexMethod")
    private fun loadWorkspaceWithCode(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): Workspace {
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
            windowsGpuCheck(workspaceCreate, userId)
        }

        val mountType = checkMountType(userId, devfile.checkWorkspaceMountType())
        logger.info("createWorkspace|mountType|$mountType")
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

        doPreparing(userId, workspace)

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

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = workspace.workspaceName,
                repositoryUrl = workspace.repositoryUrl ?: "",
                branch = workspace.branch ?: "",
                devFilePath = workspace.devFilePath,
                devFile = devfile,
                gitOAuth = gitTransferService.getAndCheckOauthToken(userId),
                settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId).envsForVariable,
                bkTicket = bkTicket,
                projectId = projectId,
                mountType = mountType
            )
        )

        return workspace
    }

    private fun loadWorkspaceWithUI(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): Workspace {
        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        val windowsConfig = windowsResourceConfigService.getConfig(workspaceCreate.windowsResourceConfigId!!)
            ?: throw throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(workspaceCreate.windowsResourceConfigId.toString())
            )

        if (windowsConfig.available == false) {
            throw throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(workspaceCreate.windowsResourceConfigId.toString())
            )
        }

        windowsGpuCheck(workspaceCreate, userId)

        logger.info("createWorkspace|mountType|$mountType")
        val workspaceName = generateWorkspaceName(userId)
        val workspace = Workspace(
            workspaceId = null,
            workspaceName = workspaceName,
            projectId = projectId,
            createUserId = userId,
            hostName = "",
            workspaceMountType = mountType,
            workspaceSystemType = systemType,
            gpu = windowsConfig.gpu,
            cpu = windowsConfig.cpu,
            memory = windowsConfig.memory,
            disk = windowsConfig.disk
        )

        doPreparing(userId, workspace)

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = workspace.workspaceName,
                repositoryUrl = "",
                branch = "",
                devFilePath = workspace.devFilePath,
                devFile = Devfile(""),
                settingEnvs = remoteDevSettingDao.fetchAnySetting(dslContext, userId).envsForVariable,
                bkTicket = bkTicket,
                projectId = projectId,
                mountType = mountType
            )
        )

        return workspace
    }

    private fun windowsGpuCheck(workspaceCreate: WorkspaceCreate, userId: String) {
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

    private fun doPreparing(userId: String, workspace: Workspace) {
        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId)
        }.onFailure { logger.warn("get $userId info error|${it.message}") }.getOrElse { null }?.data

        workspaceDao.createWorkspace(
            userId = userId,
            workspace = workspace,
            workspaceStatus = WorkspaceStatus.PREPARING,
            dslContext = dslContext,
            userInfo = userInfo
        )
    }

    private fun generateWorkspaceName(userId: String): String {
        val subUserId = if (userId.length > Constansts.subUserIdLimitLen) {
            userId.substring(0 until Constansts.subUserIdLimitLen)
        } else {
            userId
        }
        return "${subUserId.replace("_", "-")}-${UUIDUtil.generate().takeLast(Constansts.workspaceNameSuffixLimitLen)}"
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
