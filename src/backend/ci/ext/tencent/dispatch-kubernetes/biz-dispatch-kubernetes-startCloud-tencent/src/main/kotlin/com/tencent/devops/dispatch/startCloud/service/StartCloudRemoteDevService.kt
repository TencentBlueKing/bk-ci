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
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.CreateWorkspaceRes
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
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

    @Value("\${startCloud.appName}")
    val appName: String = "IEG_BKCI"

    @Value("\${startCloud.curLaunchId}")
    val curLaunchId: Int = 980007

    override fun createWorkspace(userId: String, event: WorkspaceCreateEvent): CreateWorkspaceRes {
        logger.info("User $userId create workspace: ${JsonUtil.toJson(event)}")

        kotlin.runCatching { workspaceClient.createUser(userId, EnvironmentUserCreate(userId, appName)) }.onFailure {
            logger.warn("create user failed.|${it.message}")
            if (it is BuildFailureException &&
                it.errorCode == ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode
            ) {
                throw it
            }
        }
        val pipeLineId = appName + "_" + event.projectId + "_${UUIDUtil.generate().takeLast(5)}"

        val res = workspaceClient.createWorkspace(
            userId,
            EnvironmentCreate(
                userId = userId,
                appName = appName,
                pipeLineId = pipeLineId,
                zoneId = event.devFile.zoneId,
                machineType = event.devFile.machineType
            )
        )
        return CreateWorkspaceRes(res.cgsIp, pipeLineId, res.cloudZoneId.toIntOrNull() ?: 0)
    }

    override fun startWorkspace(userId: String, workspaceName: String): String {
        return EMPTY
    }

    override fun stopWorkspace(userId: String, workspaceName: String): String {
        return EMPTY
    }

    override fun deleteWorkspace(userId: String, event: WorkspaceOperateEvent): String {
        workspaceClient.deleteWorkspace(
            userId = userId,
            workspaceName = event.workspaceName,
            EnvironmentDelete(
                userId = event.userId,
                appName = appName,
                pipeLineId = dispatchWorkspaceDao.getWorkspaceInfo(event.workspaceName, dslContext)?.taskId
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
        val workspaceInfo = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
            ?: throw BuildFailureException(
                ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
                "第三方服务-START-CLOUD 异常，异常信息 - 获取云桌面详情为空"
            )
        return WorkspaceInfo(
            status = EnvStatusEnum.running,
            hostIP = workspaceInfo.environmentUid,
            environmentIP = workspaceInfo.environmentUid,
            clusterId = "",
            namespace = "",
            environmentHost = "",
            ready = true,
            started = true,
            curLaunchId = curLaunchId,
            regionId = workspaceInfo.regionId
        )
    }
    override fun waitTaskFinish(userId: String, taskId: String): DispatchBuildTaskStatus {
        return DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.SUCCEEDED, null)
    }
    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudRemoteDevService::class.java)
        private const val EMPTY = ""
    }
}
