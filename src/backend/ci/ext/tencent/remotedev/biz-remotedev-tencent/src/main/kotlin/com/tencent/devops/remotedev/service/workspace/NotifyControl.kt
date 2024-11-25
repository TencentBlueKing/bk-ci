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

package com.tencent.devops.remotedev.service.workspace

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.NotifyMessageContextRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.dao.ProjectNotifyDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceNotifyHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyListData
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.StartWorkspaceService
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Base64
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class NotifyControl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val taiClient: TaiClient,
    private val notifyDao: ProjectNotifyDao,
    private val sharedDao: WorkspaceSharedDao,
    private val startWorkspaceService: StartWorkspaceService,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val permissionService: PermissionService,
    private val workspaceNotifyHistoryDao: WorkspaceNotifyHistoryDao
) {

    @Value("\${notice.wework:#{null}}")
    private var weworkId: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(NotifyControl::class.java)

        /*云桌面通知功能-个人名下云桌面新增时*/
        const val WINDOWS_GPU_OWNER_CHANGE_NOTIFY = "WINDOWS_GPU_OWNER_CHANGE_NOTIFY"

        /*云桌面通知功能-云桌面重启时*/
        const val WINDOWS_GPU_RESTART_NOTIFY = "WINDOWS_GPU_RESTART_NOTIFY"

        /*云桌面通知功能-云桌面已分配到项目时*/
        const val WINDOWS_GPU_ASSIGN_NOTIFY = "WINDOWS_GPU_ASSIGN_NOTIFY"

        /*云桌面通知功能-云桌面已分配到项目时*/
        const val CLIENT_VERSION_WARNING_NOTIFY = "CLIENT_VERSION_WARNING_NOTIFY"

        /*云桌面通知-您的云桌面已被强制销毁*/
        const val WORKSPACE_FORCE_DELETE = "WORKSPACE_FORCE_DELETE"

        /*云桌面通知-您的云桌面已被强制批量销毁*/
        const val WORKSPACE_BATCH_FORCE_DELETE = "WORKSPACE_BATCH_FORCE_DELETE"
    }

    fun resendByUserId(
        userId: String,
        type: RemoteDevNotifyType
    ) {
        val failHistory = workspaceNotifyHistoryDao.fetchFailMessage(dslContext, userId, type)
        val limit = LocalDateTime.now().plusDays(-3)
        failHistory.forEach { history ->
            if (history.createdTime < limit) {
                workspaceNotifyHistoryDao.updateStatus(dslContext, history.id, RemoteDevNotifyType.Status.FAIL_EXPIRED)
                return
            }
            notify4User(
                userIds = setOf(userId),
                notifyType = mutableSetOf(type),
                bodyParams = JsonUtil.to(history.bodyParams)
            )
            workspaceNotifyHistoryDao.updateStatus(dslContext, history.id, RemoteDevNotifyType.Status.FAIL_RESEND)
        }
    }

    fun notifyWorkspaceInfo(
        userId: String,
        notifyData: WorkspaceNotifyData
    ) {
        val workspace = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            sips = notifyData.ip?.toSet(),
            owners = notifyData.owner?.toSet(),
            projectIds = notifyData.projectId?.toSet(),
            notStatus = setOf(WorkspaceStatus.DELETED, WorkspaceStatus.PREPARING, WorkspaceStatus.DELIVERING_FAILED),
            checkField = listOf(
                TWorkspace.T_WORKSPACE.NAME,
                TWorkspace.T_WORKSPACE.PROJECT_ID
            )
        )
        val messageContent = "${notifyData.title}: ${notifyData.desc}"

        notifyDao.add(dslContext, userId, notifyData)

        val personalUsers = workspace.filter { it.ownerType == WorkspaceOwnerType.PERSONAL }
            .map { it.createUserId }
            .toMutableSet()

        val userList = if (!notifyData.owner.isNullOrEmpty()) {
            notifyData.owner!!.toSet()
        } else {
            workspaceSharedDao.fetchWorkspaceOwner(
                dslContext = dslContext,
                workspaceNames = workspace.map { it.workspaceName }.toSet().ifEmpty { return }
            ).values.toSet().plus(personalUsers)
        }

        // 给拥有者的客户端发送消息
        if (notifyData.notifyType == null ||
            notifyData.notifyType?.contains(RemoteDevNotifyType.CLIENT_PUSH) == true
        ) {
            workspace.forEach { ws ->
                notify4User(
                    userIds = permissionService.getWorkspaceOwner(ws.workspaceName).toSet(),
                    notifyType = setOf(RemoteDevNotifyType.CLIENT_PUSH),
                    bodyParams = mutableMapOf(
                        "operator" to userId,
                        "workspaceName" to ws.workspaceName,
                        "clientMsg" to messageContent,
                        "projectId" to ws.projectId
                    )
                )
            }
        }

        // 给所有云桌面的owner发送云桌面-跑马灯消息
        if (notifyData.notifyType == null ||
            notifyData.notifyType?.contains(RemoteDevNotifyType.DESKTOP_MARQUEE) == true
        ) {
            notify4User(
                userIds = userList,
                notifyType = mutableSetOf(RemoteDevNotifyType.DESKTOP_MARQUEE),
                bodyParams = mutableMapOf("operator" to userId, "messageContent" to messageContent)
            )
        }

        // 给所有云桌面的owner发送邮件
        if (notifyData.notifyType == null ||
            notifyData.notifyType?.contains(RemoteDevNotifyType.EMAIL) == true
        ) {
            notify4User(
                userIds = userList,
                notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL),
                bodyParams = mutableMapOf(
                    "operator" to userId,
                    "title" to notifyData.title,
                    "body" to (notifyData.desc ?: ""),
                    "notifyTemplateCode" to "REMOTEDEV_NOTIFY"
                )
            )
        }
    }

    fun notify4RemoteDevManager(
        projectId: String,
        cc: MutableSet<String>,
        notifyType: MutableSet<RemoteDevNotifyType>,
        bodyParams: MutableMap<String, String>
    ) {
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST
        )
        notify4User(
            userIds = projectInfo.properties?.remotedevManager?.split(";")?.toMutableSet()
                ?: mutableSetOf(),
            notifyType = notifyType,
            bodyParams = bodyParams,
            cc = cc
        )
    }

    fun notify4UserAndCCRemoteDevManagerAndCCShareUser(
        userIds: MutableSet<String>,
        workspaceName: String,
        cc: MutableSet<String>,
        projectId: String,
        notifyType: MutableSet<RemoteDevNotifyType>,
        bodyParams: MutableMap<String, String>
    ) {
        val shareUser = sharedDao.fetchWorkspaceSharedInfo(
            dslContext = dslContext,
            workspaceName = workspaceName,
            assignType = WorkspaceShared.AssignType.VIEWER
        )
        cc.addAll(shareUser.map { it.sharedUser })
        notify4UserAndCCRemoteDevManager(
            userIds = userIds,
            cc = cc,
            projectId = projectId,
            notifyType = notifyType,
            bodyParams = bodyParams
        )
    }

    fun notify4UserAndCCRemoteDevManager(
        userIds: MutableSet<String>,
        cc: MutableSet<String>,
        projectId: String?,
        notifyType: MutableSet<RemoteDevNotifyType>,
        bodyParams: MutableMap<String, String>
    ) {
        if (projectId != null) {
            val projectInfo = kotlin.runCatching {
                client.get(ServiceProjectResource::class).get(projectId)
            }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
                .getOrElse { null }?.data ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST
            )
            projectInfo.properties?.remotedevManager?.split(";")?.toMutableSet()?.let {
                cc.addAll(it)
            }
        }
        notify4User(
            userIds = userIds,
            notifyType = notifyType,
            bodyParams = bodyParams,
            cc = cc
        )
    }

    fun notify4User(
        userIds: Set<String>,
        notifyType: Set<RemoteDevNotifyType>,
        bodyParams: MutableMap<String, String>,
        cc: MutableSet<String> = mutableSetOf()
    ) {
        val taiUserNames = loadTaiUser(userIds, bodyParams)
        /* 发外部邮件，需要模板配置email_type=0*/
        if (notifyType.contains(RemoteDevNotifyType.EMAIL)) {
            kotlin.runCatching {
                notifyEmail(
                    taiUserNames = taiUserNames,
                    userIds = userIds,
                    bodyParams = bodyParams,
                    cc = cc
                )
            }.onFailure {
                logger.warn("notifyEmail fail", it)
            }
        }

        if (notifyType.contains(RemoteDevNotifyType.CLIENT_PUSH)) {
            kotlin.runCatching {
                notifyClient(
                    bodyParams = bodyParams,
                    userIds = userIds
                )
            }.onFailure {
                logger.warn("notifyClient fail", it)
            }
        }

        if (notifyType.contains(RemoteDevNotifyType.RTX)) {
            kotlin.runCatching {
                notifyRtx(
                    userIds = userIds,
                    bodyParams = bodyParams,
                    cc = cc
                )
            }.onFailure {
                logger.warn("notifyRtx fail", it)
            }
        }

        if (notifyType.contains(RemoteDevNotifyType.DESKTOP_MARQUEE)) {
            kotlin.runCatching {
                notifyDesktop(
                    userIds = userIds,
                    dataType = RemoteDevNotifyType.DESKTOP_MARQUEE,
                    bodyParams = bodyParams
                )
            }.onFailure {
                logger.warn("notifyDesktop fail", it)
            }
        }

        if (notifyType.contains(RemoteDevNotifyType.DESKTOP_COMPLEX)) {
            kotlin.runCatching {
                notifyDesktop(
                    userIds = userIds,
                    dataType = RemoteDevNotifyType.DESKTOP_COMPLEX,
                    bodyParams = bodyParams
                )
            }.onFailure {
                logger.warn("notifyDesktop fail", it)
            }
        }
    }

    private fun notifyDesktop(
        userIds: Set<String>,
        dataType: RemoteDevNotifyType,
        bodyParams: MutableMap<String, String>
    ) {
        logger.info("notify4User DESKTOP|$dataType|$userIds|$bodyParams")
        kotlin.runCatching {
            startWorkspaceService.sendMessage(
                operator = checkNotNull(bodyParams["operator"]),
                userIdList = userIds,
                dataType = dataType,
                messageContent = Base64.getEncoder().encodeToString(
                    checkNotNull(bodyParams["messageContent"]).toByteArray(StandardCharsets.UTF_8)
                ),
                messageStartTime = bodyParams["messageStartTime"]?.toLongOrNull() ?: LocalDateTime.now()
                    .timestampmilli(),
                messageEndTime = bodyParams["messageEndTime"]?.toLongOrNull() ?: LocalDateTime.now().plusDays(1)
                    .with(LocalTime.MIDNIGHT).timestampmilli()
            )
        }.onFailure {
            workspaceNotifyHistoryDao.add(
                dslContext = dslContext,
                operator = bodyParams["operator"] ?: "null",
                userIds = userIds.joinToString(),
                type = dataType,
                status = RemoteDevNotifyType.Status.FAIL,
                bodyParams = JsonUtil.toJson(bodyParams)
            )
        }.onSuccess {
            workspaceNotifyHistoryDao.add(
                dslContext = dslContext,
                operator = bodyParams["operator"] ?: "null",
                userIds = userIds.joinToString(),
                type = dataType,
                status = RemoteDevNotifyType.Status.SUCCESS,
                bodyParams = JsonUtil.toJson(bodyParams)
            )
        }
    }

    private fun notifyRtx(
        userIds: Set<String>,
        bodyParams: MutableMap<String, String>,
        cc: MutableSet<String>
    ) {
        val notifyTemplateCode = checkNotNull(bodyParams["notifyTemplateCode"])
        logger.info("notify4User RTX|$notifyTemplateCode|$userIds|$bodyParams")
        val receivers = userIds.plus(cc)
        kotlin.runCatching {
            sendNotifyMessageTemplateRequest(
                notifyTemplateCode = notifyTemplateCode,
                bodyParams = bodyParams,
                notifyType = mutableSetOf(NotifyType.RTX.name),
                receivers = receivers.toMutableSet(),
                markdownContent = false
            )
        }.onFailure {
            workspaceNotifyHistoryDao.add(
                dslContext = dslContext,
                operator = bodyParams["operator"] ?: "null",
                userIds = receivers.joinToString(),
                type = RemoteDevNotifyType.RTX,
                status = RemoteDevNotifyType.Status.FAIL,
                bodyParams = JsonUtil.toJson(bodyParams)
            )
        }.onSuccess {
            workspaceNotifyHistoryDao.add(
                dslContext = dslContext,
                operator = bodyParams["operator"] ?: "null",
                userIds = receivers.joinToString(),
                type = RemoteDevNotifyType.RTX,
                status = RemoteDevNotifyType.Status.SUCCESS,
                bodyParams = JsonUtil.toJson(bodyParams)
            )
        }
    }

    private fun notifyClient(
        bodyParams: MutableMap<String, String>,
        userIds: Set<String>
    ) {
        val notifyTemplateCode = bodyParams["notifyTemplateCode"]
        val res = bodyParams["clientMsg"]?.ifBlank { null } ?: getMsgFromTemplate(notifyTemplateCode, bodyParams)
        ?: kotlin.run {
            logger.warn("notifyClient fail with null body|$notifyTemplateCode")
            return
        }
        logger.info("notify4User CLIENT_PUSH|$notifyTemplateCode|$userIds|$res")
        val bodyJson = JsonUtil.toJson(bodyParams)
        userIds.forEach { user ->
            dispatchWebsocketPushEvent(
                userId = user,
                workspaceName = bodyParams["workspaceName"] ?: "",
                workspaceHost = null,
                errorMsg = res,
                type = WebSocketActionType.WORKSPACE_NOTIFY,
                status = true,
                action = WorkspaceAction.NOTIFY,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = null,
                ownerType = null,
                projectId = bodyParams["projectId"] ?: ""
            ) { result ->
                if (result) {
                    workspaceNotifyHistoryDao.add(
                        dslContext = dslContext,
                        operator = bodyParams["operator"] ?: "null",
                        userIds = user,
                        type = RemoteDevNotifyType.CLIENT_PUSH,
                        status = RemoteDevNotifyType.Status.SUCCESS,
                        bodyParams = bodyJson
                    )
                } else {
                    workspaceNotifyHistoryDao.add(
                        dslContext = dslContext,
                        operator = bodyParams["operator"] ?: "null",
                        userIds = user,
                        type = RemoteDevNotifyType.CLIENT_PUSH,
                        status = RemoteDevNotifyType.Status.FAIL,
                        bodyParams = bodyJson
                    )
                }
            }
        }
    }

    private fun notifyEmail(
        taiUserNames: Set<String>,
        userIds: Set<String>,
        bodyParams: MutableMap<String, String>,
        cc: MutableSet<String>
    ) {
        // 掉接口拿真正邮件地址
        val taiInfos = taiClient.taiUserInfo(
            TaiUserInfoRequest(usernames = taiUserNames)
        ).associateBy({
            it.username
        }, { user ->
            user.accountEmail
        })
        val receivers = userIds.map { taiInfos[it] ?: it }
        val notifyTemplateCode = checkNotNull(bodyParams["notifyTemplateCode"])
        logger.info("notify4User EMAIL|$notifyTemplateCode|$receivers|$bodyParams")
        kotlin.runCatching {
            sendNotifyMessageTemplateRequest(
                notifyTemplateCode = notifyTemplateCode,
                bodyParams = bodyParams,
                notifyType = mutableSetOf(NotifyType.EMAIL.name),
                receivers = receivers.toMutableSet(),
                cc = cc,
                markdownContent = false
            )
        }.onFailure {
            workspaceNotifyHistoryDao.add(
                dslContext = dslContext,
                operator = bodyParams["operator"] ?: "null",
                userIds = receivers.joinToString(),
                type = RemoteDevNotifyType.EMAIL,
                status = RemoteDevNotifyType.Status.FAIL,
                bodyParams = JsonUtil.toJson(bodyParams)
            )
        }.onSuccess {
            workspaceNotifyHistoryDao.add(
                dslContext = dslContext,
                operator = bodyParams["operator"] ?: "null",
                userIds = receivers.joinToString(),
                type = RemoteDevNotifyType.EMAIL,
                status = RemoteDevNotifyType.Status.SUCCESS,
                bodyParams = JsonUtil.toJson(bodyParams)
            )
        }
    }

    private fun getMsgFromTemplate(
        notifyTemplateCode: String?,
        bodyParams: MutableMap<String, String>
    ): String? {
        if (notifyTemplateCode.isNullOrBlank()) return null
        val request = NotifyMessageContextRequest(
            templateCode = notifyTemplateCode,
            notifyType = NotifyType.RTX,
            bodyParams = bodyParams
        )
        return kotlin.runCatching {
            client.get(ServiceNotifyMessageTemplateResource::class).getNotifyMessageByTemplate(request)
        }.onFailure {
            logger.warn("notify CLIENT_PUSH fail ${it.message}")
        }.getOrNull()?.data?.body
    }

    private fun loadTaiUser(
        userIds: Set<String>,
        bodyParams: MutableMap<String, String>
    ): Set<String> {
        val taiUserNames = userIds.filter { it.contains("@tai") }.toSet()
        val receiversNameWithCN = remoteDevSettingDao.fetchTaiUserInfo(dslContext, userIds = taiUserNames)
            .mapValues {
                if ((it.value["USER_NAME"] as String).isNotBlank()) {
                    "${it.value["USER_NAME"]}@${it.value["COMPANY_NAME"]}"
                } else {
                    it.key
                }
            }.values.plus(
                userIds.filter { !it.contains("@tai") }
            )
        bodyParams.putIfAbsent("receiversNameWithCN", receiversNameWithCN.joinToString())
        return taiUserNames
    }

    /*
     * 通知给系统运维人员
     * 方式是固定企微群
     */
    fun notify4SystemAdministrator(
        notifyTemplateCode: String,
        bodyParams: Map<String, String>
    ) {
        // 通知
        if (!weworkId.isNullOrBlank()) {
            kotlin.runCatching {
                sendNotifyMessageTemplateRequest(
                    notifyTemplateCode = notifyTemplateCode,
                    bodyParams = bodyParams.plus(
                        NotifyUtils.WEWORK_GROUP_KEY to weworkId!!
                    ),
                    notifyType = setOf(NotifyType.WEWORK_GROUP.name),
                    markdownContent = false
                )
            }
        }
    }

    // 封装统一分发WS的方法
    fun dispatchWebsocketPushEvent(
        userId: String,
        workspaceName: String,
        workspaceHost: String?,
        errorMsg: String? = null,
        type: WebSocketActionType,
        status: Boolean?,
        action: WorkspaceAction,
        systemType: WorkspaceSystemType? = null,
        workspaceMountType: WorkspaceMountType? = null,
        ownerType: WorkspaceOwnerType? = null,
        projectId: String = "",
        saveResult: (result: Boolean) -> Unit = {}
    ) {
        getWebSocketUsers(userId, workspaceName).parallelStream().forEach { user ->
            val push = WorkspaceWebsocketPush(
                type = type,
                status = status ?: true,
                anyMessage = WorkspaceResponse(
                    workspaceHost = workspaceHost ?: "",
                    workspaceName = workspaceName,
                    status = action,
                    errorMsg = errorMsg,
                    systemType = systemType,
                    workspaceMountType = workspaceMountType,
                    ownerType = ownerType,
                    projectId = projectId
                ),
                projectId = projectId,
                userId = user,
                redisOperation = redisOperation,
                page = WorkspacePageBuild.buildPage(user)
            )
            saveResult(push.findSession().isNotEmpty())
            webSocketDispatcher.dispatch(
                push
            )
        }
    }

    private fun getWebSocketUsers(operator: String, workspaceName: String): Set<String> {
        return if (operator == ADMIN_NAME) {
            val result = workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspaceName,
                assignType = WorkspaceShared.AssignType.OWNER
            )
            result.map { it.sharedUser }.toSet()
        } else {
            setOf(operator)
        }
    }

    private fun sendNotifyMessageTemplateRequest(
        notifyTemplateCode: String,
        bodyParams: Map<String, String>,
        notifyType: Set<String>,
        receivers: MutableSet<String> = mutableSetOf(),
        cc: MutableSet<String> = mutableSetOf(),
        markdownContent: Boolean = false
    ) {
        /*去掉 ADMIN_NAME 避免失误发送*/
        receivers.remove(ADMIN_NAME)
        cc.remove(ADMIN_NAME)
        val request = SendNotifyMessageTemplateRequest(
            templateCode = notifyTemplateCode,
            bodyParams = bodyParams,
            titleParams = bodyParams,
            notifyType = notifyType.toMutableSet(),
            markdownContent = markdownContent,
            receivers = receivers,
            cc = cc
        )
        kotlin.runCatching {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        }.onFailure {
            logger.warn("notify WINDOWS_GPU_SAFE_INIT_FAILED fail ${it.message}")
        }.getOrThrow()
    }

    fun fetchNotifyList(
        page: Int,
        pageSize: Int
    ): List<WorkspaceNotifyListData> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        return notifyDao.fetch(dslContext, sqlLimit).sortedByDescending { it.createdTime }.map {
            WorkspaceNotifyListData(
                projectId = it.projectIds.removeSurrounding("[", "]"),
                ip = it.ips.removeSurrounding("[", "]"),
                title = it.title,
                desc = it.desc,
                createTime = DateTimeUtil.toDateTime(it.createdTime as LocalDateTime),
                operator = it.operator
            )
        }
    }
}
