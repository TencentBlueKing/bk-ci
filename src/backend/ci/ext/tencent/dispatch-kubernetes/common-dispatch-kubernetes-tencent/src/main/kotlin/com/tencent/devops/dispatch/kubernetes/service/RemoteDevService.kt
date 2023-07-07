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

import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.BK_WORKSPACE_STATE_NOT_RUNNING
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceResponse
import com.tencent.devops.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
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

    fun createWorkspace(userId: String, event: WorkspaceCreateEvent): WorkspaceResponse {
        val mountType = event.devFile.checkWorkspaceMountType()
        val result = remoteDevServiceFactory.loadRemoteDevService(mountType)
            .createWorkspace(userId, event)

        // 记录创建历史
        dispatchWorkspaceDao.createWorkspace(
            userId = userId,
            event = event,
            environmentUid = result.enviromentUid,
            regionId = result.regionId,
            status = EnvStatusEnum.running,
            dslContext = dslContext
        )

        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadContainerService(mountType)
            .waitTaskFinish(userId, result.taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            logger.info("$userId create workspace success. ${result.enviromentUid}")

            val workspaceInfo = remoteDevServiceFactory.loadRemoteDevService(mountType)
                .getWorkspaceInfo(userId, event.workspaceName)

            if (workspaceInfo.status != EnvStatusEnum.running) {
                throw BuildFailureException(
                    ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorType,
                    ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorCode,
                    ErrorCodeEnum.BASE_CREATE_VM_ERROR.getErrorMessage(),
                    I18nUtil.getCodeLanMessage(BK_WORKSPACE_STATE_NOT_RUNNING)
                )
            }

            dslContext.transaction { t ->
                val context = DSL.using(t)
                dispatchWorkspaceDao.updateWorkspaceStatus(
                    workspaceName = event.workspaceName,
                    status = EnvStatusEnum.running,
                    dslContext = context
                )

                dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                    dslContext = context,
                    workspaceName = event.workspaceName,
                    environmentUid = result.enviromentUid,
                    operator = "admin",
                    action = EnvironmentAction.CREATE
                )
            }

            return WorkspaceResponse(
                environmentUid = result.enviromentUid,
                environmentHost = workspaceInfo.environmentHost,
                environmentIp = workspaceInfo.environmentIP
            )
        } else {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                dispatchWorkspaceDao.updateWorkspaceStatus(
                    workspaceName = event.workspaceName,
                    status = EnvStatusEnum.failed,
                    dslContext = context
                )

                dispatchWorkspaceOpHisDao.createWorkspaceHistory(
                    dslContext = context,
                    workspaceName = event.workspaceName,
                    environmentUid = result.enviromentUid,
                    operator = "admin",
                    action = EnvironmentAction.CREATE,
                    actionMsg = failedMsg ?: ""
                )
            }

            throw BuildFailureException(
                ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_CREATE_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_CREATE_VM_ERROR.getErrorMessage(),
                "errorMessage:$failedMsg"
            )
        }
    }

    fun startWorkspace(event: WorkspaceOperateEvent): WorkspaceResponse {
        val taskId = remoteDevServiceFactory.loadRemoteDevService(event.mountType)
            .startWorkspace(event.userId, event.workspaceName)
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadContainerService(event.mountType)
            .waitTaskFinish(event.userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
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
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadContainerService(event.mountType)
            .waitTaskFinish(event.userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.stopped,
                dslContext = dslContext
            )

            return true
        } else {
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
            .deleteWorkspace(event.userId, event)
        val (taskStatus, failedMsg) = remoteDevServiceFactory.loadContainerService(event.mountType)
            .waitTaskFinish(event.userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = EnvStatusEnum.deleted,
                dslContext = dslContext
            )

            return true
        } else {
            throw BuildFailureException(
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_DELETE_VM_ERROR.getErrorMessage(),
                "errorMessage:$failedMsg"
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
}
