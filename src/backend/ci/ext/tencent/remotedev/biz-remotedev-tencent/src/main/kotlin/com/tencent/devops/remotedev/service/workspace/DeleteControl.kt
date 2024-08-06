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
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.cron.HolidayHelper
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceWorkspaceDispatchInterface
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceKafkaInfo
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.service.BKBaseService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
    private val redisCache: RedisCacheService,
    private val workspaceCommon: WorkspaceCommon,
    private val notifyControl: NotifyControl,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService,
    private val bkBaseService: BKBaseService,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val holidayHelper: HolidayHelper,
    private val workspaceWindowsDao: WorkspaceWindowsDao
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
            permissionService.checkOwnerPermission(userId, workspaceName, workspace.projectId, workspace.ownerType)
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

            val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)

            // 发送处理事件
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = workspace.createUserId,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType,
                    gameId = gameId.first
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
        if (res && workspace.status != WorkspaceStatus.DELIVERING_FAILED) {

            // 修复待分配的机器销毁时，拥有者为空发送通知没有相关人
            val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName).ifEmpty {
                listOf(workspace.createUserId)
            }
            val windowsInfo = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)
            notifyControl.notify4UserAndCCRemoteDevManagerAndCCShareUser(
                userIds = userIds.toMutableSet(),
                workspaceName = workspace.workspaceName,
                cc = mutableSetOf(workspace.createUserId),
                projectId = workspace.projectId,
                notifyTemplateCode = NotifyControl.WORKSPACE_FORCE_DELETE,
                notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                bodyParams = mutableMapOf(
                    "cgsIp" to (windowsInfo?.hostIp ?: ""),
                    "userId" to userIds.joinToString(),
                    "projectId" to (workspace.projectId)
                )
            )
        }
        return res
    }

    fun batchDeleteWindowsWorkspace4OP(
        userId: String,
        workspaceNames: Set<String>
    ): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        workspaceNames.forEach {
            result[it] = deleteWorkspace4System(userId, it)
        }
        // 只发送删除成功的机器
        val deletedWorkspaces = result.filter { it.value }.keys

        val workspaces =
            workspaceJoinDao.fetchWindowsWorkspaces(dslContext, workspaceNames = deletedWorkspaces, size = null)
                .associateBy {
                    it.workspaceName
                }

        // 将工作空间按工作空间拥有者划分
        val data = mutableMapOf<String, MutableSet<WorkspaceRecordWithWindows>>()
        deletedWorkspaces.forEach { workspaceName ->
            val workspace = workspaces[workspaceName]
            if (workspace == null || workspace.status == WorkspaceStatus.DELIVERING_FAILED) {
                logger.error("batchDeleteWorkspace4OP $workspaceName not find in records $workspaces")
                return@forEach
            }
            // 待分配实例没有拥有者，通知给创建人
            val userIds = permissionService.getWorkspaceOwner(workspaceName).ifEmpty {
                listOf(workspace.createUserId)
            }
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
                (it.hostIp?.substringAfter(".") ?: "") to Pair((lastTimeMap[it.hostIp] ?: ""), it.projectId)
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
    fun deleteWorkspace4System(
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
            val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)

            // 发送处理事件
            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = workspace.createUserId,
                    traceId = bizId,
                    type = UpdateEventType.DELETE,
                    workspaceName = workspace.workspaceName,
                    mountType = workspace.workspaceMountType,
                    gameId = gameId.first
                )
            )
            return true
        }
    }

    fun afterDeleteWorkspace(event: RemoteDevUpdateEvent) {
        logger.debug("afterDeleteWorkspace|RemoteDevUpdateEvent{}|", event)
        if (!event.status) {
            // 调devcloud接口查询是否已经成功，如果成功还是走成功的逻辑.
            val workspaceInfo = SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java)
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

        if (status) {
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

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceCommon.updateLastHistory(transactionContext, workspaceName, operator)
        }

        // 删除时给 cmdb 去掉字段方便监控检索
        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            windowsAfterDelete(workspaceName, workspace)
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

    private fun windowsAfterDelete(workspaceName: String, workspace: WorkspaceRecord) {
        val windowsInfo = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)
        // 删除成功后发送kafka消息给安全侧消费
        workspaceCommon.sendCgsInfo2Kafka(
            workspaceKafkaInfo = WorkspaceKafkaInfo(
                workspaceName = workspaceName,
                projectId = workspace.projectId,
                ip = windowsInfo?.hostIp ?: "",
                regionId = (windowsInfo?.regionId ?: "").toString()
            )
        )
        val hostIdSub = windowsInfo?.hostIp?.split(".") ?: return
        workspaceCommon.updateHostMonitor(
            workspaceName = workspaceName,
            props = mapOf("devx_meta" to ""),
            type = workspace.workspaceSystemType
        )

        val ip = windowsInfo.hostIp?.substringAfter(".") ?: return
        // 删除 cmdb 的机器别名
        workspaceCommon.updateHostMonitor(
            workspaceName = workspaceName,
            props = mapOf("bk_host_name" to "VM-${hostIdSub.joinToString("-")}"),
            type = workspace.workspaceSystemType
        )

        // 删除cfs的权限组规则
        tCloudCfsService.addOrRemoveCfsPermissionRule(workspace.projectId, ip, true)

            // 关联tgit相关
            gitProxyTGitService.addOrRemoveAclIp(workspace.projectId, setOf(ip), true, null)
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
            } else {
                throw it
            }
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
