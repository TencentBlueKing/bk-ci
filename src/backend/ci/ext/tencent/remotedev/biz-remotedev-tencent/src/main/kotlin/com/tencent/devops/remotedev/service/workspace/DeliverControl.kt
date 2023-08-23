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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.software.SoftwareManageService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@Suppress("LongMethod")
class DeliverControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val sharedDao: WorkspaceSharedDao,
    private val workspaceCommon: WorkspaceCommon,
    private val softwareManageService: SoftwareManageService
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
            when (val status = WorkspaceStatus.values()[workspace.status]) {
                WorkspaceStatus.DELIVERING, WorkspaceStatus.PREPARING -> {
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = userId,
                        action = WorkspaceAction.START,
                        actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.SAFE_INITIALIZATION)
                    )
                    val bizId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz()
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
                    logger.info("${workspace.name} is $status, return error.")
                }
            }
        }
    }

    fun assignUser2Workspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ) {
        logger.info("assignUser2Workspace|$userId|$projectId|$workspaceName|$assigns")
        val assign2Owner = assigns.firstOrNull { it.type == WorkspaceShared.AssignType.OWNER }
        val alreadyExist = sharedDao.fetchWorkspaceSharedInfo(dslContext, workspaceName)
        val existOwner = alreadyExist.firstOrNull { it.type == WorkspaceShared.AssignType.OWNER }
        logger.info("assignUser2Workspace|assign2Owner|$assign2Owner|alreadyExist|$alreadyExist")
        if (assign2Owner != null && assign2Owner.userId != existOwner?.sharedUser) {
            if (existOwner != null) {
                logger.warn("PROJECT_WORKSPACE_ALREADY_ASSIGN_OWNER|$userId|$projectId|$workspaceName")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_WORKSPACE_ALREADY_ASSIGN_OWNER.errorCode,
                    params = arrayOf(workspaceName)
                )
            }
            val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
            val status = WorkspaceStatus.values()[workspace.status]
            if (!status.checkDistributing()) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    params = arrayOf(workspace.name, "status is $status, can't assign user now")
                )
            }
            val detail = workspaceCommon.getWorkspaceDetail(workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                    params = arrayOf(workspaceName)
                )
            logger.info("assignUser2Workspace|$userId|${assign2Owner.userId}|detail|$detail")
            sharedDao.batchCreate(dslContext, workspaceName, userId, listOf(assign2Owner))
            softwareManageService.installUserSoftwares(
                projectId = projectId,
                userId = assign2Owner.userId,
                ip = detail.environmentIP,
                workspaceName = workspaceName
            )
            // 异步发起用户软件安装，更新为运行中
            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspace.name,
                status = WorkspaceStatus.RUNNING
            )
        }
        val em = alreadyExist.map { m -> m.sharedUser }
        val add = assigns.filter { it.type == WorkspaceShared.AssignType.VIEWER && it.userId !in em }
        if (add.isNotEmpty()) {
            sharedDao.batchCreate(dslContext, workspaceName, userId, add)
        }

        val am = assigns.map { m -> m.userId }
        val reduce = alreadyExist.filter { it.type == WorkspaceShared.AssignType.VIEWER && it.sharedUser !in am }
        if (reduce.isNotEmpty()) {
            sharedDao.batchDelete(
                dslContext = dslContext,
                workspaceName = workspaceName,
                sharedUsers = reduce.map { it.sharedUser },
                assignType = WorkspaceShared.AssignType.VIEWER
            )
        }
    }

    fun jobCallback(workspaceName: String) {
        logger.info("jobCallBack $workspaceName")
        updateWorkspaceStatus(workspaceName) { workspace ->
            when (val status = WorkspaceStatus.values()[workspace.status]) {
                // 团队云桌面
                WorkspaceStatus.DELIVERING -> {
                    workspaceDao.updateWorkspaceStatus(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        status = WorkspaceStatus.DISTRIBUTING
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = workspace.creator,
                        action = WorkspaceAction.CREATE,
                        actionMessage = String.format(
                            workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            status.name,
                            WorkspaceStatus.DISTRIBUTING.name
                        )
                    )
                }
                // 个人云桌面
                WorkspaceStatus.PREPARING -> {
                    workspaceDao.updateWorkspaceStatus(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        status = WorkspaceStatus.RUNNING
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = workspace.creator,
                        action = WorkspaceAction.CREATE,
                        actionMessage = String.format(
                            workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            status.name,
                            WorkspaceStatus.RUNNING.name
                        )
                    )
                    workspaceCommon.dispatchWebsocketPushEvent(
                        userId = workspace.creator,
                        workspaceName = workspace.name,
                        workspaceHost = workspace.hostName,
                        type = WebSocketActionType.WORKSPACE_CREATE,
                        status = true,
                        action = WorkspaceAction.START,
                        systemType = WorkspaceSystemType.valueOf(workspace.systemType),
                        workspaceMountType = WorkspaceMountType.valueOf(workspace.workspaceMountType),
                        ownerType = WorkspaceOwnerType.valueOf(workspace.ownerType)
                    )
                }
                else -> {
                    logger.info("${workspace.name} is $status, return error.")
                }
            }
        }
    }

    fun updateStatusAndCreateHistory(
        type: String,
        workspace: TWorkspaceRecord,
        newStatus: WorkspaceStatus,
        softwareList: SoftwareCallbackRes,
        action: WorkspaceAction
    ) {
        val oldStatus = WorkspaceStatus.values()[workspace.status]
        workspaceDao.updateWorkspaceStatus(
            dslContext = dslContext,
            workspaceName = workspace.name,
            status = newStatus
        )
        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspace.name,
            operator = workspace.creator,
            action = action,
            actionMessage = String.format(
                workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                oldStatus.name,
                newStatus.name
            )
        )
        // 添加软件安装历史
        softwareManageService.updateSoftwareInstalledRecords(
            type = type,
            softwareList = softwareList
        )
    }

    fun softwareInstallationCompleteCallback(
        type: String,
        workspaceName: String,
        projectId: String,
        userId: String,
        autoAssign: Boolean?,
        softwareList: SoftwareCallbackRes
    ) {
        logger.info("softwareInstallationCompleteCallback|workspaceName|$workspaceName|softwareList|$softwareList")
        updateWorkspaceStatus(workspaceName) { workspace ->
            when (WorkspaceStatus.values()[workspace.status]) {
                WorkspaceStatus.DELIVERING -> {
                    if (type == "SYSTEM") {
                        updateStatusAndCreateHistory(
                            type = type,
                            workspace = workspace,
                            newStatus = WorkspaceStatus.DISTRIBUTING,
                            softwareList = softwareList,
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
                                        type = WorkspaceShared.AssignType.OWNER
                                    )
                                )

                            )
                        }
                    }
                }
                WorkspaceStatus.DISTRIBUTING -> {
                    if (type != "SYSTEM") {
                        updateStatusAndCreateHistory(
                            type = type,
                            workspace = workspace,
                            newStatus = WorkspaceStatus.RUNNING,
                            softwareList = softwareList,
                            action = WorkspaceAction.CREATE
                        )
                    }
                }
                else -> {
                    logger.info("${workspace.name} is ${WorkspaceStatus.values()[workspace.status]}, return error.")
                }
            }
        }
    }

    private fun updateWorkspaceStatus(workspaceName: String, update: (ws: TWorkspaceRecord) -> Unit) {
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
