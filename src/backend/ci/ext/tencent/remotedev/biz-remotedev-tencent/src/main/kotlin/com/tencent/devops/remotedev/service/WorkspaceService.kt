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

package com.tencent.devops.remotedev.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.auth.api.service.ServiceMonitorSpaceResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.devx.ServiceDEVXResource
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCode.NOT_FIND_NODE_FOR_ENV_ID
import com.tencent.devops.remotedev.common.exception.ErrorCode.NOT_FIND_USER_ENV_FOR_ENV_ID
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WindowsGpuResourceDao
import com.tencent.devops.remotedev.dao.WindowsResourceTypeDao
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectAccessDevicePermissionsResp
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceEnv
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInf
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.expert.FetchSupportResp
import com.tencent.devops.remotedev.pojo.project.DepartmentsInfo
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.RemotedevProjectNew
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.tai.Moa2faReqData
import com.tencent.devops.remotedev.pojo.tai.Moa2faRespData
import com.tencent.devops.remotedev.pojo.tai.Moa2faVerifyReqData
import com.tencent.devops.remotedev.pojo.tai.Moa2faVerifyRespData
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusEnum
import com.tencent.devops.remotedev.service.client.StartCloudClient
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.tai.TaiService
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon.Companion.DEFAULT_WAIT_TIME
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.max
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
@Suppress("ALL")
class WorkspaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceResourceTypeDao: WindowsResourceTypeDao,
    private val workspaceResourceZoneDao: WindowsResourceZoneDao,
    private val permissionService: PermissionService,
    private val client: Client,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val expertSupportDao: ExpertSupportDao,
    private val apiGwService: ApiGwService,
    private val startWorkspaceService: StartWorkspaceService,
    private val taiClient: TaiClient,
    private val taiService: TaiService,
    private val startCloudClient: StartCloudClient,
    private val windowsGpuResourceDao: WindowsGpuResourceDao
) {
    @Value("\${remoteDev.projectMonitorUrl:}")
    val projectMonitorUrl = ""

    @ActionAuditRecord(
        actionId = ActionId.CGS_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_EDIT_CONTENT
    )
    @Deprecated("不要新增功能，希望废弃该方法")
    // 修改workspace备注名称
    fun editWorkspace(
        userId: String,
        workspaceName: String,
        displayName: String,
        checkPermission: Boolean = true
    ): Boolean {
        logger.info("$userId edit workspace $workspaceName|$displayName")

        val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, ws.projectId)
            .scopeId = ws.projectId

        if (checkPermission && !permissionService.hasUserManager(userId, ws.projectId)) {
            permissionService.checkViewerPermission(userId, workspaceName, ws.projectId)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceDao.updateWorkspaceDisplayName(
                dslContext = transactionContext,
                workspaceName = workspaceName,
                displayName = displayName
            )
        }
        return true
    }

    fun modifyWorkspaceProperty(
        userId: String,
        workspaceName: String?,
        ip: String?,
        workspaceProperty: WorkspaceProperty
    ): Boolean {
        logger.info("$userId modify workspace property $workspaceName|$workspaceProperty")

        val ws = when {
            workspaceName != null -> workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ip != null -> workspaceJoinDao.fetchWindowsWorkspacesSimple(
                dslContext, sip = ip, notStatus = listOf(
                WorkspaceStatus.PREPARING,
                WorkspaceStatus.DELETED,
                WorkspaceStatus.DELIVERING_FAILED
            ),
                checkField = listOf(TWorkspace.T_WORKSPACE.PROJECT_ID)
            ).firstOrNull()

            else -> throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf("null workspaceName and null ip")
            )
        } ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName ?: ip ?: "unknown")
        )
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, ws.projectId)
            .scopeId = ws.projectId

        if (!permissionService.hasManagerOrViewerPermission(userId, ws.projectId, ws.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to modify $workspaceName property")
            )
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceDao.modifyWorkspaceProperty(
                dslContext = transactionContext,
                projectId = ws.projectId,
                workspaceName = ws.workspaceName,
                workspaceProperty = workspaceProperty
            )
        }
        return true
    }

    @AuditEntry(actionId = ActionId.CGS_SHARE)
    fun shareWorkspace4OP(
        userId: String,
        shareWorkspace: ShareWorkspace
    ): Boolean {
        if (shareWorkspace.sharedUser.isEmpty()) return false
        when (shareWorkspace.opType) {
            ShareWorkspace.OpType.ADD -> shareWorkspace.sharedUser.forEach { user ->
                shareWorkspace(
                    userId = userId,
                    workspaceName = shareWorkspace.workspaceName,
                    sharedUser = user,
                    needPermission = false
                )
            }

            ShareWorkspace.OpType.DELETE -> shareWorkspace.sharedUser.forEach { user ->
                deleteSharedWorkspace(
                    workspaceName = shareWorkspace.workspaceName,
                    sharedUser = user
                )
            }

            else -> Unit
        }

        return true
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_SHARE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_SHARE_CONTENT
    )
    fun shareWorkspace(
        userId: String,
        workspaceName: String,
        sharedUser: String,
        needPermission: Boolean = true
    ): Boolean {
        logger.info("$userId share workspace $workspaceName|$sharedUser")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId

        if (needPermission) {
            permissionService.checkOwnerPermission(userId, workspaceName, workspace.projectId, workspace.ownerType)
        }

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:shareWorkspace:${workspaceName}_$sharedUser",
            expiredTimeInSeconds
        ).tryLock().use {

            // 共享时创建START云桌面的用户
            if (workspace.workspaceMountType == WorkspaceMountType.START) {
                val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)
                SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
                    .createStartCloudUser(sharedUser, gameId.first)
            }
            if (workspaceSharedDao.existWorkspaceSharedInfo(workspaceName, sharedUser, dslContext)) {
                logger.info("$workspaceName has already shared to $sharedUser")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_SHARE_FAIL.errorCode,
                    params = arrayOf("$workspaceName has already shared to $sharedUser")
                )
            }

            workspaceCommon.shareWorkspace(
                workspaceName = workspaceName,
                projectId = workspace.projectId,
                operator = userId,
                assigns = listOf(ProjectWorkspaceAssign(sharedUser, WorkspaceShared.AssignType.VIEWER, null)),
                mountType = workspace.workspaceMountType,
                ownerType = workspace.ownerType
            )
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.SHARE,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.SHARE),
                    sharedUser
                )
            )
            return true
        }
    }

    fun getProjectWorkspaceList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        search: WorkspaceSearch?
    ): Page<ProjectWorkspace> {
        logger.info("$userId get project $projectId workspace list")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val theSearch = search?.apply {
            this.projectId = listOf(projectId)
        } ?: WorkspaceSearch(
            projectId = listOf(projectId)
        )
        val count = workspaceJoinDao.countProjectWorkspace(
            dslContext = dslContext,
            queryType = QueryType.WEB,
            search = theSearch
        )
        val result = limitFetchProjectWorkspace(
            queryType = QueryType.WEB,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull),
            search = theSearch
        )

        val records = parseWorkspaceList(userId = userId, result = result, enableExportSup = false, expertSupId = null)

        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = count, records = records)
    }

    fun getProjectWorkspaceList4Op(
        userId: String,
        data: ProjectWorkspaceFetchData
    ): Page<ProjectWorkspace> {
        logger.info("op get project ${data.projectId} workspace list {}", data)
        val pageNotNull = data.page ?: 1
        val pageSizeNotNull = data.pageSize ?: 6666
        val fastSelect = data.ips?.find { it -> it.any { it in 'A'..'Z' } } == null
        val search = with(data) {
            WorkspaceSearch(
                projectId = projectId?.let { listOf(it) },
                workspaceName = workspaceName?.let { listOf(it) },
                workspaceSystemType = systemType?.let { listOf(it) },
                ips = if (!fastSelect) ips else null,
                sips = if (fastSelect) ips?.map { it.removeSuffix("$") } else null,
                owner = owners?.toList() ?: owner?.let { listOf(it) },
                status = status?.let { listOf(it) },
                zoneShortName = zoneId?.let { listOf(it) },
                size = machineType?.let { listOf(it) },
                expertSupId = expertSupId?.let { listOf(it) },
                onFuzzyMatch = true
            )
        }
        val count = workspaceJoinDao.countProjectWorkspace(
            dslContext = dslContext,
            queryType = QueryType.OP,
            search = search
        )
        val result = limitFetchProjectWorkspace(
            queryType = QueryType.OP,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull),
            search = search
        )

        val records = parseWorkspaceList(
            userId = userId,
            result = result,
            enableExportSup = true,
            expertSupId = data.expertSupId
        )

        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = count, records = records)
    }

    // 拆出来 fetch 方便同步修改
    fun limitFetchProjectWorkspace(
        limit: SQLLimit,
        queryType: QueryType,
        search: WorkspaceSearch
    ): List<WorkspaceRecordInf> {
        return workspaceJoinDao.limitFetchProjectWorkspace(
            dslContext = dslContext,
            queryType = queryType,
            limit = limit,
            search = search
        ) ?: emptyList()
    }

    fun parseWorkspaceList(
        userId: String,
        result: List<WorkspaceRecordInf>,
        enableExportSup: Boolean,
        expertSupId: Long?
    ): List<ProjectWorkspace> {
        val owners = mutableMapOf<String, String>()
        val viewers = mutableMapOf<String, MutableList<String>>()
        val taiUsers = mutableSetOf<String>()
        val workspaceNames = result.map { it.workspaceName }.toSet()

        workspaceSharedDao.batchFetchWorkspaceSharedInfo(dslContext, workspaceNames).forEach {
            when (it.type) {
                WorkspaceShared.AssignType.OWNER -> {
                    owners.putIfAbsent(it.workspaceName, it.sharedUser)
                }

                WorkspaceShared.AssignType.VIEWER -> {
                    viewers.putIfAbsent(it.workspaceName, mutableListOf(it.sharedUser))?.add(it.sharedUser)
                }
            }
            if (UserUtil.isTaiUser(it.sharedUser)) {
                taiUsers.add(it.sharedUser)
            }
        }

        val allConfig = windowsResourceConfigService.getAllType(true, null).associateBy { it.id!! }
        val defaultZoneConfig = windowsResourceConfigService.getAllZone().associateBy { it.zoneShortName }
        val specZoneConfig = windowsResourceConfigService.getAllSpecZone().associateBy { it.zoneShortName }
        val taiUserCN = remoteDevSettingDao.fetchTaiUserInfo(dslContext, userIds = taiUsers)
            .mapValues {
                if ((it.value["USER_NAME"] as String).isNotBlank()) {
                    "${it.value["USER_NAME"]}@${it.value["COMPANY_NAME"]}"
                } else it.key
            }

        val allWindows = workspaceWindowsDao.batchFetchWorkspaceWindowsInfo(
            dslContext,
            result.filter { it.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU }.map { it.workspaceName }.toSet()
        ).associateBy { it.workspaceName }

        // 专家协助
        var expertMap: MutableMap<String, MutableList<FetchSupportResp>>? = null
        if (enableExportSup) {
            expertMap = mutableMapOf()
            if (expertSupId != null) {
                listOf(expertSupportDao.getSup(dslContext, expertSupId))
            } else {
                expertSupportDao.fetchSupByWorkspaceNames(dslContext, workspaceNames)
            }.filterNotNull().forEach {
                val resp = FetchSupportResp(
                    id = it.id,
                    creator = it.creator,
                    content = it.content,
                    createTime = it.createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))
                )
                if (expertMap[it.workspaceName] != null) {
                    expertMap[it.workspaceName]?.add(resp)
                } else {
                    expertMap[it.workspaceName] = mutableListOf(resp)
                }
            }
        }

        val windowsWorkspaces = result.filterIsInstance<WorkspaceRecordWithWindows>().associateBy { it.workspaceName }
        val loginUserMap =
            startWorkspaceService.cachingLoginUsers(windowsWorkspaces.map { it.value.hostIp ?: "" }.toSet())

        val records = mutableListOf<ProjectWorkspace>()
        result.forEach {
            val detail = windowsWorkspaces[it.workspaceName]
            val status = checkStatus(it, userId)
            records.add(
                ProjectWorkspace(
                    workspaceId = it.workspaceId,
                    workspaceName = it.workspaceName,
                    projectId = it.projectId,
                    displayName = it.displayName,
                    status = status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime?.timestamp(),
                    sleepingTime = if (status.checkSleeping()) {
                        it.lastStatusUpdateTime?.timestamp()
                    } else {
                        null
                    },
                    createUserId = it.createUserId,
                    hostName = detail?.hostIp,
                    workspaceMountType = it.workspaceMountType,
                    workspaceSystemType = it.workspaceSystemType,
                    winConfig = allWindows[it.workspaceName]?.let { i -> allConfig[i.winConfigId.toLong()] },
                    zoneConfig = allWindows[it.workspaceName]?.let { i ->
                        specZoneConfig[i.zoneId] ?: defaultZoneConfig[i.zoneId.removeSuffixNumb()]
                    },
                    owner = owners[it.workspaceName],
                    viewers = viewers[it.workspaceName],
                    ownerCN = taiUserCN[owners[it.workspaceName]] ?: owners[it.workspaceName],
                    viewersCN = viewers[it.workspaceName]?.map { viewer ->
                        taiUserCN[viewer] ?: viewer
                    },
                    currentLoginUsers = detail?.hostIp?.let { ip -> loginUserMap[ip] } ?: emptyList(),
                    ownerType = it.ownerType,
                    expertSupportList = expertMap?.get(it.workspaceName),
                    macAddress = allWindows[it.workspaceName]?.macAddress,
                    remark = it.remark,
                    labels = it.labels,
                    createTime = it.createTime.timestamp(),
                    imageId = detail?.imageId ?: "",
                    recordEnabled = !allWindows[it.workspaceName]?.enableRecordUser.isNullOrBlank()
                )
            )
        }
        return records
    }

    fun getWorkspaceList4WeSec(
        projectId: String? = null,
        ip: String? = null,
        hasDepartmentsInfo: Boolean? = null,
        hasCurrentUser: Boolean? = null,
        businessLineName: String? = null,
        ownerName: String? = null,
        workspaceName: String? = null,
        notStatus: List<WorkspaceStatus>? = listOf(
            WorkspaceStatus.DELETED,
            WorkspaceStatus.PREPARING,
            WorkspaceStatus.DELIVERING_FAILED
        )
    ): List<WeSecProjectWorkspace> {
        val startTime = System.currentTimeMillis()
        val owners = mutableMapOf<String, String>()
        val viewers = mutableMapOf<String, MutableList<String>>()

        val result = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            projectId = projectId,
            sip = ip,
            businessLineName = businessLineName,
            owner = ownerName,
            workspaceName = workspaceName,
            notStatus = notStatus
        )

        val fetchWorkspaceWithOwnerEndTime = System.currentTimeMillis()

        val workspaceNames = result.map { it.workspaceName }.toSet()
        workspaceSharedDao.batchFetchWorkspaceSharedInfo(dslContext, workspaceNames).forEach {
            when (it.type) {
                WorkspaceShared.AssignType.VIEWER -> {
                    viewers.putIfAbsent(it.workspaceName, mutableListOf(it.sharedUser))?.add(it.sharedUser)
                }

                WorkspaceShared.AssignType.OWNER -> {
                    owners.putIfAbsent(it.workspaceName, it.sharedUser)
                }
            }
        }

        val workspaceWindows = workspaceWindowsDao.batchFetchWorkspaceWindowsInfo(
            dslContext, workspaceNames = workspaceNames
        ).associateBy { it.workspaceName }

        val allConfig = windowsResourceConfigService.getAllType(withUnavailable = true, onlySpecModel = null)
            .associateBy { it.id!!.toString() }
        val fetchDetailEndTime = System.currentTimeMillis()

        val tailUsers = if (hasDepartmentsInfo == true) {
            val taiOwners = owners.values.filter {
                it.isNotBlank() && it.contains("@tai")
            }.toSet()
            taiClient.taiUserInfo(TaiUserInfoRequest(usernames = taiOwners)).associate { it.username to it.departments }
        } else {
            null
        }

        val loginUserMap = if (hasCurrentUser == true) {
            startWorkspaceService.cachingLoginUsers(result.map { it.hostIp ?: "" }.toSet())
        } else {
            null
        }

        // 获取CDS对应的母机ip信息
        val hostIps = result.mapNotNull { it.hostIp }.filter { it.isNotEmpty() }
        val cdsInfo = windowsGpuResourceDao.batchFetchWindowsGpuPool(dslContext = dslContext, hostIps = hostIps)
            .associateBy { it.cgsId }

        val defaultZoneConfig = windowsResourceConfigService.getAllZone()
            .associateBy { it.zoneShortName }
        val specZoneConfig = windowsResourceConfigService.getAllSpecZone().associateBy { it.zoneShortName }
        val data = result.map { res ->
            val name = res.workspaceName

            val owner = when {
                res.ownerType.projectUse() -> owners[res.workspaceName]
                else -> res.createUserId
            }
            val depInfo = if (!owner.isNullOrBlank() && (hasDepartmentsInfo == true)) {
                // 判断是不是太湖用户
                if (owner.contains("@tai")) {
                    tailUsers?.get(owner)?.map {
                        DepartmentsInfo(
                            deptId = it.id.toString(),
                            deptName = it.name
                        )
                    }
                } else {
                    val info = client.get(ServiceTxUserResource::class).get(owner).data
                    listOf(
                        DepartmentsInfo(
                            deptId = info?.deptId,
                            deptName = info?.deptName
                        )
                    )
                }
            } else {
                null
            }
            WeSecProjectWorkspace(
                workspaceName = name,
                projectId = res.projectId,
                creator = res.createUserId,
                regionId = res.regionId.toString(),
                innerIp = res.hostIp,
                createTime = DateTimeUtil.toDateTime(res.createTime),
                owner = owner ?: res.createUserId,
                realOwner = owner ?: "",
                status = res.status.name,
                displayName = res.displayName,
                ownerDepartments = depInfo,
                currentLoginUsers = res.hostIp?.let { ip -> loginUserMap?.get(ip) }?.toSet() ?: emptySet(),
                machineType = workspaceWindows[name]?.let { win -> allConfig[win.winConfigId.toString()]?.size },
                macAddress = workspaceWindows[name]?.macAddress,
                zoneType = specZoneConfig[res.zoneId]?.type?.name
                    ?: defaultZoneConfig[res.zoneId?.removeSuffixNumb()]?.type?.name,
                viewers = viewers[name],
                nodeIp = cdsInfo[res.hostIp]?.node
            )
        }

        val buildDataEndTime = System.currentTimeMillis()

        logger.info(
            "getProjectWorkspaceList4WeSec fetchWorkspaceWithOwner {}ms, fetchDetail {}ms, buildData{}ms",
            fetchWorkspaceWithOwnerEndTime - startTime,
            fetchDetailEndTime - fetchWorkspaceWithOwnerEndTime,
            buildDataEndTime - fetchDetailEndTime
        )

        return data
    }

    fun getWorkspaceProject(projectId: String?): List<RemotedevProject> {
        logger.info("get workspace project list")
        val result = workspaceDao.getWorkspaceProject(
            dslContext = dslContext,
            mountType = WorkspaceMountType.START,
            projectId = projectId
        ) ?: emptyList()

        val projectIds = result.map { it.value1() as String }.toSet()

        val projectInfoData =
            client.get(ServiceProjectResource::class).listOnlyByProjectCode(projectIds).data ?: return emptyList()

        val detailMap = projectInfoData.associateBy { it.projectCode }

        return result.map {
            RemotedevProject(
                projectId = it.value1(),
                projectName = detailMap[it.value1()]?.projectName ?: "",
                remotedevManager = detailMap[it.value1()]?.properties?.remotedevManager ?: ""
            )
        }
    }

    fun getWorkspaceProjectNew(projectId: String?, page: Int, pageSize: Int): List<RemotedevProjectNew> {
        logger.info("get workspace project list new")
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val projects = client.get(ServiceProjectResource::class).listProjectsByCondition(
            projectConditionDTO = ProjectConditionDTO(
                queryRemoteDevFlag = true,
                projectCodes = if (projectId != null) {
                    listOf(projectId)
                } else {
                    null
                }
            ),
            limit = limit.limit,
            offset = limit.offset
        ).data ?: return emptyList()

        val projectAndBizs = client.get(ServiceMonitorSpaceResource::class).listMonitorSpaceBizIds(
            projects.map { it.englishName }
        ).data ?: emptyMap()

        return projects.map {
            RemotedevProjectNew(
                projectId = it.englishName,
                projectName = it.projectName,
                remotedevManager = it.remotedevManager ?: "",
                monitorUrl = "$projectMonitorUrl?orgName=${projectAndBizs[it.englishName] ?: ""}"
            )
        }
    }

    fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?, search: WorkspaceSearch?): Page<Workspace> {
        logger.info("$userId get user workspace list")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 6666
        val workspaceSearch = search?.apply {
            // 客户端获取必须包含用户本身
            if (this.viewers == null || !this.viewers!!.contains(userId)) {
                this.viewers = listOf(userId)
            }
        } ?: WorkspaceSearch(
            viewers = listOf(userId),
            onFuzzyMatch = false
        )
        val result = limitFetchProjectWorkspace(
            queryType = QueryType.CLIENT,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull),
            search = workspaceSearch
        )

        val sharedWorkspace = result.filter { it.ownerType.projectUse() }.ifEmpty { null }?.let {
            workspaceSharedDao.batchFetchWorkspaceSharedInfo(dslContext, it.map { i -> i.workspaceName }.toSet())
        }?.groupBy { it.workspaceName } ?: emptyMap()
        val taiUsers = sharedWorkspace.values.flatMap {
            it.find { shared ->
                shared.type == WorkspaceShared.AssignType.OWNER
            }?.sharedUser?.lineSequence() ?: emptySequence()
        }
        val taiUserCN = remoteDevSettingDao.fetchTaiUserInfo(
            dslContext,
            userIds = taiUsers.filter { UserUtil.isTaiUser(it) }.toSet()
        ).mapValues {
            if ((it.value["USER_NAME"] as String).isNotBlank()) {
                "${it.value["USER_NAME"]}@${it.value["COMPANY_NAME"]}"
            } else it.key
        }
        val allConfig = windowsResourceConfigService.getAllType(true, null).associateBy { it.id!! }
        val defaultZoneConfig = windowsResourceConfigService.getAllZone()
            .associateBy { it.zoneShortName }
        val specZoneConfig = windowsResourceConfigService.getAllSpecZone().associateBy { it.zoneShortName }

        val allWindows = workspaceWindowsDao.batchFetchWorkspaceWindowsInfo(
            dslContext,
            result.filter { it.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU }.map { it.workspaceName }.toSet()
        ).associateBy { it.workspaceName }

        val windowsWorkspace = result.filterIsInstance<WorkspaceRecordWithWindows>().associateBy { it.workspaceName }

        val loginUserMap =
            startWorkspaceService.cachingLoginUsers(windowsWorkspace.map { it.value.hostIp ?: "" }.toSet())

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = result.count().toLong(),
            records = result.map {
                val status = checkStatus(it, userId)
                val owner = sharedWorkspace[it.workspaceName]?.find { shared ->
                    shared.type == WorkspaceShared.AssignType.OWNER
                }?.sharedUser ?: if (it.ownerType == WorkspaceOwnerType.PERSONAL) it.createUserId else null
                val hostIp = windowsWorkspace[it.workspaceName]?.hostIp
                val zone = if (hostIp != null && it.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
                    /*后续直接取windows表中的zoneId，不通过ip进行解析*/
                    val zoneId = hostIp.substringBefore(".")
                    specZoneConfig[zoneId] ?: defaultZoneConfig[zoneId.removeSuffixNumb()]
                } else {
                    null
                }
                Workspace(
                    workspaceId = it.workspaceId,
                    workspaceName = it.workspaceName,
                    projectId = it.projectId,
                    displayName = it.displayName,
                    status = status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime?.timestamp(),
                    sleepingTime = if (status.checkSleeping()) it.lastStatusUpdateTime?.timestamp() else null,
                    createUserId = it.createUserId,
                    owner = owner,
                    ownerCN = taiUserCN[owner] ?: owner,
                    hostName = hostIp,
                    workspaceMountType = it.workspaceMountType,
                    workspaceSystemType = it.workspaceSystemType,
                    ownerType = it.ownerType,
                    assignType = sharedWorkspace[it.workspaceName]?.find { shared -> shared.sharedUser == userId }?.type
                        ?: WorkspaceShared.AssignType.OWNER,
                    winConfigId = allWindows[it.workspaceName]?.winConfigId,
                    winConfig = allWindows[it.workspaceName]?.let { i -> allConfig[i.winConfigId.toLong()] },
                    zoneConfig = zone,
                    currentLoginUsers = hostIp?.let { ip -> loginUserMap[ip] } ?: emptyList()
                )
            }
        )
    }

    private fun checkStatus(
        it: WorkspaceRecordInf,
        userId: String
    ): WorkspaceStatus {
        if (it.status.notOk2doNextAction() && Duration.between(
                it.lastStatusUpdateTime ?: LocalDateTime.now(),
                LocalDateTime.now()
            ).seconds > DEFAULT_WAIT_TIME
        ) {
            return workspaceCommon.fixUnexpectedStatus(
                userId = userId,
                workspaceName = it.workspaceName,
                status = it.status,
                mountType = it.workspaceMountType
            )
        }
        return it.status
    }

    fun getWorkspaceUserDetail(userId: String): WorkspaceUserDetail {
        logger.info("$userId get his all workspace ")
        val workspaces = workspaceDao.limitFetchUserWorkspace(
            dslContext = dslContext,
            userId = userId,
            deleted = true
        ) ?: emptyList()
        val status = workspaces.map { it.status }
        val now = LocalDateTime.now()

        // 查出所有正在运行ws的最新历史记录
        val latestHistory = workspaceHistoryDao.fetchLatestHistory(
            dslContext,
            workspaces.asSequence()
                .filter { it.status.checkRunning() }.map { it.workspaceName }.toSet()
        ).associateBy { it.workspaceName }

        // 查出所有已休眠状态ws的最新历史记录
        val latestSleepHistory = workspaceHistoryDao.fetchLatestHistory(
            dslContext,
            workspaces.asSequence()
                .filter { it.status.checkSleeping() }.map { it.workspaceName }.toSet()
        ).associateBy { it.workspaceName }
        val usageTime = workspaces.sumOf {
            it.usageTime + if (it.status.checkRunning()) {
                // 如果正在运行，需要加上目前距离该次启动的时间
                Duration.between(latestHistory[it.workspaceName]?.startTime ?: now, now).seconds
            } else {
                0
            }
        }
        val sleepingTime = workspaces.sumOf {
            it.sleepingTime + if (it.status.checkSleeping()) {
                // 如果正在休眠，需要加上目前距离上次结束的时间
                Duration.between(latestSleepHistory[it.workspaceName]?.endTime ?: now, now).seconds
            } else {
                0
            }
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)

        val discountTime = 10000L
        return WorkspaceUserDetail(
            runningCount = status.count { it.checkRunning() },
            sleepingCount = status.count { it.checkSleeping() },
            deleteCount = status.count { it.checkDeleted() },
            chargeableTime = endBilling.second +
                (endBilling.first - discountTime * 60).coerceAtLeast(0),
            usageTime = usageTime,
            sleepingTime = sleepingTime,
            discountTime = discountTime,
            winUsageTimeLeft = remoteDevSettingService.userWinTimeLeft(userId)
        )
    }

    fun getWorkspaceRecord(
        workspaceName: String
    ): WorkspaceRecord? {
        return workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_VIEW_CONTENT
    )
    fun getWorkspaceDetail(userId: String, workspaceName: String, checkPermission: Boolean = true): WorkspaceDetail? {
        logger.info("$userId get workspace from id $workspaceName")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: return null
        if (checkPermission) {
            permissionService.checkViewerPermission(userId, workspaceName, workspace.projectId)
        }
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId

        val winInfo = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)
        val workspaceConf = if (winInfo != null) {
            workspaceResourceTypeDao.fetchAny(dslContext, winInfo.winConfigId)
        } else {
            null
        }
        val zone = if (winInfo != null) {
            workspaceResourceZoneDao.fetchAll(dslContext)
                .firstOrNull { it.zoneShortName == winInfo.zoneId.removeSuffixNumb() }?.zone
        } else {
            null
        }

        val sharedList = workspaceSharedDao.fetchWorkspaceSharedInfo(dslContext, workspaceName)

        val currentLoginUser = if (winInfo != null) {
            startWorkspaceService.cachingLoginUsers(setOf(winInfo.hostIp)).values.flatten().toSet()
        } else {
            null
        }

        return WorkspaceDetail(
            workspaceId = workspace.workspaceId,
            workspaceName = workspaceName,
            displayName = workspace.displayName,
            projectId = workspace.projectId,
            status = workspace.status,
            lastUpdateTime = workspace.updateTime.timestamp(),
            chargeableTime = 0,
            usageTime = 0,
            sleepingTime = 0,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            ip = winInfo?.hostIp,
            macAddress = winInfo?.macAddress,
            machineType = workspaceConf?.size,
            region = zone,
            owner = sharedList.filter { it.type == WorkspaceShared.AssignType.OWNER }.map { it.sharedUser }.toSet(),
            sharedUsers = sharedList.filter { it.type == WorkspaceShared.AssignType.VIEWER }.map { it.sharedUser }
                .toSet(),
            creator = workspace.createUserId,
            createTime = workspace.createTime.timestamp(),
            imageId = winInfo?.imageId,
            remark = workspace.remark,
            currentLoginUser = currentLoginUser
        )
    }

    fun startCloudWorkspaceDetail(userId: String, workspaceName: String?, envId: String?): WorkspaceStartCloudDetail {
        if (workspaceName != null) {
            val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(dslContext, workspaceName = workspaceName)
                ?: throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    params = arrayOf(workspaceName)
                )
            if (!workspace.status.checkRunning()) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                    params = arrayOf(workspaceName)
                )
            }

            // 检测cds的状态
            val cgsStatus = try {
                startCloudClient.computerStatus(setOf(workspace.hostIp!!))?.firstOrNull()
            } catch (e: Exception) {
                logger.warn("get computerStatus error", e)
                null
            }
            if (cgsStatus?.state != ComputerStatusEnum.NORMAL.ordinal) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_CDS_ERROR.errorCode,
                    params = arrayOf(workspaceName)
                )
            }
            return startCloudWorkspaceDetail(userId, workspace)
        }
        if (envId != null) {
            val userEnvs = getUserEnv4Use(userId)
            val find = userEnvs.find { it.envHashId == envId } ?: run {
                logger.warn("not find user env for $envId")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                    params = arrayOf(I18nUtil.getCodeLanMessage(NOT_FIND_USER_ENV_FOR_ENV_ID, params = arrayOf(envId)))
                )
            }

            val nodeHashIds = find.nodeHashIds?.toSet() ?: run {
                logger.warn("env for $envId has empty public node")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                    params = arrayOf(I18nUtil.getCodeLanMessage(NOT_FIND_NODE_FOR_ENV_ID))
                )
            }

            val public/*<WORKSPACE_NAME, HOST_IP, NODE_HASH_ID>*/ =
                workspaceWindowsDao.batchFetchWorkspaceWindowsInfoWithNodeIds(dslContext, nodeHashIds)
            val loginUserMap = startWorkspaceService.loginUsers(public.map { it.value2() ?: "" }.toSet())
            val workspaceStatus = workspaceJoinDao.fetchWindowsWorkspaces(
                dslContext = dslContext,
                workspaceNames = public.map { it.value1() }.toSet(),
                checkField = listOf(TWorkspace.T_WORKSPACE.NAME, TWorkspace.T_WORKSPACE.STATUS)
            ).associateBy({ it.workspaceName }, { it.status })

            // 检测cds的状态
            val cgsStatus = startCloudClient.computerStatus(
                public.map { it.value2() }.toSet()
            )?.associateBy(
                { it.cgsId }, { it.state }
            )

            val oneReady = public.filter {
                loginUserMap[it.value2()].isNullOrEmpty() &&
                    workspaceStatus[it.value1()] in setOf(WorkspaceStatus.RUNNING, WorkspaceStatus.DISTRIBUTING) &&
                    cgsStatus?.get(it.value2()) == ComputerStatusEnum.NORMAL.ordinal
            }.randomOrNull() ?: run {
                logger.warn("there are no idle public cloud desktops|$envId|$loginUserMap|$workspaceStatus")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                    params = arrayOf(I18nUtil.getCodeLanMessage(NOT_FIND_NODE_FOR_ENV_ID))
                )
            }
            val workspace = workspaceJoinDao.fetchAnyWindowsWorkspace(
                dslContext = dslContext, workspaceName = oneReady.value1()
            ) ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(oneReady.value1())
            )
            /*注册检查*/
            workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspace.workspaceName,
                sharedUsers = listOf(userId)
            ).firstOrNull()?.resourceId ?: run {
                logger.info("start register collaborator user to workspace|$userId|${oneReady.value1()}")
                workspaceCommon.shareWorkspace(
                    workspaceName = workspace.workspaceName,
                    projectId = workspace.projectId,
                    operator = workspace.createUserId,
                    assigns = listOf(
                        ProjectWorkspaceAssign(
                            userId = userId, type = WorkspaceShared.AssignType.COLLABORATOR, expiration = null
                        )
                    ),
                    mountType = workspace.workspaceMountType,
                    ownerType = workspace.ownerType,
                    notify = false
                )
            }
            return startCloudWorkspaceDetail(userId, workspace)
        }
        throw ErrorCodeException(
            errorCode = ErrorCodeEnum.DENIAL_OF_SERVICE.errorCode
        )
    }

    fun getEnvs4PublicWorkspace(userId: String): List<WorkspaceEnv> {
        /*提供给查询接口的走缓存*/
        val data = userEnvCache.get(userId) ?: return emptyList()
        val nodeHashIds = data.flatMap { it.nodeHashIds ?: emptyList() }.toSet()

        val public/*<WORKSPACE_NAME, HOST_IP, NODE_HASH_ID>*/ =
            workspaceWindowsDao.batchFetchWorkspaceWindowsInfoWithNodeIds(dslContext, nodeHashIds)
        val host2NodeMap = public.associateBy { it.value2() }
        val loginUserMap = startWorkspaceService.cachingLoginUsers(public.map { it.value2() ?: "" }.toSet())
        val nodeLoginMap = loginUserMap.mapKeys { host2NodeMap[it.key]?.value3() }
        val workspaceStatus = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            workspaceNames = public.map { it.value1() }.toSet(),
            checkField = listOf(TWorkspaceWindows.T_WORKSPACE_WINDOWS.NODE_HASH_ID, TWorkspace.T_WORKSPACE.STATUS)
        ).associateBy({ it.nodeHashId }, { it.status })
        val normalStatuses = setOf(WorkspaceStatus.RUNNING, WorkspaceStatus.DISTRIBUTING)
        return data.map { it ->
            val normalNodeCount = it.nodeHashIds?.count { workspaceStatus[it] in normalStatuses } ?: 0
            val abnormalNodeCount = (it.nodeHashIds?.size ?: 0) - normalNodeCount
            WorkspaceEnv(
                projectId = it.projectId,
                envHashId = it.envHashId,
                name = it.name,
                normalNodeCount = normalNodeCount,
                abnormalNodeCount = max(abnormalNodeCount, 0),
                inUseNodeCount = it.nodeHashIds?.count { !nodeLoginMap[it].isNullOrEmpty() } ?: 0,
                nodeHashIds = it.nodeHashIds
            )
        }
    }

    private val userEnvCache: LoadingCache<String, List<EnvWithNodeCount>> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build { key -> getUserEnv4Use(key) }

    private fun getUserEnv4Use(userId: String): List<EnvWithNodeCount> {
        /*
        * 现有：user   ->      project     ->      env
        * 无法直接做到: user    ->    env
        * 暂时采用project缓存缓解
        * */
        val all = workspaceDao.fetchAllProject4Public(dslContext)
        val userAll = kotlin.runCatching {
            client.get(ServiceTxProjectResource::class).list(userId, null).data
        }.onFailure {
            logger.error("error in ServiceTxProjectResource::list|$userId", it)
        }.getOrNull()?.map { it.englishName }?.toSet() ?: kotlin.run {
            logger.error("fail to get user projects|$userId")
            return emptyList()
        }
        val userHas = all.intersect(userAll)
        return kotlin.runCatching {
            client.get(ServiceDEVXResource::class).getUserDEVXEnv(userId, userHas).data
        }.onFailure {
            logger.error("error in ServiceDEVXResource::getUserDEVXEnv|$userId|$userHas", it)
        }.getOrNull() ?: kotlin.run {
            logger.error("fail to get user env|$userId|$userHas")
            emptyList()
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.CGS_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_VIEW_CONTENT
    )
    private fun startCloudWorkspaceDetail(
        userId: String,
        workspace: WorkspaceRecordWithWindows
    ): WorkspaceStartCloudDetail {
        logger.info("$userId get startCloud workspace from workspaceName ${workspace.workspaceName}")

        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId
        permissionService.checkViewerPermission(userId, workspace.workspaceName, workspace.projectId)
        ActionAuditContext.current().addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
        val resourceId = if (userId != workspace.createUserId) {
            workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspace.workspaceName,
                sharedUsers = listOf(userId)
            ).firstOrNull()?.resourceId
        } else {
            workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspace.workspaceName)?.resourceId
        }
        val owner = if (workspace.ownerType.projectUse()) {
            workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspace.workspaceName,
                assignType = WorkspaceShared.AssignType.OWNER
            ).firstOrNull()?.sharedUser
        } else {
            workspace.createUserId
        }
        val gameId = workspaceCommon.getGameIdAndAppId(workspace.projectId, workspace.ownerType)
        return WorkspaceStartCloudDetail(
            ip = workspace.hostIp ?: "",
            curLaunchId = workspace.curLaunchId ?: gameId.second.toInt(),
            regionId = workspace.regionId,
            projectId = workspace.projectId,
            name = workspace.workspaceName,
            creator = workspace.createUserId,
            owner = owner,
            resourceId = resourceId,
            displayName = workspace.displayName
        )
    }

    fun getWorkspaceTimeline(
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Page<WorkspaceOpHistory> {
        logger.info("$userId get workspace time line from id $workspaceName")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )

        if (!permissionService.hasManagerOrViewerPermission(userId, workspace.projectId, workspace.workspaceName)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to get $workspaceName info")
            )
        }
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: DEFAULT_PAGE_SIZE
        val count = workspaceOpHistoryDao.countOpHistory(dslContext, workspaceName)
        val result = workspaceOpHistoryDao.limitFetchOpHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.mapNotNull {
                WorkspaceOpHistory(
                    createdTime = it.createdTime.timestamp(),
                    operator = it.operator,
                    action = WorkspaceAction.values().getOrNull(it.action) ?: return@mapNotNull null,
                    actionMessage = it.actionMsg
                )
            }
        )
    }

    fun getShareWorkspace(workspaceName: String?): List<WorkspaceShared> {
        logger.info("get all shared workspace")
        return workspaceSharedDao.fetchSharedWorkspace(dslContext, workspaceName)?.map {
            WorkspaceShared(
                id = it["ID"] as Long,
                workspaceName = it["WORKSPACE_NAME"] as String,
                operator = it["OPERATOR"] as String,
                sharedUser = it["SHARED_USER"] as String,
                type = WorkspaceShared.AssignType.valueOf(it["ASSIGN_TYPE"] as String),
                resourceId = it["RESOURCE_ID"] as String
            )
        } ?: emptyList()
    }

    fun deleteSharedWorkspace(id: Long): Boolean {
        val info = workspaceSharedDao.fetchSharedWorkspaceById(
            id = id,
            dslContext = dslContext
        )
        if (info != null) {
            workspaceCommon.unShareWorkspace(
                workspaceName = info.workspaceName,
                operator = ADMIN_NAME,
                sharedUsers = listOf(info.sharedUser),
                assignType = info.type,
                mountType = if (info.resourceId.isNotBlank()) WorkspaceMountType.START else null
            )
            return true
        }
        return false
    }

    fun deleteSharedWorkspace(
        workspaceName: String,
        sharedUser: String
    ): Boolean {
        logger.info("deleteSharedWorkspace|workspaceName|$workspaceName|sharedUser|$sharedUser")
        workspaceSharedDao.fetchSharedWorkspaceByUser(
            dslContext = dslContext,
            workspaceName = workspaceName,
            sharedUser = sharedUser
        )?.let {
            workspaceCommon.unShareWorkspace(
                workspaceName = it.workspaceName,
                operator = ADMIN_NAME,
                sharedUsers = listOf(it.sharedUser),
                assignType = it.type,
                mountType = if (it.resourceId.isNotBlank()) WorkspaceMountType.START else null
            )
            return true
        }
        return false
    }

    fun changeWorkspaceOwnerType(
        workspaceName: String,
        old: WorkspaceOwnerType,
        new: WorkspaceOwnerType
    ) {
        workspaceDao.updateWorkspaceOwnerType(
            dslContext = dslContext,
            workspaceName = workspaceName,
            old = old,
            new = new
        )
    }

    fun projectAccessDevicePermissions(
        userId: String,
        macAddress: String
    ): Map<String, ProjectAccessDevicePermissionsResp> {
        // 获取用户当前的项目列表
        val projects = workspaceJoinDao.fetchProjectFromUser(dslContext, userId)
        if (projects.isEmpty()) {
            logger.debug("projectAccessDevicePermissions|userId|$userId|empty projects")
            return emptyMap()
        }
        // 调用安全接口
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val ip = attributes?.request?.getHeader("X-Forwarded-For")?.split(",")
            ?.firstOrNull { it.isNotBlank() }?.trim()
        if (ip.isNullOrBlank()) {
            logger.debug("projectAccessDevicePermissions ip null")
            return emptyMap()
        }
        return apiGwService.projectAccessDevicePermissions(
            mac = macAddress,
            userId = userId,
            projects = projects.joinToString(","),
            ip = ip
        ) ?: emptyMap()
    }

    @Deprecated("不要新增功能，希望废弃该接口")
    fun modifyWorkspaceDisplayName(userId: String, ip: String, displayName: String): Boolean {
        logger.info("$userId modifyWorkspaceDisplayName $ip|$displayName")
        val record = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext, sip = ip, notStatus = listOf(
            WorkspaceStatus.PREPARING,
            WorkspaceStatus.DELETED,
            WorkspaceStatus.DELIVERING_FAILED
        ),
            checkField = listOf(TWorkspace.T_WORKSPACE.NAME)
        ).ifEmpty {
            logger.warn("$ip fetchWorkspaceByIp not found")
            return false
        }
        if (record.size > 1) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REMOTEDEV_CLIENT_IP_DUPLICATE_ERROR.errorCode,
                params = arrayOf(ip)
            )
        }
        return editWorkspace(
            userId = userId,
            workspaceName = record.first().workspaceName,
            displayName = displayName,
            checkPermission = false
        )
    }

    // 校验是否需要moa 2fa验证，true:需要 ；false：不需要
    fun checkMoa2fa(userId: String, workspaceName: String): Boolean {
        /*1.个人云桌面 + 内部员工拥有者 --直接返回true
         *2.团队云桌面 + 内部员工拥有者，则调用wesec接口判断
          */
        logger.info("$userId check moa 2fa workspace $workspaceName")
        val ws = getWorkspaceList4WeSec(
            workspaceName = workspaceName,
            notStatus = null
        ).firstOrNull() ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        // TODO 目前阶段先对内部员工做 moa 验证，后续放开 cp 也需要验证。
        if (!ws.realOwner.isNullOrBlank() && !ws.realOwner!!.endsWith("@tai")) {
            return kotlin.runCatching {
                apiGwService.checkMoa2fa(
                    project = ws.projectId,
                    workspactName = workspaceName
                )
            }.getOrNull() ?: false.also { logger.warn("MOA 2FA check failed for workspace $workspaceName") }
        }
        return false
    }

    fun createMoa2faRequest(userId: String, moa2faReqData: Moa2faReqData): Moa2faRespData {
        return taiService.createMoa2faRequest(userId = userId, moa2faReqData = moa2faReqData)
    }

    fun verifyMoa2faResult(userId: String, moa2faVerifyReqData: Moa2faVerifyReqData): Moa2faVerifyRespData {
        return taiService.verifyMoa2faRequest(userId = userId, moa2faVerifyReqData = moa2faVerifyReqData)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val DEFAULT_PAGE_SIZE = 20

        private fun String.removeSuffixNumb(): String {
            for (i in this.lastIndex downTo 0) {
                if (this[i].isDigit()) {
                    continue
                }
                return this.substring(0..i)
            }
            return ""
        }
    }
}
