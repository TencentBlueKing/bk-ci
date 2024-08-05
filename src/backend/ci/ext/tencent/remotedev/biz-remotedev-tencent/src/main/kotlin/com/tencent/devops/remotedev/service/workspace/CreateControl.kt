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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.HTTP_400
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceWorkspaceDispatchInterface
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOrganization
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.remotedev.Devfile
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmRespData
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.software.SoftwareManageService
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class CreateControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val permissionService: PermissionService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val redisCache: RedisCacheService,
    private val whiteListService: WhiteListService,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val notifyControl: NotifyControl,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val softwareManageService: SoftwareManageService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CreateControl::class.java)
        private const val BLANK_TEMPLATE_YAML_NAME = "BLANK"
        private const val BLANK_TEMPLATE_ID = 1

        fun sumResourceVmFree(res: List<ResourceVmRespData>?, zoneShortName: String, size: String): Int? {
            return res?.filter {
                it.zoneId.startsWith(zoneShortName) &&
                    it.machineResources?.any { ma -> ma.machineType == size } == true
            }?.sumOf {
                it.machineResources
                    ?.filter { res -> res.machineType == size }
                    ?.sumOf { ma -> ma.free ?: 0 } ?: 0
            }
        }
    }

    // 用于控制台上创建
    @ActionAuditRecord(
        actionId = ActionId.CGS_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_CREATE_CONTENT
    )
    fun projectCreateWorkspace(
        pmUserId: String,
        projectId: String,
        cgsId: String?,
        workspaceCreate: WindowsWorkspaceCreate,
        zoneType: WindowsResourceZoneConfigType?
    ) {
        logger.info("start async create workspace |$pmUserId|$projectId|$cgsId|$workspaceCreate|$zoneType")
        val windowsConfig = windowsResourceConfigService.getTypeConfig(workspaceCreate.windowsType)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(workspaceCreate.windowsType)
            )

        if (windowsConfig.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(workspaceCreate.windowsType)
            )
        }
        val windowsZone = windowsResourceConfigService.getZoneConfig(workspaceCreate.windowsZone)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(workspaceCreate.windowsZone)
            )
        if (windowsZone.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(workspaceCreate.windowsZone)
            )
        }
        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project $projectId", HTTP_400
        )

        // 检查项目配额
        val projectLimit = projectInfo.properties?.cloudDesktopNum
            ?: redisCache.get(RedisKeys.REDIS_PROJECT_WIN_COUNT_LIMIT)?.toInt()
            ?: 20
        val workspaceNames = workspaceDao.fetchUserWorkspaceName(
            dslContext = dslContext,
            projectId = projectInfo.englishName,
            ownerType = WorkspaceOwnerType.PROJECT
        )
        if (workspaceNames.size + workspaceCreate.count > projectLimit) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.PROJECT_DESKTOP_RESOURCES_INSUFFICIENT.errorCode,
                params = arrayOf(projectLimit.toString())
            )
        }

        // 检查是否有特殊机型的配额限制
        windowsResourceConfigService.createCheckSpecLimit(
            workspaceCreate.windowsType,
            projectInfo.englishName,
            workspaceNames
        )

        prepareWindowsCreate(
            creator = pmUserId,
            projectId = projectId,
            windowsZone = windowsZone,
            zoneType = zoneType,
            workspaceCreate = workspaceCreate,
            cgsId = cgsId,
            windowsConfig = windowsConfig,
            organization = WorkspaceOrganization(
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                groupName = null,
                projectName = projectInfo.projectName,
                businessLineName = projectInfo.businessLineName
            ),
            ownerType = WorkspaceOwnerType.PROJECT
        )
    }

    private fun prepareWindowsCreate(
        creator: String,
        projectId: String,
        windowsZone: WindowsResourceZoneConfig,
        zoneType: WindowsResourceZoneConfigType?,
        workspaceCreate: WindowsWorkspaceCreate,
        cgsId: String?,
        windowsConfig: WindowsResourceTypeConfig,
        organization: WorkspaceOrganization,
        ownerType: WorkspaceOwnerType
    ): Boolean {
        // 自定义镜像检查是否有相对的显卡
        // 非自定义镜像先检查池子里是否有已经生产出来的可以直接用，没有再去看显卡
        val newNum: Int

        val gameId = workspaceCommon.getGameIdAndAppId(projectId, ownerType)
        val availableZone = windowsResourceConfigService.getAvailableZone(windowsZone, zoneType) ?: run {
            logger.warn("not has available zone ${windowsZone.zoneShortName}")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                params = arrayOf("not has available zone ${windowsZone.zoneShortName}")
            )
        }

        val generateWorkspaceName = workspaceCreate.assignNames.ifEmpty {
            buildList {
                repeat(workspaceCreate.count) {
                    add(generateWorkspaceName())
                }
            }
        }
        val zoneIds = mutableListOf<String>()
        if (workspaceCreate.imageCosFile.isBlank()) {
            val spec = lazy { windowsResourceConfigService.getAllSpecZoneShortName() }
            val resource = startCloudResourceCountCheck(
                workspaceCreate.windowsType, QuotaType.parse(ownerType)
            ).filter {
                when {
                    /*指定了cgsId*/
                    cgsId != null -> it.cgsId == cgsId
                    /*其余情况不应放开lock==true的情况*/
                    it.locked == true -> false
                    /*不同云区域的忽略*/
                    it.internal != QuotaType.parse(ownerType).getInternal() -> false
                    /*使用普通区域，则应到避免落入特殊区域*/
                    availableZone.type == WindowsResourceZoneConfigType.DEFAULT && it.zoneId in spec.value -> false
                    /*特殊情况，限制具体的区域id*/
                    availableZone.type != WindowsResourceZoneConfigType.DEFAULT -> {
                        it.zoneId == availableZone.zoneShortName
                    }
                    /*默认情况使用普通区域，处理逻辑不变*/
                    else -> {
                        it.zoneId.replace(Regex("\\d+"), "") == availableZone.zoneShortName
                    }
                }
            }

            if (resource.count() >= generateWorkspaceName.size) {
                repeat(generateWorkspaceName.size) { i ->
                    val workspaceName = generateWorkspaceName[i]
                    val owner = workspaceCreate.assignOwners.getOrNull(i)
                    doCreateWorkspace(
                        i = i,
                        workspaceCreate = workspaceCreate,
                        workspaceName = workspaceName,
                        projectId = projectId,
                        creator = creator,
                        owner = owner,
                        windowsConfig = windowsConfig,
                        organization = organization,
                        zoneId = resource[i].zoneId,
                        appName = gameId.first,
                        gameId = gameId.second,
                        cgsId = cgsId,
                        ownerType = ownerType
                    )
                }
                return true
            }
            if (cgsId != null) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                    params = arrayOf("not find $cgsId in listcgs")
                )
            }
            zoneIds.addAll(resource.map { it.zoneId })
            newNum = generateWorkspaceName.size - resource.count()
        } else {
            // 自定义镜像都是新生产的
            newNum = generateWorkspaceName.size
        }

        zoneIds.addAll(
            windowsResourceConfigService.createCheckWhenWinNotAlready(
                windowsZone = availableZone,
                windowsConfig = windowsConfig,
                newNum = newNum,
                quotaType = QuotaType.parse(ownerType)
            )
        )
        repeat(generateWorkspaceName.size) { i ->
            val workspaceName = generateWorkspaceName[i]
            val owner = workspaceCreate.assignOwners.getOrNull(i)
            doCreateWorkspace(
                i = i,
                workspaceCreate = workspaceCreate,
                workspaceName = workspaceName,
                projectId = projectId,
                creator = creator,
                owner = owner,
                windowsConfig = windowsConfig,
                organization = organization,
                zoneId = zoneIds[i],
                appName = gameId.first,
                gameId = gameId.second,
                cgsId = cgsId,
                ownerType = ownerType
            )
        }
        return false
    }

    private fun doCreateWorkspace(
        i: Int,
        workspaceName: String,
        workspaceCreate: WindowsWorkspaceCreate,
        projectId: String,
        creator: String,
        owner: String?,
        windowsConfig: WindowsResourceTypeConfig,
        organization: WorkspaceOrganization,
        zoneId: String,
        appName: String,
        gameId: Long,
        cgsId: String?,
        ownerType: WorkspaceOwnerType
    ) {
        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        logger.info(
            "doCreateWorkspace|$i|$workspaceName|$workspaceCreate|$projectId|$creator|$owner|" +
                "$windowsConfig|$organization|$zoneId|$appName|$gameId|$cgsId|$ownerType"
        )
        if (!owner.isNullOrBlank()) {
            workspaceSharedDao.batchCreate(
                dslContext = dslContext, workspaceName = workspaceName, operator = creator, assigns = listOf(
                    ProjectWorkspaceAssign(owner, WorkspaceShared.AssignType.OWNER, null)
                ), resourceId = ""
            )
        }
        val ws = Workspace(
            workspaceId = null,
            workspaceName = workspaceName,
            projectId = projectId,
            createUserId = creator,
            hostName = "",
            workspaceMountType = mountType,
            workspaceSystemType = systemType,
            ownerType = ownerType,
            winConfigId = windowsConfig.id?.toInt(),
            imageId = workspaceCreate.imageId,
            zoneId = zoneId
        )

        workspaceDao.createWorkspace(
            workspace = ws,
            workspaceStatus = WorkspaceStatus.PREPARING,
            organization = organization,
            dslContext = dslContext
        )

        // 审计
        ActionAuditContext.current().addInstanceInfo(
            workspaceName,
            workspaceName,
            null,
            ws
        )

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = creator,
                traceId = bizId,
                workspaceName = ws.workspaceName,
                devFile = Devfile(
                    zoneId = zoneId,
                    machineType = windowsConfig.size,
                    cgsId = cgsId,
                    imageCosFile = workspaceCreate.imageCosFile,
                    quotaType = QuotaType.parse(ws.ownerType)
                ),
                projectId = projectId,
                mountType = mountType,
                ownerType = ws.ownerType,
                delayMills = i * 2000,
                appName = appName,
                gameId = gameId
            )
        )
    }

    fun devcloudCreateWorkspace(
        userId: String,
        workspaceCreate: WindowsWorkspaceCreate,
        projectId: String?,
        zoneType: WindowsResourceZoneConfigType?
    ): Boolean {
        logger.info("create workspace from devcloud |$userId|$workspaceCreate|$projectId")
        if (projectId != null) {
            projectCreateWorkspace(
                pmUserId = userId,
                projectId = projectId,
                cgsId = null,
                workspaceCreate = workspaceCreate,
                zoneType = zoneType
            )
        } else {
            loadWorkspaceWithPersonalWindows(userId = userId, workspaceCreate = workspaceCreate, zoneType = zoneType)
        }
        return true
    }

    // 处理创建工作空间逻辑，用于客户端上创建
    @ActionAuditRecord(
        actionId = ActionId.CGS_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_CREATE_CONTENT
    )
    @Deprecated("待废弃")
    fun createWorkspace(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): WorkspaceResponse {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")
        permissionService.checkUserCreate(userId)

        throw ErrorCodeException(
            errorCode = ErrorCodeEnum.CLIENT_NEED_UPDATED.errorCode,
            params = arrayOf(redisCache.get(RedisKeys.REDIS_CLIENT_INSTALL_URL).toString()),
            defaultMessage = ErrorCodeEnum.CLIENT_NEED_UPDATED.formatErrorMessage
        )
    }

    fun afterCreateWorkspace(event: RemoteDevUpdateEvent) {
        val ws = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = event.workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(event.workspaceName)
            )
        kotlin.runCatching {
            afterCreateWorkspace(event, ws)
        }.onFailure {
            logger.error("create workspace ${event.workspaceName} error ${it.message}", it)
            workspaceCreateFail(ws, event)
        }
    }

    // k8s创建workspace后回调的方法
    fun afterCreateWorkspace(event: RemoteDevUpdateEvent, ws: WorkspaceRecord) {
        if (event.status) {
            val opActions = kotlin.runCatching {
                arrayOf(
                    WorkspaceAction.CREATE to getOpHistoryCreate(
                        ws.workspaceSystemType
                    ),
                    WorkspaceAction.START to workspaceCommon.getOpHistory(OpHistoryCopyWriting.FIRST_START)
                )
            }.getOrElse { emptyArray() }
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                workspaceDao.updateWorkspaceStatus(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    status = ws.workspaceSystemType.afterCreateStatus(ws.ownerType),
                    hostName = event.environmentHost
                )
                workspaceHistoryDao.createWorkspaceHistory(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    startUserId = event.userId,
                    lastSleepTimeCost = 0
                )
                opActions.forEach { (action, actionMessage) ->
                    workspaceOpHistoryDao.createWorkspaceHistory(
                        dslContext = transactionContext,
                        workspaceName = event.workspaceName,
                        operator = event.userId,
                        action = action,
                        actionMessage = actionMessage
                    )
                }
            }

            workspaceCommon.updateWorkspaceWinDetail(
                ws = ws,
                workspaceName = event.workspaceName
            )

            if (ws.workspaceSystemType.checkWindows()) {
                workspaceWindowsDao.updateWindowsResourceId(
                    dslContext,
                    event.workspaceName,
                    event.resourceId,
                    event.environmentIp,
                    event.macAddress
                )
            }

            if (ws.workspaceSystemType.needSafeInitialization()) {
                softwareManageService.safeInitialization(ws.projectId, event.userId, event.workspaceName)
            }

            // 创建成功时给 cmdb 添加字段方便监控检索
            val ip = event.environmentIp?.substringAfter(".")
            if (!ip.isNullOrBlank() && ws.workspaceSystemType.checkWindows()) {
                workspaceCommon.updateHostMonitor(
                    workspaceName = ws.workspaceName,
                    props = workspaceCommon.genWorkspaceCCInfo(
                        ws.projectId,
                        ws.displayName.ifBlank { ws.workspaceName },
                        null
                    ),
                    type = ws.workspaceSystemType
                )

                // 个人云桌面创建成功后做异步设置，团队项目改到分配时做L盘挂载
                if (ws.ownerType == WorkspaceOwnerType.PERSONAL) {
                    workspaceCommon.makeDiskMount(ip, event.userId)
                }

                // 给有cfs的机器绑定权限组
                tCloudCfsService.addOrRemoveCfsPermissionRule(ws.projectId, ip, false)

                // 关联tgit相关
                gitProxyTGitService.addOrRemoveAclIp(ws.projectId, setOf(ip), false, null)
            }

            if (!ws.workspaceSystemType.afterCreateNeedWs(ws.ownerType)) {
                // 直接return 不做websocket
                return
            }

            // websocket 通知成功
        } else {
            // 创建失败
            // websocket 通知失败
            logger.warn("create workspace ${event.workspaceName} failed")
            workspaceCreateFail(ws, event)
        }

        notifyControl.dispatchWebsocketPushEvent(
            userId = event.userId,
            workspaceName = event.workspaceName,
            workspaceHost = event.environmentHost,
            errorMsg = event.errorMsg,
            type = WebSocketActionType.WORKSPACE_CREATE,
            status = event.status,
            action = WorkspaceAction.START,
            systemType = ws.workspaceSystemType,
            workspaceMountType = ws.workspaceMountType,
            ownerType = ws.ownerType,
            projectId = ws.projectId
        )
    }

    fun createWinWorkspaceByVm(
        userId: String,
        oldWorkspaceName: String?,
        projectCode: String?,
        ownerType: WorkspaceOwnerType?,
        uid: String
    ): Boolean {
        val taskInfo = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getTaskInfoByUid(uid).data!!
        }.onFailure {
            logger.warn("createWinWorkspaceByVm not find uid $uid")
            return false
        }.getOrThrow()
        val envId = taskInfo.vmCreateResp?.envId ?: kotlin.run {
            logger.warn("createWinWorkspaceByVm check envId fail $taskInfo")
            return false
        }
        val vmInfo = kotlin.runCatching {
            SpringContextUtil.getBean(ServiceStartCloudInterface::class.java).getWorkspaceInfoByEid(envId).data!!
        }.onFailure {
            logger.warn("createWinWorkspaceByVm not find envId $uid")
            return false
        }.getOrThrow()

        logger.info("createWinWorkspaceByVm get vm info $vmInfo")

        // 检查vm状态，如果不是正常的，中止自动修复。转失败状态
        if (vmInfo.status != EnvStatusEnum.running) {
            logger.warn("createWinWorkspaceByVm vm not running, fix failed.")
            return false
        }

        val vm = workspaceCommon.syncStartCloudResourceList().find { it.cgsId == taskInfo.vmCreateResp?.cgsIp }
            ?: kotlin.run {
                logger.warn("createWinWorkspaceByVm not find cgsIp ${taskInfo.vmCreateResp?.cgsIp}")
                return false
            }

        val oldWs = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = oldWorkspaceName)
        val projectId = when {
            oldWs != null -> oldWs.projectId
            else -> checkNotNull(projectCode)
        }

        val checkOwnerType = when {
            oldWs != null -> oldWs.ownerType
            else -> checkNotNull(ownerType)
        }
        val gameId = workspaceCommon.getGameIdAndAppId(projectId, checkOwnerType)
        val workspaceName = oldWorkspaceName ?: generateWorkspaceName()
        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        val windowsConfig = windowsResourceConfigService.getTypeConfig(vm.machineType)
            ?: kotlin.run {
                logger.warn("createWinWorkspaceByVm not find machineType ${vm.machineType}")
                return false
            }
        val ws = Workspace(
            workspaceId = null,
            workspaceName = workspaceName,
            projectId = projectId,
            createUserId = userId,
            hostName = "",
            workspaceMountType = mountType,
            workspaceSystemType = systemType,
            ownerType = checkOwnerType,
            winConfigId = windowsConfig.id?.toInt(),
            zoneId = vm.zoneId
        )
        if (oldWs != null) {
            // 直接硬删除记录。新的工作空间会复用原先的name
            val bakName = "$workspaceName.bak.${LocalDateTime.now()}"
            // 备份windows config
            workspaceWindowsDao.bakWindowsConfig(dslContext, oldWs.workspaceName, bakName)
            // 备份分享信息
            workspaceSharedDao.bakWorkspaceShareInfo(dslContext, oldWs.workspaceName, bakName)
            ws.bakWorkspaceName = bakName
            workspaceDao.bakWorkspace(
                dslContext = dslContext,
                workspaceName = oldWs.workspaceName,
                bakName = bakName,
                status = if (oldWs.status.checkUpgrading()) WorkspaceStatus.UNUSED else WorkspaceStatus.DELETED
            )
            SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java)
                .deleteWorkspace(userId, workspaceName, bakName)
        }
        when (checkOwnerType) {
            WorkspaceOwnerType.PROJECT -> {
                val projectInfo = kotlin.runCatching {
                    client.get(ServiceProjectResource::class).get(projectId)
                }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
                    .getOrElse { null }?.data ?: kotlin.run {
                    logger.warn("createWinWorkspaceByVm not find projectInfo $projectId")
                    return false
                }
                workspaceDao.createWorkspace(
                    workspace = ws,
                    workspaceStatus = WorkspaceStatus.PREPARING,
                    organization = WorkspaceOrganization(
                        bgName = projectInfo.bgName,
                        deptName = projectInfo.deptName,
                        centerName = projectInfo.centerName,
                        groupName = null,
                        projectName = projectInfo.projectName,
                        businessLineName = projectInfo.businessLineName
                    ),
                    dslContext = dslContext
                )
            }

            WorkspaceOwnerType.PERSONAL -> {
                val userInfo = kotlin.runCatching {
                    client.get(ServiceTxUserResource::class).get(userId)
                }.onFailure { logger.warn("get user $userId info error|${it.message}") }
                    .getOrElse { null }?.data
                workspaceDao.createWorkspace(
                    workspace = ws,
                    workspaceStatus = WorkspaceStatus.PREPARING,
                    organization = WorkspaceOrganization(
                        bgName = userInfo?.bgName,
                        deptName = userInfo?.deptName,
                        centerName = userInfo?.centerName,
                        groupName = userInfo?.groupName,
                        projectName = ws.projectId ?: "",
                        businessLineName = userInfo?.businessLineName
                    ),
                    dslContext = dslContext
                )
            }
        }

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = ws.workspaceName,
                devFile = Devfile(
                    uid = uid,
                    environmentUid = envId
                ),
                projectId = projectId,
                mountType = mountType,
                ownerType = ws.ownerType,
                delayMills = 2000,
                appName = gameId.first,
                gameId = gameId.second
            )
        )
        return true
    }

    private fun workspaceCreateFail(
        ws: WorkspaceRecord,
        event: RemoteDevUpdateEvent
    ) {
        if (ws.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            val windowsInfo = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, ws.workspaceName)
            val windowsConfig = windowsInfo?.winConfigId?.let {
                windowsResourceConfigService.getTypeConfig(it)
            }
            val envId = event.environmentUid ?: ""
            workspaceCommon.updateStatus2DeliveringFailed(
                workspace = ws,
                action = WorkspaceAction.CREATE,
                notifyTemplateCode = "WINDOWS_GPU_CREATE_FAILED",
                noticeParams = mapOf(
                    "imageId" to (windowsInfo?.imageId ?: ""), "envId" to envId,
                    "zoneId" to (windowsInfo?.zoneId ?: ""),
                    "windowsSize" to (windowsConfig?.size ?: "")
                )
            )
        } else {
            workspaceDao.deleteWorkspace(ws.workspaceName, dslContext)
        }
    }

    private fun getOpHistoryCreate(type: WorkspaceSystemType) = when (type) {
        WorkspaceSystemType.WINDOWS_GPU -> workspaceCommon.getOpHistory(OpHistoryCopyWriting.CREATE_WINDOWS)
        else -> ""
    }

    fun loadWorkspaceWithPersonalWindows(
        userId: String,
        workspaceCreate: WindowsWorkspaceCreate,
        zoneType: WindowsResourceZoneConfigType?,
        cgsId: String? = null
    ) {
        logger.info("loadWorkspaceWithPersonalWindows|$userId|$workspaceCreate|$zoneType|$cgsId")
        val windowsConfig = windowsResourceConfigService.getTypeConfig(workspaceCreate.windowsType)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(workspaceCreate.windowsType.toString())
            )

        if (windowsConfig.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(workspaceCreate.windowsType.toString())
            )
        }

        val windowsZone = windowsResourceConfigService.getZoneConfig(workspaceCreate.windowsZone)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(workspaceCreate.windowsZone)
            )
        if (windowsZone.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(workspaceCreate.windowsZone)
            )
        }

        val projectId = checkOrInitPersonalProject(userId)

        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId)
        }.onFailure { logger.warn("get user $userId info error|${it.message}") }
            .getOrElse { null }?.data

        val workspaceNames = workspaceCreate.assignNames.ifEmpty {
            buildList { repeat(workspaceCreate.count) { add(generateWorkspaceName()) } }
        }

        whiteListService.windowsGpuCheck(userId, workspaceNames.size)
        prepareWindowsCreate(
            creator = userId,
            projectId = projectId,
            windowsZone = windowsZone,
            zoneType = zoneType,
            workspaceCreate = workspaceCreate,
            cgsId = cgsId,
            windowsConfig = windowsConfig,
            organization = WorkspaceOrganization(
                bgName = userInfo?.bgName,
                deptName = userInfo?.deptName,
                centerName = userInfo?.centerName,
                groupName = userInfo?.groupName,
                projectName = projectId,
                businessLineName = userInfo?.businessLineName
            ),
            ownerType = WorkspaceOwnerType.PERSONAL
        )
    }

    private fun checkOrInitPersonalProject(userId: String): String {
        val userProjectId = "_$userId"
        val projectInfo = getProjectInfo(userProjectId)

        if (projectInfo == null || projectInfo.properties?.remotedev != true) {
            initPersonalProject(userId, userProjectId)
        }

        remoteDevSettingDao.fetchOneSetting(dslContext, userId)
        return userProjectId
    }

    private fun getProjectInfo(userProjectId: String) = kotlin.runCatching {
        client.get(ServiceProjectResource::class).get(userProjectId)
    }.onFailure { logger.warn("get project $userProjectId info error|${it.message}") }
        .getOrThrow().data

    private fun initPersonalProject(userId: String, userProjectId: String) {
        val ok = client.get(ServiceTxProjectResource::class).updateRemotedev(
            userId = userId,
            projectCode = userProjectId,
            addcloudDesktopNum = null,
            enable = true
        ).data

        if (ok != true) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.USERINFO_ERROR.errorCode,
                params = arrayOf("init user project fail.")
            )
        }
    }

    private fun startCloudResourceCountCheck(type: String, quotaType: QuotaType) =
        workspaceCommon.syncStartCloudResourceList().filter {
            it.status == 11 &&
                it.machineType == type
//                it.zoneId.replace(Regex("\\d+"), "") == zone &&
//                it.locked != true &&
//                it.internal == quotaType.getInternal()
        }

    private fun generateWorkspaceName(): String {
        return "ins-${UUIDUtil.generate().takeLast(Constansts.workspaceNameSuffixLimitLen)}"
    }
}
