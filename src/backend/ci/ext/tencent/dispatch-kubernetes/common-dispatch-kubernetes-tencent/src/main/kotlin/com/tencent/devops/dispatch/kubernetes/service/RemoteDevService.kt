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
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
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
    }

    fun createWorkspace(userId: String, event: WorkspaceCreateEvent): WorkspaceResponse {
        val (enviromentUid, taskId) = remoteDevServiceFactory.load("test-sawyer2").createWorkspace(userId, event)

        // 记录创建历史
        dispatchWorkspaceDao.createWorkspace(
            userId = userId,
            event = event,
            environmentUid = enviromentUid,
            status = EnvStatusEnum.running,
            dslContext = dslContext
        )

        val (taskStatus, failedMsg) = containerServiceFactory.load("test-sawyer2")
            .waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            logger.info("$userId create workspace success. $enviromentUid")

            val workspaceInfo = remoteDevServiceFactory.load("test-sawyer2")
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
                "工作空间启动失败，错误信息:$failedMsg"
            )
        }
    }

    fun startWorkspace(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("test-sawyer2").startWorkspace(userId, workspaceName)
    }

    fun stopWorkspace(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("test-sawyer2").stopWorkspace(userId, workspaceName)
    }

    fun deleteWorkspace(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("test-sawyer2").deleteWorkspace(userId, workspaceName)
    }

    fun getWorkspaceUrl(userId: String, workspaceName: String): String? {
        return remoteDevServiceFactory.load("test-sawyer2").getWorkspaceUrl(userId, workspaceName)
    }

    fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        return remoteDevServiceFactory.load("test-sawyer2").getWorkspaceInfo(userId, workspaceName)
    }

    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        return remoteDevServiceFactory.load("test-sawyer2").workspaceTaskCallback(taskStatus)
    }
}
