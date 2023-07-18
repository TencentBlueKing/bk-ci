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
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.WorkspaceNotifyTemplateEnum
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceProxyDetail
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DISCOUNT_TIME_KEY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OFFICIAL_DEVFILE_KEY
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
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
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val redisCache: RedisCacheService,
    private val workspaceCommon: WorkspaceCommon
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val defaultPageSize = 20
        private const val DEFAULT_WAIT_TIME = 60
        private const val DISCOUNT_TIME = 10000
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
            // 共享时创建START云桌面的用户
            if (workspace.workspaceMountType == WorkspaceMountType.START.name) {
                client.get(ServiceStartCloudResource::class)
                    .createStartCloudUser(userId)
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
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.SHARE),
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
                        status = workspaceCommon.fixUnexpectedStatus(
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
        workspaceCommon.checkWorkspaceAvailability(userId, workspace)
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

    fun initBilling(freeTime: Int? = null) {
        remoteDevBillingDao.monthlyInit(
            dslContext,
            (freeTime ?: redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: DISCOUNT_TIME) * 60
        )
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
}
