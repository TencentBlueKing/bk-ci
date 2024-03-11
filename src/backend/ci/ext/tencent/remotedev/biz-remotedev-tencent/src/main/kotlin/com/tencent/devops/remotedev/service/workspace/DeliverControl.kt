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
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent.ASSIGNS_TEMPLATE
import com.tencent.devops.common.audit.ActionAuditContent.CGS_ASSIGN_USER_CONTENT
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_CODE_TEMPLATE
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.pojo.software.TaskStatusEnum
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.software.SoftwareManageService
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.WINDOWS_GPU_ASSIGN_NOTIFY
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class DeliverControl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val sharedDao: WorkspaceSharedDao,
    private val workspaceCommon: WorkspaceCommon,
    private val softwareManageService: SoftwareManageService,
    private val notifyControl: NotifyControl
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeliverControl::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    fun safeInitialization(
        projectId: String,
        userId: String,
        workspaceName: String,
        autoAssign: Boolean? = false
    ) {
        logger.info("$userId start workspace $workspaceName")
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
            when (workspace.status) {
                WorkspaceStatus.DELIVERING, WorkspaceStatus.PREPARING -> {
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = userId,
                        action = WorkspaceAction.START,
                        actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.SAFE_INITIALIZATION)
                    )
                    // todo job接口执行
                    val detail = workspaceCommon.getWorkspaceDetail(workspaceName)
                        ?: throw ErrorCodeException(
                            errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                            params = arrayOf(workspaceName)
                        )
                    logger.info("safeInitialization|$userId|$userId|detail|$detail")
                    softwareManageService.installSystemSoftwares(
                        projectId,
                        userId,
                        regionId = detail.regionId.toString(),
                        ip = detail.environmentIP,
                        workspaceName = workspaceName,
                        autoAssign = autoAssign
                    )
                }

                else -> {
                    logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                }
            }
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_ASSIGN,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        attributes = [AuditAttribute(name = PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = CGS_ASSIGN_USER_CONTENT
    )
    fun assignUser2Workspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ) {
        logger.info("assignUser2Workspace|$userId|$projectId|$workspaceName|$assigns")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        val assign2Owner = assigns.firstOrNull { it.type == WorkspaceShared.AssignType.OWNER }
        val alreadyExist = sharedDao.fetchWorkspaceSharedInfo(dslContext, workspaceName)
        val existOwner = alreadyExist.firstOrNull { it.type == WorkspaceShared.AssignType.OWNER }
        logger.info("assignUser2Workspace|assign2Owner|$assign2Owner|alreadyExist|$alreadyExist")
        ActionAuditContext.current()
            .addAttribute(ASSIGNS_TEMPLATE, assigns.joinToString(",") { it.userId })
        when {
            existOwner == null && assign2Owner != null -> {
                logger.info("assignUser2Workspace|$userId|${assign2Owner.userId}")
                workspaceCommon.shareWorkspace(
                    workspaceName = workspaceName,
                    projectId = workspace.projectId,
                    operator = userId,
                    assigns = listOf(assign2Owner),
                    mountType = WorkspaceMountType.START
                )
                if (workspace.status.checkDistributing()) {
                    workspaceDao.updateWorkspaceStatus(
                        dslContext = dslContext,
                        workspaceName = workspace.workspaceName,
                        status = WorkspaceStatus.RUNNING
                    )
                }
            }

            existOwner != null && assign2Owner?.userId != existOwner.sharedUser -> {
                workspaceCommon.unShareWorkspace(
                    workspaceName = workspaceName,
                    operator = userId,
                    sharedUsers = listOf(existOwner.sharedUser),
                    mountType = WorkspaceMountType.START,
                    assignType = WorkspaceShared.AssignType.OWNER
                )
                if (assign2Owner != null) {
                    workspaceCommon.shareWorkspace(
                        workspaceName = workspaceName,
                        projectId = workspace.projectId,
                        operator = userId,
                        assigns = listOf(
                            ProjectWorkspaceAssign(
                                userId = assign2Owner.userId,
                                type = WorkspaceShared.AssignType.OWNER,
                                expiration = null
                            )
                        ),
                        mountType = WorkspaceMountType.START
                    )
                }
            }
        }
        val em = alreadyExist.asSequence()
            .filter { it.type == WorkspaceShared.AssignType.VIEWER }.map { m -> m.sharedUser }
        val add = assigns.filter { it.type == WorkspaceShared.AssignType.VIEWER && it.userId !in em }
        if (add.isNotEmpty()) {
            workspaceCommon.shareWorkspace(
                workspaceName = workspaceName,
                projectId = workspace.projectId,
                operator = userId,
                assigns = add,
                mountType = WorkspaceMountType.START
            )
        }

        val am = assigns.filter { it.type == WorkspaceShared.AssignType.VIEWER }.map { m -> m.userId }
        val reduce = alreadyExist.filter { it.type == WorkspaceShared.AssignType.VIEWER && it.sharedUser !in am }
        if (reduce.isNotEmpty()) {
            workspaceCommon.unShareWorkspace(
                workspaceName = workspaceName,
                operator = userId,
                sharedUsers = reduce.map { it.sharedUser },
                mountType = WorkspaceMountType.START
            )
        }
    }

    fun softwareInstallationCompleteCallback(
        type: String,
        workspaceName: String,
        projectId: String,
        userId: String,
        autoAssign: Boolean?,
        softwareList: SoftwareCallbackRes
    ) {
        logger.info(
            "softwareInstallationCompleteCallback|type|$type|workspaceName|$workspaceName" +
                    "|projectId|$projectId|userId|$userId|softwareList|$softwareList"
        )
        // 添加软件安装历史
        softwareManageService.updateSoftwareInstalledRecords(
            type = type,
            softwareList = softwareList
        )
        updateWorkspaceStatus(workspaceName) { workspace ->
            when (workspace.status) {
                // 交付中安装IOA后
                WorkspaceStatus.DELIVERING, WorkspaceStatus.DELIVERING_FAILED -> {
                    if (type == "SYSTEM") {
                        checkSafeInitSuccess(softwareList, workspace)
                        workspaceCommon.updateStatusAndCreateHistory(
                            workspace = workspace,
                            newStatus = WorkspaceStatus.DISTRIBUTING,
                            action = WorkspaceAction.CREATE
                        )
                        if (autoAssign == true) {
                            assignUser2Workspace(
                                userId = userId,
                                projectId = projectId,
                                workspaceName = workspaceName,
                                assigns = listOf(
                                    ProjectWorkspaceAssign(
                                        userId = userId,
                                        type = WorkspaceShared.AssignType.OWNER,
                                        expiration = null
                                    )
                                )
                            )
                        }
                        notifyControl.notify4RemoteDevManager(
                            projectId = projectId,
                            cc = mutableSetOf(workspace.createUserId),
                            notifyTemplateCode = WINDOWS_GPU_ASSIGN_NOTIFY,
                            notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                            bodyParams = mutableMapOf(
                                "workspaceName" to workspace.workspaceName,
                                "cgsId" to (workspace.hostName ?: workspace.workspaceName),
                                "projectId" to projectId,
                                "creator" to workspace.createUserId
                            )
                        )
                    }
                }

                WorkspaceStatus.RUNNING -> {
                    if (type != "SYSTEM") {
                        workspaceCommon.updateStatusAndCreateHistory(
                            workspace = workspace,
                            newStatus = WorkspaceStatus.RUNNING,
                            action = WorkspaceAction.CREATE
                        )
                    }
                }
                // 个人云桌面
                WorkspaceStatus.PREPARING -> {
                    checkSafeInitSuccess(softwareList, workspace)
                    workspaceDao.updateWorkspaceStatus(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        status = WorkspaceStatus.RUNNING
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = workspace.createUserId,
                        action = WorkspaceAction.CREATE,
                        actionMessage = String.format(
                            workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            workspace.status.name,
                            WorkspaceStatus.RUNNING.name
                        )
                    )
                    notifyControl.dispatchWebsocketPushEvent(
                        userId = workspace.createUserId,
                        workspaceName = workspace.workspaceName,
                        workspaceHost = workspace.hostName,
                        type = WebSocketActionType.WORKSPACE_CREATE,
                        status = true,
                        action = WorkspaceAction.START,
                        systemType = workspace.workspaceSystemType,
                        workspaceMountType = workspace.workspaceMountType,
                        ownerType = workspace.ownerType,
                        projectId = workspace.projectId
                    )
                }

                else -> {
                    logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                }
            }
        }
    }

    private fun checkSafeInitSuccess(
        softwareList: SoftwareCallbackRes,
        ws: WorkspaceRecord
    ) {
        if (softwareList.taskStatus == TaskStatusEnum.FAILED) {
            workspaceCommon.updateStatus2DeliveringFailed(
                workspace = ws,
                action = WorkspaceAction.CREATE,
                notifyTemplateCode = "WINDOWS_GPU_SAFE_INIT_FAILED"
            )
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DELIVERING_FAILED.errorCode
            )
        }
    }

    private fun updateWorkspaceStatus(workspaceName: String, update: (ws: WorkspaceRecord) -> Unit) {
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
            // 更新状态
            update(workspace)
        }
    }
}
