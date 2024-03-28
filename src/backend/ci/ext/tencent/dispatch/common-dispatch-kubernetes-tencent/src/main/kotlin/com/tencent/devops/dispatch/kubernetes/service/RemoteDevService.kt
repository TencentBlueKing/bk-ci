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

package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.BK_CREATE_WORKSPACE_ERROR
import com.tencent.devops.dispatch.kubernetes.pojo.BK_WORKSPACE_STATE_NOT_RUNNING
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceResponse
import com.tencent.devops.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.dispatch.kubernetes.utils.WorkspaceCreateFailureException
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.image.WorkspaceImageInfo
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
            throw WorkspaceCreateFailureException(
                errorType = ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.BASE_CREATE_VM_ERROR.getErrorMessage(),
                envId = workspace.environmentUid,
                errorMessage = I18nUtil.getCodeLanMessage(BK_CREATE_WORKSPACE_ERROR)
            )
        }
        val mountType = event.mountType ?: event.devFile.checkWorkspaceMountType()
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
                    throw WorkspaceCreateFailureException(
                        errorType = ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorType,
                        errorCode = ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorCode,
                        formatErrorMessage = ErrorCodeEnum.BASE_CREATE_VM_ERROR.getErrorMessage(),
                        envId = result.enviromentUid,
                        errorMessage = I18nUtil.getCodeLanMessage(BK_WORKSPACE_STATE_NOT_RUNNING)
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

            throw WorkspaceCreateFailureException(
                errorType = ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorCode,
                formatErrorMessage = ErrorCodeEnum.BASE_CREATE_VM_ERROR.getErrorMessage(),
                envId = result.enviromentUid,
                errorMessage = "errorMessage:$taskMessage"
            )
        }
    }

    fun makeWorkspaceImageWithBackEvent(event: WorkspaceOperateEvent, backEvent: RemoteDevUpdateEvent) {
        // 先检测工作空间状态
        val environmentInfoRspData = remoteDevServiceFactory.loadRemoteDevService(event.mountType).getWorkspaceInfo(
            userId = event.userId,
            workspaceName = event.workspaceName
        )

        var workspaceRunning = false
        if (environmentInfoRspData.status == EnvStatusEnum.running ||
            environmentInfoRspData.status == EnvStatusEnum.startFailed ||
            environmentInfoRspData.status == EnvStatusEnum.stopFailed ||
            environmentInfoRspData.status == EnvStatusEnum.abnormalAfterRunning
        ) {
            // 制作镜像前先关机
            stopWorkspace(event)

            // 标识这是一次开机制作镜像
            workspaceRunning = true
        }

        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .makeWorkspaceImage(event.userId, event.workspaceName, event.cgsId)
        val (taskStatus, taskMessage) = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .waitTaskFinish(event.userId, taskId, event.type)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.SUCCEEDED
            )
            logger.info("${event.userId} make workspaceImage success. ${event.workspaceName}")
            val image = JsonUtil.to(taskMessage ?: "", TaskStatus::class.java).image
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.stopped,
                dslContext = dslContext
            )

            // 如果是开机制作镜像，镜像制作完成后要开机
            if (workspaceRunning) {
                startWorkspace(event)
            }

            backEvent.status = true
            backEvent.workspaceImageInfo = WorkspaceImageInfo(
                imageId = event.imageId ?: "",
                imageCosFile = image?.cosFile ?: "",
                size = image?.size ?: "",
                sourceCgsId = image?.sourceCgsId ?: "",
                sourceCgsType = image?.sourceType ?: "",
                sourceCgsZone = image?.zoneId ?: ""
            )
        } else {
            dispatchWorkspaceOpHisDao.update(
                dslContext, taskId, EnvironmentActionStatus.FAILED
            )
            throw BuildFailureException(
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.getErrorMessage(),
                "errorMessage:$taskMessage"
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

    fun workspaceTaskCallback(
        taskStatus: TaskStatus,
        mountType: WorkspaceMountType = WorkspaceMountType.DEVCLOUD
    ): Boolean {
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
                throw BuildFailureException(
                    ErrorCodeEnum.BASE_START_VM_ERROR.errorType,
                    ErrorCodeEnum.BASE_START_VM_ERROR.errorCode,
                    ErrorCodeEnum.BASE_START_VM_ERROR.getErrorMessage(),
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
            throw BuildFailureException(
                ErrorCodeEnum.BASE_START_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_START_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_START_VM_ERROR.getErrorMessage(),
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
            throw BuildFailureException(
                ErrorCodeEnum.BASE_STOP_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_STOP_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_STOP_VM_ERROR.getErrorMessage(),
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
            throw BuildFailureException(
                ErrorCodeEnum.BASE_STOP_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_STOP_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_STOP_VM_ERROR.getErrorMessage(),
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
            throw BuildFailureException(
                ErrorCodeEnum.BASE_STOP_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_STOP_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_STOP_VM_ERROR.getErrorMessage(),
                "errorMessage:$failedMsg"
            )
        }
    }

    fun deleteWorkspace(event: WorkspaceOperateEvent): Boolean {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .deleteWorkspace(event.userId, event.workspaceName)
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
            throw BuildFailureException(
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.getErrorMessage(),
                "errorMessage:$failedMsg"
            )
        }
    }
}
