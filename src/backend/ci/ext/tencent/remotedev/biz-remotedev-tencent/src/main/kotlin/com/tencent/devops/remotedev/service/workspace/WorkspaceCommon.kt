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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.FetchWinPoolData
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.CgsResourceConfig
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceKafkaInfo
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.SshPublicKeysService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OP_HISTORY_KEY_PREFIX
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.WINDOWS_GPU_OWNER_CHANGE_NOTIFY
import java.time.Duration
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    private val sshService: SshPublicKeysService,
    private val client: Client,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val redisCache: RedisCacheService,
    private val profile: Profile,
    @org.springframework.context.annotation.Lazy
    private val startControl: StartControl,
    @org.springframework.context.annotation.Lazy
    private val sleepControl: SleepControl,
    @org.springframework.context.annotation.Lazy
    private val deleteControl: DeleteControl,
    private val objectMapper: ObjectMapper,
    private val whiteListService: WhiteListService,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val notifyControl: NotifyControl,
    private val kafkaClient: KafkaClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceCommon::class.java)
        private const val DEFAULT_WAIT_TIME = 60
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

    fun getOrSaveWorkspaceDetail(
        workspaceName: String,
        mountType: WorkspaceMountType,
        event: RemoteDevUpdateEvent? = null
    ): WorkSpaceCacheInfo {
        return getWorkspaceDetail(workspaceName) ?: run {
            return updateWorkspaceDetail(workspaceName, mountType, event)
        }
    }

    fun updateWorkspaceDetail(
        workspaceName: String,
        mountType: WorkspaceMountType,
        event: RemoteDevUpdateEvent? = null
    ): WorkSpaceCacheInfo {
        logger.info("$workspaceName update workspaceDetail, $event")
        // START非create时不更新detail
        if (mountType == WorkspaceMountType.START && (event == null || event.type != UpdateEventType.CREATE)) {
            return JsonUtil.to(
                workspaceDao.getWorkspaceDetail(dslContext, workspaceName)?.detail ?: "",
                WorkSpaceCacheInfo::class.java
            )
        }

        val cache = if (mountType == WorkspaceMountType.START && event != null) {
            val workspaceInfo = client.get(ServiceRemoteDevResource::class)
                .getWorkspaceInfo(event.userId, workspaceName, mountType).data!!
            WorkSpaceCacheInfo(
                sshKey = "",
                environmentHost = event.environmentHost ?: "",
                hostIP = event.environmentIp ?: "",
                environmentIP = event.environmentIp ?: "",
                clusterId = "",
                namespace = workspaceInfo.namespace,
                curLaunchId = workspaceInfo.curLaunchId,
                regionId = workspaceInfo.regionId
            )
        } else {
            val userSet = workspaceDao.fetchWorkspaceUser(
                dslContext,
                workspaceName
            ).toSet()
            val sshKey = sshService.getSshPublicKeys4Ws(userSet)
            val workspaceInfo =
                client.get(ServiceRemoteDevResource::class)
                    .getWorkspaceInfo(userSet.first(), workspaceName, mountType).data!!
            WorkSpaceCacheInfo(
                sshKey = sshKey,
                environmentHost = workspaceInfo.environmentHost,
                hostIP = workspaceInfo.hostIP,
                environmentIP = workspaceInfo.environmentIP,
                clusterId = workspaceInfo.environmentIP,
                namespace = workspaceInfo.namespace,
                curLaunchId = workspaceInfo.curLaunchId,
                regionId = workspaceInfo.regionId
            )
        }

        workspaceDao.saveOrUpdateWorkspaceDetail(
            dslContext = dslContext,
            workspaceName = workspaceName,
            detail = JsonUtil.toJson(cache)
        )
        return cache
    }

    fun checkAndFixExceptionWS(
        status: WorkspaceStatus,
        userId: String,
        workspaceName: String,
        mountType: WorkspaceMountType
    ) {
        if (status.checkException()) {
            when (val fix = fixUnexpectedStatus(userId, workspaceName, status, mountType)) {
                WorkspaceStatus.EXCEPTION -> {
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
        workspaceDao.fetchWorkspace(
            dslContext, status = WorkspaceStatus.EXCEPTION
        )?.parallelStream()?.forEach {
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

    fun fixUnexpectedStatus(
        userId: String,
        workspaceName: String,
        status: WorkspaceStatus,
        mountType: WorkspaceMountType
    ): WorkspaceStatus {
        val workspaceInfo = kotlin.runCatching {
            client.get(ServiceRemoteDevResource::class)
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

            workspaceInfo.status == EnvStatusEnum.deleted -> {
                deleteControl.doDeleteWS(true, userId, workspaceName, workspaceInfo.environmentIP)
                return WorkspaceStatus.DELETED
            }

            workspaceInfo.status == EnvStatusEnum.running && workspaceInfo.started != false -> {
                startControl.doStartWS(true, userId, workspaceName, workspaceInfo.environmentHost)
                return WorkspaceStatus.RUNNING
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
    fun notOk2doNextAction(workspace: WorkspaceRecord): Boolean {
        return (
                workspace.status.notOk2doNextAction(workspace.workspaceSystemType) && Duration.between(
                    workspace.lastStatusUpdateTime ?: LocalDateTime.now(),
                    LocalDateTime.now()
                ).seconds < DEFAULT_WAIT_TIME
                ) ||
            workspace.status.checkDeleted() || workspace.status.workspaceInitializing() ||
            workspace.status.checkInProcess()
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
        workspace: WorkspaceRecord,
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
            workspaceDao.updateWorkspaceUsageTime(
                workspaceName = workspaceName,
                usageTime = Duration.between(
                    lastHistory.startTime, LocalDateTime.now()
                ).seconds.toInt(),
                dslContext = transactionContext
            )
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

        val projectId = when (workspaceOwnerType) {
            WorkspaceOwnerType.PERSONAL -> remoteDevSettingDao.fetchOneSetting(
                dslContext = dslContext,
                userId = creator
            ).projectId.ifBlank { null }

            WorkspaceOwnerType.PROJECT -> workspaceDao.fetchAnyWorkspace(
                dslContext = dslContext,
                workspaceName = workspaceName
            )?.projectId
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

    fun getSystemOperator(workspaceOwner: String, mountType: WorkspaceMountType): String =
        when (mountType) {
            WorkspaceMountType.START -> workspaceOwner
            else -> ADMIN_NAME
        }

    fun checkWorkspaceAvailability(
        userId: String,
        type: WorkspaceMountType,
        ownerType: WorkspaceOwnerType
    ) {
        when {
            type == WorkspaceMountType.START && ownerType == WorkspaceOwnerType.PERSONAL -> {
                val timeLeft = remoteDevSettingService.userWinTimeLeft(userId)
                if (timeLeft <= 0) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.WORKSPACE_UNAVAILABLE_WIN_GPU.errorCode
                    )
                }
            }

            else -> {}
        }
    }

    fun syncStartCloudResourceList(): List<EnvironmentResourceData> {
        return kotlin.runCatching {
            client.get(ServiceStartCloudResource::class)
                .syncStartCloudResourceList().data
        }.onFailure {
            logger.warn("Error syncing start cloud resource list: ${it.message}")
        }.getOrNull() ?: emptyList()
    }

    fun getWorkspaceDetail(workspaceName: String): WorkSpaceCacheInfo? {
        return try {
            val result = workspaceDao.getWorkspaceDetail(dslContext, workspaceName)?.detail
            if (result != null) {
                objectMapper.readValue<WorkSpaceCacheInfo>(result)
            } else {
                null
            }
        } catch (ignore: Exception) {
            logger.warn(
                "get workspace detail from redis error|$workspaceName",
                ignore
            )
            null
        }
    }

    fun getCgsData(
        cgsIds: List<String>?,
        ips: List<String>?
    ): List<EnvironmentResourceData>? {
        return kotlin.runCatching {
            client.get(ServiceStartCloudResource::class)
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

    // 获取cgs机型、区域
    fun getCgsConfig(): CgsResourceConfig {
        return kotlin.runCatching {
            client.get(ServiceStartCloudResource::class)
                .getCgsConfig().data
        }.onFailure {
            logger.warn("Error get cgs config: ${it.message}")
        }.getOrNull() ?: CgsResourceConfig(
            zoneList = emptyList(),
            machineTypeList = emptyList()
        )
    }

    fun shareWorkspace(
        workspaceName: String,
        projectId: String,
        operator: String,
        assigns: List<ProjectWorkspaceAssign>,
        mountType: WorkspaceMountType
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
            client.get(ServiceStartCloudResource::class)
                .shareWorkspace(
                    operator = operator,
                    cgsId = cgsId,
                    receivers = assigns.map { it.userId }
                ).data!!
        } else {
            ""
        }
        sharedDao.batchCreate(dslContext, workspaceName, operator, assigns, resourceId)
        assigns.forEach {
            // 没有注册setting就注册
            remoteDevSettingDao.fetchOneSetting(dslContext, it.userId)
            whiteListService.shareWorkspace(operator, it.userId)
            if (it.type == WorkspaceShared.AssignType.OWNER) {
                notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                    userIds = mutableSetOf(it.userId),
                    workspaceName = workspaceName,
                    cc = mutableSetOf(operator),
                    projectId = projectId,
                    notifyTemplateCode = WINDOWS_GPU_OWNER_CHANGE_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                    bodyParams = mutableMapOf(
                        "workspaceName" to workspaceName,
                        "cgsId" to cgsId,
                        "userId" to it.userId
                    )
                )
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
        if (mountType == WorkspaceMountType.START && checkUserNeedUnShare(unShareInfo, assignType)) {
            unShareInfo.groupBy { it.resourceId }.forEach { (resourceId, info) ->
                val receivers = info.map { it.sharedUser }
                logger.info("unShareWorkspace|$workspaceName|$operator|$receivers")
                kotlin.runCatching {
                    client.get(ServiceStartCloudResource::class)
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
    }

    private fun checkUserNeedUnShare(ws: List<WorkspaceShared>, assignType: WorkspaceShared.AssignType): Boolean {
        var res = false
        ws.forEach {
            if (it.type != assignType) {
                return false
            } else {
                res = true
            }
        }
        return res
    }

    fun genWorkspaceCCInfo(
        projectId: String
    ): Map<String, Any> {
        return mapOf("devx_meta" to JsonUtil.toJson(listOf(mapOf("projectId" to projectId)), formatted = false))
    }

    /*
     * 工作空间进入不使用状态，对数据进行统计和闭合处理
     * */
    fun statisticalData(
        workspace: WorkspaceRecord,
        operator: String
    ) {
        updateLastHistory(dslContext, workspace.workspaceName, operator)
        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            remoteDevSettingService.computeWinUsageTime(userId = workspace.createUserId)
        }

        // 个人云桌面即使关机也需要计费
        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU &&
            workspace.ownerType == WorkspaceOwnerType.PERSONAL
        ) {
            return
        }
        remoteDevBillingDao.endBilling(
            dslContext = dslContext,
            workspaceName = workspace.workspaceName,
            computeUsageTime = workspace.ownerType == WorkspaceOwnerType.PERSONAL
        )
    }

    // 按天备份数据
    fun backupDailyCsgData() {
        workspaceDao.backupDailyCsgData(dslContext)
    }

    fun updateStatus2DeliveringFailed(
        workspace: WorkspaceRecord,
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
                    "repoId" -> newParam[k] = REPOID ?: ""
                    "localDriver" -> newParam[k] = LOCALDRIVER ?: ""
                    else -> newParam[k] = v
                }
            }
            client.get(ServiceBuildResource::class).manualStartupNew(
                userId = info.userId ?: user,
                projectId = info.projectId,
                pipelineId = info.pipelineId,
                values = newParam,
                channelCode = ChannelCode.BS,
                buildNo = null,
                startType = StartType.SERVICE
            )
        } catch (e: Exception) {
            logger.warn("execute make disk mount pipeline error", e)
        }
    }
}
