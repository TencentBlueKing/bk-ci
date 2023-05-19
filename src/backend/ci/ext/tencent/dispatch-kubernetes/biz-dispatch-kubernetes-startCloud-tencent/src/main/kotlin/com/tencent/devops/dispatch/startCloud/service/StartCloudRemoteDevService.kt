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

package com.tencent.devops.dispatch.startCloud.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.dispatch.startCloud.client.WorkspaceStartCloudClient
import com.tencent.devops.dispatch.startCloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentCreate
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentDelete
import com.tencent.devops.dispatch.startCloud.pojo.EnvironmentUserCreate
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("startcloudRemoteDevService")
class StartCloudRemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceClient: WorkspaceStartCloudClient
) : RemoteDevInterface {

    @Value("\${startCloud.appName:IEG_BKCI}")
    val appName: String = "IEG_BKCI"

    override fun createWorkspace(userId: String, event: WorkspaceCreateEvent): Pair<String, String> {
        logger.info("User $userId create workspace: ${JsonUtil.toJson(event)}")

        workspaceClient.createUser(userId, EnvironmentUserCreate(userId, appName))

        val ip = workspaceClient.createWorkspace(
            userId,
            EnvironmentCreate(
                userId = userId,
                ticket = event.bkTicket,
                appName = appName,
                pipeLineId = null
            )
        )

        return Pair(ip, EMPTY)
    }

    override fun startWorkspace(userId: String, workspaceName: String): String {
        throw BuildFailureException(
            ErrorCodeEnum.CREATE_VM_USER_ERROR.errorType,
            ErrorCodeEnum.CREATE_VM_USER_ERROR.errorCode,
            ErrorCodeEnum.CREATE_VM_USER_ERROR.formatErrorMessage,
            "第三方服务-START-CLOUD 异常，异常信息 - 用户操作异常 - 不支持START操作"
        )
    }

    override fun stopWorkspace(userId: String, workspaceName: String): String {
        throw BuildFailureException(
            ErrorCodeEnum.CREATE_VM_USER_ERROR.errorType,
            ErrorCodeEnum.CREATE_VM_USER_ERROR.errorCode,
            ErrorCodeEnum.CREATE_VM_USER_ERROR.formatErrorMessage,
            "第三方服务-START-CLOUD 异常，异常信息 - 用户操作异常 - 不支持STOP操作"
        )
    }

    override fun deleteWorkspace(userId: String, event: WorkspaceOperateEvent): String {
        workspaceClient.deleteWorkspace(
            userId = userId,
            workspaceName = event.workspaceName,
            EnvironmentDelete(
                userId = event.userId,
                ticket = event.bkTicket,
                pipeLineId = null
            )
        )

        // 更新db状态
        dispatchWorkspaceDao.updateWorkspaceStatus(
            workspaceName = event.workspaceName,
            status = EnvStatusEnum.deleted,
            dslContext = dslContext
        )

        return EMPTY
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        TODO("Not yet implemented")
    }

    override fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        val ip = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)?.environmentUid
            ?: throw BuildFailureException(
                ErrorCodeEnum.CREATE_VM_USER_ERROR.errorType,
                ErrorCodeEnum.CREATE_VM_USER_ERROR.errorCode,
                ErrorCodeEnum.CREATE_VM_USER_ERROR.formatErrorMessage,
                "第三方服务-START-CLOUD 异常，异常信息 - ip 为空"
            )
        return WorkspaceInfo(
            status = EnvStatusEnum.running,
            hostIP = ip,
            environmentIP = ip,
            clusterId = "",
            namespace = "",
            environmentHost = "",
            ready = true,
            started = true
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudRemoteDevService::class.java)
        private const val EMPTY = ""

        private const val WORKSPACE_PATH = "/data/landun/workspace"
        private const val VOLUME_MOUNT_NAME = "workspace"
    }
}
