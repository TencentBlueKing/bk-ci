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

package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.CreateWorkspaceRes
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.remotedev.dispatch.kubernetes.service.StartAndBcsCommonService
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceBcsClient
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentCreate
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentCreateBasicBody
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentOperate
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.pojo.image.ListImagesData
import com.tencent.devops.remotedev.pojo.image.ListImagesResp
import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus
import com.tencent.devops.remotedev.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("startcloudRemoteDevService")
class StartCloudRemoteDevService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val workspaceBcsClient: WorkspaceBcsClient,
    private val workspaceRedisUtils: WorkspaceRedisUtils,
    private val startAndBcsCommonService: StartAndBcsCommonService
) : RemoteDevInterface {

    override fun createWorkspace(
        userId: String,
        event: WorkspaceCreateEvent
    ): CreateWorkspaceRes {
        logger.info("User $userId create workspace: ${JsonUtil.toJson(event)}")

        if (event.devFile.checkWorkspaceAutomaticCorrection()) {
            val orderId = workspaceBcsClient.startListCgs().find {
                it.basic?.envId == event.devFile.environmentUid
            }?.basic?.orderId ?: kotlin.run {
                logger.error("AutomaticCorrection orderId should not be null")
                ""
            }
            // 迁移orderId
            workspaceRedisUtils.setStartCloudOrder(userId, event.workspaceName, orderId)
            return CreateWorkspaceRes(event.devFile.environmentUid!!, event.devFile.uid!!, "")
        }

        // 生产创建start资源的订单号
        val orderId = checkNotNull(event.appName) + "_" + event.projectId + "_${UUIDUtil.generate().takeLast(16)}"
        val zoneId = if (event.devFile.cgsId.isNullOrBlank()) {
            event.devFile.zoneId
        } else {
            checkNotNull(event.devFile.cgsId).substringBefore(".")
        }

        val res = workspaceBcsClient.startCreateWorkspace(
            userId,
            EnvironmentCreate(
                basicBody = EnvironmentCreateBasicBody(
                    userId = userId,
                    appName = checkNotNull(event.appName),
                    gameId = checkNotNull(event.gameId).toString(),
                    pipelineId = orderId,
                    zoneId = zoneId,
                    machineType = event.devFile.machineType,
                    cgsId = event.devFile.cgsId,
                    projectId = event.projectId,
                    image = event.devFile.imageCosFile,
                    internal = event.devFile.quotaType?.getInternal() ?: false,
                    pvcs = event.devFile.pvcs,
                    tolerations = if (event.devFile.specifyTaints != null) {
                        listOf(EnvironmentCreateBasicBody.Toleration(value = checkNotNull(event.devFile.specifyTaints)))
                    } else null,
                    nodeSelector = if (event.devFile.specifyTaints != null) {
                        mapOf("bkbcs.tencent.com/node-group" to checkNotNull(event.devFile.specifyTaints))
                    } else null
                )
            )
        )

        // 创建成功后保存pipelineId
        workspaceRedisUtils.setStartCloudOrder(userId, event.workspaceName, orderId)

        return CreateWorkspaceRes(res.environmentUid, res.taskUid, res.taskID)
    }

    override fun startWorkspace(userId: String, workspaceName: String): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.START,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName)
            )
        )

        return resp.taskUid
    }

    override fun stopWorkspace(userId: String, workspaceName: String): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.STOP,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName)
            )
        )

        return resp.taskUid
    }

    override fun restartWorkspace(userId: String, workspaceName: String): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.RESTART,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName)
            )
        )

        return resp.taskUid
    }

    override fun rebuildWorkspace(
        userId: String,
        workspaceName: String,
        imageCosFile: String,
        formatDataDisk: Boolean?
    ): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.REBUILD,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                image = imageCosFile,
                formatDataDisk = formatDataDisk
            )
        )

        return resp.taskUid
    }

    override fun deleteWorkspace(userId: String, event: WorkspaceOperateEvent): String {
        return deleteWorkspace(userId, event.workspaceName, event.appName)
    }

    private fun deleteWorkspace(
        userId: String,
        workspaceName: String,
        appName: String?
    ): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.DELETE_VM,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                appName = appName,
                userId = userId,
                pipelineId = workspaceRedisUtils.getStartCloudOrder(workspaceName)
            )
        )
        return resp.taskUid
    }

    override fun makeWorkspaceImage(
        userId: String,
        workspaceName: String,
        gameId: String,
        cgsId: String,
        imageId: String,
        imageName: String
    ): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.MAKE_IMAGE,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                appName = gameId,
                userId = userId,
                pipelineId = workspaceRedisUtils.getStartCloudOrder(workspaceName),
                cgsId = cgsId,
                imageName = imageName
            ),
            actionMsg = imageId
        )
        return resp.taskUid
    }

    override fun upgradeWorkspaceVm(
        userId: String,
        workspaceName: String,
        machineType: String,
        pipelineId: String
    ): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.UPGRADE_VM,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                machineType = machineType,
                userId = userId,
                pipelineId = pipelineId
            )
        )
        return resp.taskUid
    }

    override fun cloneWorkspaceVm(
        userId: String,
        workspaceName: String,
        pipelineId: String,
        machineType: String?,
        zoneId: String?,
        live: Boolean?
    ): String {
        val resp = workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.CLONE_VM,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                userId = userId,
                pipelineId = pipelineId,
                zoneId = zoneId,
                machineType = machineType,
                live = live
            )
        )
        return resp.taskUid
    }

    override fun expandDisk(workspaceName: String, userId: String, size: String): ExpandDiskValidateResp {
        val envId = getEnvironmentUid(workspaceName)
        val expandData = EnvironmentOperate(uid = envId, size = size)
        val validateRes = workspaceBcsClient.expandDiskValidate(expandData) ?: run {
            logger.warn("expandDiskValidate $workspaceName|$size validateRes is null")
            return ExpandDiskValidateResp(false, "validateRes is null")
        }
        if (!validateRes.valid) {
            return validateRes
        }
        workspaceBcsClient.startOperateWorkspace(
            userId = userId,
            action = EnvironmentAction.EXPAND_DISK,
            workspaceName = workspaceName,
            environmentOperate = expandData
        )
        return validateRes
    }

    override fun fetchImages(data: ListImagesData): ListImagesResp? {
        return workspaceBcsClient.fetchImages(data)
    }

    override fun deleteImage(imageId: String, delaySeconds: Int): String? {
        return workspaceBcsClient.deleteImage(imageId, delaySeconds)?.taskUid
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        return startAndBcsCommonService.workspaceTaskCallback(taskStatus)
    }

    override fun workspaceTaskCreate(
        taskStatus: TaskStatus,
        workspaceName: String,
        operator: String
    ) {
        startAndBcsCommonService.createWorkspace(taskStatus, workspaceName, operator)
    }

    override fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        val workspaceInfo = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
            ?: throw WorkspaceDispatchException(
                "第三方服务-START-CLOUD 异常，异常信息 - 获取云桌面详情为空"
            )
        val workspaceStatus = workspaceBcsClient.startGetWorkspaceInfo(
            userId = userId,
            environmentOperate = EnvironmentOperate(getEnvironmentUid(workspaceName))
        )
        return WorkspaceInfo(
            status = workspaceStatus.status,
            hostIP = workspaceStatus.hostIP,
            environmentIP = workspaceStatus.environmentIP,
            clusterId = workspaceStatus.clusterId ?: "",
            namespace = workspaceStatus.namespace ?: "",
            environmentHost = workspaceStatus.environmentIP,
            ready = true,
            started = true,
            regionId = workspaceInfo.regionId
        )
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No start environment with $workspaceName")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudRemoteDevService::class.java)
    }
}
