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

package com.tencent.devops.remotedev.service.software

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.SoftwareManageDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.software.CommonArgs
import com.tencent.devops.remotedev.pojo.software.InstallSoftwareRes
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import com.tencent.devops.remotedev.pojo.software.SoftwareCreate
import com.tencent.devops.remotedev.pojo.software.SoftwareInfo
import com.tencent.devops.remotedev.pojo.software.TaskStatusEnum
import com.tencent.devops.remotedev.pojo.windows.WindowsDevCouldCallback
import com.tencent.devops.remotedev.service.HttpCallBackService
import com.tencent.devops.remotedev.service.projectworkspace.UpgradeWorkspaceHandler
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.WINDOWS_GPU_ASSIGN_NOTIFY
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SoftwareManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceCommon: WorkspaceCommon,
    private val upgradeWorkspaceHandler: UpgradeWorkspaceHandler,
    private val notifyControl: NotifyControl,
    private val workspaceDao: WorkspaceDao,
    private val httpCallBackService: HttpCallBackService,
    private val softwareManageDao: SoftwareManageDao
) {
    @Value("\${remoteDev.appCode:}")
    val appCode = ""

    @Value("\${remoteDev.appToken:}")
    val appSecret = ""

    @Value("\${xingyun.software_group_url:}")
    val softwareGroupUrl = ""

    @Value("\${xingyun.install_software_url:}")
    val installSoftwareUrl = ""

    @Value("\${devopsGateway.host:#{null}}")
    val backendHost = ""

    /*请求合法性校验时使用的密钥*/
    @Value("\${externalKey:}")
    val externalKey = ""

    companion object {
        private val logger = LoggerFactory.getLogger(SoftwareManageService::class.java)
        private const val IOANAME = "IOA"
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }

    fun safeInitialization(
        projectId: String,
        userId: String,
        workspaceName: String
    ) {
        logger.info("$userId start workspace $workspaceName")
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
            // 校验状态
            when (workspace.status) {
                WorkspaceStatus.DELIVERING, WorkspaceStatus.PREPARING -> {
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = userId,
                        action = WorkspaceAction.START,
                        actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.SAFE_INITIALIZATION)
                    )
                    // todo job接口执行
                    logger.info("safeInitialization|$userId|$userId")
                    installSystemSoftwares(
                        projectId = projectId,
                        creator = userId,
                        regionId = workspace.regionId.toString(),
                        ip = workspace.hostIp ?: "",
                        workspaceName = workspaceName
                    )
                }

                else -> {
                    logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                }
            }
        }
    }

    fun softwareInstallationCompleteCallback(
        type: String,
        workspaceName: String,
        projectId: String,
        userId: String,
        softwareList: SoftwareCallbackRes
    ) {
        logger.info(
            "softwareInstallationCompleteCallback|type|$type|workspaceName|$workspaceName" +
                "|projectId|$projectId|userId|$userId|softwareList|$softwareList"
        )
        // 添加软件安装历史
        updateSoftwareInstalledRecords(
            type = type,
            softwareList = softwareList
        )
        updateWindowsWorkspaceStatus(workspaceName) { workspace ->
            when (workspace.status) {
                // 交付中安装IOA后
                WorkspaceStatus.DELIVERING, WorkspaceStatus.DELIVERING_FAILED -> {
                    if (type == "SYSTEM") {
                        checkSafeInitSuccess(softwareList, workspace)
                        workspaceCommon.updateStatusAndCreateHistory(
                            workspace = workspace,
                            newStatus = WorkspaceStatus.DISTRIBUTING,
                            action = WorkspaceAction.CREATE
                        )
                        workspaceCommon.autoAssignOwner(workspace)

                        upgradeWorkspaceHandler.checkAndUpgradeVm(workspaceName)

                        notifyControl.notify4RemoteDevManager(
                            projectId = projectId,
                            cc = mutableSetOf(workspace.createUserId),
                            notifyTemplateCode = WINDOWS_GPU_ASSIGN_NOTIFY,
                            notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                            bodyParams = mutableMapOf(
                                "workspaceName" to workspace.workspaceName,
                                "cgsId" to (workspace.hostIp ?: workspace.workspaceName),
                                "projectId" to projectId,
                                "creator" to workspace.createUserId
                            )
                        )
                    }
                }

                WorkspaceStatus.RUNNING -> {
                    if (type != "SYSTEM") {
                        workspaceCommon.updateStatusAndCreateHistory(
                            workspace = workspace,
                            newStatus = WorkspaceStatus.RUNNING,
                            action = WorkspaceAction.CREATE
                        )
                    }
                }
                // 个人云桌面
                WorkspaceStatus.PREPARING -> {
                    checkSafeInitSuccess(softwareList, workspace)
                    workspaceDao.updateWorkspaceStatus(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        status = WorkspaceStatus.RUNNING
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        operator = workspace.createUserId,
                        action = WorkspaceAction.CREATE,
                        actionMessage = String.format(
                            workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            workspace.status.name,
                            WorkspaceStatus.RUNNING.name
                        )
                    )
                    upgradeWorkspaceHandler.checkAndUpgradeVm(workspaceName)
                    httpCallBackService.asyncTask(
                        WindowsDevCouldCallback(
                            workspaceName,
                            WorkspaceStatus.RUNNING,
                            WorkspaceAction.CREATE
                        )
                    )
                    notifyControl.dispatchWebsocketPushEvent(
                        userId = workspace.createUserId,
                        workspaceName = workspace.workspaceName,
                        workspaceHost = workspace.hostIp,
                        type = WebSocketActionType.WORKSPACE_CREATE,
                        status = true,
                        action = WorkspaceAction.START,
                        systemType = workspace.workspaceSystemType,
                        workspaceMountType = workspace.workspaceMountType,
                        ownerType = workspace.ownerType,
                        projectId = workspace.projectId
                    )
                }

                else -> {
                    logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                }
            }
        }
    }

    private fun checkSafeInitSuccess(
        softwareList: SoftwareCallbackRes,
        ws: WorkspaceRecordWithWindows
    ) {
        if (softwareList.taskStatus == TaskStatusEnum.FAILED) {
            workspaceCommon.updateStatus2DeliveringFailed(
                workspace = ws,
                action = WorkspaceAction.CREATE,
                notifyTemplateCode = "WINDOWS_GPU_SAFE_INIT_FAILED"
            )
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DELIVERING_FAILED.errorCode
            )
        }
    }

    private fun updateWindowsWorkspaceStatus(workspaceName: String, update: (ws: WorkspaceRecordWithWindows) -> Unit) {
        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds
        ).lock().use {
            val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
            // 更新状态
            update(workspace)
        }
    }

    /** 云桌面创建完成后安全初始化：安装ioa
     * ioa安装的脚步严格安装以下格式字符串，转base64后传入。
     * base64(-project_id "cmk-tke" -creator "raylzhang" -region_id "555" -inner_ip "SZ3.11.171.77.15")
     */
    fun installSystemSoftwares(
        projectId: String,
        creator: String,
        regionId: String,
        ip: String,
        workspaceName: String
    ) {
        val params = "-project_id \"$projectId\" -creator \"$workspaceName\" -region_id \"$regionId\" -inner_ip \"$ip\""
        val base64Val = Base64Util.encode(params.toByteArray())
        val systemSoftwareInfoList = softwareManageDao.getSystemSoftwareList(dslContext)
        logger.info("installSoftwareFromXingyun|systemSoftwareInfoList|$systemSoftwareInfoList|params|$params")
        if (systemSoftwareInfoList.isEmpty()) {
            return
        }
        val softwareInfoList = mutableListOf<SoftwareInfo>()
        systemSoftwareInfoList.forEach { record ->
            softwareInfoList.add(
                SoftwareInfo(
                    name = record["NAME"] as String,
                    version = record["VERSION"] as String,
                    commonArgs = CommonArgs(
                        base64 = base64Val,
                        cloudDesktopId = workspaceName
                    ).takeIf { record["NAME"] == IOANAME }
                )
            )
        }
        val callBackUrl = "$backendHost/remotedev/api/external/remotedev/software_install_callback" +
            "?type=SYSTEM&key=$externalKey&workspaceName=$workspaceName&" +
            "projectId=$projectId&userId=$creator&x-devops-project-id=$projectId"
        installSoftwareFromXingyun(
            userId = creator,
            ip = regionId + ":" + ip.substringAfter("."),
            callBackUrl = callBackUrl,
            softwareInfoList = softwareInfoList
        )?.also {
            // 插入软件安装记录
            softwareManageDao.batchAddSystemInstalledRecords(
                dslContext = dslContext,
                tadkId = it.data.taskId,
                workspaceName = workspaceName,
                softwareInfoList = softwareInfoList
            )
        }
    }

    // 调用行云接口执行软件安装
    fun installSoftwareFromXingyun(
        userId: String,
        ip: String,
        callBackUrl: String,
        softwareInfoList: List<SoftwareInfo>
    ): InstallSoftwareRes? {
        // 先获取userId安装的软件列表，封装成SoftwareCreate
        val softwareCreate = SoftwareCreate(
            ip = ip,
            username = userId,
            softwareInfo = softwareInfoList,
            callbackUrl = callBackUrl
        )
        val body = JsonUtil.toJson(softwareCreate, false)
        logger.info("installSoftwareFromXingyun|installSoftwareUrl|$installSoftwareUrl|body|$body")
        val headerStr = ObjectMapper().writeValueAsString(mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret))
            .replace("\\s".toRegex(), "")
        val request = Request.Builder()
            .url(installSoftwareUrl)
            .addHeader("x-bkapi-authorization", headerStr)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()
        return kotlin.runCatching {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info(
                    "installSoftwareFromXingyun|response code|$ip" +
                        "|${response.code}|responseContent|$responseContent"
                )
                if (!response.isSuccessful) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                        errorCode = ErrorCodeEnum.INSTALL_SOFTWARE_FAIL.errorCode
                    )
                }
                val createSoftwareRes: InstallSoftwareRes = jacksonObjectMapper().readValue(responseContent)
                logger.info("installSoftwareFromXingyun|createSoftwareRes|$createSoftwareRes")
                if (response.code == Response.Status.OK.statusCode && !createSoftwareRes.result) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.OK.statusCode,
                        errorCode = ErrorCodeEnum.INSTALL_SOFTWARE_FAIL.errorCode,
                        defaultMessage = createSoftwareRes.message
                    )
                }
                createSoftwareRes
            }
        }.onFailure {
            logger.error("install software from xingyun failed.", it)
        }.getOrThrow()
    }

    // 添加系统软件安装记录
    fun updateSoftwareInstalledRecords(type: String, softwareList: SoftwareCallbackRes) {
        logger.info("updateSoftwareInstalledRecords|type|$type|softwareList|$softwareList")
        if (type == "SYSTEM") {
            softwareManageDao.updateSystemInstalledRecords(dslContext, softwareList)
        }
    }
}
