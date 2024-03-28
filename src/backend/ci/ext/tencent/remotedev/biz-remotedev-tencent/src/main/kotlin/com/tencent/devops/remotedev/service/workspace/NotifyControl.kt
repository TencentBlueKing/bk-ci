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
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.NotifyMessageContextRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ProjectNotifyDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyListData
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencent.devops.remotedev.websocket.page.WorkspacePageBuild
import com.tencent.devops.remotedev.websocket.push.WorkspaceWebsocketPush
import java.time.LocalDateTime
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
    private val workspaceDao: WorkspaceDao,
    private val redisOperation: RedisOperation,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val taiClient: TaiClient,
    private val notifyDao: ProjectNotifyDao,
    private val sharedDao: WorkspaceSharedDao
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

        /*云桌面处于待分配超过3天的自动回收，并邮件提醒	*/
        const val NOT_ASSIGN_AUTO_DELETE_NOTIFY = "NOT_ASSIGN_AUTO_DELETE_NOTIFY"

        /*云桌面通知-关机超过7天时自动销毁*/
        const val SLEEP_7_DAY_AUTO_DELETE_NOTIFY = "SLEEP_7_DAY_AUTO_DELETE_NOTIFY"

        /*云桌面通知-关机超过3天时提醒*/
        const val SLEEP_3_DAY_NOTIFY = "SLEEP_3_DAY_NOTIFY"

        /*云桌面通知-未登录7天时自动降配并关机*/
        const val NOT_LOGIN_AUTO_SLEEP_NOTIFY = "NOT_LOGIN_AUTO_SLEEP_NOTIFY"

        /*云桌面通知-未登录3天时提醒*/
        const val NOT_LOGIN_NOTIFY = "NOT_LOGIN_NOTIFY"

        /*云桌面通知-您的云桌面已被强制销毁*/
        const val WORKSPACE_FORCE_DELETE = "WORKSPACE_FORCE_DELETE"

        /*云桌面通知-未达到云桌面4星活跃自动销毁*/
        const val NOT_4_STAR_ACTIVE_AUTO_DELETE_NOTIFY = "NOT_4_STAR_ACTIVE_AUTO_DELETE_NOTIFY"

        /*云桌面通知-您的云桌面已被强制批量销毁*/
        const val WORKSPACE_BATCH_FORCE_DELETE = "WORKSPACE_BATCH_FORCE_DELETE"

        /*云桌面通知-云桌面连续14天活跃度不足通知*/
        const val NOT_ACTIVE_IN_14_DAYS_NOTIFY = "NOT_ACTIVE_IN_14_DAYS_NOTIFY"
    }

    fun notifyWorkspaceInfo(
        userId: String,
        notifyData: WorkspaceNotifyData
    ) {
        val workspace = workspaceDao.fetchNotifyWorkspaces(
            dslContext = dslContext,
            mountType = WorkspaceMountType.START,
            ips = notifyData.ip?.toSet(),
            projectIds = notifyData.projectId?.toSet()
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(notifyData.ip?.joinToString(";") ?: "")
        )

        // 分发到WS
        workspace.forEach { ws ->
            dispatchWebsocketPushEvent(
                userId = ADMIN_NAME,
                workspaceName = ws["NAME"] as String,
                workspaceHost = null,
                errorMsg = "${notifyData.title}\n${notifyData.desc}",
                type = WebSocketActionType.WORKSPACE_NOTIFY,
                status = true,
                action = WorkspaceAction.NOTIFY,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = null,
                ownerType = null,
                projectId = ws["PROJECT_ID"] as String
            )
        }
        notifyDao.add(dslContext, userId, notifyData)
    }

    fun notify4RemoteDevManager(
        projectId: String,
        cc: MutableSet<String>,
        notifyTemplateCode: String,
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
            notifyTemplateCode = notifyTemplateCode,
            notifyType = notifyType,
            bodyParams = bodyParams,
            cc = cc
        )
    }

    fun notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
        userIds: MutableSet<String>,
        workspaceName: String,
        cc: MutableSet<String>,
        projectId: String,
        notifyTemplateCode: String,
        notifyType: MutableSet<RemoteDevNotifyType>,
        bodyParams: MutableMap<String, String>
    ) {
        val shareUser = sharedDao.fetchWorkspaceSharedInfo(
            dslContext = dslContext,
            workspaceName = workspaceName,
            assignType = WorkspaceShared.AssignType.OWNER
        )
        cc.addAll(shareUser.map { it.operator })
        notify4UserAndCCRemoteDevManager(
            userIds = userIds,
            cc = cc,
            projectId = projectId,
            notifyTemplateCode = notifyTemplateCode,
            notifyType = notifyType,
            bodyParams = bodyParams
        )
    }

    fun notify4UserAndCCRemoteDevManager(
        userIds: MutableSet<String>,
        cc: MutableSet<String>,
        projectId: String?,
        notifyTemplateCode: String,
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
            notifyTemplateCode = notifyTemplateCode,
            notifyType = notifyType,
            bodyParams = bodyParams,
            cc = cc
        )
    }

    fun notify4User(
        userIds: MutableSet<String>,
        notifyTemplateCode: String,
        notifyType: MutableSet<RemoteDevNotifyType>,
        bodyParams: MutableMap<String, String>,
        cc: MutableSet<String> = mutableSetOf()
    ) {
        val taiUserNames = userIds.filter { it.contains("@tai") }.toSet()
        val receiversNameWithCN = remoteDevSettingDao.fetchTaiUserInfo(dslContext, userIds = taiUserNames)
            .mapValues {
                if (it.value.first.isNotBlank()) {
                    "${it.value.first}@${it.value.second}"
                } else it.key
            }.values.plus(
                userIds.filter { !it.contains("@tai") }
            )
        bodyParams.putIfAbsent("receiversNameWithCN", receiversNameWithCN.joinToString())
        /* 发外部邮件，需要模板配置email_type=0*/
        if (notifyType.contains(RemoteDevNotifyType.EMAIL)) {
            // 掉接口拿真正邮件地址
            val taiInfos = taiClient.taiUserInfo(
                TaiUserInfoRequest(usernames = taiUserNames)
            ).associateBy({
                it.username
            }, { user ->
                user.accountEmail
            })
            val receivers = userIds.map { taiInfos[it] ?: it }
            logger.info("notify4User EMAIL|$notifyTemplateCode|$receivers|$bodyParams")
            sendNotifyMessageTemplateRequest(
                notifyTemplateCode = notifyTemplateCode,
                bodyParams = bodyParams,
                notifyType = mutableSetOf(NotifyType.EMAIL.name),
                receivers = receivers.toMutableSet(),
                cc = cc,
                markdownContent = false
            )
        }

        if (notifyType.contains(RemoteDevNotifyType.CLIENT_PUSH)) {
            val request = NotifyMessageContextRequest(
                templateCode = notifyTemplateCode,
                notifyType = NotifyType.RTX,
                bodyParams = bodyParams
            )
            val res = kotlin.runCatching {
                client.get(ServiceNotifyMessageTemplateResource::class).getNotifyMessageByTemplate(request)
            }.onFailure {
                logger.warn("notify CLIENT_PUSH fail ${it.message}")
            }.getOrNull()?.data?.body ?: kotlin.run {
                logger.warn("notify CLIENT_PUSH fail with null body|$notifyTemplateCode")
                return
            }
            logger.info("notify4User CLIENT_PUSH|$notifyTemplateCode|$userIds|$res")
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
                    ownerType = null
                )
            }
        }

        if (notifyType.contains(RemoteDevNotifyType.RTX)) {
            logger.info("notify4User RTX|$notifyTemplateCode|$userIds|$bodyParams")
            sendNotifyMessageTemplateRequest(
                notifyTemplateCode = notifyTemplateCode,
                bodyParams = bodyParams,
                notifyType = mutableSetOf(NotifyType.RTX.name),
                receivers = userIds.plus(cc).toMutableSet(),
                markdownContent = false
            )
        }
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
        projectId: String = ""
    ) {
        webSocketDispatcher.dispatch(
            WorkspaceWebsocketPush(
                type = type,
                status = status ?: true,
                anyMessage = WorkspaceResponse(
                    workspaceHost = workspaceHost ?: "",
                    workspaceName = workspaceName,
                    status = action,
                    errorMsg = errorMsg,
                    systemType = systemType,
                    workspaceMountType = workspaceMountType,
                    ownerType = ownerType
                ),
                projectId = projectId,
                userIds = getWebSocketUsers(userId, workspaceName),
                redisOperation = redisOperation,
                page = WorkspacePageBuild.buildPage(workspaceName),
                notifyPost = NotifyPost(
                    module = "remotedev",
                    level = NotityLevel.LOW_LEVEL.getLevel(),
                    message = "",
                    dealUrl = null,
                    code = 200,
                    webSocketType = "IFRAME",
                    page = WorkspacePageBuild.buildPage(workspaceName)
                )
            )
        )
    }

    private fun getWebSocketUsers(operator: String, workspaceName: String): Set<String> {
        return if (operator == ADMIN_NAME) {
            val result = workspaceDao.fetchWorkspaceWithOwner(
                dslContext = dslContext,
                workspaceName = workspaceName,
                assignType = WorkspaceShared.AssignType.OWNER
            ) ?: emptyList()
            result.map { it["SHARED_USER"] as String }.toSet()
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
        }
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
