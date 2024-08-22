package com.tencent.devops.remotedev.service.expert

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceWorkspaceDispatchInterface
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.async.AsyncPipelineEvent
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Duration
import java.time.LocalDateTime

@Service
class ExpertSupportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val expertSupportDao: ExpertSupportDao,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val streamBridge: StreamBridge,
    private val notifyControl: NotifyControl,
    private val workspaceJoinDao: WorkspaceJoinDao
) {
    @Suppress("ComplexMethod")
    fun createSupport(
        userId: String,
        data: CreateSupportData
    ) {
        // 校验机器在不在
        val record = workspaceJoinDao.fetchAnyWindowsWorkspace(
            dslContext = dslContext,
            workspaceName = data.workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(data.workspaceName)
        )

        if (!permissionService.hasManagerOrViewerPermission(userId, record.projectId, record.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to apply for assistance in ${record.workspaceName}")
            )
        }
        if (record.status.checkDeleted() || record.status.checkInProcess()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                params = arrayOf(data.workspaceName)
            )
        }

        val fetchExpertSupportData = expertSupportDao.fetchSupports(
            dslContext = dslContext,
            projectId = data.projectId,
            hostIp = data.hostIp,
            creator = data.creator,
            status = ExpertSupportStatus.CREATE,
            content = data.content,
            internalTime = DEFAULT_WAIT_TIME
        )
        if (fetchExpertSupportData.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REAPPLY_EXPERT_SUPPORT_ERROR.errorCode,
                params = arrayOf(data.content)
            )
        }

        val id = expertSupportDao.addSupport(
            dslContext = dslContext,
            projectId = data.projectId,
            hostIp = data.hostIp,
            workspaceName = data.workspaceName,
            creator = data.creator,
            status = ExpertSupportStatus.CREATE,
            content = data.content,
            city = data.city,
            machineType = data.machineType
        )
        val taiUserCN = remoteDevSettingDao.fetchTaiUserInfo(dslContext, userIds = mutableSetOf(data.creator))
            .mapValues {
                if ((it.value["USER_NAME"] as String).isNotBlank()) {
                    Triple(
                        "${it.value["USER_NAME"]}@${it.value["COMPANY_NAME"]}",
                        it.value["PHONE"] as String,
                        it.value["PHONE_COUNTRY_CODE"] as String
                    )
                } else {
                    Triple(it.key, "", "")
                }
            }
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(data.projectId)
        }.onFailure { logger.warn("get project ${data.projectId} info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project ${data.projectId}", HTTP_400
        )

        // 异步执行流水线完成其他动作
        /**
         * redis 中，xxx 为需要替换
         * {
         *     "userId": "xxx",
         *     "projectId": "xxx",
         *     "pipelineId": "xxx",
         *     "buildParam": {
         *         "xxx": "ip",
         *         "xxx": "projectId",
         *         "xxx": "projectName",
         *         "xxx": "ticketId",
         *         "xxx": "creator",
         *         "xxx": "content",
         *         "xxx": "city",
         *         "xxx": "machineType"
         *     }
         * }
         */
        val infoS = redisOperation.get(PIPELINE_EXPORT_CONFIG_INFO) ?: return
        val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)
        val newParam = mutableMapOf<String, String>()
        val hostIdSub = data.hostIp.split(".")
        val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")

        // 获取请求来源ip
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val requestIp = attributes?.request?.getHeader("X-Forwarded-For")?.split(",")
            ?.firstOrNull { it.isNotBlank() }?.trim()

        info.buildParam.forEach { (k, v) ->
            when (v) {
                "ip" -> newParam[k] = record.regionId.toString().plus(":").plus(ip)
                "projectId" -> newParam[k] = data.projectId
                "projectName" -> newParam[k] = projectInfo.projectName
                "ticketId" -> newParam[k] = id.toString()
                "creator" -> newParam[k] = taiUserCN[data.creator]?.first ?: data.creator
                "content" -> newParam[k] = data.content
                "city" -> newParam[k] = data.city
                "machineType" -> newParam[k] = data.machineType
                "createTime" -> newParam[k] = DateTimeUtil.toDateTime(
                    LocalDateTime.now(), DateTimeUtil.YYYY_MM_DD_HH_MM_SS
                )

                "zone" -> newParam[k] = record.regionId.toString()
                "workspaceName" -> newParam[k] = data.workspaceName
                "phone" -> newParam[k] = taiUserCN[data.creator]?.second ?: ""
                "phoneCountryCode" -> newParam[k] = taiUserCN[data.creator]?.third ?: ""
                "taiUser" -> newParam[k] = if (UserUtil.isTaiUser(data.creator)) {
                    UserUtil.removeTaiSuffix(data.creator)
                } else {
                    ""
                }

                "managers" -> newParam[k] = projectInfo.properties?.remotedevManager ?: ""
                "requestIp" -> newParam[k] = requestIp ?: ""
                "displayName" -> newParam[k] = record.displayName

                else -> newParam[k] = v
            }
        }
        AsyncExecute.dispatch(
            streamBridge,
            AsyncPipelineEvent(
                userId = info.userId ?: "",
                projectId = info.projectId,
                pipelineId = info.pipelineId,
                values = newParam
            )
        )
    }

    fun updateSupportStatus(
        data: UpdateSupportData
    ) {
        expertSupportDao.updateSupport(
            dslContext = dslContext,
            id = data.id,
            status = data.status,
            supporter = if (data.supporter == null) {
                null
            } else {
                setOf(data.supporter!!)
            }
        )
    }

    fun fetchSupportConfig(
        type: ExpertSupportConfigType
    ): List<FetchExpertSupResp> {
        return expertSupportDao.fetchExpertSupportConfig(dslContext, type).map {
            FetchExpertSupResp(
                id = it.id,
                content = it.content
            )
        }
    }

    fun addSupportConfig(
        data: CreateExpertSupportConfigData
    ) {
        expertSupportDao.addExpertSupportConfig(dslContext, data.type, data.content)
    }

    fun deleteSupportConfig(
        id: Long
    ) {
        expertSupportDao.deleteExpertSupportConfig(dslContext, id)
    }

    fun assignExpSup(userId: String, id: Long, workspaceName: String): Pair<Boolean, String?> {
        // 校验这个人是不是可以分配的运维
        if (!expertSupportDao.fetchExpertSupportConfig(dslContext, ExpertSupportConfigType.SUPPORTER)
                .map { it.content.trim() }.toSet().contains(userId.trim())
        ) {
            return Pair(false, "${userId}不是云研发运维，不可认领")
        }

        // 校验 1 小时之内是否分配过
        if (workspaceSharedDao.checkAlreadyExpireShare(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = "system",
                sharedUser = userId,
                assignType = WorkspaceShared.AssignType.VIEWER
            )
        ) {
            return Pair(false, "${userId}已认领该工单")
        }

        // 校验求助单是否已过期：1小时
        if (Duration.between(
                expertSupportDao.getSup(dslContext, id)?.createTime ?: LocalDateTime.now(),
                LocalDateTime.now()
            ).seconds > DEFAULT_WAIT_TIME
        ) {
            return Pair(false, "单据[$id]已超过1小时过期")
        }

        // 校验机器在不在
        val record = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = workspaceName,
            mountType = WorkspaceMountType.START
        )
        if (record == null || record.status == WorkspaceStatus.DELETED) {
            return Pair(false, "云桌面${workspaceName}不存在或者已销毁")
        }

        // 分配
        workspaceCommon.shareWorkspace(
            workspaceName = workspaceName,
            projectId = record.projectId,
            operator = ADMIN_NAME,
            assigns = listOf(
                ProjectWorkspaceAssign(
                    userId = userId,
                    type = WorkspaceShared.AssignType.VIEWER,
                    expiration = LocalDateTime.now().plusHours(1)
                )
            ),
            mountType = WorkspaceMountType.START,
            ownerType = record.ownerType
        )

        // 添加认领人信息
        expertSupportDao.getSup(dslContext, id)?.let {
            val sups = mutableSetOf(userId)
            if (it.supporter != null) {
                sups.addAll(JsonUtil.to<List<String>>(it.supporter))
            }

            expertSupportDao.updateSupport(
                dslContext = dslContext,
                id = id,
                status = ExpertSupportStatus.RUNNING,
                supporter = sups
            )
        }

        return Pair(true, null)
    }

    fun queryCgsPwd(userId: String, cgsId: String): Pair<Boolean, String?> {
        // 校验这个人是不是可以分配的运维
        if (!expertSupportDao.fetchExpertSupportConfig(dslContext, ExpertSupportConfigType.SUPPORTER)
                .map { it.content.trim() }.toSet().contains(userId.trim())
        ) {
            return Pair(false, "${userId}不是云研发运维，不可查询")
        }
        try {
            val infoS = redisOperation.get(PIPELINE_QUERY_CGS_PWD) ?: return Pair(false, null)
            val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)

            val newParam = mutableMapOf<String, String>()
            val hostIdSub = cgsId.split(".")
            val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")
            info.buildParam.forEach { (k, v) ->
                when (v) {
                    "ip" -> newParam[k] = ip
                    "user" -> newParam[k] = userId
                    else -> newParam[k] = v
                }
            }

            client.get(ServiceBuildResource::class).manualStartupNew(
                userId = info.userId ?: "",
                projectId = info.projectId,
                pipelineId = info.pipelineId,
                values = newParam,
                channelCode = ChannelCode.BS,
                buildNo = null,
                startType = StartType.SERVICE
            )
        } catch (e: Exception) {
            logger.warn("execute createSupport pipeline error", e)
        }

        return Pair(true, "已发起查询，稍后通知密码")
    }

    fun fetchSupRecord(
        workspaceName: String,
        createLaterTimestamp: Long
    ): List<SupRecordData> {
        val records = expertSupportDao.fetchSupByWorkspaceName(
            dslContext = dslContext,
            workspaceName = workspaceName,
            createLaterTime = DateTimeUtil.convertTimestampToLocalDateTime(createLaterTimestamp)
        )
        return records.map {
            SupRecordData(
                id = it.id,
                createTime = it.createTime,
                content = it.content
            )
        }
    }

    fun deleteConfigWithData(
        data: CreateExpertSupportConfigData
    ) {
        expertSupportDao.deleteExpertSupportConfigWithData(dslContext, data.type, data.content)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_EXPAND_DISK,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_EXPAND_DISK_CONTENT
    )
    fun expandDisk(
        workspaceName: String,
        userId: String,
        size: String
    ): ExpandDiskValidateResp? {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )

        if (!permissionService.hasManagerOrOwnerPermission(userId, workspace.projectId, workspace.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to expand disk in $workspaceName")
            )
        }
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId

        // 暂时定死 mountType
        val data = SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java).expandDisk(
            workspaceName = workspaceName,
            userId = userId,
            size = size,
            mountType = WorkspaceMountType.START
        ).data ?: return null
        if (!data.valid) {
            return data
        }
        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            operator = userId,
            action = WorkspaceAction.EXPAND_DISK,
            actionMessage = size
        )
        return data
    }

    fun expandDiskCallback(
        taskId: String,
        workspaceName: String,
        operator: String
    ) {
        val taskInfo = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getTaskInfoByUid(taskId).data!!
        }.onFailure {
            logger.warn("expandDiskCallback not find uid $taskId")
            return
        }.getOrThrow()

        val owner = permissionService.getWorkspaceOwner(workspaceName)
        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName) ?: run {
            logger.warn("expandDiskCallback workspace is null $workspaceName")
            return
        }
        val projectId = workspace.projectId
        val cc = kotlin.runCatching {
            client.get(ServiceProjectResource::class)
                .listByProjectCodeList(listOf(projectId))
        }.onFailure {
            logger.warn("expandDiskCallback get project $projectId info error|${it.message}")
        }.getOrElse { null }?.data?.map {
            it.properties?.remotedevManager?.split(";")?.toSet() ?: emptySet()
        }?.flatten()?.toMutableSet() ?: mutableSetOf()
        cc.addAll(owner)

        val dSize = workspaceOpHistoryDao.fetchLastOp(
            dslContext = dslContext,
            workspaceName = workspaceName,
            action = WorkspaceAction.EXPAND_DISK
        )?.actionMsg ?: ""

        notifyControl.notify4UserAndCCRemoteDevManager(
            userIds = mutableSetOf(operator),
            cc = cc,
            projectId = null,
            notifyTemplateCode = "REMOTEDEV_EXPAND_DISK_DONE",
            notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL),
            bodyParams = mutableMapOf(
                "projectId" to projectId,
                "operator" to operator,
                "taskStatus" to (taskInfo.status?.name ?: ""),
                "taskLogs" to taskInfo.logs.joinToString(";"),
                "host" to (workspace.hostIp ?: ""),
                "dsize" to dSize
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExpertSupportService::class.java)
        private const val DEFAULT_WAIT_TIME = 3600
        private const val PIPELINE_EXPORT_CONFIG_INFO = "remotedev:createExpSupport.pipelineinfo"
        private const val PIPELINE_QUERY_CGS_PWD = "remotedev:queryCgsPwd.pipelineinfo"
    }
}
