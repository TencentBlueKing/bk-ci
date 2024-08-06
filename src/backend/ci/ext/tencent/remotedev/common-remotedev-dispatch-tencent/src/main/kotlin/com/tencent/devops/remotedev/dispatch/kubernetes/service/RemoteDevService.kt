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

package com.tencent.devops.remotedev.dispatch.kubernetes.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.DispatchBuildTaskStatusEnum
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.remotedev.WorkspaceResponse
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevService::class.java)
    }

    @Suppress("ComplexMethod")
    fun createWorkspace(userId: String, event: WorkspaceCreateEvent): WorkspaceResponse {
        // 查询是否已经存在记录
        val workspace = dispatchWorkspaceDao.getWorkspaceInfo(
            dslContext = dslContext,
            workspaceName = event.workspaceName
        )
        if (workspace != null) {
            logger.warn("Repeated creation, return failure")
            dispatchWorkspaceOpHisDao.updateStatusByWorkspaceName(
                dslContext = dslContext,
                status = EnvironmentActionStatus.FAILED,
                fStatus = EnvironmentActionStatus.PENDING,
                workspaceName = event.workspaceName
            )
            throw WorkspaceDispatchException(
                envId = workspace.environmentUid,
                errorMessage = "Repeated creation for ${workspace.workspaceName}"
            )
        }
        val mountType = event.mountType
        val result = remoteDevServiceFactory.loadRemoteDevService(mountType)
            .createWorkspace(userId, event)

        dispatchWorkspaceOpHisDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = event.workspaceName,
            environmentUid = result.enviromentUid,
            operator = userId,
            uid = result.taskId,
            action = EnvironmentAction.CREATE
        )

        // 记录创建历史
        dispatchWorkspaceDao.createWorkspace(
            userId = userId,
            event = event,
            environmentUid = result.enviromentUid,
            regionId = result.regionId,
            taskId = result.taskId,
            status = EnvStatusEnum.running,
            dslContext = dslContext
        )

        val (taskStatus, taskMessage) = remoteDevServiceFactory.loadRemoteDevService(mountType)
            .waitTaskFinish(userId, result.taskId, UpdateEventType.CREATE)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            logger.info("$userId create workspace success. ${result.enviromentUid}")
            if (event.devFile.checkWorkspaceAutomaticCorrection()) {
                dispatchWorkspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = result.taskId,
                    status = EnvironmentActionStatus.AUTOMATIC_CORRECTION,
                    workspaceName = event.workspaceName
                )
            } else {
                dispatchWorkspaceOpHisDao.update(
                    dslContext, result.taskId, EnvironmentActionStatus.SUCCEEDED
                )
            }
            if (mountType == WorkspaceMountType.START) {
                val vmCreateResp = JsonUtil.to(taskMessage ?: "", TaskStatus::class.java).vmCreateResp
                dslContext.transaction { t ->
                    val context = DSL.using(t)
                    dispatchWorkspaceDao.updateWorkspace(
                        workspaceName = event.workspaceName,
                        status = EnvStatusEnum.running,
                        envId = vmCreateResp?.envId ?: "",
                        regionId = vmCreateResp?.cloudZoneId?.toInt() ?: 0,
                        dslContext = context
                    )
                }

                return WorkspaceResponse(
                    environmentUid = vmCreateResp?.envId ?: "",
                    environmentHost = vmCreateResp?.cgsIp ?: "",
                    environmentIp = vmCreateResp?.cgsIp ?: "",
                    resourceId = vmCreateResp?.resourceId,
                    macAddress = vmCreateResp?.macAddress ?: ""
                )
            } else {
                dslContext.transaction { t ->
                    val context = DSL.using(t)
                    dispatchWorkspaceDao.updateWorkspaceStatus(
                        workspaceName = event.workspaceName,
                        status = EnvStatusEnum.running,
                        dslContext = context
                    )
                }

                // 检验workspace状态
                val workspaceInfo = remoteDevServiceFactory.loadRemoteDevService(mountType)
                    .getWorkspaceInfo(userId, event.workspaceName)

                if (workspaceInfo.status != EnvStatusEnum.running) {
                    throw WorkspaceDispatchException(
                        envId = result.enviromentUid,
                        errorMessage = "workspace not running"
                    )
                }

                return WorkspaceResponse(
                    environmentUid = result.enviromentUid,
                    environmentHost = workspaceInfo.environmentHost,
                    environmentIp = workspaceInfo.environmentIP,
                    resourceId = result.resourceId
                )
            }
        } else {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                dispatchWorkspaceDao.updateWorkspaceStatus(
                    workspaceName = event.workspaceName,
                    status = EnvStatusEnum.failed,
                    dslContext = context
                )

                dispatchWorkspaceOpHisDao.update(
                    dslContext = context,
                    uid = result.taskId,
                    status = EnvironmentActionStatus.FAILED,
                    fStatus = EnvironmentActionStatus.PENDING,
                    actionMsg = taskMessage ?: ""
                )
            }

            throw WorkspaceDispatchException(
                envId = result.enviromentUid,
                errorMessage = "errorMessage:$taskMessage"
            )
        }
    }

    fun getWorkspaceUrl(
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType = WorkspaceMountType.DEVCLOUD
    ): String? {
        return remoteDevServiceFactory.loadRemoteDevService(mountType).getWorkspaceUrl(userId, workspaceName)
    }

    fun getWorkspaceInfo(
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType = WorkspaceMountType.DEVCLOUD
    ): WorkspaceInfo {
        return remoteDevServiceFactory.loadRemoteDevService(mountType).getWorkspaceInfo(userId, workspaceName)
    }

    fun deleteWorkspace(
        workspaceName: String,
        bakWorkspaceName: String?
    ) {
        dispatchWorkspaceDao.deleteWorkspace(dslContext, workspaceName, bakWorkspaceName)
    }

    fun workspaceTaskCallback(
        taskStatus: TaskStatus,
        mountType: WorkspaceMountType = WorkspaceMountType.DEVCLOUD
    ): Boolean {
        logger.info("workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        return remoteDevServiceFactory.loadRemoteDevService(mountType).workspaceTaskCallback(taskStatus)
    }

    fun startWorkspace(event: WorkspaceOperateEvent): WorkspaceResponse {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .startWorkspace(event.userId, event.workspaceName)
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .waitTaskFinish(event.userId, taskId, event.type)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.SUCCEEDED
            )
            val workspaceInfo = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
                .getWorkspaceInfo(event.userId, event.workspaceName)

            if (workspaceInfo.status != EnvStatusEnum.running) {
                throw WorkspaceDispatchException(
                    "The workspace state is not RUNNING"
                )
            }

            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.running,
                dslContext = dslContext
            )

            return WorkspaceResponse(
                environmentHost = workspaceInfo.environmentHost,
                environmentUid = "",
                environmentIp = workspaceInfo.environmentIP
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.FAILED
            )
            throw WorkspaceDispatchException(
                "errorMessage:$failedMsg"
            )
        }
    }

    fun stopWorkspace(event: WorkspaceOperateEvent): Boolean {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .stopWorkspace(event.userId, event.workspaceName)
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .waitTaskFinish(event.userId, taskId, event.type)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.SUCCEEDED
            )
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.stopped,
                dslContext = dslContext
            )

            return true
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.FAILED
            )
            throw WorkspaceDispatchException(
                "errorMessage:$failedMsg"
            )
        }
    }

    fun restartWorkspace(event: WorkspaceOperateEvent): Boolean {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .restartWorkspace(event.userId, event.workspaceName)
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .waitTaskFinish(event.userId, taskId, event.type)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.SUCCEEDED
            )
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.running,
                dslContext = dslContext
            )

            return true
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.FAILED
            )
            throw WorkspaceDispatchException(
                "errorMessage:$failedMsg"
            )
        }
    }

    fun rebuildWorkspace(event: WorkspaceOperateEvent): Boolean {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .rebuildWorkspace(event.userId, event.workspaceName, event.imageCosFile ?: "")
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .waitTaskFinish(event.userId, taskId, event.type)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.SUCCEEDED
            )
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.running,
                dslContext = dslContext
            )

            return true
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.FAILED
            )
            throw WorkspaceDispatchException(
                "errorMessage:$failedMsg"
            )
        }
    }

    fun deleteWorkspace(event: WorkspaceOperateEvent): Boolean {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .deleteWorkspace(event.userId, event)
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .waitTaskFinish(event.userId, taskId, event.type)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.SUCCEEDED
            )
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.deleted,
                dslContext = dslContext
            )

            return true
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.FAILED
            )
            throw WorkspaceDispatchException(
                "errorMessage:$failedMsg"
            )
        }
    }

    fun expandDisk(
        workspaceName: String,
        userId: String,
        size: String,
        mountType: WorkspaceMountType
    ): ExpandDiskValidateResp {
        return remoteDevServiceFactory.loadRemoteDevService(mountType).expandDisk(workspaceName, userId, size)
    }

    fun upgradeWorkspace(event: WorkspaceOperateEvent) {
        // 需要生成一个新的 pipelineId 进行操作
        val orderId = "${event.projectId}_${event.projectId}_${UUIDUtil.generate().takeLast(16)}"
        remoteDevServiceFactory.loadRemoteDevService(event.mountType).upgradeWorkspaceVm(
            userId = event.userId,
            workspaceName = event.workspaceName,
            machineType = event.machineType!!,
            pipelineId = orderId
        )
    }
}
