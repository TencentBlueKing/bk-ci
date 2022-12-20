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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceReq
import com.tencent.devops.dispatch.kubernetes.service.factory.ContainerServiceFactory
import com.tencent.devops.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevService @Autowired constructor(
    private val containerServiceFactory: ContainerServiceFactory,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevService::class.java)
    }

    fun createWorkspace(userId: String, workspaceReq: WorkspaceReq): String {
        val (workspaceId, taskId) = remoteDevServiceFactory.load("test-sawyer2").createWorkspace(userId, workspaceReq)

        val (taskStatus, failedMsg) = containerServiceFactory.load("test-sawyer2")
            .waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info("$userId create workspace success.")
            return workspaceId
        } else {
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "构建机启动失败，错误信息:$failedMsg"
            )
        }
    }

    fun startWorkspace(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("").startWorkspace(userId, workspaceName)
    }

    fun stopWorkspace(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("").stopWorkspace(userId, workspaceName)
    }

    fun deleteWorkspace(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("").deleteWorkspace(userId, workspaceName)
    }

    fun getWorkspaceUrl(userId: String, workspaceName: String): String? {
        return remoteDevServiceFactory.load("").getWorkspaceUrl(userId, workspaceName)
    }

    fun workspaceHeartbeat(userId: String, workspaceName: String): Boolean {
        return remoteDevServiceFactory.load("").workspaceHeartbeat(userId, workspaceName)
    }

    fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        return remoteDevServiceFactory.load("").workspaceTaskCallback(taskStatus)
    }
}
