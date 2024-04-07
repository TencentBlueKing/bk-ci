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
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.cron.HolidayHelper
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceKafkaInfo
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.BKBaseService
import com.tencent.devops.remotedev.service.BKCCService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.NOT_ASSIGN_AUTO_DELETE_NOTIFY
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.SLEEP_7_DAY_AUTO_DELETE_NOTIFY
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    private val bkccService: BKCCService,
    private val notifyControl: NotifyControl,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService,
    private val bkBaseService: BKBaseService,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val holidayHelper: HolidayHelper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeleteControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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
            notifyControl.dispatchWebsocketPushEvent(
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

    fun deleteWorkspace4OP(
        userId: String,
        workspaceName: String
    ): Boolean {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        val res = deleteWorkspace4System(userId, workspaceName)
        if (res) {
            val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
            notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                userIds = userIds.toMutableSet(),
                workspaceName = workspace.workspaceName,
                cc = mutableSetOf(workspace.createUserId),
                projectId = workspace.projectId,
                notifyTemplateCode = NotifyControl.WORKSPACE_FORCE_DELETE,
                notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                bodyParams = mutableMapOf(
                    "cgsIp" to (workspace.hostName ?: ""),
                    "userId" to userIds.joinToString(),
                    "projectId" to (workspace.projectId)
                )
            )
        }
        return res
    }

    fun batchDeleteWorkspace4OP(
        userId: String,
        workspaceNames: Set<String>
    ): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        workspaceNames.forEach {
            result[it] = deleteWorkspace4System(userId, it)
        }
        // 只发送删除成功的机器
        val deletedWorkspaces = result.filter { it.value }.keys

        val workspaces = workspaceDao.fetchWorkspaces(dslContext, deletedWorkspaces).associateBy {
            it.workspaceName
        }

        // 将工作空间按工作空间拥有者划分
        val data = mutableMapOf<String, MutableSet<WorkspaceRecord>>()
        deletedWorkspaces.forEach { workspaceName ->
            val workspace = workspaces[workspaceName]
            if (workspace == null) {
                logger.error("batchDeleteWorkspace4OP $workspaceName not find in records $workspaces")
                return@forEach
            }
            val userIds = permissionService.getWorkspaceOwner(workspaceName)
            userIds.forEach { userId ->
                if (data[userId] == null) {
                    data[userId] = mutableSetOf(workspace)
                } else {
                    data[userId]!!.add(workspace)
                }
            }
        }

        // 获取机器最后上线时间
        val lastTimeMap = bkBaseService.fetchLastOnline(data.values.flatten().toSet().map { it.workspaceName }.toSet())

        data.forEach { (user, workspaces) ->
            // 同时抄送给工作空间所属项目的管理员和工作空间的创建人
            val projects = kotlin.runCatching {
                client.get(ServiceProjectResource::class)
                    .listByProjectCodeList(workspaces.map { it.projectId }.toList())
            }.onFailure {
                logger.warn("get project ${workspaces.map { w -> w.projectId }.toList()} info error|${it.message}")
            }.getOrElse { null }?.data ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST
            )

            val cc = projects.map {
                it.properties?.remotedevManager?.split(";")?.toSet() ?: emptySet()
            }.flatten().toMutableSet()
            cc.addAll(workspaces.map { w -> w.createUserId }.toSet())

            // 生成表格数据
            val tableData = workspaces.associate {
                (it.hostName?.substringAfter(".") ?: "") to Pair((lastTimeMap[it.hostName] ?: ""), it.projectId)
            }
            val (emailTable, rtxTable) = generateTable(user, tableData)

            notifyControl.notify4UserAndCCRemoteDevManager(
                userIds = mutableSetOf(user),
                cc = cc,
                projectId = null,
                notifyTemplateCode = NotifyControl.WORKSPACE_BATCH_FORCE_DELETE,
                notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                bodyParams = mutableMapOf(
                    "userId" to user,
                    "rtxTable" to rtxTable,
                    "emailTable" to emailTable
                )
            )
        }

        return result
    }

    fun generateTable(
        userId: String,
        data: Map<String, Pair<String, String>>
    ): Pair<String, String> {
        val emailSb = StringBuilder()
        val td = "<td style=\"border: 1px solid black; padding: 4px; text-align: left;\">%s</td>"
        data.forEach { (ip, workspace) ->
            emailSb.append("<tr>")
            emailSb.append(td.format(ip))
            emailSb.append(td.format(userId))
            emailSb.append(td.format(workspace.second))
            emailSb.append(td.format(workspace.first))
            emailSb.append("</tr>")
        }

        val rtxSb = StringBuilder()
        data.keys.forEach {
            rtxSb.append(it).append("\n")
        }
        rtxSb.removeSuffix("\n")
        rtxSb.toString()

        return Pair(emailSb.toString(), rtxSb.toString())
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS
        ),
        content = ActionAuditContent.CGS_DELETE_CONTENT
    )
    private fun deleteWorkspace4System(
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
                .addInstanceInfo(
                    workspaceName, workspaceName, null, null
                )
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
                    userId = workspace.createUserId,
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
    fun deleteLinuxInactivityWorkspace() {
        logger.info("deleteLinuxInactivityWorkspace")
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
    }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun deleteWinInactivityWorkspace() {
        logger.info("deleteWinInactivityWorkspace")
        val now = LocalDateTime.now()
        workspaceDao.fetchNotUsageTimeWinWorkspace(dslContext, status = WorkspaceStatus.STOPPED)
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

    fun autoDeleteWhenNotAssign(
        onDelete: Boolean = false,
        readyDeleteWorkspace: MutableList<String> = mutableListOf()
    ) {
        val limitDay = holidayHelper.getLastWorkingDays(3).last()
        logger.info("autoDeleteWhenNotAssign|$limitDay")
        val notifyGroups = mutableMapOf<String, MutableList<Pair<String, String>>>()
        val whiteListProject = redisCache.getSetMembers(
            RedisKeys.REDIS_WORKSPACE_AUTO_DELETE_WHITE_LIST_PROJECT
        ) ?: emptySet()
        workspaceDao.fetchWorkspace(
            dslContext = dslContext,
            status = WorkspaceStatus.DISTRIBUTING,
            systemType = WorkspaceSystemType.WINDOWS_GPU
        )?.parallelStream()?.forEach { workspace ->
            if ((workspace.lastStatusUpdateTime ?: LocalDateTime.now()) < limitDay) {
                if (workspace.projectId in whiteListProject) {
                    readyDeleteWorkspace.add(
                        "project=${workspace.projectId}, ip=${workspace.hostName}," +
                                " 原因=超过3天未分配(创建时间: ${workspace.lastStatusUpdateTime?.format(formatter)}" +
                                " 早于检测时间 ${limitDay.format(formatter)}) 白名单已命中，只展示，将不会销毁。"
                    )
                    return@forEach
                }
                logger.info(
                    "ready to delete when not assign " +
                            "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|${workspace.hostName}"
                )

                readyDeleteWorkspace.add(
                    "project=${workspace.projectId}, ip=${workspace.hostName}," +
                            " 原因=超过3天未分配(创建时间: ${workspace.lastStatusUpdateTime?.format(formatter)}" +
                            " 早于检测时间 ${limitDay.format(formatter)})"
                )
                if (onDelete) {
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspace.workspaceName,
                        operator = ADMIN_NAME,
                        action = WorkspaceAction.DELETE,
                        actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
                    )
                    kotlin.runCatching { deleteWorkspace4System(ADMIN_NAME, workspace.workspaceName) }
                        .onFailure { i ->
                            logger.warn("auto delete fail|${i.message}", i)
                        }.onSuccess {
                            logger.info(
                                "delete $it when not assign " +
                                        "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|" +
                                        "${workspace.hostName}"
                            )
                            if (it) {
                                val value = Pair(workspace.hostName ?: "", workspace.createUserId)
                                notifyGroups.putIfAbsent(
                                    workspace.projectId,
                                    mutableListOf(value)
                                )?.add(value)
                            }
                        }
                }
            }
        }
        if (onDelete) {
            notifyGroups.forEach { (projectId, values) ->
                // 邮件通知
                notifyControl.notify4RemoteDevManager(
                    projectId = projectId,
                    cc = values.mapTo(mutableSetOf()) { it.second },
                    notifyTemplateCode = NOT_ASSIGN_AUTO_DELETE_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                    bodyParams = mutableMapOf(
                        "cgsIps" to values.joinToString("\n") { it.first },
                        "projectId" to projectId
                    )
                )
            }
        }
    }

    // 缓冲期先改成14天限制
    fun autoDeleteWhenSleep14Day(
        onDelete: Boolean = false,
        readyDeleteWorkspace: MutableList<String> = mutableListOf()
    ) {
        val limitDay = holidayHelper.getLastWorkingDays(14).last()
        logger.info("autoDeleteWhenSleep7Day|$limitDay")
        val whiteListProject = redisCache.getSetMembers(
            RedisKeys.REDIS_WORKSPACE_AUTO_DELETE_WHITE_LIST_PROJECT
        ) ?: emptySet()
        workspaceDao.fetchWorkspace(
            dslContext = dslContext,
            status = WorkspaceStatus.STOPPED,
            systemType = WorkspaceSystemType.WINDOWS_GPU
        )?.parallelStream()?.forEach { workspace ->
            if ((workspace.lastStatusUpdateTime ?: LocalDateTime.now()) < limitDay) {
                if (workspace.projectId in whiteListProject) {
                    readyDeleteWorkspace.add(
                        "project=${workspace.projectId}, ip=${workspace.hostName}" +
                                ", 原因=关机超过14天(关机时间: ${workspace.lastStatusUpdateTime?.format(formatter)}" +
                                " 早于检测时间 ${limitDay.format(formatter)}) 白名单已命中，只展示，将不会销毁。"
                    )
                    return@forEach
                }
                logger.info(
                    "ready to delete when sleep 14 day " +
                            "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|${workspace.hostName}"
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspace.workspaceName,
                    operator = ADMIN_NAME,
                    action = WorkspaceAction.DELETE,
                    actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
                )
                readyDeleteWorkspace.add(
                    "project=${workspace.projectId}, ip=${workspace.hostName}" +
                            ", 原因=关机超过14天(关机时间: ${workspace.lastStatusUpdateTime?.format(formatter)}" +
                            " 早于检测时间 ${limitDay.format(formatter)})"
                )
                if (onDelete) {
                    kotlin.runCatching { deleteWorkspace4System(ADMIN_NAME, workspace.workspaceName) }.onFailure { i ->
                        logger.warn("auto delete fail|${i.message}", i)
                    }.onSuccess {
                        logger.info(
                            "delete $it when sleep 14 day " +
                                    "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|" +
                                    "${workspace.hostName}"
                        )
                        if (it) {
                            val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
                            notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                                userIds = userIds.toMutableSet(),
                                workspaceName = workspace.workspaceName,
                                cc = mutableSetOf(workspace.createUserId),
                                projectId = workspace.projectId,
                                notifyTemplateCode = SLEEP_7_DAY_AUTO_DELETE_NOTIFY,
                                notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                                bodyParams = mutableMapOf(
                                    "cgsIp" to (workspace.hostName ?: ""),
                                    "projectId" to (workspace.projectId),
                                    "userId" to userIds.joinToString()
                                )
                            )
                        }
                    }
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

            notifyControl.dispatchWebsocketPushEvent(
                userId = workspace.createUserId,
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

        val projectId = remoteDevSettingDao.fetchOneSetting(dslContext, workspace.createUserId).projectId
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

            // 删除成功后发送kafka消息给安全侧消费
            workspaceCommon.sendCgsInfo2Kafka(
                workspaceKafkaInfo = WorkspaceKafkaInfo(
                    workspaceName = workspaceName,
                    projectId = workspace.projectId,
                    ip = detail?.environmentIP ?: "",
                    regionId = (detail?.regionId ?: "").toString()
                )
            )
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
        val ip = hostIdSub?.subList(1, hostIdSub.size)?.joinToString(separator = ".")
        if (!ip.isNullOrBlank() && workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            bkccService.updateHostMonitor(
                regionId = null,
                workspaceName = workspaceName,
                ips = setOf(ip),
                props = mapOf("devx_meta" to "")
            )

            // 删除 cmdb 的机器别名
            bkccService.updateHostName("VM-${hostIdSub.joinToString("-")}", workspaceName)

            // 删除cfs的权限组规则
            tCloudCfsService.addOrRemoveCfsPermissionRule(workspace.projectId, ip, true)

            // 关联tgit相关
            gitProxyTGitService.addOrRemoveAclIp(workspace.projectId, ip, true)
        }

        notifyControl.dispatchWebsocketPushEvent(
            userId = ADMIN_NAME,
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
