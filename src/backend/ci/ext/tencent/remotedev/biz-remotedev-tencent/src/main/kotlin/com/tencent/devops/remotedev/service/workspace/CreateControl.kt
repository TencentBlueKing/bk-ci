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
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WindowsResourceTypeDao
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
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOrganization
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.remotedev.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.remotedev.pojo.remotedev.Devfile
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.projectworkspace.image.ImageManageService
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
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
    private val client: Client,
    private val dispatcher: SampleEventDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val workspaceResourceTypeDao: WindowsResourceTypeDao,
    private val redisCache: ConfigCacheService,
    private val whiteListService: WhiteListService,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val notifyControl: NotifyControl,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val softwareManageService: SoftwareManageService,
    private val imageManageService: ImageManageService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CreateControl::class.java)
        private const val WORKSPACE_PREFIX = "ins-"
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

        // 校验传入的镜像是否该项目自定义镜像，禁止跨项目使用
        val notProjectImage = workspaceCreate.imageCosFile.isNotBlank() &&
            (!imageManageService.checkBaseImage(workspaceCreate.imageCosFile) && !imageManageService.checkProjectImage(
                projectId,
                workspaceCreate.imageCosFile
            ))
        if (notProjectImage) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.IMAGE_NOT_FOUND_ERROR.errorCode,
                params = arrayOf(workspaceCreate.imageCosFile, projectId)
            )
        }

        val projectInfo = kotlin.runCatching {
            client.get(ServiceProjectResource::class).get(projectId)
        }.onFailure { logger.warn("get project $projectId info error|${it.message}") }
            .getOrElse { null }?.data ?: throw RemoteServiceException(
            "not find project $projectId", HTTP_400
        )

        // 检查项目配额
        val projectLimit = projectInfo.properties?.cloudDesktopNum ?: 1
        val workspaceNames = workspaceDao.fetchProjectWorkspaceName(
            dslContext = dslContext,
            projectId = projectInfo.englishName
        )
        val createCount = workspaceCreate.assignNames.ifEmpty { null }?.size ?: workspaceCreate.count
        if (workspaceNames.size + createCount > projectLimit) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.PROJECT_DESKTOP_RESOURCES_INSUFFICIENT.errorCode,
                params = arrayOf(projectLimit.toString())
            )
        }

        // 检查是否有特殊机型的配额限制
        windowsResourceConfigService.createCheckSpecLimit(
            windowsType = workspaceCreate.windowsType,
            projectId = projectInfo.englishName,
            workspaceNames = workspaceNames,
            createCount = createCount
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
            ownerType = when {
                workspaceCreate.ownerType?.projectUse() == true -> workspaceCreate.ownerType!!
                else -> WorkspaceOwnerType.PROJECT
            }
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
        SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
            .createStartCloudUser(creator, gameId.first)
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
                workspaceCreate.windowsType
            ).filter {
                when {
                    /*指定了cgsId*/
                    cgsId != null -> it.cgsId == cgsId
                    /*其余情况不应放开lock==true的情况*/
                    it.locked == true -> false
                    /*不同云区域的忽略*/
//                    it.internal != QuotaType.parse(ownerType).getInternal() -> false
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
                        ownerType = ownerType,
                        quotaType = QuotaType.parse(zoneType)
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
                quotaType = QuotaType.parse(zoneType)
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
                ownerType = ownerType,
                quotaType = QuotaType.parse(zoneType)
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
        ownerType: WorkspaceOwnerType,
        quotaType: QuotaType
    ) {
        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        logger.info(
            "doCreateWorkspace|$i|$workspaceName|$workspaceCreate|$projectId|$creator|$owner|" +
                "$windowsConfig|$organization|$zoneId|$appName|$gameId|$cgsId|$ownerType|$quotaType"
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
            imageId = workspaceCreate.imageCosFile,
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
                    quotaType = quotaType,
                    pvcs = workspaceCreate.pvcs,
                    specifyTaints = workspaceCreate.specifyTaints
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
            loadWorkspaceWithPersonalWindows(userId = userId, workspaceCreate = workspaceCreate)
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
                workspaceDao.updateWorkspaceIp(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    hostName = event.environmentHost,
                    ip = event.environmentIp
                )
                workspaceDao.updateWorkspaceStatus(
                    dslContext = transactionContext,
                    workspaceName = event.workspaceName,
                    status = ws.workspaceSystemType.afterCreateStatus(ws.ownerType)
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
                    event.environmentHost,
                    event.macAddress
                )
            }

            if (ws.workspaceSystemType.needSafeInitialization()) {
                softwareManageService.safeInitialization(ws.projectId, event.userId, event.workspaceName)
            }

            // 创建成功时给 cmdb 添加字段方便监控检索
            val ip = event.environmentIp
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
                if (ws.ownerType.personalUse()) {
                    workspaceCommon.makeDiskMount(ip, event.userId)
                }

                if (ws.ownerType.projectPublicUse()) {
                    val winInfo = checkNotNull(
                        workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, ws.workspaceName)
                    )
                    val winConfig = workspaceResourceTypeDao.fetchAny(dslContext, winInfo.winConfigId)
                    workspaceCommon.devxEnvNodeInit(
                        userId = event.userId,
                        projectId = ws.projectId,
                        workspaceName = ws.workspaceName,
                        ip = ip,
                        size = winConfig?.size ?: ""
                    )
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
        uid: String,
        bak: Boolean
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

        val vm = workspaceCommon.realtimeStartCloudResourceList().find { it.cgsId == taskInfo.vmCreateResp?.cgsIp }
            ?: kotlin.run {
                logger.warn("createWinWorkspaceByVm not find cgsIp ${taskInfo.vmCreateResp?.cgsIp}")
                return false
            }

        val copy = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = oldWorkspaceName)
        val projectId = when {
            copy != null -> copy.projectId
            else -> checkNotNull(projectCode)
        }

        val checkOwnerType = when {
            copy != null -> copy.ownerType
            else -> checkNotNull(ownerType)
        }
        val gameId = workspaceCommon.getGameIdAndAppId(projectId, checkOwnerType)
        val workspaceName = if (bak) checkNotNull(oldWorkspaceName) else generateWorkspaceName()
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
        if (copy != null) {
            // 当存在引用副本的情况，将细分多种情况
            val bakName = when {
                /*克隆副本，但不删除副本*/
                !bak -> "clone.${copy.workspaceName}.bak.${LocalDateTime.now()}"
                /*升级副本，升级后需删除副本*/
                copy.status.checkUpgrading() -> "upgrade.${copy.workspaceName}.bak.${LocalDateTime.now()}"
                /*修复副本，旧副本立即删除*/
                else -> "fix.${copy.workspaceName}.bak.${LocalDateTime.now()}"
            }
            ws.bakWorkspaceName = bakName
            if (bak) {
                // 备份windows config
                workspaceWindowsDao.bakWindowsConfig(dslContext, copy.workspaceName, bakName)
                // 备份分享信息
                workspaceSharedDao.bakWorkspaceShareInfo(dslContext, copy.workspaceName, bakName)
                workspaceDao.bakWorkspace(
                    dslContext = dslContext,
                    workspaceName = copy.workspaceName,
                    bakName = bakName,
                    status = if (copy.status.checkUpgrading()) WorkspaceStatus.UNUSED else WorkspaceStatus.DELETED
                )
                SpringContextUtil.getBean(ServiceWorkspaceDispatchInterface::class.java)
                    .deleteWorkspace(userId, workspaceName, bakName)
            }
        }
        when {
            checkOwnerType.projectUse() -> {
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

            else -> {
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
        SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
            .createStartCloudUser(userId, gameId.first)
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
        cgsId: String? = null
    ) {
        logger.info("loadWorkspaceWithPersonalWindows|$userId|$workspaceCreate|$cgsId")
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

        val windowsZone = windowsResourceConfigService.getAvailableZone(
            zoneId = workspaceCreate.windowsZone,
            type = WindowsResourceZoneConfigType.DEVCLOUD
        ) ?: throw ErrorCodeException(
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
            /* 个人云桌面强制指定为devcloud专区 */
            zoneType = WindowsResourceZoneConfigType.DEVCLOUD,
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
        var projectId = ""
        if (projectInfo == null) {
            kotlin.runCatching {
                client.get(ServiceTxProjectResource::class).getRemoteDevUserProject(userId)
            }.onFailure { logger.warn("create user project fail ${it.message}", it) }.getOrNull().let {
                if (it?.data == null) {
                    logger.warn("create user project fail ${it?.message}")
                }
                remoteDevSettingDao.updateProjectId(dslContext, userId, it?.data?.englishName ?: "")
                projectId = it?.data?.englishName ?: ""
            }
        }

        if (projectId.isNotBlank() || projectInfo?.properties?.remotedev != true) {
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

    private fun startCloudResourceCountCheck(type: String) =
        workspaceCommon.realtimeStartCloudResourceList().filter {
            it.status == 11 &&
                it.machineType == type
        }

    private fun generateWorkspaceName(): String {
        return "${WORKSPACE_PREFIX}${UUIDUtil.generate().takeLast(Constansts.workspaceNameSuffixLimitLen)}"
    }
}
