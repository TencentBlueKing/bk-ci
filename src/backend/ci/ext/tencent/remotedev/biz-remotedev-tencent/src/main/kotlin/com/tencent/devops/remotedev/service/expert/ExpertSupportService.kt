package com.tencent.devops.remotedev.service.expert

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.TencentActionAuditContent
import com.tencent.devops.common.auth.api.TencentActionId
import com.tencent.devops.common.auth.api.TencentResourceTypeId
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
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
import com.tencent.devops.remotedev.dispatch.kubernetes.pojo.EnvironmentActionStatus
import com.tencent.devops.remotedev.dispatch.kubernetes.service.RemoteDevService
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.SupRecordInfo
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceShared.AssignType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.async.AsyncPipelineEvent
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskTaskDetail
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.remotedev.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.remotedev.VmDiskInfo
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.service.BKNodemanService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.client.StartCloudClient
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.PIPELINE_EXPORT_CONFIG_INFO
import com.tencent.devops.remotedev.service.redis.RedisKeys.PIPELINE_QUERY_CGS_PWD
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import java.time.Duration
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class ExpertSupportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val expertSupportDao: ExpertSupportDao,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceDao: WorkspaceDao,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val streamBridge: StreamBridge,
    private val notifyControl: NotifyControl,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val remoteDevService: RemoteDevService,
    private val startCloudClient: StartCloudClient,
    private val bkNodemanService: BKNodemanService,
    private val configCacheService: ConfigCacheService,
    private val remoteDevServiceFactory: RemoteDevServiceFactory
) {
    @Deprecated("等客户端版本都升级到支持createNew接口后，当前接口废弃")
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

        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val requestIp = attributes?.request?.getHeader("X-Forwarded-For")?.split(",")
            ?.firstOrNull { it.isNotBlank() }?.trim()
        val clientVersion = attributes?.request?.getHeader("Bk-Ci-Client-Version")
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(data.projectId)
        }.onFailure { logger.warn("get project ${data.projectId} info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project ${data.projectId}", HTTP_400
        )
        val owner: String?
        var viewers: Set<String>? = null
        if (record.ownerType == WorkspaceOwnerType.PERSONAL) {
            owner = record.createUserId
        } else {
            val sharedInfo = workspaceSharedDao.fetchWorkspaceSharedInfo(dslContext, data.workspaceName)
            owner = sharedInfo.firstOrNull { it.type == AssignType.OWNER }?.sharedUser
            viewers = sharedInfo.filter { it.type == AssignType.VIEWER }.map { it.sharedUser }.toSet().ifEmpty { null }
        }
        val projectManager = permissionService.managers(data.projectId).toSet()
        val recordInfo = SupRecordInfo(
            requestIp = requestIp,
            projectManager = projectManager,
            clientVersion = clientVersion,
            machineStatus = record.status.name,
            cdsVersion = null,
            cdsRegion = null,
            cdsStatus = null,
            cdsPort = null,
            agentStatus = null,
            owner = owner,
            viewers = viewers
        )

        val id = expertSupportDao.addSupport(
            dslContext = dslContext,
            projectId = data.projectId,
            hostIp = data.hostIp,
            workspaceName = data.workspaceName,
            creator = data.creator,
            status = ExpertSupportStatus.CREATE,
            content = data.content,
            city = data.city,
            machineType = data.machineType,
            info = recordInfo
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
        val infoS = configCacheService.get(PIPELINE_EXPORT_CONFIG_INFO) ?: return
        val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)
        val newParam = mutableMapOf<String, String>()
        val hostIdSub = data.hostIp.split(".")
        val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")

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

                "managers" -> newParam[k] = projectManager.joinToString(separator = ";")
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

    fun createSupportNew(
        userId: String,
        data: CreateSupportData
    ): Long {
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

        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val requestIp = attributes?.request?.getHeader("X-Forwarded-For")?.split(",")
            ?.firstOrNull { it.isNotBlank() }?.trim()
        val clientVersion = attributes?.request?.getHeader("Bk-Ci-Client-Version")
        val owner: String?
        var viewers: Set<String>? = null
        if (record.ownerType == WorkspaceOwnerType.PERSONAL) {
            owner = record.createUserId
        } else {
            val sharedInfo = workspaceSharedDao.fetchWorkspaceSharedInfo(dslContext, data.workspaceName)
            owner = sharedInfo.firstOrNull { it.type == AssignType.OWNER }?.sharedUser
            viewers = sharedInfo.filter { it.type == AssignType.VIEWER }.map { it.sharedUser }.toSet().ifEmpty { null }
        }

        val cgsStatus = try {
            startCloudClient.computerStatus(setOf(data.hostIp))?.firstOrNull()
        } catch (e: Exception) {
            logger.warn("createSupportNew computerStatus error", e)
            null
        }

        val agentStatus = if (record.regionId != null) {
            bkNodemanService.ipchooserHostDetail(data.hostIp.substringAfter("."), record.regionId!!)
        } else {
            logger.warn("createSupportNew ${data.workspaceName} regionId is null")
            null
        }

        val info = SupRecordInfo(
            requestIp = requestIp,
            projectManager = permissionService.managers(data.projectId).toSet(),
            clientVersion = clientVersion,
            machineStatus = record.status.name,
            cdsVersion = cgsStatus?.cgsVersion,
            cdsRegion = data.hostIp.split(".").first(),
            cdsStatus = cgsStatus?.state?.toString(),
            cdsPort = CGS_PORT,
            agentStatus = agentStatus?.alive.toString(),
            owner = owner,
            viewers = viewers
        )

        val id = expertSupportDao.addSupport(
            dslContext = dslContext,
            projectId = data.projectId,
            hostIp = data.hostIp,
            workspaceName = data.workspaceName,
            creator = data.creator,
            status = ExpertSupportStatus.CREATE,
            content = data.content,
            city = data.city,
            machineType = data.machineType,
            info = info
        )
        return id
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
            val infoS = configCacheService.get(PIPELINE_QUERY_CGS_PWD) ?: return Pair(false, null)
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

    @Deprecated("未来fetch_expert_sup_record_any使用会把这个接口废弃")
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
            val recordInfo = if (it.info == null) {
                null
            } else {
                JsonUtil.to(it.info.data(), object : TypeReference<SupRecordInfo>() {})
            }
            SupRecordData(
                id = it.id,
                createTime = it.createTime,
                content = it.content,
                requestIp = recordInfo?.requestIp,
                hostIp = it.hostIp,
                projectId = it.projectId,
                projectManager = recordInfo?.projectManager,
                machineType = it.machineType,
                city = it.city,
                clientVersion = recordInfo?.clientVersion,
                machineStatus = recordInfo?.machineStatus,
                cdsVersion = recordInfo?.cdsVersion,
                cdsRegion = recordInfo?.cdsRegion,
                cdsStatus = recordInfo?.cdsStatus,
                cdsPort = recordInfo?.cdsPort,
                agentStatus = recordInfo?.agentStatus,
                owner = recordInfo?.owner,
                viewers = recordInfo?.viewers,
                loginName = it.creator
            )
        }
    }

    fun fetchSupRecordAny(
        id: Long
    ): SupRecordData? {
        val record = expertSupportDao.getSup(dslContext, id) ?: return null
        val recordInfo = if (record.info == null) {
            null
        } else {
            JsonUtil.to(record.info.data(), object : TypeReference<SupRecordInfo>() {})
        }
        return SupRecordData(
            id = record.id,
            createTime = record.createTime,
            content = record.content,
            requestIp = recordInfo?.requestIp,
            hostIp = record.hostIp,
            projectId = record.projectId,
            projectManager = recordInfo?.projectManager,
            machineType = record.machineType,
            city = record.city,
            clientVersion = recordInfo?.clientVersion,
            machineStatus = recordInfo?.machineStatus,
            cdsVersion = recordInfo?.cdsVersion,
            cdsRegion = recordInfo?.cdsRegion,
            cdsStatus = recordInfo?.cdsStatus,
            cdsPort = recordInfo?.cdsPort,
            agentStatus = recordInfo?.agentStatus,
            owner = recordInfo?.owner,
            viewers = recordInfo?.viewers,
            loginName = record.creator
        )
    }

    fun deleteConfigWithData(
        data: CreateExpertSupportConfigData
    ) {
        expertSupportDao.deleteExpertSupportConfigWithData(dslContext, data.type, data.content)
    }

    @ActionAuditRecord(
        actionId = TencentActionId.CGS_EXPAND_DISK,
        instance = AuditInstanceRecord(
            resourceType = TencentResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = TencentActionAuditContent.CGS_EXPAND_DISK_CONTENT
    )
    fun expandDisk(
        workspaceName: String,
        userId: String,
        size: String,
        pvcId: String?
    ): ExpandDiskValidateResp? {
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
                params = arrayOf("You do not have permission to expand disk in $workspaceName")
            )
        }
        ActionAuditContext.current()
            .addAttribute(TencentActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId

        // 暂时定死 mountType
        val data = SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java).expandDisk(
            workspaceName = workspaceName,
            userId = userId,
            size = size,
            pvcId,
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
            actionMessage = if (pvcId != null) {
                "$pvcId: $size"
            } else {
                size
            }
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

        val cc = permissionService.getWorkspaceOwner(workspaceName).toMutableSet()
        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName) ?: run {
            logger.warn("expandDiskCallback workspace is null $workspaceName")
            return
        }
        cc.addAll(permissionService.managers(workspace.projectId))

        val dSize = workspaceOpHistoryDao.fetchLastOp(
            dslContext = dslContext,
            workspaceName = workspaceName,
            action = WorkspaceAction.EXPAND_DISK
        )?.actionMsg ?: ""

        notifyControl.notify4UserAndCCRemoteDevManager(
            userIds = mutableSetOf(operator),
            cc = cc,
            projectId = null,
            notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL),
            bodyParams = mutableMapOf(
                "projectId" to workspace.projectId,
                "operator" to operator,
                "taskStatus" to (taskInfo.status?.name ?: ""),
                "taskLogs" to taskInfo.logs.joinToString(";"),
                "host" to (workspace.hostIp ?: ""),
                "notifyTemplateCode" to "REMOTEDEV_EXPAND_DISK_DONE",
                "dsize" to dSize
            )
        )
    }

    fun expandDiskDetail(workspaceName: String): ExpandDiskTaskDetail? {
        val record =
            workspaceOpHistoryDao.fetchLastOp(dslContext, workspaceName, WorkspaceAction.EXPAND_DISK) ?: return null
        val (status, updateTime) = remoteDevService.getLastExpandDiskStatusAndTime(workspaceName)

        val rStatus = if (status != null) {
            when (status) {
                EnvironmentActionStatus.PENDING -> "RUNNING"
                EnvironmentActionStatus.SUCCEEDED -> "SUCCEEDED"
                else -> "FAILED"
            }
        } else {
            "UNKNOW"
        }
        return ExpandDiskTaskDetail(
            expandSize = record.actionMsg,
            operator = record.operator,
            operateDate = record.createdTime,
            status = rStatus,
            completeDate = if (rStatus == "FAILED" || rStatus == "SUCCEEDED") {
                updateTime
            } else {
                null
            }
        )
    }

    @ActionAuditRecord(
        actionId = TencentActionId.CGS_CREATE_DISK,
        instance = AuditInstanceRecord(
            resourceType = TencentResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        // TODO: 未来挪了之后需要改成CREATE
        content = TencentActionAuditContent.CGS_EXPAND_DISK_CONTENT
    )
    fun createDisk(
        workspaceName: String,
        userId: String,
        size: String
    ): CreateDiskResp {
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
                params = arrayOf("You do not have permission to expand disk in $workspaceName")
            )
        }
        ActionAuditContext.current()
            .addAttribute(TencentActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId

        // 暂时定死 mountType
        val data = remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).createDisk(
            workspaceName = workspaceName,
            userId = userId,
            size = size
        )

        workspaceOpHistoryDao.createWorkspaceHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            operator = userId,
            action = WorkspaceAction.CREATE_DISK,
            actionMessage = size
        )
        return data
    }

    fun createDiskCallback(
        taskId: String,
        workspaceName: String,
        operator: String
    ) {
        val taskInfo = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getTaskInfoByUid(taskId).data!!
        }.onFailure {
            logger.warn("createDiskCallback not find uid $taskId")
            return
        }.getOrThrow()

        val cc = permissionService.getWorkspaceOwner(workspaceName).toMutableSet()
        val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName) ?: run {
            logger.warn("createDiskCallback workspace is null $workspaceName")
            return
        }
        cc.addAll(permissionService.managers(workspace.projectId))

        val dSize = workspaceOpHistoryDao.fetchLastOp(
            dslContext = dslContext,
            workspaceName = workspaceName,
            action = WorkspaceAction.CREATE_DISK
        )?.actionMsg ?: ""

        notifyControl.notify4UserAndCCRemoteDevManager(
            userIds = mutableSetOf(operator),
            cc = cc,
            projectId = null,
            notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL),
            bodyParams = mutableMapOf(
                "projectId" to workspace.projectId,
                "operator" to operator,
                "taskStatus" to (taskInfo.status?.name ?: ""),
                "taskLogs" to taskInfo.logs.joinToString(";"),
                "host" to (workspace.hostIp ?: ""),
                "notifyTemplateCode" to "REMOTEDEV_CREATE_DISK_DONE",
                "dsize" to dSize
            )
        )
    }

    fun fetchDiskList(
        userId: String,
        workspaceName: String
    ): List<VmDiskInfo> {
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
                params = arrayOf("You do not have permission to expand disk in $workspaceName")
            )
        }

        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).fetchDiskList(
            workspaceName = workspaceName,
            userId = userId
        )
    }

    fun getTaskStatus(
        userId: String,
        taskId: String
    ): WorkspaceTaskStatus? {
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).taskStatus(taskId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExpertSupportService::class.java)
        private const val DEFAULT_WAIT_TIME = 3600
        private const val CGS_PORT = "10080"
    }
}
