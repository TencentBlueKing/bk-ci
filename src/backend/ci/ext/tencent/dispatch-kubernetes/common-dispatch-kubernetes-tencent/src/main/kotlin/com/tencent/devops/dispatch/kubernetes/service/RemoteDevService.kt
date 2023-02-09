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
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceResponse
import com.tencent.devops.dispatch.kubernetes.service.factory.ContainerServiceFactory
import com.tencent.devops.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
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
    private val containerServiceFactory: ContainerServiceFactory,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevService::class.java)

        private const val WORKSPACE_PROJECT = ""
    }

    fun createWorkspace(userId: String, event: WorkspaceCreateEvent): WorkspaceResponse {
        val (enviromentUid, taskId) = remoteDevServiceFactory.load(WORKSPACE_PROJECT).createWorkspace(userId, event)

        // 记录创建历史
        dispatchWorkspaceDao.createWorkspace(
            userId = userId,
            event = event,
            environmentUid = enviromentUid,
            status = EnvStatusEnum.running,
            dslContext = dslContext
        )

        val (taskStatus, failedMsg) = containerServiceFactory.load(WORKSPACE_PROJECT)
            .waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            logger.info("$userId create workspace success. $enviromentUid")

            val workspaceInfo = remoteDevServiceFactory.load(WORKSPACE_PROJECT)
                .getWorkspaceInfo(userId, event.workspaceName)

            if (workspaceInfo.status != EnvStatusEnum.running) {
                throw BuildFailureException(
                    ErrorCodeEnum.START_VM_ERROR.errorType,
                    ErrorCodeEnum.START_VM_ERROR.errorCode,
                    ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                    "工作空间状态非RUNNING"
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
                    environmentUid = enviromentUid,
                    operator = "admin",
                    action = EnvironmentAction.CREATE
                )
            }

            return WorkspaceResponse(
                enviromentUid = enviromentUid,
                environmentHost = workspaceInfo.environmentHost
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
                    environmentUid = enviromentUid,
                    operator = "admin",
                    action = EnvironmentAction.CREATE,
                    actionMsg = failedMsg ?: ""
                )
            }

            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "工作空间创建失败，错误信息:$failedMsg"
            )
        }
    }

    fun startWorkspace(userId: String, workspaceName: String): WorkspaceResponse {
        val taskId = remoteDevServiceFactory.load(WORKSPACE_PROJECT).startWorkspace(userId, workspaceName)
        val (taskStatus, failedMsg) = containerServiceFactory.load(WORKSPACE_PROJECT)
            .waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            val workspaceInfo = remoteDevServiceFactory.load(WORKSPACE_PROJECT)
                .getWorkspaceInfo(userId, workspaceName)

            if (workspaceInfo.status != EnvStatusEnum.running) {
                throw BuildFailureException(
                    ErrorCodeEnum.START_VM_ERROR.errorType,
                    ErrorCodeEnum.START_VM_ERROR.errorCode,
                    ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                    "工作空间状态非RUNNING"
                )
            }

            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = workspaceName,
                status = EnvStatusEnum.running,
                dslContext = dslContext
            )

            return WorkspaceResponse(
                environmentHost = workspaceInfo.environmentHost,
                enviromentUid = ""
            )
        } else {
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "工作空间启动失败，错误信息:$failedMsg"
            )
        }
    }

    fun stopWorkspace(userId: String, workspaceName: String): Boolean {
        val taskId = remoteDevServiceFactory.load(WORKSPACE_PROJECT).stopWorkspace(userId, workspaceName)
        val (taskStatus, failedMsg) = containerServiceFactory.load(WORKSPACE_PROJECT)
            .waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = workspaceName,
                status = EnvStatusEnum.stopped,
                dslContext = dslContext
            )

            return true
        } else {
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "工作空间休眠失败，错误信息:$failedMsg"
            )
        }
    }

    fun deleteWorkspace(userId: String, workspaceName: String): Boolean {
        val taskId = remoteDevServiceFactory.load(WORKSPACE_PROJECT).deleteWorkspace(userId, workspaceName)
        val (taskStatus, failedMsg) = containerServiceFactory.load(WORKSPACE_PROJECT)
            .waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 更新db状态
            dispatchWorkspaceDao.updateWorkspaceStatus(
                workspaceName = workspaceName,
                status = EnvStatusEnum.deleted,
                dslContext = dslContext
            )

            return true
        } else {
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "工作空间删除失败，错误信息:$failedMsg"
            )
        }
    }

    fun getWorkspaceUrl(userId: String, workspaceName: String): String? {
        return remoteDevServiceFactory.load(WORKSPACE_PROJECT).getWorkspaceUrl(userId, workspaceName)
    }

    fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        return remoteDevServiceFactory.load(WORKSPACE_PROJECT).getWorkspaceInfo(userId, workspaceName)
    }

    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        return remoteDevServiceFactory.load(WORKSPACE_PROJECT).workspaceTaskCallback(taskStatus)
    }
}
