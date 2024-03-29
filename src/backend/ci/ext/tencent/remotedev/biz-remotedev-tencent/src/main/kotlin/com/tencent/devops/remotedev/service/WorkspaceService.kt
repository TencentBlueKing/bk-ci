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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.WorkspaceNotifyTemplateEnum
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.cron.HolidayHelper
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectAccessDevicePermissionsResp
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceFetchData
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.ShareWorkspace
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceProxyDetail
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInf
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStartCloudDetail
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.pojo.expert.FetchSupportResp
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.project.DepartmentsInfo
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_DISCOUNT_TIME_KEY
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OFFICIAL_DEVFILE_KEY
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.NOT_LOGIN_NOTIFY
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.SLEEP_3_DAY_NOTIFY
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import com.tencent.devops.scm.utils.code.git.GitUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
@Suppress("ALL")
class WorkspaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val remoteDevCvmService: RemoteDevCvmService,
    private val remoteDevGitTransfer: RemoteDevGitTransfer,
    private val permissionService: PermissionService,
    private val client: Client,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val redisCache: RedisCacheService,
    private val workspaceCommon: WorkspaceCommon,
    private val bkccService: BKCCService,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val expertSupportDao: ExpertSupportDao,
    private val apiGwService: ApiGwService,
    private val notifyControl: NotifyControl,
    private val startWorkspaceService: StartWorkspaceService,
    private val bkBaseService: BKBaseService,
    private val holidayHelper: HolidayHelper,
    private val taiClient: TaiClient
) {
    @ActionAuditRecord(
        actionId = ActionId.CGS_EDIT,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        content = ActionAuditContent.CGS_EDIT_CONTENT
    )
    // 修改workspace备注名称
    fun editWorkspace(userId: String, workspaceName: String, displayName: String): Boolean {
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

        if (!permissionService.hasUserManager(userId, ws.projectId)) {
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

        // 同步修改 cc 主机名称
        if (ws.workspaceSystemType.checkWindows()) {
            bkccService.updateHostName(displayName, workspaceName)
        }

        return true
    }

    fun modifyWorkspaceProperty(userId: String, workspaceName: String, workspaceProperty: WorkspaceProperty): Boolean {
        logger.info("$userId modify workspace property $workspaceName|$workspaceProperty")

        val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, ws.projectId)
            .scopeId = ws.projectId

        if (!permissionService.hasUserManager(userId, ws.projectId)) {
            permissionService.checkViewerPermission(userId, workspaceName, ws.projectId)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            workspaceDao.modifyWorkspaceProperty(
                dslContext = transactionContext,
                workspaceName = workspaceName,
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
            permissionService.checkOwnerPermission(userId, workspaceName, workspace.projectId)
        }

        RedisCallLimit(
            redisOperation,
            "$REDIS_CALL_LIMIT_KEY_PREFIX:shareWorkspace:${workspaceName}_$sharedUser",
            expiredTimeInSeconds
        ).tryLock().use {

            // 共享时创建START云桌面的用户
            if (workspace.workspaceMountType == WorkspaceMountType.START) {
                client.get(ServiceStartCloudResource::class)
                    .createStartCloudUser(sharedUser)
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
                mountType = workspace.workspaceMountType
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
        val search = with(data) {
            WorkspaceSearch(
                projectId = projectId?.let { listOf(it) },
                workspaceName = workspaceName?.let { listOf(it) },
                workspaceSystemType = systemType?.let { listOf(it) },
                ips = ips,
                owner = owner?.let { listOf(it) },
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
        val zoneConfig = windowsResourceConfigService.getAllZone().associateBy { it.zoneShortName }
        val taiUserCN = remoteDevSettingDao.fetchTaiUserInfo(dslContext, userIds = taiUsers)
            .mapValues {
                if (it.value.first.isNotBlank()) {
                    "${it.value.first}@${it.value.second}"
                } else it.key
            }

        val allWindows = workspaceWindowsDao.batchFetchWorkspaceWindowsInfo(
            dslContext,
            result.filter { it.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU }.map { it.workspaceName }
        ).associateBy { it.workspaceName }

        // 专家协助
        var expertMap: MutableMap<String, MutableList<FetchSupportResp>>? = null
        if (enableExportSup) {
            expertMap = mutableMapOf()
            if (expertSupId != null) {
                listOf(expertSupportDao.getSup(dslContext, expertSupId))
            } else {
                expertSupportDao.fetchSupByWorkspaceName(dslContext, workspaceNames)
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

        val detailMap = workspaceDao.fetchWorkspaceDetailByNames(dslContext, workspaceNames)
            .associateBy({ it.workspaceName }) { objectMapper.readValue<WorkSpaceCacheInfo>(it.detail) }
        val loginUserMap = startWorkspaceService.loginUsers(detailMap.values.map { it.hostIP }.toSet())

        val records = mutableListOf<ProjectWorkspace>()
        result.forEach {
            val detail = detailMap[it.workspaceName]
            records.add(
                ProjectWorkspace(
                    workspaceId = it.workspaceId,
                    workspaceName = it.workspaceName,
                    projectId = it.projectId,
                    displayName = it.displayName,
                    status = it.status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime?.timestamp(),
                    sleepingTime = if (it.status.checkSleeping()) {
                        it.lastStatusUpdateTime?.timestamp()
                    } else {
                        null
                    },
                    createUserId = it.createUserId,
                    hostName = detail?.hostIP,
                    workspaceMountType = it.workspaceMountType,
                    workspaceSystemType = it.workspaceSystemType,
                    winConfig = allWindows[it.workspaceName]?.let { i -> allConfig[i.winConfigId.toLong()] },
                    zoneConfig = detail?.hostIP?.let { ip -> zoneConfig[ip.replace(Regex("[\\d\\.]+"), "")] },
                    owner = owners[it.workspaceName],
                    viewers = viewers[it.workspaceName],
                    ownerCN = taiUserCN[owners[it.workspaceName]] ?: owners[it.workspaceName],
                    viewersCN = viewers[it.workspaceName]?.map { viewer ->
                        taiUserCN[viewer] ?: viewer
                    },
                    gpu = it.gpu,
                    cpu = it.cpu,
                    memory = it.memory,
                    disk = it.memory,
                    currentLoginUsers = detail?.hostIP?.let { ip -> loginUserMap[ip] } ?: emptyList(),
                    expertSupportList = expertMap?.get(it.workspaceName),
                    macAddress = allWindows[it.workspaceName]?.macAddress,
                    remark = it.remark
                )
            )
        }
        return records
    }

    fun getProjectWorkspaceList4WeSec(
        projectId: String?,
        ip: String?,
        hasDepartmentsInfo: Boolean?,
        hasCurrentUser: Boolean?,
        businessLineName: String? = null,
        ownerName: String? = null
    ): List<WeSecProjectWorkspace> {
        val startTime = System.currentTimeMillis()

        val result = workspaceDao.fetchWorkspaceWithOwner(
            dslContext = dslContext,
            mountType = WorkspaceMountType.START,
            projectIds = projectId?.let { setOf(projectId) },
            ip = ip,
            businessLineName = businessLineName,
            ownerName = ownerName
        ) ?: emptyList()

        val fetchWorkspaceWithOwnerEndTime = System.currentTimeMillis()

        val workspaceNames = result.map { it["NAME"] as String }.toSet()

        val detailMap = workspaceDao.fetchWorkspaceDetailByNames(dslContext, workspaceNames)
            .associateBy { it.workspaceName }

        val workspaceWindows = workspaceDao.fetchNotifyWorkspaces(
            dslContext = dslContext,
            mountType = WorkspaceMountType.START,
            workspaceNames = workspaceNames
        )?.associateBy { it["NAME"] as String }

        val allConfig = windowsResourceConfigService.getAllType(true, null).associateBy { it.id!!.toString() }
        val fetchDetailEndTime = System.currentTimeMillis()

        val tailUsers = if (hasDepartmentsInfo == true) {
            val taiOwners = result.map { it["SHARED_USER"] as? String ?: "" }.filter {
                it.isNotBlank() && it.contains("@tai")
            }.toSet()
            taiClient.taiUserInfo(TaiUserInfoRequest(usernames = taiOwners)).associate { it.username to it.departments }
        } else {
            null
        }

        val data = result.map { res ->
            val workspaceName = res["NAME"] as String
            val detail = detailMap[workspaceName]?.let { det ->
                try {
                    objectMapper.readValue<WorkSpaceCacheInfo>(det.detail)
                } catch (ignore: Exception) {
                    logger.warn("get workspace detail from redis error|$workspaceName", ignore)
                    null
                }
            }

            val owner = res["SHARED_USER"] as? String ?: ""
            val depInfo = if (owner.isNotBlank() && (hasDepartmentsInfo == true)) {
                // 判断是不是太湖用户
                if (owner.contains("@tai")) {
                    tailUsers?.get(owner)?.map {
                        DepartmentsInfo(
                            it.id.toString(),
                            it.name
                        )
                    }
                } else {
                    val info = client.get(ServiceTxUserResource::class).get(owner).data
                    listOf(
                        DepartmentsInfo(
                            info?.deptId,
                            info?.deptName
                        )
                    )
                }
            } else {
                null
            }
            val currUser = if (hasCurrentUser == true && ip != null) {
                startWorkspaceService.loginUsers(setOf(detail?.hostIP ?: "")).values.flatten().toSet()
            } else {
                null
            }
            WeSecProjectWorkspace(
                workspaceName = workspaceName,
                projectId = res["PROJECT_ID"] as String,
                creator = res["CREATOR"] as String,
                regionId = detail?.regionId.toString(),
                innerIp = detail?.hostIP,
                createTime = DateTimeUtil.toDateTime(res["CREATE_TIME"] as LocalDateTime),
                owner = res["SHARED_USER"] as? String ?: res["CREATOR"] as String,
                realOwner = owner,
                status = WorkspaceStatus.values()[res["STATUS"] as Int],
                displayName = res["DISPLAY_NAME"] as String,
                ownerDepartments = depInfo,
                currentLoginUsers = currUser,
                machineType = workspaceWindows?.get(workspaceName)
                    ?.let { win -> allConfig[win["WIN_CONFIG_ID"].toString()]?.size }
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

    fun getRemotedevCvm(projectId: String?): List<RemotedevCvmData> {
        logger.debug("get remotedev cvm list")
        return remoteDevCvmService.getAllRemotedevCvm(projectId)
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

        val sharedWorkspace = result.filter { it.ownerType == WorkspaceOwnerType.PROJECT }.ifEmpty { null }?.let {
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
            if (it.value.first.isNotBlank()) {
                "${it.value.first}@${it.value.second}"
            } else it.key
        }
        val allConfig = windowsResourceConfigService.getAllType(true, null).associateBy { it.id!! }
        val zoneConfig = windowsResourceConfigService.getAllZone().associateBy { it.zoneShortName }

        val allWindows = workspaceWindowsDao.batchFetchWorkspaceWindowsInfo(
            dslContext,
            result.filter { it.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU }.map { it.workspaceName }
        ).associateBy { it.workspaceName }

        val detailMap = workspaceDao.fetchWorkspaceDetailByNames(dslContext, result.map { it.workspaceName }.toSet())
            .associateBy({ it.workspaceName }) { objectMapper.readValue<WorkSpaceCacheInfo>(it.detail) }
        val loginUserMap = startWorkspaceService.loginUsers(detailMap.values.map { it.hostIP }.toSet())

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = result.count().toLong(),
            records = result.map {
                var status = it.status
                run {
                    if (status.notOk2doNextAction(it.workspaceSystemType) && Duration.between(
                            it.lastStatusUpdateTime ?: LocalDateTime.now(),
                            LocalDateTime.now()
                        ).seconds > DEFAULT_WAIT_TIME
                    ) {
                        status = workspaceCommon.fixUnexpectedStatus(
                            userId = userId,
                            workspaceName = it.workspaceName,
                            status = status,
                            mountType = it.workspaceMountType
                        )
                    }
                }
                val owner = sharedWorkspace[it.workspaceName]?.find { shared ->
                    shared.type == WorkspaceShared.AssignType.OWNER
                }?.sharedUser ?: if (it.ownerType == WorkspaceOwnerType.PERSONAL) it.createUserId else null
                val hostName = detailMap[it.workspaceName]?.hostIP
                val zone = if (it.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
                    hostName?.let { ip -> zoneConfig[ip.replace(Regex("[\\d\\.]+"), "")] }
                } else {
                    null
                }
                Workspace(
                    workspaceId = it.workspaceId,
                    workspaceName = it.workspaceName,
                    projectId = it.projectId,
                    displayName = it.displayName,
                    repositoryUrl = it.repositoryUrl,
                    branch = it.branch,
                    devFilePath = it.devFilePath,
                    yaml = it.yaml,
                    wsTemplateId = it.templateId,
                    status = status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime?.timestamp(),
                    sleepingTime = if (status.checkSleeping()) it.lastStatusUpdateTime?.timestamp() else null,
                    createUserId = it.createUserId,
                    owner = owner,
                    ownerCN = taiUserCN[owner] ?: owner,
                    workPath = it.workPath,
                    workspaceFolder = it.workspaceFolder,
                    hostName = hostName,
                    workspaceMountType = it.workspaceMountType,
                    workspaceSystemType = it.workspaceSystemType,
                    ownerType = it.ownerType,
                    assignType = sharedWorkspace[it.workspaceName]?.find { shared -> shared.sharedUser == userId }?.type
                        ?: WorkspaceShared.AssignType.OWNER,
                    winConfigId = allWindows[it.workspaceName]?.winConfigId,
                    winConfig = allWindows[it.workspaceName]?.let { i -> allConfig[i.winConfigId.toLong()] },
                    zoneConfig = zone,
                    gpu = it.gpu,
                    cpu = it.cpu,
                    memory = it.memory,
                    disk = it.memory,
                    currentLoginUsers = hostName?.let { ip -> loginUserMap[ip] } ?: emptyList()
                )
            }
        )
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

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toLong() ?: 10000
        return WorkspaceUserDetail(
            runningCount = status.count { it.checkRunning() },
            sleepingCount = status.count { it.checkSleeping() },
            deleteCount = status.count { it.checkDeleted() },
            chargeableTime = endBilling.second +
                (notEndBillingTime + endBilling.first - discountTime * 60).coerceAtLeast(0),
            usageTime = usageTime,
            sleepingTime = sleepingTime,
            discountTime = discountTime,
            cpu = workspaces.sumOf {
                if (it.status == WorkspaceStatus.RUNNING) {
                    it.cpu
                } else {
                    0
                }
            },
            memory = workspaces.sumOf {
                if (it.status == WorkspaceStatus.RUNNING) {
                    it.memory
                } else {
                    0
                }
            },
            disk = workspaces.sumOf {
                if (it.status in
                    setOf(WorkspaceStatus.RUNNING, WorkspaceStatus.STOPPED)
                ) {
                    it.disk
                } else {
                    0
                }
            },
            winUsageTimeLeft = remoteDevSettingService.userWinTimeLeft(userId)
        )
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

        val now = LocalDateTime.now()

        val lastHistory = workspaceHistoryDao.fetchAnyHistory(dslContext, workspaceName)

        val discountTime = redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: DISCOUNT_TIME

        val usageTime = workspace.usageTime + if (workspace.status.checkRunning()) {
            // 如果正在运行，需要加上目前距离该次启动的时间
            Duration.between(lastHistory?.startTime ?: now, now).seconds
        } else {
            0
        }

        val sleepingTime = workspace.sleepingTime + if (workspace.status.checkSleeping()) {
            // 如果正在休眠，需要加上目前距离上次结束的时间
            Duration.between(lastHistory?.endTime ?: now, now).seconds
        } else {
            0
        }

        val notEndBillingTime = remoteDevBillingDao.fetchNotEndBilling(dslContext, userId).sumOf {
            Duration.between(it, now).seconds
        }

        val endBilling = remoteDevSettingDao.fetchSingleUserBilling(dslContext, userId)
        return with(workspace) {
            WorkspaceDetail(
                workspaceId = workspaceId,
                workspaceName = workspaceName,
                displayName = displayName,
                status = workspace.status,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = endBilling.second +
                    (notEndBillingTime + endBilling.first - discountTime * 60).coerceAtLeast(0),
                usageTime = usageTime,
                sleepingTime = sleepingTime,
                cpu = cpu,
                memory = memory,
                disk = disk,
                yaml = yaml,
                systemType = workspaceSystemType,
                workspaceMountType = workspaceMountType,
                ownerType = ownerType
            )
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
    fun startCloudWorkspaceDetail(userId: String, workspaceName: String): WorkspaceStartCloudDetail {
        logger.info("$userId get startCloud workspace from workspaceName $workspaceName")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        // 审计
        ActionAuditContext.current()
            .addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
            .scopeId = workspace.projectId
        permissionService.checkViewerPermission(userId, workspaceName, workspace.projectId)
        workspaceCommon.checkWorkspaceAvailability(userId, workspace.workspaceMountType, workspace.ownerType)
        ActionAuditContext.current().addAttribute(ActionAuditContent.PROJECT_CODE_TEMPLATE, workspace.projectId)
        val detail = workspaceCommon.getWorkspaceDetail(workspaceName)
        if (detail == null || !workspace.status.checkRunning()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                params = arrayOf(workspaceName)
            )
        }
        val resourceId = if (userId != workspace.createUserId) {
            workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspaceName,
                sharedUsers = listOf(userId)
            ).firstOrNull()?.resourceId
        } else {
            workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)?.resourceId
        }
        val owner = if (workspace.ownerType == WorkspaceOwnerType.PROJECT) {
            workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspaceName,
                assignType = WorkspaceShared.AssignType.OWNER
            ).firstOrNull()?.sharedUser
        } else {
            workspace.createUserId
        }
        return WorkspaceStartCloudDetail(
            ip = detail.environmentIP,
            curLaunchId = detail.curLaunchId!!,
            regionId = detail.regionId,
            projectId = workspace.projectId,
            name = workspace.workspaceName,
            creator = workspace.createUserId,
            owner = owner,
            resourceId = resourceId
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
        permissionService.checkViewerPermission(userId, workspaceName, workspace.projectId)
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: defaultPageSize
        val count = workspaceOpHistoryDao.countOpHistory(dslContext, workspaceName)
        val result = workspaceOpHistoryDao.limitFetchOpHistory(
            dslContext = dslContext,
            workspaceName = workspaceName,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                WorkspaceOpHistory(
                    createdTime = it.createdTime.timestamp(),
                    operator = it.operator,
                    action = WorkspaceAction.values()[it.action],
                    actionMessage = it.actionMsg
                )
            }
        )
    }

    fun getWorkspaceProxyDetail(workspaceName: String): WorkspaceProxyDetail {
        val workspace = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = workspaceName,
            status = WorkspaceStatus.RUNNING
        )

        return workspace?.let {
            workspaceCommon.getOrSaveWorkspaceDetail(
                workspaceName,
                workspace.workspaceMountType
            ).let {
                WorkspaceProxyDetail(
                    workspaceName = workspaceName,
                    podIp = it.environmentIP,
                    sshKey = it.sshKey,
                    environmentHost = it.environmentHost
                )
            }
        } ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode
        )
    }

    // 更新用户运行中的空间的detail缓存信息
    fun updateUserWorkspaceDetailCache(userId: String) {
        workspaceDao.fetchWorkspace(
            dslContext, userId = userId, status = WorkspaceStatus.RUNNING
        )?.parallelStream()?.forEach {
            workspaceCommon.updateWorkspaceDetail(it.workspaceName, it.workspaceMountType)
        }
    }

    fun preCiAgent(agentId: String, workspaceName: String): Boolean {
        logger.info("update preCiAgent id|$workspaceName|$agentId")
        return workspaceDao.updatePreCiAgentId(dslContext, agentId, workspaceName)
    }

    fun checkDevfile(
        userId: String,
        pathWithNamespace: String,
        branch: String,
        gitType: RemoteDevGitType
    ): List<String> {
        logger.info("$userId get devfile list from git. $pathWithNamespace|$branch")
        return permissionService.checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getFileNameTree(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                path = Constansts.devFileDirectoryName, // 根目录
                ref = branch,
                recursive = false // 不递归
            ).map { Constansts.devFileDirectoryName + "/" + it }
        }
    }

    fun getUnavailableWorkspace(): List<String> {
        return workspaceDao.fetchNotUsageTimeWinWorkspace(
            dslContext, status = WorkspaceStatus.RUNNING
        )?.map { it.workspaceName } ?: emptyList()
    }

    fun sendNotification(workspaceMap: Map<String, List<WorkspaceRecord>>, templateCode: String) {
        workspaceMap.forEach { (creator, workspaces) ->
            val request = SendNotifyMessageTemplateRequest(
                templateCode = templateCode,
                receivers = mutableSetOf(creator),
                cc = mutableSetOf(creator),
                titleParams = null,
                bodyParams = mapOf(
                    "userId" to creator,
                    "workspaceName" to workspaces.joinToString(separator = "\n") { it.workspaceName }
                ),
                notifyType = mutableSetOf(NotifyType.EMAIL.name)
            )
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        }
    }

    // 提前7天邮件提醒，云环境即将自动回收
    fun sendLinuxInactivityWorkspaceNotify() {
        logger.info("sendLinuxInactivityWorkspaceNotify")
        val inactivityWorkspaceMap = workspaceDao.getTimeOutInactivityWorkspace(
            timeOutDays = Constansts.timeoutDays - Constansts.sendNotifyDays,
            dslContext = dslContext,
            systemType = WorkspaceSystemType.LINUX
        ).groupBy { it.createUserId }
        logger.info("sendInactivityWorkspaceNotify|workspaceMap|$inactivityWorkspaceMap")
        sendNotification(
            workspaceMap = inactivityWorkspaceMap,
            templateCode = WorkspaceNotifyTemplateEnum.REMOTEDEV_WORKSPACE_RECYCLE_TEMPLATE.templateCode
        )
    }

    // 提前7天邮件提醒，云环境即将自动回收
    fun sendWinInactivityWorkspaceNotify() {
        logger.info("sendWinInactivityWorkspaceNotify")

        val retentionTime = redisCache.get(RedisKeys.REDIS_DESTRUCTION_RETENTION_TIME)?.toInt() ?: 3
        val startWorkspaceMap = workspaceDao.getTimeOutInactivityWorkspace(
            timeOutDays = retentionTime - 1,
            dslContext = dslContext,
            systemType = WorkspaceSystemType.WINDOWS_GPU
        ).groupBy { it.createUserId }
        logger.info("sendInactivityWorkspaceNotify|startWorkspaceMap|$startWorkspaceMap")
        sendNotification(
            workspaceMap = startWorkspaceMap,
            templateCode = WorkspaceNotifyTemplateEnum.REMOTEDEV_START_DESKTOP_RECYCLE_TEMPLATE.templateCode
        )
    }

    fun initBilling(freeTime: Int? = null) {
        remoteDevBillingDao.monthlyInit(
            dslContext,
            (freeTime ?: redisCache.get(REDIS_DISCOUNT_TIME_KEY)?.toInt() ?: DISCOUNT_TIME) * 60
        )
    }

    fun getWorkspaceHost(workspaceName: String): String {
        val url = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)?.repositoryUrl
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        return GitUtils.getDomainAndRepoName(url).first
    }

    fun getDevfile(): String {
        return redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY) ?: ""
    }

    fun getShareWorkspace(workspaceName: String?): List<WorkspaceShared> {
        logger.info("get all shared workspace")
        return workspaceDao.fetchSharedWorkspace(dslContext, workspaceName)?.map {
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

    fun notifyWinBeforeSleep() {
        logger.info("start notifyWinBeforeSleep")
        val viewers = mutableMapOf<String, MutableList<String>>()
        workspaceDao.fetchCreators(dslContext, WorkspaceStatus.RUNNING).forEach {
            viewers.putIfAbsent(it.value1(), mutableListOf(it.value2()))?.add(it.value2())
        }
        logger.info("notifyWinBeforeSleep start check $viewers")
        viewers.forEach { (userId, workspaces) ->
            // 不重复提醒
            if (redisOperation.get(RedisKeys.notifyWinBeforeSleep(userId)) != null) {
                logger.info("$userId is notify yet. return")
                return@forEach
            }
            val duration = remoteDevSettingService.userWinTimeLeft(userId)
            val limit = redisCache.get(RedisKeys.REDIS_NOTICE_AHEAD_OF_TIME)?.toLong() ?: 60
            if (duration < limit * 60) {
                logger.info("start notify to user $userId")
                notifyControl.dispatchWebsocketPushEvent(
                    userId = userId,
                    workspaceName = workspaces.first(),
                    workspaceHost = null,
                    errorMsg = null, type = WebSocketActionType.WORKSPACE_NEED_RENEWAL,
                    status = true, action = WorkspaceAction.NEED_RENEWAL
                )
                val request = SendNotifyMessageTemplateRequest(
                    templateCode = WorkspaceNotifyTemplateEnum.REMOTEDEV_WORKSPACE_RENEWAL_TEMPLATE.templateCode,
                    receivers = mutableSetOf(userId),
                    cc = mutableSetOf(userId),
                    titleParams = null,
                    bodyParams = mapOf(
                        "userId" to userId,
                        "workspaceName" to workspaces.joinToString()
                    ),
                    notifyType = mutableSetOf(NotifyType.EMAIL.name)
                )
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
                redisOperation.set(
                    key = RedisKeys.notifyWinBeforeSleep(userId),
                    value = workspaces.joinToString(),
                    expiredInSecond = limit * 60
                )
            } else {
                logger.info("no need to notify now|$userId|$duration|${limit * 60}")
            }
        }
    }

    fun notifyWinSleep3Day() {
        logger.info("notifyWinSleep3Day")
        val limitDay = holidayHelper.getLastWorkingDays(3).last()
        workspaceDao.fetchWorkspace(
            dslContext = dslContext,
            status = WorkspaceStatus.STOPPED,
            systemType = WorkspaceSystemType.WINDOWS_GPU
        )?.parallelStream()?.forEach { workspace ->
            if ((workspace.lastStatusUpdateTime ?: LocalDateTime.now()) < limitDay) {
                logger.info(
                    "ready to notify when sleep in 3 day " +
                        "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|${workspace.hostName}"
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspace.workspaceName,
                    operator = ADMIN_NAME,
                    action = WorkspaceAction.NOTIFY,
                    actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
                )
                val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
                notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                    userIds = userIds.toMutableSet(),
                    workspaceName = workspace.workspaceName,
                    cc = mutableSetOf(workspace.createUserId),
                    projectId = workspace.projectId,
                    notifyTemplateCode = SLEEP_3_DAY_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                    bodyParams = mutableMapOf(
                        "cgsIp" to (workspace.hostName ?: ""),
                        "projectId" to (workspace.projectId),
                        "userId" to userIds.joinToString()
                    )
                )
            }
        }
    }

    fun notifyWinNotLogin3Day() {
        val limitDay = holidayHelper.getLastWorkingDays(3).last()
        val logins = bkBaseService.fetchOnlineIps(limitDay)
        logger.info("notifyWinNotLogin3Day|$limitDay|${logins.size}")
        val running = workspaceDao.fetchWorkspace(
            dslContext = dslContext,
            status = WorkspaceStatus.RUNNING,
            systemType = WorkspaceSystemType.WINDOWS_GPU
        ) ?: return
        running.parallelStream().forEach { workspace ->
            if ((workspace.lastStatusUpdateTime ?: LocalDateTime.now()) < limitDay &&
                workspace.hostName != null && workspace.hostName !in logins
            ) {
                logger.info(
                    "ready to notify when not login in 3 day " +
                        "|${workspace.workspaceName}|${workspace.lastStatusUpdateTime}|${workspace.hostName}"
                )
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspace.workspaceName,
                    operator = ADMIN_NAME,
                    action = WorkspaceAction.NOTIFY,
                    actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_SLEEP)
                )
                val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
                notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                    userIds = userIds.toMutableSet(),
                    workspaceName = workspace.workspaceName,
                    cc = mutableSetOf(workspace.createUserId),
                    projectId = workspace.projectId,
                    notifyTemplateCode = NOT_LOGIN_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                    bodyParams = mutableMapOf(
                        "cgsIp" to (workspace.hostName ?: ""),
                        "projectId" to (workspace.projectId),
                        "userId" to userIds.joinToString()
                    )
                )
            }
        }
    }

    // 未达到云桌面4星活跃：自然月（排除法定节假日）内小于 5 天达到日活标准的云桌面，并邮件提醒
    fun notifyWhenNot4StarActive() {
        val limitDay = holidayHelper.getLastWorkingDays(30).last()
        logger.info("autoDeleteWhenNot4StarActive|$limitDay")
        val actives = bkBaseService.fetchActiveIps(limitDay)
        val whiteListProject = redisCache.getSetMembers(
            RedisKeys.REDIS_WORKSPACE_AUTO_DELETE_WHITE_LIST_PROJECT
        ) ?: emptySet()
        val highEndWindowsList = redisCache.getSetMembers(
            RedisKeys.REDIS_WINDOWS_HIGH_END_MODEL
        ) ?: emptySet()
        workspaceJoinDao.limitFetchProjectWorkspace(
            dslContext = dslContext,
            limit = null,
            queryType = QueryType.WEB,
            search = WorkspaceSearch(
                workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU),
                size = highEndWindowsList.toList(),
                onFuzzyMatch = false
            )
        )?.parallelStream()?.forEach { workspace ->
            if (workspace.createTime > limitDay) {
                logger.info(
                    "skip check| workspace create time less than limit day|" +
                        "${workspace.workspaceName}|${workspace.createTime}|$limitDay"
                )
                return@forEach
            }
            if (actives[workspace.hostName] == null || actives[workspace.hostName]!! < 5) {
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspace.workspaceName,
                    operator = ADMIN_NAME,
                    action = WorkspaceAction.NOTIFY,
                    actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
                )
                val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
                notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                    userIds = userIds.toMutableSet(),
                    workspaceName = workspace.workspaceName,
                    cc = mutableSetOf(workspace.createUserId),
                    projectId = workspace.projectId,
                    notifyTemplateCode = NotifyControl.NOT_4_STAR_ACTIVE_AUTO_DELETE_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                    bodyParams = mutableMapOf(
                        "cgsIp" to (workspace.hostName ?: ""),
                        "projectId" to (workspace.projectId),
                        "userId" to userIds.joinToString()
                    )
                )
            }
        }
    }

    // 云桌面连续14天活跃度不足，总活跃时长小于10小时
    fun notifyWhenNotActiveIn14Days() {
        val limitDay = holidayHelper.getLastWorkingDays(14).last()
        logger.info("autoDeleteWhenNotActiveIn14Days|$limitDay")
        val actives = bkBaseService.fetchActiveTimes(limitDay)
        workspaceJoinDao.limitFetchProjectWorkspace(
            dslContext = dslContext,
            limit = null,
            queryType = QueryType.WEB,
            search = WorkspaceSearch(
                workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU),
                onFuzzyMatch = false
            )
        )?.parallelStream()?.forEach { workspace ->
            if (workspace.createTime > limitDay) {
                logger.info(
                    "skip check| workspace create time less than limit day|" +
                        "${workspace.workspaceName}|${workspace.createTime}|$limitDay"
                )
                return@forEach
            }
            if (actives[workspace.hostName] == null || actives[workspace.hostName]!! < 10 * 60) {
                workspaceOpHistoryDao.createWorkspaceHistory(
                    dslContext = dslContext,
                    workspaceName = workspace.workspaceName,
                    operator = ADMIN_NAME,
                    action = WorkspaceAction.NOTIFY,
                    actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.TIMEOUT_STOP)
                )
                val userIds = permissionService.getWorkspaceOwner(workspace.workspaceName)
                notifyControl.notify4UserAndCCRemoteDevManagerAndCCOwnerShareUser(
                    userIds = userIds.toMutableSet(),
                    workspaceName = workspace.workspaceName,
                    cc = mutableSetOf(workspace.createUserId),
                    projectId = workspace.projectId,
                    notifyTemplateCode = NotifyControl.NOT_ACTIVE_IN_14_DAYS_NOTIFY,
                    notifyType = mutableSetOf(RemoteDevNotifyType.EMAIL, RemoteDevNotifyType.RTX),
                    bodyParams = mutableMapOf(
                        "cgsIp" to (workspace.hostName ?: ""),
                        "projectId" to (workspace.projectId),
                        "userId" to userIds.joinToString()
                    )
                )
            }
        }
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

    // 检测过期的工作空间分享并且取消
    fun checkAndUnshared() {
        workspaceSharedDao.fetchExpireShare(dslContext).forEach { record ->
            workspaceCommon.unShareWorkspace(
                workspaceName = record.workspaceName,
                operator = record.operator,
                sharedUsers = listOf(record.sharedUser),
                mountType = WorkspaceMountType.START
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
        private const val defaultPageSize = 20
        private const val DEFAULT_WAIT_TIME = 60
        private const val DISCOUNT_TIME = 10000
    }
}
