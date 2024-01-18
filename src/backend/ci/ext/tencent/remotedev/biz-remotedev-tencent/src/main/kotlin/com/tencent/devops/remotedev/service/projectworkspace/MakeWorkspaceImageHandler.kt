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

package com.tencent.devops.remotedev.service.projectworkspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ImageManageDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.pojo.image.ImageStatus
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.SshPublicKeysService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MakeWorkspaceImageHandler @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val permissionService: PermissionService,
    private val sshService: SshPublicKeysService,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val workspaceCommon: WorkspaceCommon,
    private val imageManageDao: ImageManageDao,
    private val notifyControl: NotifyControl
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MakeWorkspaceImageHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_MAKE_IMAGE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_MAKE_IMAGE_CONTENT
    )
    fun makeWorkspaceImage(
        userId: String,
        projectId: String,
        workspaceName: String,
        makeImageReq: MakeWorkspaceImageReq
    ): WorkspaceResponse {
        logger.info("$userId make image ${makeImageReq.imageName} workspace $workspaceName")
        permissionService.checkUserManager(userId, projectId)

        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).tryLock().use {
            if (workspaceCommon.notOk2doNextAction(workspace)) {
                logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    params = arrayOf(
                        workspace.workspaceName,
                        "status is already ${workspace.status}, can't make image now"
                    )
                )
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.MAKE_IMAGE,
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.MANUAL_STOP)
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.MAKE_IMAGE,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status,
                    WorkspaceStatus.MAKING_IMAGE.name
                )
            )

            // 更新工作区状态
            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.MAKING_IMAGE
            )

            val imageId = "img_${RandomStringUtils.randomAlphabetic(8)}"
            // 新增镜像信息
            imageManageDao.createWorkspaceImage(
                projectId = projectId,
                imageId = imageId,
                imageName = makeImageReq.imageName,
                userId = userId,
                imageStatus = ImageStatus.BUILDING,
                dslContext = dslContext
            )

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = userId,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.MAKE_IMAGE,
                    sshKeys = sshService.getSshPublicKeys4Ws(
                        workspaceDao.fetchWorkspaceUser(
                            dslContext,
                            workspaceName
                        ).toSet()
                    ),
                    workspaceName = workspaceName,
                    settingEnvs = remoteDevSettingDao.fetchOneSetting(dslContext, userId).envsForVariable,
                    bkTicket = "",
                    cgsId = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)?.hostIp ?: "",
                    imageId = imageId,
                    mountType = WorkspaceMountType.START
                )
            )

            notifyControl.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_MAKE_IMAGE,
                status = true,
                action = WorkspaceAction.MAKE_IMAGE,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START,
                ownerType = WorkspaceOwnerType.PROJECT,
                projectId = projectId
            )

            return WorkspaceResponse(
                workspaceName = workspaceName,
                workspaceHost = "",
                status = WorkspaceAction.MAKE_IMAGE,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START
            )
        }
    }

    fun makeWorkspaceImageCallback(event: RemoteDevUpdateEvent) {
        val workspace = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = event.workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(event.workspaceName)
        )

        val workspaceInfo = client.get(ServiceRemoteDevResource::class)
            .getWorkspaceInfo(event.userId, event.workspaceName, event.mountType).data!!

        val workspaceStatus = if (workspaceInfo.status == EnvStatusEnum.running) {
            WorkspaceStatus.RUNNING
        } else {
            WorkspaceStatus.STOPPED
        }

        if (event.status) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = event.workspaceName,
                    status = workspaceStatus,
                    dslContext = transactionContext
                )

                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    operator = event.userId,
                    action = WorkspaceAction.MAKE_IMAGE,
                    actionMessage = String.format(
                        workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                        workspace.status.name,
                        workspaceStatus.name
                    )
                )

                // 更新镜像信息
                imageManageDao.updateWorkspaceImage(
                    projectId = workspace.projectId,
                    workspaceImageInfo = event.workspaceImageInfo!!,
                    imageStatus = ImageStatus.SUCCESS,
                    dslContext = transactionContext
                )
            }
        } else {
            // 启动失败,记录为EXCEPTION
            logger.warn("Make workspaceImage ${event.workspaceName} failed")
            workspaceDao.updateWorkspaceStatus(
                workspaceName = event.workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = event.workspaceName,
                operator = event.userId,
                action = WorkspaceAction.MAKE_IMAGE,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status.name,
                    WorkspaceStatus.EXCEPTION.name
                )
            )

            // 更新镜像信息
            imageManageDao.updateWorkspaceImage(
                projectId = workspace.projectId,
                workspaceImageInfo = event.workspaceImageInfo!!,
                imageStatus = ImageStatus.FAILURE,
                dslContext = dslContext
            )
        }

        // 分发到WS
        notifyControl.dispatchWebsocketPushEvent(
            userId = event.userId,
            workspaceName = event.workspaceName,
            workspaceHost = "",
            errorMsg = event.errorMsg,
            type = WebSocketActionType.WORKSPACE_MAKE_IMAGE,
            status = event.status,
            action = WorkspaceAction.MAKE_IMAGE,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }
}
