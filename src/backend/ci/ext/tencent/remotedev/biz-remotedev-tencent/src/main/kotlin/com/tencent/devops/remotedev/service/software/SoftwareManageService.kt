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
import com.tencent.devops.remotedev.service.projectworkspace.CloneWorkspaceHandler
import com.tencent.devops.remotedev.service.projectworkspace.UpgradeWorkspaceHandler
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.WINDOWS_GPU_ASSIGN_NOTIFY
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.core.Response
import java.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
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
    private val softwareManageDao: SoftwareManageDao,
    private val cloneWorkspaceHandler: CloneWorkspaceHandler
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

    companion object {
        private val logger = LoggerFactory.getLogger(SoftwareManageService::class.java)
        private const val IOANAME = "IOA"
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val CALLBACK_TOKEN_BYTES = 32
        // 软件安装回调 token 有效期（秒），需覆盖安装耗时，到期失效
        private const val SOFTWARE_CALLBACK_TOKEN_TTL_SECONDS = 7200L
    }

    /**
     * 生成软件安装回调一次性 token，绑定 workspaceName 写入 Redis，短期有效。
     * token 到期失效，TTL 内可重复校验以容忍回调重试。
     */
    private fun generateSoftwareCallbackToken(workspaceName: String): String {
        val bytes = ByteArray(CALLBACK_TOKEN_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val token = bytes.joinToString("") { "%02x".format(it) }
        redisOperation.set(
            key = "${RedisKeys.SOFTWARE_CALLBACK_TOKEN_PREFIX}$token",
            value = workspaceName,
            expiredInSecond = SOFTWARE_CALLBACK_TOKEN_TTL_SECONDS
        )
        return token
    }

    /**
     * 校验软件安装回调 token：token 必须存在且绑定的 workspaceName 与请求一致。
     * 秘密性由 token 本身（122bit 随机）与 Redis 存在性保证，workspaceName 用常量时间比较。
     */
    fun verifySoftwareCallbackToken(token: String, workspaceName: String): Boolean {
        if (token.isBlank()) return false
        val bound = redisOperation.get("${RedisKeys.SOFTWARE_CALLBACK_TOKEN_PREFIX}$token") ?: return false
        return java.security.MessageDigest.isEqual(bound.toByteArray(), workspaceName.toByteArray())
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
                WorkspaceStatus.DELIVERING, WorkspaceStatus.PREPARING, WorkspaceStatus.REBUILDING -> {
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
                            action = WorkspaceAction.CREATE_SUCCESS
                        )
                        workspaceCommon.autoAssignOwner(workspace)

                        upgradeWorkspaceHandler.checkAndUpgradeVm(workspaceName)
                        cloneWorkspaceHandler.checkAndCloneVm(workspaceName)

                        notifyControl.notify4RemoteDevManager(
                            projectId = projectId,
                            cc = mutableSetOf(workspace.createUserId),
                            notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                            bodyParams = mutableMapOf(
                                "workspaceName" to workspace.workspaceName,
                                "cgsId" to (workspace.hostIp ?: workspace.workspaceName),
                                "projectId" to projectId,
                                "notifyTemplateCode" to WINDOWS_GPU_ASSIGN_NOTIFY,
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
                        action = WorkspaceAction.CREATE_SUCCESS,
                        actionMessage = String.format(
                            workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            workspace.status.name,
                            WorkspaceStatus.RUNNING.name
                        )
                    )
                    upgradeWorkspaceHandler.checkAndUpgradeVm(workspaceName)
                    cloneWorkspaceHandler.checkAndCloneVm(workspaceName)

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
        val base64Val = Base64.getEncoder().encodeToString(params.toByteArray())
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
        val callbackToken = generateSoftwareCallbackToken(workspaceName)
        val callBackUrl = "$backendHost/remotedev/api/external/remotedev/software_install_callback" +
            "?type=SYSTEM&key=$callbackToken&workspaceName=$workspaceName&" +
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
        // 不打印 body，其含回调 token；仅记录可观测信息
        logger.info(
            "installSoftwareFromXingyun|installSoftwareUrl|$installSoftwareUrl|ip|$ip|" +
                "software|${softwareInfoList.map { it.name }}"
        )
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
