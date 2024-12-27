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

import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val remoteDevServiceFactory: RemoteDevServiceFactory,
    private val workspaceRedisUtils: WorkspaceRedisUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteDevService::class.java)
    }

    @Suppress("ComplexMethod")
    fun createWorkspaceSyn(event: WorkspaceCreateEvent) {
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
            .createWorkspace(event.userId, event)

        dispatchWorkspaceOpHisDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = event.workspaceName,
            environmentUid = result.enviromentUid,
            operator = event.userId,
            uid = result.taskUid,
            action = EnvironmentAction.CREATE,
            taskId = result.taskId
        )

        // 记录创建历史
        dispatchWorkspaceDao.createWorkspace(
            userId = event.userId,
            event = event,
            environmentUid = result.enviromentUid,
            regionId = 0,
            taskUid = result.taskUid,
            dslContext = dslContext
        )
        if (event.devFile.checkWorkspaceAutomaticCorrection()) {
            val taskStatus = workspaceRedisUtils.getTaskStatus(result.taskUid)
            if (taskStatus != null) {
                remoteDevServiceFactory.loadRemoteDevService(mountType)
                    .workspaceTaskCreate(taskStatus, event.workspaceName, event.userId)
                dispatchWorkspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = result.taskUid,
                    status = EnvironmentActionStatus.AUTOMATIC_CORRECTION,
                    workspaceName = event.workspaceName
                )
            }
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
        if (bakWorkspaceName != null) {
            /*临时逻辑待后期下掉，不在我们这维护order*/
            workspaceRedisUtils.setStartCloudOrder(
                "SYSTEM",
                bakWorkspaceName,
                workspaceRedisUtils.getStartCloudOrder(workspaceName) ?: ""
            )
        }
        dispatchWorkspaceDao.deleteWorkspace(dslContext, workspaceName, bakWorkspaceName)
    }

    fun workspaceTaskCallback(
        taskStatus: TaskStatus,
        mountType: WorkspaceMountType = WorkspaceMountType.DEVCLOUD
    ): Boolean {
        logger.info("workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        return remoteDevServiceFactory.loadRemoteDevService(mountType).workspaceTaskCallback(taskStatus)
    }

    fun expandDisk(
        workspaceName: String,
        userId: String,
        size: String,
        pvcId: String?,
        mountType: WorkspaceMountType
    ): ExpandDiskValidateResp {
        return remoteDevServiceFactory.loadRemoteDevService(mountType).expandDisk(workspaceName, userId, size, pvcId)
    }

    fun getLastExpandDiskStatusAndTime(
        workspaceName: String
    ): Pair<EnvironmentActionStatus?, LocalDateTime?> {
        val record = dispatchWorkspaceOpHisDao.fetchLastTaskByWorkspaceName(
            dslContext = dslContext,
            workspaceName = workspaceName,
            action = EnvironmentAction.EXPAND_DISK
        ) ?: return Pair(null, null)

        return Pair(EnvironmentActionStatus.parse(record.status), record.updateTime)
    }

    fun createDisk(
        workspaceName: String,
        userId: String,
        size: String,
        mountType: WorkspaceMountType
    ): CreateDiskResp {
        return remoteDevServiceFactory.loadRemoteDevService(mountType).createDisk(workspaceName, userId, size)
    }
}
