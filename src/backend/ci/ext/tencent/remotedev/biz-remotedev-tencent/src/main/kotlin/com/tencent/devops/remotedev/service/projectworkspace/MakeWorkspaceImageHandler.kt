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
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.auth.api.TencentResourceTypeId
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ImageManageDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceWorkspaceDispatchInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.image.ImageStatus
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.image.WorkspaceImageInfo
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatusEnum
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class MakeWorkspaceImageHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val permissionService: PermissionService,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val workspaceCommon: WorkspaceCommon,
    private val imageManageDao: ImageManageDao,
    private val notifyControl: NotifyControl,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MakeWorkspaceImageHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    @ActionAuditRecord(
        actionId = TencentActionId.CGS_MAKE_IMAGE,
        instance = AuditInstanceRecord(
            resourceType = TencentResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = TencentActionAuditContent.CGS_MAKE_IMAGE_CONTENT
    )
    fun makeWorkspaceImage(
        userId: String,
        workspaceName: String,
        makeImageReq: MakeWorkspaceImageReq
    ): WorkspaceResponse {
        logger.info("$userId make image ${makeImageReq.imageName} workspace $workspaceName")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        if (!permissionService.hasManagerOrOwnerPermission(
                userId = userId,
                projectId = workspace.projectId,
                workspaceName = workspace.workspaceName,
                ownerType = workspace.ownerType
            )
        ) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to make $workspaceName image")
            )
        }
        ActionAuditContext.current()
            .addAttribute(TencentActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .setScopeId(workspace.projectId)

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).tryLock().use {
            /*处理异常的情况*/
            workspaceCommon.checkAndFixExceptionWS(
                status = workspace.status,
                userId = userId,
                workspaceName = workspaceName,
                mountType = workspace.workspaceMountType
            )
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

            val imageId = "img_${RandomStringUtils.randomAlphabetic(8)}"
            // 新增镜像信息
            imageManageDao.createWorkspaceImage(
                projectId = workspace.projectId,
                imageId = imageId,
                imageName = makeImageReq.imageName,
                userId = userId,
                imageStatus = ImageStatus.BUILDING,
                dslContext = dslContext
            )

            val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)
            val taskId = kotlin.runCatching {
                remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.START).makeWorkspaceImage(
                    userId = userId,
                    workspaceName = workspaceName,
                    gameId = gameId.first,
                    cgsId = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)?.hostIp ?: "",
                    imageId = imageId,
                    imageName = makeImageReq.imageName
                )
            }.onFailure {
                // 更新镜像信息
                imageManageDao.updateWorkspaceImage(
                    projectId = workspace.projectId,
                    workspaceImageInfo = WorkspaceImageInfo(imageId),
                    imageStatus = ImageStatus.FAILURE,
                    dslContext = dslContext,
                    errorMsg = it.localizedMessage
                )
                return WorkspaceResponse(
                    workspaceName = workspaceName,
                    workspaceHost = "",
                    status = WorkspaceAction.MAKE_IMAGE,
                    systemType = WorkspaceSystemType.WINDOWS_GPU,
                    workspaceMountType = WorkspaceMountType.START
                )
            }
            logger.info("$workspaceName make image task $taskId")

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
                ownerType = workspace.ownerType,
                projectId = workspace.projectId
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

    fun makeWorkspaceImageCallback(taskId: String, userId: String, workspaceName: String, imageId: String) {
        val taskInfo = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getTaskInfoByUid(taskId).data!!
        }.onFailure {
            logger.warn("makeWorkspaceImageCallback not find uid $taskId")
            return
        }.getOrThrow()

        val mountType = WorkspaceMountType.START

        val workspace = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )

        val workspaceInfo = SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java)
            .getWorkspaceInfo(userId, workspaceName, mountType).data!!

        val workspaceStatus = if (workspaceInfo.status == EnvStatusEnum.running) {
            WorkspaceStatus.RUNNING
        } else {
            WorkspaceStatus.STOPPED
        }

        val workspaceImageInfo = WorkspaceImageInfo(
            imageId = imageId,
            imageCosFile = taskInfo.image?.cosFile ?: "",
            size = taskInfo.image?.size ?: "",
            sourceCgsId = taskInfo.image?.sourceCgsId ?: "",
            sourceCgsType = taskInfo.image?.sourceType ?: "",
            sourceCgsZone = taskInfo.image?.zoneId ?: ""
        )

        var errorMsg: String? = taskInfo.logs.joinToString(";")
        if (taskInfo.status == TaskStatusEnum.successed) {
            errorMsg = null
        } else if ((errorMsg?.length ?: 0) > 1023) {
            errorMsg = errorMsg?.substring(0, 1023)
        }

        if (taskInfo.status == TaskStatusEnum.successed) {
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    workspaceName = workspaceName,
                    status = workspaceStatus,
                    dslContext = transactionContext
                )

                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = workspaceName,
                    operator = userId,
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
                    workspaceImageInfo = workspaceImageInfo,
                    imageStatus = ImageStatus.SUCCESS,
                    dslContext = transactionContext,
                    errorMsg = null
                )
            }
        } else {
            // 启动失败,记录为EXCEPTION
            logger.warn("Make workspaceImage $workspaceName failed")
            workspaceDao.updateWorkspaceStatus(
                workspaceName = workspaceName,
                status = WorkspaceStatus.EXCEPTION,
                dslContext = dslContext
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
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
                workspaceImageInfo = workspaceImageInfo,
                imageStatus = ImageStatus.FAILURE,
                dslContext = dslContext,
                errorMsg = errorMsg
            )
        }

        // 分发到WS
        notifyControl.dispatchWebsocketPushEvent(
            userId = userId,
            workspaceName = workspaceName,
            workspaceHost = "",
            errorMsg = errorMsg,
            type = WebSocketActionType.WORKSPACE_MAKE_IMAGE,
            status = taskInfo.status == TaskStatusEnum.successed,
            action = WorkspaceAction.MAKE_IMAGE,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = workspace.projectId
        )
    }
}
