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
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.api.devx.ServiceDEVXResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.RemoteDevCommonConfig
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDailyCgsdataDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceWorkspaceDispatchInterface
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceKafkaInfo
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInf
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.async.AsyncPipelineEvent
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.remotedev.pojo.remotedev.FetchWinPoolData
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.service.BKCCService
import com.tencent.devops.remotedev.service.RemotedevProjectService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OP_HISTORY_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.WINDOWS_GPU_OWNER_CHANGE_NOTIFY
import java.time.Duration
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class WorkspaceCommon @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val sharedDao: WorkspaceSharedDao,
    private val client: Client,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val redisCache: RedisCacheService,
    private val profile: Profile,
    @Lazy
    private val startControl: StartControl,
    @Lazy
    private val sleepControl: SleepControl,
    @Lazy
    private val deleteControl: DeleteControl,
    private val whiteListService: WhiteListService,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val notifyControl: NotifyControl,
    private val kafkaClient: KafkaClient,
    private val bkccService: BKCCService,
    private val remotedevProjectService: RemotedevProjectService,
    private val projectStartAppLinkDao: ProjectStartAppLinkDao,
    private val config: RemoteDevCommonConfig,
    private val streamBridge: StreamBridge,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceDailyCgsdataDao: WorkspaceDailyCgsdataDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCommon::class.java)
        const val DEFAULT_WAIT_TIME = 300
        private const val REPOID = "lsync"
        private const val LOCALDRIVER = "L"
        private const val PIPELINE_CONFIG_INFO = "remotedev:assignWorkspace.pipelineinfo"
    }

    @Value("\${spring.kafka.topics.cgsInfoTopic:#{null}}")
    val buildCommitsTopic: String? = null

    fun getOpHistory(key: OpHistoryCopyWriting) =
        redisCache.get(REDIS_OP_HISTORY_KEY_PREFIX + key.name)?.ifBlank {
            key.default
        } ?: key.default

    fun updateWorkspaceWinDetail(
        ws: WorkspaceRecord?,
        workspaceName: String
    ) {
        val workspace =
            ws ?: workspaceDao.fetchAnyWorkspace(dslContext = dslContext, workspaceName = workspaceName) ?: return
        val gameId = getGameIdAndAppId(workspace.projectId, workspace.ownerType)
        val workspaceInfo = SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java)
            .getWorkspaceInfo(workspace.createUserId, workspaceName, workspace.workspaceMountType).data!!
        workspaceWindowsDao.updateDetailInfo(
            dslContext = dslContext,
            launchId = gameId.second.toInt(),
            regionId = workspaceInfo.regionId,
            workspaceName = workspaceName
        )
    }

    fun checkAndFixExceptionWS(
        status: WorkspaceStatus,
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType
    ) {
        if (status.checkException()) {
            val fix = fixUnexpectedStatus(userId, workspaceName, status, mountType)
            when {
                fix.checkException() -> {
                    logger.info("$workspaceName is EXCEPTION and not repaired, return error.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_ERROR.errorCode
                    )
                }

                else -> {
                    logger.info("$workspaceName is $status to $fix , return info.")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_ERROR_FIX.errorCode,
                        params = arrayOf(fix.name)
                    )
                }
            }
        }
    }

    // 尝试修复异常工作空间状态
    fun fixUnexpectedWorkspace() {
        logger.info("fixUnexpectedWorkspace")
        workspaceDao.fetchErrorWorkspace(dslContext)?.parallelStream()?.forEach {
            MDC.put(TraceTag.BIZID, TraceTag.buildBiz())
            logger.info(
                "workspace ${it.workspaceName} is EXCEPTION, try to fix."
            )
            if (!checkProjectRouter(
                    creator = it.createUserId,
                    workspaceName = it.workspaceName,
                    workspaceOwnerType = it.ownerType
                )
            ) {
                return@forEach
            }
            fixUnexpectedStatus(
                userId = ADMIN_NAME,
                workspaceName = it.workspaceName,
                status = it.status,
                mountType = it.workspaceMountType
            )
        }
    }

    @Suppress("ComplexMethod")
    fun fixUnexpectedStatus(
        userId: String,
        workspaceName: String,
        status: WorkspaceStatus,
        mountType: WorkspaceMountType
    ): WorkspaceStatus {
        val workspaceInfo = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java)
                .getWorkspaceInfo(userId, workspaceName, mountType).data!!
        }.getOrElse { ignore ->
            logger.warn(
                "get workspace info error $workspaceName|${ignore.message}"
            )
            workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION)
            return WorkspaceStatus.EXCEPTION
        }
        logger.info("fixUnexpectedStatus|$workspaceName|$status|$workspaceInfo")
        when {
            workspaceInfo.status == EnvStatusEnum.readyToRun || workspaceInfo.status == EnvStatusEnum.stopped -> {
                sleepControl.doStopWS(true, userId, workspaceName)
                return WorkspaceStatus.STOPPED
            }

            workspaceInfo.status == EnvStatusEnum.deleted || workspaceInfo.status == EnvStatusEnum.readyDelete -> {
                deleteControl.doDeleteWS(true, userId, workspaceName, workspaceInfo.environmentIP)
                return WorkspaceStatus.DELETED
            }

            workspaceInfo.status == EnvStatusEnum.restarting -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.RESTARTING)
                return WorkspaceStatus.RESTARTING
            }

            workspaceInfo.status == EnvStatusEnum.starting -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.STARTING)
                return WorkspaceStatus.STARTING
            }

            workspaceInfo.status == EnvStatusEnum.stopping -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.STOPPING)
                return WorkspaceStatus.STOPPING
            }

            workspaceInfo.status == EnvStatusEnum.rebuilding -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.REBUILDING)
                return WorkspaceStatus.REBUILDING
            }

            workspaceInfo.status == EnvStatusEnum.upgrading -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.UPGRADING)
                return WorkspaceStatus.UPGRADING
            }

            workspaceInfo.status == EnvStatusEnum.copying -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.MAKING_IMAGE)
                return WorkspaceStatus.MAKING_IMAGE
            }

            workspaceInfo.status == EnvStatusEnum.running && workspaceInfo.started != false -> {
                startControl.doStartWS(true, userId, workspaceName, workspaceInfo.environmentHost)
                return WorkspaceStatus.RUNNING
            }

            workspaceInfo.status == EnvStatusEnum.startFailed -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION_START_FAILED)
                return WorkspaceStatus.EXCEPTION_START_FAILED
            }

            workspaceInfo.status == EnvStatusEnum.stopFailed -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION_STOP_FAILED)
                return WorkspaceStatus.EXCEPTION_STOP_FAILED
            }

            workspaceInfo.status == EnvStatusEnum.abnormalAfterRunning -> {
                workspaceDao.updateWorkspaceStatus(
                    dslContext = dslContext,
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.EXCEPTION_ABNORMAL_AFTER_RUNNING
                )
                return WorkspaceStatus.EXCEPTION_ABNORMAL_AFTER_RUNNING
            }

            workspaceInfo.status == EnvStatusEnum.abnormalAfterReady -> {
                workspaceDao.updateWorkspaceStatus(
                    dslContext = dslContext,
                    workspaceName = workspaceName,
                    status = WorkspaceStatus.EXCEPTION_ABNORMAL_AFTER_READY
                )
                return WorkspaceStatus.EXCEPTION_ABNORMAL_AFTER_READY
            }

            workspaceInfo.status == EnvStatusEnum.createFailed -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION_CREATE_FAILED)
                return WorkspaceStatus.EXCEPTION_CREATE_FAILED
            }

            workspaceInfo.status == EnvStatusEnum.cloning -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.CLONING)
                return WorkspaceStatus.CLONING
            }

            workspaceInfo.status == EnvStatusEnum.unknow -> {
                workspaceDao.updateWorkspaceStatus(dslContext, workspaceName, WorkspaceStatus.EXCEPTION)
                return WorkspaceStatus.EXCEPTION
            }

            else -> logger.warn(
                "wait workspace change over $DEFAULT_WAIT_TIME second |" +
                    "$workspaceName|${workspaceInfo.status}"
            )
        }
        return status
    }

    /**
     * workspace 正在变更状态时，不能新建任务去执行。但如果超过 60s 便不做该限制。 以免因下游某服务节点故障状态未闭环回传导致问题。
     * 如果已经销毁，直接返回false
     */
    fun notOk2doNextAction(workspace: WorkspaceRecordInf): Boolean {
        return (
            workspace.status.notOk2doNextAction() && Duration.between(
                workspace.lastStatusUpdateTime ?: LocalDateTime.now(),
                LocalDateTime.now()
            ).seconds < DEFAULT_WAIT_TIME
            ) ||
            workspace.status.checkDeleted() || workspace.status.workspaceInitializing() ||
            workspace.status.checkInProcess() || workspace.status.checkUnused()
    }

    fun updateStatusAndCreateHistory(
        workspaceName: String,
        newStatus: WorkspaceStatus,
        action: WorkspaceAction
    ) {
        logger.info("updateStatusAndCreateHistory|$workspaceName|$newStatus|$action")
        workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)?.let {
            updateStatusAndCreateHistory(
                it, newStatus, action
            )
        }
    }

    fun updateStatusAndCreateHistory(
        workspace: WorkspaceRecordInf,
        newStatus: WorkspaceStatus,
        action: WorkspaceAction
    ) {
        logger.info(
            "updateStatusAndCreateHistory|workspace|$workspace|oldStatus|${workspace.status}" +
                "newStatus|$newStatus|action|$action"
        )
        workspaceDao.updateWorkspaceStatus(
            dslContext = dslContext,
            workspaceName = workspace.workspaceName,
            status = newStatus
        )
        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspace.workspaceName,
            operator = workspace.createUserId,
            action = action,
            actionMessage = String.format(
                getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                workspace.status.name,
                newStatus.name
            )
        )
    }

    fun updateLastHistory(
        transactionContext: DSLContext,
        workspaceName: String,
        operator: String
    ) {
        val lastHistory = workspaceHistoryDao.fetchAnyHistory(
            dslContext = transactionContext,
            workspaceName = workspaceName
        )
        if (lastHistory?.startTime != null) {
            workspaceHistoryDao.updateWorkspaceHistory(
                dslContext = transactionContext,
                id = lastHistory.id,
                stopUserId = operator
            )
        } else {
            logger.error("$workspaceName get last history info null")
        }
    }

    fun checkProjectRouter(
        creator: String,
        workspaceName: String,
        workspaceOwnerType: WorkspaceOwnerType
    ): Boolean {
        if (profile.isDebug()) return true

        val projectId = when {
            workspaceOwnerType.projectUse() -> workspaceDao.fetchAnyWorkspace(
                dslContext = dslContext,
                workspaceName = workspaceName
            )?.projectId

            else -> remoteDevSettingDao.fetchOneSetting(
                dslContext = dslContext,
                userId = creator
            ).projectId.ifBlank { null }
        } ?: run {
            logger.info("$workspaceName creator not init setting, ignore it.")
            return false
        }

        val projectRouterTagCheck =
            client.get(ServiceProjectTagResource::class).checkProjectRouter(projectId).data
        if (!projectRouterTagCheck!!) {
            logger.info("project $projectId router tag is not this cluster")
            return false
        }
        return true
    }

    fun realtimeStartCloudResourceList(): List<EnvironmentResourceData> {
        return kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
                .realtimeStartCloudResourceList().data
        }.onFailure {
            logger.warn("Error syncing start cloud resource list: ${it.message}")
        }.getOrNull() ?: emptyList()
    }

    fun getCgsData(
        cgsIds: List<String>?,
        ips: List<String>?
    ): List<EnvironmentResourceData>? {
        return kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
                .getCgsData(FetchWinPoolData(cgsIds = cgsIds, ips = ips)).data
        }.onFailure {
            logger.warn("Error syncing start cloud resource list: ${it.message}")
        }.getOrNull()
    }

    /**判断是否已有存在的云桌面归属在项目下
     * true:表示存在
     * false:表示不存在
     */

    fun checkCgsRunning(cgsId: String): Boolean {
        return workspaceDao.getAvailableCgsWorkspace(
            dslContext = dslContext,
            cgsId = cgsId
        ) > 0
    }

    /**
     * 团队项目可以在创建时指定owner，将会自动挂载owner
     *
     */
    fun autoAssignOwner(
        ws: WorkspaceRecordInf
    ) {
        if (!ws.ownerType.projectUse()) return
        val owners = sharedDao.fetchWorkspaceOwner(dslContext, setOf(ws.workspaceName)).values
        if (owners.isEmpty()) return
        shareWorkspace(
            workspaceName = ws.workspaceName,
            projectId = ws.projectId,
            operator = ws.createUserId,
            assigns = owners.map {
                ProjectWorkspaceAssign(
                    userId = it,
                    type = WorkspaceShared.AssignType.OWNER,
                    expiration = null
                )
            },
            mountType = WorkspaceMountType.START,
            ownerType = ws.ownerType
        )
        workspaceDao.updateWorkspaceStatus(
            dslContext = dslContext,
            workspaceName = ws.workspaceName,
            status = WorkspaceStatus.RUNNING
        )
    }

    fun shareWorkspace(
        workspaceName: String,
        projectId: String,
        operator: String,
        assigns: List<ProjectWorkspaceAssign>,
        mountType: WorkspaceMountType,
        ownerType: WorkspaceOwnerType,
        notify: Boolean = true
    ) {
        // 获取workspaceName对应的cgsId
        val cgsId = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)?.hostIp
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(
                    workspaceName,
                    "cgsIp is null"
                )
            )

        val resourceId = if (mountType == WorkspaceMountType.START) {
            val gameId = getGameIdAndAppId(projectId, ownerType)
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
                .shareWorkspace(
                    operator = operator,
                    cgsId = cgsId,
                    receivers = assigns.map { it.userId },
                    gameId = gameId.first
                ).data!!
        } else {
            ""
        }
        sharedDao.batchCreate(dslContext, workspaceName, operator, assigns, resourceId)
        if (notify) {
            assigns.forEach {
                // 没有注册setting就注册
                remoteDevSettingDao.fetchOneSetting(dslContext, it.userId)
                whiteListService.shareWorkspace(operator, it.userId)
                if (it.type == WorkspaceShared.AssignType.OWNER) {
                    notifyControl.notify4UserAndCCRemoteDevManagerAndCCShareUser(
                        userIds = mutableSetOf(it.userId),
                        workspaceName = workspaceName,
                        cc = mutableSetOf(operator),
                        projectId = projectId,
                        notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                        bodyParams = mutableMapOf(
                            "workspaceName" to workspaceName,
                            "cgsId" to cgsId,
                            "notifyTemplateCode" to WINDOWS_GPU_OWNER_CHANGE_NOTIFY,
                            "userId" to it.userId
                        )
                    )
                    // 分配拥有者后触发L盘挂载
                    makeDiskMount(cgsId.substringAfter("."), operator)
                }
                notifyControl.dispatchWebsocketPushEvent(
                    userId = it.userId,
                    workspaceName = workspaceName,
                    workspaceHost = null,
                    errorMsg = null,
                    type = WebSocketActionType.WORKSPACE_ASSIGN,
                    status = true,
                    action = WorkspaceAction.ASSIGN,
                    systemType = null,
                    workspaceMountType = mountType,
                    ownerType = null,
                    projectId = ""
                )
            }
        }
    }

    fun removeUserWorkspaceShare(
        operator: String,
        userId: String
    ) {
        val records = workspaceJoinDao.fetchWorkspaceFromUser(dslContext, userId).ifEmpty { return }
        records.forEach { (workspaceName, status, assignType) ->
            unShareWorkspace(
                workspaceName = workspaceName,
                operator = operator,
                sharedUsers = listOf(userId),
                mountType = null,
                assignType = assignType,
                forceDelete = true
            )
            // 是OWNER进入待分配状态
            if (assignType == WorkspaceShared.AssignType.OWNER) {
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    val toStatus = WorkspaceStatus.DISTRIBUTING
                    workspaceDao.updateWorkspaceStatus(
                        workspaceName = workspaceName,
                        status = toStatus,
                        dslContext = transactionContext
                    )
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceName = workspaceName,
                        operator = operator,
                        action = WorkspaceAction.SYSTEM_CHANGES,
                        actionMessage = String.format(
                            getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                            status.name,
                            toStatus.name
                        )
                    )
                }
            }
        }
    }

    fun unShareWorkspace(
        workspaceName: String,
        operator: String,
        sharedUsers: List<String>,
        mountType: WorkspaceMountType?,
        assignType: WorkspaceShared.AssignType = WorkspaceShared.AssignType.VIEWER,
        forceDelete: Boolean = false
    ) {
        val unShareInfo = sharedDao.fetchWorkspaceSharedInfo(
            dslContext = dslContext,
            workspaceName = workspaceName,
            sharedUsers = sharedUsers
        )
        if (mountType == WorkspaceMountType.START && unShareInfo.find { it.type != assignType } == null) {
            unShareInfo.groupBy { it.resourceId }.forEach { (resourceId, info) ->
                val receivers = info.map { it.sharedUser }
                logger.info("unShareWorkspace|$workspaceName|$operator|$receivers")
                kotlin.runCatching {
                    SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
                        .unShareWorkspace(
                            operator = operator, resourceId = resourceId, receivers = receivers
                        ).data!!
                }.onFailure {
                    if (!forceDelete) throw it
                }.getOrNull()
            }
        }
        sharedDao.batchDelete(
            dslContext = dslContext,
            workspaceName = workspaceName,
            sharedUsers = sharedUsers,
            assignType = assignType
        )
        // 解绑后对原先的共享人推送websocket刷新客户端列表
        sharedUsers.forEach {
            notifyControl.dispatchWebsocketPushEvent(
                userId = it,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_ASSIGN,
                status = true,
                action = WorkspaceAction.ASSIGN,
                systemType = null,
                workspaceMountType = mountType,
                ownerType = null,
                projectId = ""
            )
        }
    }

    fun genWorkspaceCCInfo(
        projectId: String,
        workspaceName: String,
        owner: String?
    ): Map<String, Any> {
        return mapOf(
            "devx_meta" to JsonUtil.toJson(
                listOf(
                    mapOf(
                        "projectId" to projectId,
                        "workspaceName" to workspaceName,
                        "owner" to (owner ?: "")
                    )
                ),
                formatted = false
            )
        )
    }

    /*
     * 工作空间进入不使用状态，对数据进行统计和闭合处理
     * */
    fun statisticalData(
        workspace: WorkspaceRecord,
        operator: String
    ) {
        updateLastHistory(dslContext, workspace.workspaceName, operator)
        // 个人云桌面即使关机也需要计费
        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU &&
            workspace.ownerType == WorkspaceOwnerType.PERSONAL
        ) {
            return
        }
    }

    // 按天备份数据
    fun backupDailyCsgData() {
        workspaceDailyCgsdataDao.backupDailyCsgData(dslContext)
    }

    fun updateStatus2DeliveringFailed(
        workspace: WorkspaceRecordInf,
        action: WorkspaceAction,
        notifyTemplateCode: String,
        noticeParams: Map<String, String> = emptyMap()
    ) {
        updateStatusAndCreateHistory(
            workspace = workspace,
            newStatus = WorkspaceStatus.DELIVERING_FAILED,
            action = action
        )
        // 通知
        notifyControl.notify4SystemAdministrator(
            notifyTemplateCode,
            mapOf(
                WorkspaceRecord::workspaceName.name to workspace.workspaceName,
                WorkspaceRecord::projectId.name to workspace.projectId,
                WorkspaceRecord::createUserId.name to workspace.createUserId
            ).plus(noticeParams)
        )
    }

    // 云桌面删除成功后往kafka发送消息
    fun sendCgsInfo2Kafka(workspaceKafkaInfo: WorkspaceKafkaInfo) {
        if (buildCommitsTopic.isNullOrBlank()) return
        logger.info("sendCgsInfo2Kafka|workspaceKafkaInfo|{}", workspaceKafkaInfo)
        kotlin.runCatching {
            kafkaClient.send(
                buildCommitsTopic!!,
                JsonUtil.toJson(
                    workspaceKafkaInfo
                )
            )
        }.onFailure {
            logger.warn("send cgs info 2 kafka fail")
        }
    }

    // 创建实例成功后做异步设置，包含L盘挂载
    fun makeDiskMount(ip: String, user: String) {
        try {
            val infoS = redisOperation.get(PIPELINE_CONFIG_INFO) ?: return
            val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)
            val resIps = mutableSetOf<String>()
            resIps.add(ip)
            val newParam = mutableMapOf<String, String>()
            info.buildParam.forEach { (k, v) ->
                when (v) {
                    "job_ip_list" -> newParam[k] = resIps.joinToString(separator = " ")
                    "repoId" -> newParam[k] = REPOID
                    "localDriver" -> newParam[k] = LOCALDRIVER
                    else -> newParam[k] = v
                }
            }
            AsyncExecute.dispatch(
                streamBridge,
                AsyncPipelineEvent(
                    userId = info.userId ?: user,
                    projectId = info.projectId,
                    pipelineId = info.pipelineId,
                    values = newParam
                )
            )
        } catch (e: Exception) {
            logger.warn("execute make disk mount pipeline error", e)
        }
    }

    fun updateHostMonitor(workspaceName: String, props: Map<String, Any>, type: WorkspaceSystemType) {
        if (!type.checkWindows()) {
            return
        }
        val detail = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName) ?: return
        val regId = detail.regionId ?: run {
            logger.warn("update $workspaceName but regionid is null")
            return
        }
        val ip = detail.hostIp?.substringAfter(".") ?: run {
            logger.warn("update $workspaceName but hostIp is null")
            return
        }
        updateHostMonitor(regId, ip, props, type)
    }

    fun updateHostMonitor(regionId: Int, ip: String, props: Map<String, Any>, type: WorkspaceSystemType) {
        if (!type.checkWindows()) {
            return
        }
        bkccService.updateHostMonitor(
            regionId = regionId,
            ip = ip,
            props = props
        )
    }

    fun getGameIdAndAppId(projectId: String?, ownerType: WorkspaceOwnerType): Pair<String, Long> {
        if (projectId.isNullOrBlank() || ownerType == WorkspaceOwnerType.PERSONAL) {
            return config.devcouldAppName to config.devcouldCurLaunchId
        }
        return projectStartAppLinkDao.getAppId(dslContext, projectId)?.let { projectId to it } ?: kotlin.run {
            remotedevProjectService.migrateOldData(projectId)
            checkNotNull(projectStartAppLinkDao.getAppId(dslContext, projectId)?.let { projectId to it })
        }
    }

    fun devxEnvNodeInit(
        userId: String,
        projectId: String,
        workspaceName: String,
        ip: String,
        size: String
    ): Boolean {
        val nodeId = client.get(ServiceDEVXResource::class).createNode(
            userId = userId, projectId = projectId, workspaceName = workspaceName, ip = ip, size = size
        ).data ?: return false
        workspaceWindowsDao.updateNodeHashId(dslContext, HashUtil.encodeLongId(nodeId), workspaceName)
        return true
    }

    fun devxEnvNodeDel(
        userId: String,
        workspaceName: String
    ): Boolean {
        logger.info("$userId del devx env node|$workspaceName")
        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        if (workspace.nodeHashId == null) {
            logger.info("ignore del devx env node|$workspaceName")
            return true
        }
        val ok = client.get(ServiceNodeResource::class).deleteNodes(
            userId = workspace.createUserId,
            projectId = workspace.projectId,
            nodeHashIds = listOf(checkNotNull(workspace.nodeHashId))
        ).data ?: return false
        if (ok) {
            workspaceWindowsDao.updateNodeHashId(dslContext, null, workspaceName)
        }
        return true
    }
}
