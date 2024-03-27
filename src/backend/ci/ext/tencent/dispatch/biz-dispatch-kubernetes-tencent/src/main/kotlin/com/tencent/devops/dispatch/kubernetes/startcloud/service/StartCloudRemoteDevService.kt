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

package com.tencent.devops.dispatch.kubernetes.startcloud.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchWorkspaceOpHisDao
import com.tencent.devops.dispatch.kubernetes.interfaces.RemoteDevInterface
import com.tencent.devops.dispatch.kubernetes.pojo.BK_DEVCLOUD_TASK_TIMED_OUT
import com.tencent.devops.dispatch.kubernetes.pojo.CreateWorkspaceRes
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentAction
import com.tencent.devops.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.WorkspaceInfo
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmReq
import com.tencent.devops.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.dispatch.kubernetes.startcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentCreate
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentCreateBasicBody
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentOperate
import com.tencent.devops.dispatch.kubernetes.startcloud.pojo.EnvironmentUserCreate
import com.tencent.devops.dispatch.kubernetes.startcloud.utils.StartCloudRedisUtils
import com.tencent.devops.dispatch.kubernetes.utils.WorkspaceRedisUtils
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("startcloudRemoteDevService")
class StartCloudRemoteDevService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val dispatchWorkspaceOpHisDao: DispatchWorkspaceOpHisDao,
    private val workspaceClient: WorkspaceStartCloudClient,
    private val workspaceRedisUtils: WorkspaceRedisUtils,
    private val startCloudRedisUtils: StartCloudRedisUtils,
    private val startCloudInterfaceService: StartCloudInterfaceService
) : RemoteDevInterface {

    @Value("\${startCloud.appName}")
    val appName: String = "IEG_BKCI"

    @Value("\${startCloud.curLaunchId}")
    val curLaunchId: Int = 980007

    override fun createWorkspace(userId: String, event: WorkspaceCreateEvent): CreateWorkspaceRes {
        logger.info("User $userId create workspace: ${JsonUtil.toJson(event)}")

        if (event.devFile.checkWorkspaceAutomaticCorrection()) {
            val orderId = workspaceClient.listCgs().find {
                it.basic?.envId == event.devFile.environmentUid
            }?.basic?.orderId ?: kotlin.run {
                logger.error("AutomaticCorrection orderId should not be null")
                ""
            }
            // 迁移orderId
            startCloudRedisUtils.setStartCloudOrder(userId, event.workspaceName, orderId)
            return CreateWorkspaceRes(event.devFile.environmentUid!!, event.devFile.uid!!, 0, "")
        }

        kotlin.runCatching { workspaceClient.createUser(userId, EnvironmentUserCreate(userId, appName)) }.onFailure {
            logger.warn("create user failed.|${it.message}")
            if (it is BuildFailureException &&
                it.errorCode == ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_ERROR.errorCode
            ) {
                throw it
            }
        }

        // 生产创建start资源的订单号
        val orderId = appName + "_" + event.projectId + "_${UUIDUtil.generate().takeLast(16)}"

        // 先检查基础镜像在池子中是否有配额，再看有没有可以新生产的显卡
        var zoneId = ""
        var createFlag = false
        if (event.devFile.imageCosFile.isNullOrBlank()) {
            val random = startCloudInterfaceService.syncStartCloudResourceList().filter {
                it.status == 11 &&
                    it.machineType == event.devFile.machineType &&
                    it.zoneId.replace(Regex("\\d+"), "") == event.devFile.zoneId &&
                    it.locked != true
            }.randomOrNull()
            if (random != null) {
                logger.info("get random resource to running|$random")
                createFlag = true
                zoneId = random.zoneId
            }
        }
        // 说明池子中没有，或者是自定义镜像，需要使用显卡重新创建
        if (!createFlag) {
            val random = workspaceClient.getResourceVm(
                ResourceVmReq(
                    zoneId = event.devFile.zoneId,
                    machineType = event.devFile.machineType
                )
            )?.filter {
                (it.zoneId.replace(Regex("\\d+"), "") == event.devFile.zoneId) &&
                        (it.machineResources?.any { ma -> ma.machineType == event.devFile.machineType } == true)
            }?.randomOrNull() ?: throw BuildFailureException(
                ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorType,
                ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.errorCode,
                ErrorCodeEnum.CREATE_ENVIRONMENT_INTERFACE_FAIL.formatErrorMessage,
                " ${event.devFile.zoneId}地区${event.devFile.machineType}型云桌面资源不足"
            )
            logger.info("get random resource to running|$random")
            zoneId = random.zoneId
        }

        val res = workspaceClient.createWorkspace(
            userId,
            EnvironmentCreate(
                basicBody = EnvironmentCreateBasicBody(
                    userId = userId,
                    appName = appName,
                    pipelineId = orderId,
                    zoneId = zoneId,
                    machineType = event.devFile.machineType,
                    cgsId = event.devFile.cgsId,
                    projectId = event.projectId,
                    image = event.devFile.imageCosFile
                )
            )
        )

        // 创建成功后保存pipelineId
        startCloudRedisUtils.setStartCloudOrder(userId, event.workspaceName, orderId)

        return CreateWorkspaceRes(res.environmentUid, res.taskUid, 0, "")
    }

    override fun startWorkspace(userId: String, workspaceName: String): String {
        val resp = workspaceClient.operateWorkspace(
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
        val resp = workspaceClient.operateWorkspace(
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
        val resp = workspaceClient.operateWorkspace(
            userId = userId,
            action = EnvironmentAction.RESTART,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName)
            )
        )

        return resp.taskUid
    }

    override fun rebuildWorkspace(userId: String, workspaceName: String, imageCosFile: String): String {
        val resp = workspaceClient.operateWorkspace(
            userId = userId,
            action = EnvironmentAction.REBUILD,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                image = imageCosFile
            )
        )

        return resp.taskUid
    }

    override fun deleteWorkspace(userId: String, workspaceName: String): String {
        val resp = workspaceClient.operateWorkspace(
            userId = userId,
            action = EnvironmentAction.DELETE_VM,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                appName = appName,
                userId = userId,
                pipelineId = startCloudRedisUtils.getStartCloudOrder(workspaceName)
            )
        )

        return resp.taskUid
    }

    override fun makeWorkspaceImage(userId: String, workspaceName: String, cgsId: String?): String {
        val resp = workspaceClient.operateWorkspace(
            userId = userId,
            action = EnvironmentAction.MAKE_IMAGE,
            workspaceName = workspaceName,
            environmentOperate = EnvironmentOperate(
                uid = getEnvironmentUid(workspaceName),
                appName = appName,
                userId = userId,
                pipelineId = startCloudRedisUtils.getStartCloudOrder(workspaceName),
                cgsId = cgsId
            )
        )

        return resp.taskUid
    }

    override fun getWorkspaceUrl(userId: String, workspaceName: String): String {
        TODO("Not yet implemented")
    }

    override fun workspaceTaskCallback(taskStatus: TaskStatus): Boolean {
        logger.info("workspaceTaskCallback|${taskStatus.uid}|$taskStatus")
        val task = dispatchWorkspaceOpHisDao.getTask(dslContext, taskStatus.uid)
        workspaceRedisUtils.refreshTaskStatus("bcs", taskStatus.uid, taskStatus)
        if (task?.status?.needFix() == true && task.action == EnvironmentAction.CREATE) {
            val oldWs = dispatchWorkspaceDao.getWorkspaceInfo(task.workspaceName, dslContext) ?: kotlin.run {
                logger.warn("workspaceTaskCallback|try to fix fail with wrong workspace|$task")
                return false
            }
            kotlin.runCatching {
                client.get(ServiceRemoteDevResource::class)
                    .createWinWorkspaceByVm(oldWs.userId, oldWs.workspaceName, null, taskStatus.uid)
            }.onFailure {
                logger.warn("workspaceTaskCallback|createWinWorkspaceByVm fail ${it.message}", it)
            }
        }
        return true
    }

    override fun getWorkspaceInfo(userId: String, workspaceName: String): WorkspaceInfo {
        val workspaceInfo = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
            ?: throw BuildFailureException(
                ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorType,
                ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.errorCode,
                ErrorCodeEnum.ENVIRONMENT_STATUS_INTERFACE_ERROR.formatErrorMessage,
                "第三方服务-START-CLOUD 异常，异常信息 - 获取云桌面详情为空"
            )
        val workspaceStatus = workspaceClient.getWorkspaceInfo(
            userId = userId,
            environmentOperate = EnvironmentOperate(getEnvironmentUid(workspaceName))
        )
        return WorkspaceInfo(
            status = workspaceStatus.status,
            hostIP = workspaceStatus.hostIP,
            environmentIP = workspaceStatus.environmentIP,
            clusterId = workspaceStatus.clusterId,
            namespace = workspaceStatus.namespace,
            environmentHost = workspaceStatus.environmentIP,
            ready = true,
            started = true,
            curLaunchId = curLaunchId,
            regionId = workspaceInfo.regionId
        )
    }

    override fun waitTaskFinish(
        userId: String,
        taskId: String,
        type: UpdateEventType
    ): DispatchBuildTaskStatus {
        logger.info("StartCloud remoteDevService waitTaskFinish|userId|$userId|taskId|$taskId")
        val startTime = System.currentTimeMillis()
        val timeout = if (type == UpdateEventType.CREATE || type == UpdateEventType.REBUILD) {
            START_CREATE_TIMEOUT
        } else {
            START_OTHER_TIMEOUT
        }
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > timeout) {
                logger.error("Wait task: $taskId finish timeout($timeout)")
                dispatchWorkspaceOpHisDao.update(
                    dslContext = dslContext,
                    uid = taskId,
                    status = EnvironmentActionStatus.WAIT_TIMEOUT,
                    fStatus = EnvironmentActionStatus.PENDING,
                    actionMsg = "$taskId finish timeout($timeout)"
                )
                return DispatchBuildTaskStatus(
                    DispatchBuildTaskStatusEnum.FAILED,
                    I18nUtil.getCodeLanMessage(BK_DEVCLOUD_TASK_TIMED_OUT)
                )
            }

            Thread.sleep(START_CREATE_LOOP_INTERVAL)

            val taskStatus = workspaceRedisUtils.getTaskStatus(taskId)
            if (taskStatus?.status != null) {
                logger.info("Loop task taskId: $taskId, status: ${JsonUtil.toJson(taskStatus)}")
                workspaceRedisUtils.deleteTask(taskId)
                return if (taskStatus.status == TaskStatusEnum.successed) {
                    DispatchBuildTaskStatus(
                        DispatchBuildTaskStatusEnum.SUCCEEDED,
                        JsonUtil.toJson(taskStatus)
                    )
                } else {
                    DispatchBuildTaskStatus(DispatchBuildTaskStatusEnum.FAILED, taskStatus.logs.toString())
                }
            }
        }
    }

    fun refreshStartCloudOrderId(userId: String): Boolean {
        logger.info("$userId refresh startCloud orderId.")
        val startCloudWorkspaceList = dispatchWorkspaceDao.getStartCloudWorkspaceInfo(dslContext)
        startCloudWorkspaceList.forEach {
            startCloudRedisUtils.setStartCloudOrder(userId, it.workspaceName, it.taskId)
        }

        return true
    }

    private fun getEnvironmentUid(workspaceName: String): String {
        val workspaceRecord = dispatchWorkspaceDao.getWorkspaceInfo(workspaceName, dslContext)
        return workspaceRecord?.environmentUid ?: throw RuntimeException("No start environment with $workspaceName")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartCloudRemoteDevService::class.java)
        private const val START_CREATE_TIMEOUT = 60 * 60 * 1000 // start生成资源最长轮训时间
        private const val START_OTHER_TIMEOUT = 30 * 60 * 1000
        private const val START_CREATE_LOOP_INTERVAL = 1000L
    }
}
