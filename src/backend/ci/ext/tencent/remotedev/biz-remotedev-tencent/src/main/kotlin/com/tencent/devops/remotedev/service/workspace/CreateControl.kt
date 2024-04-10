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
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceCreateEvent
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Devfile
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmReq
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.ResourceVmRespData
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.remotedev.common.Constansts
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.RemoteDevCommonConfig
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WindowsSpecResourceDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.event.RemoteDevUpdateEvent
import com.tencent.devops.remotedev.service.BKCCService
import com.tencent.devops.remotedev.service.BkTicketService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_OFFICIAL_DEVFILE_KEY
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.remotedev.utils.CommonUtil
import com.tencent.devops.remotedev.utils.DevfileUtil
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
@Suppress("ALL")
class CreateControl @Autowired constructor(
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val remoteDevGitTransfer: RemoteDevGitTransfer,
    private val permissionService: PermissionService,
    private val client: Client,
    private val dispatcher: RemoteDevDispatcher,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val redisHeartBeat: RedisHeartBeat,
    private val remoteDevBillingDao: RemoteDevBillingDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val redisCache: RedisCacheService,
    private val bkTicketServie: BkTicketService,
    private val whiteListService: WhiteListService,
    private val commonConfig: RemoteDevCommonConfig,
    private val workspaceCommon: WorkspaceCommon,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val deliverControl: DeliverControl,
    private val bkccService: BKCCService,
    private val windowsSpecResourceDao: WindowsSpecResourceDao,
    private val notifyControl: NotifyControl,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService
) {
    private val executor = Executors.newCachedThreadPool()

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
    fun asyncCreateWorkspace(
        pmUserId: String,
        projectId: String,
        cgsId: String?,
        autoAssign: Boolean?,
        workspaceCreate: ProjectWorkspaceCreate
    ) {
        logger.info("start async create workspace |$pmUserId|$projectId|$cgsId|$autoAssign|$workspaceCreate")
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
        val allSpecSize = windowsResourceConfigService.getAllType(true, true).map { it.size }.toSet()
        if (workspaceCreate.windowsType.trim() in allSpecSize) {
            val specQuota = windowsSpecResourceDao.fetchQuota(
                dslContext = dslContext,
                projectId = projectInfo.englishName,
                size = workspaceCreate.windowsType.trim()
            )
            if (specQuota != null) {
                val count = workspaceWindowsDao.fetchUsedSizeCount(
                    dslContext = dslContext,
                    workspaceNames = workspaceNames,
                    size = workspaceCreate.windowsType.trim()
                )
                if (count >= specQuota) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.PROJECT_DESKTOP_SPEC_RESOURCES_INSUFFICIENT.errorCode,
                        params = arrayOf(workspaceCreate.windowsType.trim(), specQuota.toString(), count.toString())
                    )
                }
            } else {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.PROJECT_DESKTOP_SPEC_RESOURCES_INSUFFICIENT.errorCode,
                    params = arrayOf(workspaceCreate.windowsType.trim(), "0", "0")
                )
            }
        }

        // 自定义镜像检查是否有相对的显卡
        // 非自定义镜像先检查池子里是否有已经生产出来的可以直接用，没有再去看显卡
        var newNum = 0
        if (workspaceCreate.imageCosFile.isBlank()) {
            val resourceCount = startCloudResourceCountCheck(workspaceCreate.windowsType, workspaceCreate.windowsZone)
            if (cgsId != null || resourceCount > workspaceCreate.count) {
                doCreateWorkspace(
                    workspaceCreate = workspaceCreate,
                    projectId = projectId,
                    pmUserId = pmUserId,
                    windowsConfig = windowsConfig,
                    projectInfo = projectInfo,
                    windowsZone = windowsZone,
                    cgsId = cgsId,
                    autoAssign = autoAssign
                )
                return
            }
            newNum = workspaceCreate.count - resourceCount
        } else {
            // 自定义镜像都是新生产的
            newNum = workspaceCreate.count
        }

        val data = client.get(ServiceStartCloudResource::class).getResourceVm(
            ResourceVmReq(
                zoneId = windowsZone.zoneShortName,
                machineType = windowsConfig.size
            )
        ).data
        val free = sumResourceVmFree(
            res = data,
            zoneShortName = windowsZone.zoneShortName,
            size = windowsConfig.size
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.ZONE_VM_RESOURCE_NOT_ENOUGH.errorCode,
            params = arrayOf(
                windowsZone.zone,
                windowsConfig.size,
                "0",
                newNum.toString()
            )
        )
        if (free < newNum) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.ZONE_VM_RESOURCE_NOT_ENOUGH.errorCode,
                params = arrayOf(
                    windowsZone.zone,
                    windowsConfig.size,
                    free.toString(),
                    newNum.toString()
                )
            )
        }

        doCreateWorkspace(
            workspaceCreate = workspaceCreate,
            projectId = projectId,
            pmUserId = pmUserId,
            windowsConfig = windowsConfig,
            projectInfo = projectInfo,
            windowsZone = windowsZone,
            cgsId = cgsId,
            autoAssign = autoAssign
        )
    }

    private fun doCreateWorkspace(
        workspaceCreate: ProjectWorkspaceCreate,
        projectId: String,
        pmUserId: String,
        windowsConfig: WindowsResourceTypeConfig,
        projectInfo: ProjectVO,
        windowsZone: WindowsResourceZoneConfig,
        cgsId: String?,
        autoAssign: Boolean?
    ) {
        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        for (i in 0 until workspaceCreate.count) {
            logger.info("createWorkspace|mountType|$mountType")
            val workspaceName = if (CommonUtil.ifProjectPersonal(projectId)) {
                generateWorkspaceName(projectId.removePrefix("_"))
            } else {
                generateWorkspaceName(projectId)
            }
            val ws = Workspace(
                workspaceId = null,
                workspaceName = workspaceName,
                projectId = projectId,
                createUserId = if (CommonUtil.ifProjectPersonal(projectId)) {
                    projectId.removePrefix("_")
                } else {
                    pmUserId
                },
                hostName = "",
                workspaceMountType = mountType,
                workspaceSystemType = systemType,
                ownerType = if (CommonUtil.ifProjectPersonal(projectId)) {
                    WorkspaceOwnerType.PERSONAL
                } else {
                    WorkspaceOwnerType.PROJECT
                },
                gpu = windowsConfig.gpu,
                cpu = windowsConfig.cpu,
                memory = windowsConfig.memory,
                disk = windowsConfig.workspaceDisk(),
                winConfigId = windowsConfig.id?.toInt(),
                imageId = workspaceCreate.imageId
            )

            workspaceDao.createWorkspace(
                workspace = ws,
                workspaceStatus = WorkspaceStatus.PREPARING,
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                groupName = null,
                dslContext = dslContext,
                projectName = projectInfo.projectName,
                businessLineNmae = projectInfo.businessLineName ?: ""
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
                    userId = if (CommonUtil.ifProjectPersonal(projectId)) {
                        projectId.removePrefix("_")
                    } else {
                        pmUserId
                    },
                    traceId = bizId,
                    workspaceName = ws.workspaceName,
                    devFilePath = ws.devFilePath,
                    devFile = Devfile(
                        zoneId = windowsZone.zoneShortName,
                        machineType = windowsConfig.size,
                        cgsId = cgsId,
                        autoAssign = autoAssign,
                        imageCosFile = workspaceCreate.imageCosFile
                    ),
                    settingEnvs = emptyMap(),
                    projectId = projectId,
                    mountType = mountType,
                    ownerType = ws.ownerType,
                    delayMills = i * 2000
                )
            )
        }
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
    fun createWorkspace(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): WorkspaceResponse {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspaceCreate, false)}")
        permissionService.checkUserCreate(userId)

        if (workspaceCreate.windowsResourceConfigId != null) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.CLIENT_NEED_UPDATED.errorCode,
                params = arrayOf(redisCache.get(RedisKeys.REDIS_CLIENT_INSTALL_URL).toString()),
                defaultMessage = ErrorCodeEnum.CLIENT_NEED_UPDATED.formatErrorMessage
            )
        }

        val workspace = if (workspaceCreate.windowsType != null) {
            loadWorkspaceWithUI(userId, bkTicket, projectId, workspaceCreate)
        } else loadWorkspaceWithCode(userId, bkTicket, projectId, workspaceCreate)

        // 审计
        ActionAuditContext.current().addInstanceInfo(
            workspace.workspaceName,
            workspace.workspaceName,
            null,
            workspace
        )

        // 发送给用户
        notifyControl.dispatchWebsocketPushEvent(
            userId = userId,
            workspaceName = workspace.workspaceName,
            workspaceHost = null,
            errorMsg = null,
            type = WebSocketActionType.WORKSPACE_CREATE,
            status = true,
            action = WorkspaceAction.PREPARING,
            systemType = workspace.workspaceSystemType, workspaceMountType = workspace.workspaceMountType,
            ownerType = workspace.ownerType,
            projectId = projectId
        )

        return WorkspaceResponse(
            workspaceName = workspace.workspaceName,
            status = WorkspaceAction.PREPARING,
            systemType = workspace.workspaceSystemType,
            workspaceMountType = workspace.workspaceMountType
        )
    }

    fun checkMountType(userId: String, devfileMountType: WorkspaceMountType): WorkspaceMountType {
        logger.info("checkMountType|userId|$userId|devfileMountType|$devfileMountType")
        if (devfileMountType == WorkspaceMountType.START) {
            return devfileMountType
        }
        val mountType = remoteDevSettingDao.fetchOneSetting(dslContext, userId).userSetting.mountType
        logger.info("checkMountType|userId|$userId|devfileMountType|$devfileMountType|mountType|$mountType")

        // 简化判断逻辑，优先处理 WorkspaceMountType.BCS 的情况
        if (devfileMountType == WorkspaceMountType.DEVCLOUD && mountType == WorkspaceMountType.BCS) {
            return WorkspaceMountType.BCS
        }

        // 处理其他情况时，均返回 WorkspaceMountType.DEVCLOUD
        return WorkspaceMountType.DEVCLOUD
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
            val pathWithNamespace = kotlin.runCatching {
                GitUtils.getDomainAndRepoName(ws.repositoryUrl!!).second
            }.getOrNull()
            val opActions = kotlin.runCatching {
                arrayOf(
                    WorkspaceAction.CREATE to getOpHistoryCreate(
                        ws.workspaceSystemType,
                        pathWithNamespace ?: "",
                        ws.branch ?: ""
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
                remoteDevBillingDao.newBilling(transactionContext, event.workspaceName, event.userId)
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

            val detail = workspaceCommon.getOrSaveWorkspaceDetail(event.workspaceName, event.mountType, event)

            if (ws.workspaceSystemType.needHeartbeat()) {
                redisHeartBeat.refreshHeartbeat(event.workspaceName)
            }

            if (ws.workspaceSystemType.needUpdateBkTicket()) {
                kotlin.runCatching {
                    bkTicketServie.updateBkTicket(event.userId, event.bkTicket, event.environmentHost, event.mountType)
                }
            }

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
                deliverControl.safeInitialization(ws.projectId, event.userId, event.workspaceName, event.autoAssign)
            }

            // 创建成功时给 cmdb 添加字段方便监控检索
            val ip = event.environmentIp?.substringAfter(".")
            if (!ip.isNullOrBlank() && ws.workspaceSystemType.checkWindows()) {
                bkccService.updateHostMonitor(
                    regionId = detail.regionId,
                    workspaceName = null,
                    ips = setOf(ip),
                    props = workspaceCommon.genWorkspaceCCInfo(ws.projectId)
                )

                // 创建成功后做异步设置
                executor.execute {
                    workspaceCommon.makeDiskMount(ip, event.userId)
                }

                // 给有cfs的机器绑定权限组
                tCloudCfsService.addOrRemoveCfsPermissionRule(ws.projectId, ip, false)

                // 关联tgit相关
                gitProxyTGitService.addOrRemoveAclIp(ws.projectId, ip, false)
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
        uid: String
    ): Boolean {
        val taskInfo = kotlin.runCatching {
            client.get(ServiceStartCloudResource::class).getTaskInfoByUid(uid).data!!
        }.onFailure {
            logger.warn("createWinWorkspaceByVm not find uid $uid")
            return false
        }.getOrThrow()
        val envId = taskInfo.vmCreateResp?.envId ?: kotlin.run {
            logger.warn("createWinWorkspaceByVm check envId fail $taskInfo")
            return false
        }
        val vmInfo = kotlin.runCatching {
            client.get(ServiceStartCloudResource::class).getWorkspaceInfoByEid(envId).data!!
        }.onFailure {
            logger.warn("createWinWorkspaceByVm not find uid $uid")
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
                logger.warn("createWinWorkspaceByVm not find ${taskInfo.vmCreateResp?.cgsIp}")
                return false
            }

        val oldWs = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = oldWorkspaceName)
        val projectId = when {
            oldWs == null -> projectCode
            oldWs.ownerType == WorkspaceOwnerType.PROJECT -> oldWs.projectId
            else -> null
        }

        val workspaceName = generateWorkspaceName(projectId ?: userId)
        val ownerType = if (projectId != null) WorkspaceOwnerType.PROJECT else WorkspaceOwnerType.PERSONAL
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
            ownerType = ownerType,
            gpu = windowsConfig.gpu,
            cpu = windowsConfig.cpu,
            memory = windowsConfig.memory,
            disk = windowsConfig.workspaceDisk(),
            winConfigId = windowsConfig.id?.toInt()
        )

        if (projectId != null) {
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
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                groupName = null,
                dslContext = dslContext,
                projectName = projectInfo.projectName,
                businessLineNmae = projectInfo.businessLineName
            )
        } else {
            val userInfo = kotlin.runCatching {
                client.get(ServiceTxUserResource::class).get(userId)
            }.onFailure { logger.warn("get user $userId info error|${it.message}") }
                .getOrElse { null }?.data
            workspaceDao.createWorkspace(
                workspace = ws,
                workspaceStatus = WorkspaceStatus.PREPARING,
                bgName = userInfo?.bgName,
                deptName = userInfo?.deptName,
                centerName = userInfo?.centerName,
                groupName = userInfo?.groupName,
                dslContext = dslContext,
                projectName = ws.projectId ?: "",
                businessLineNmae = userInfo?.businessLineName
            )
        }

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = ws.workspaceName,
                devFilePath = ws.devFilePath,
                devFile = Devfile(
                    uid = uid,
                    environmentUid = envId
                ),
                settingEnvs = emptyMap(),
                projectId = projectId,
                mountType = mountType,
                ownerType = ws.ownerType,
                delayMills = 2000
            )
        )
        if (oldWs?.status?.checkDelivering() == true) {
            workspaceDao.updateWorkspaceStatus(dslContext, oldWs.workspaceName, WorkspaceStatus.DELETED)
        }
        return true
    }

    private fun workspaceCreateFail(
        ws: WorkspaceRecord,
        event: RemoteDevUpdateEvent
    ) {
        if (ws.ownerType == WorkspaceOwnerType.PROJECT) {
            val imageId = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, ws.workspaceName)?.imageId ?: ""
            val envId = event.environmentUid ?: ""
            workspaceCommon.updateStatus2DeliveringFailed(
                workspace = ws,
                action = WorkspaceAction.CREATE,
                notifyTemplateCode = "WINDOWS_GPU_CREATE_FAILED",
                noticeParams = mapOf("imageId" to imageId, "envId" to envId)
            )
        } else {
            workspaceDao.deleteWorkspace(ws.workspaceName, dslContext)
        }
    }

    private fun getOpHistoryCreate(type: WorkspaceSystemType, vararg args: Any) = when (type) {
        WorkspaceSystemType.WINDOWS_GPU -> workspaceCommon.getOpHistory(OpHistoryCopyWriting.CREATE_WINDOWS)
        WorkspaceSystemType.LINUX -> workspaceCommon.getOpHistory(OpHistoryCopyWriting.CREATE).format(*args)
    }

    @Suppress("ComplexMethod")
    private fun loadWorkspaceWithCode(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): Workspace {
        val gitTransferService = remoteDevGitTransfer.loadByGitUrl(workspaceCreate.repositoryUrl)
        val pathWithNamespace = GitUtils.getDomainAndRepoName(workspaceCreate.repositoryUrl).second
        val projectName = pathWithNamespace.substring(pathWithNamespace.lastIndexOf("/") + 1)
        val yaml = if (workspaceCreate.useOfficialDevfile != true) {
            kotlin.runCatching {
                permissionService.checkOauthIllegal(userId) {
                    gitTransferService.getFileContent(
                        userId = userId,
                        pathWithNamespace = pathWithNamespace,
                        filePath = workspaceCreate.devFilePath!!,
                        ref = workspaceCreate.branch
                    )
                }
            }.getOrElse {
                logger.warn("get yaml failed ${it.message}")
                if (it is ErrorCodeException) throw it
                else throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                    params = arrayOf("获取 devfile 异常。")
                )
            }
        } else {
            // 防止污传,如果是基于BLANK模板创建的则用BLANK作为devFilePath
            workspaceCreate.devFilePath = if (workspaceCreate.wsTemplateId == BLANK_TEMPLATE_ID) {
                BLANK_TEMPLATE_YAML_NAME
            } else null
            redisCache.get(REDIS_OFFICIAL_DEVFILE_KEY) ?: ""
        }

        if (yaml.isBlank()) {
            logger.warn(
                "create workspace get devfile blank,return." +
                    "|useOfficialDevfile=${workspaceCreate.useOfficialDevfile}"
            )
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVFILE_ERROR.errorCode,
                params = arrayOf("devfile 为空，请确认。")
            )
        }

        val devfile = DevfileUtil.parseDevfile(yaml).apply {
            gitEmail = kotlin.runCatching {
                permissionService.checkOauthIllegal(userId) {
                    gitTransferService.getUserEmail(
                        userId = userId
                    )
                }
            }.getOrElse {
                logger.warn("get user $userId info failed ${it.message}")
                if (it is ErrorCodeException) throw it
                else throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.USERINFO_ERROR.errorCode,
                    params = arrayOf("get user($userId) info from git failed")
                )
            }

            dotfileRepo = remoteDevSettingDao.fetchOneSetting(dslContext, userId).dotfileRepo
        }

        if (devfile.checkWorkspaceSystemType() == WorkspaceSystemType.WINDOWS_GPU) {
            windowsGpuCheck(workspaceCreate, userId)
        }

        val mountType = checkMountType(userId, devfile.checkWorkspaceMountType())
        workspaceCommon.checkWorkspaceAvailability(userId, mountType, WorkspaceOwnerType.PERSONAL)

        logger.info("createWorkspace|mountType|$mountType")
        val workspaceName = generateWorkspaceName(userId)
        val workspace = with(workspaceCreate) {
            Workspace(
                workspaceId = null,
                workspaceName = workspaceName,
                projectId = projectId,
                displayName = null,
                repositoryUrl = repositoryUrl,
                branch = branch,
                devFilePath = devFilePath,
                yaml = yaml,
                wsTemplateId = wsTemplateId,
                status = null,
                lastStatusUpdateTime = null,
                sleepingTime = null,
                createUserId = userId,
                workPath = Constansts.prefixWorkPath.plus(projectName),
                workspaceFolder = devfile.workspaceFolder ?: "",
                hostName = "",
                workspaceMountType = mountType,
                workspaceSystemType = devfile.checkWorkspaceSystemType(),
                ownerType = WorkspaceOwnerType.PERSONAL
            )
        }

        doPreparing(workspace)

        // 替换部分devfile内容，兼容使用老remoting的情况
        if (!isImageInDefaultList(
                devfile.runsOn?.container?.image,
                redisCache.getSetMembers(RedisKeys.REDIS_DEFAULT_IMAGES_KEY) ?: emptySet()
            )
        ) {
            devfile.runsOn?.container?.image =
                "${commonConfig.workspaceImageRegistryHost}/remote/${workspace.workspaceName}"
        }

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = workspace.workspaceName,
                repositoryUrl = workspace.repositoryUrl ?: "",
                branch = workspace.branch ?: "",
                devFilePath = workspace.devFilePath,
                devFile = devfile,
                gitOAuth = gitTransferService.getAndCheckOauthToken(userId),
                settingEnvs = remoteDevSettingDao.fetchOneSetting(dslContext, userId).envsForVariable,
                bkTicket = bkTicket,
                projectId = projectId,
                mountType = mountType
            )
        )

        return workspace
    }

    private fun loadWorkspaceWithUI(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspaceCreate: WorkspaceCreate
    ): Workspace {
        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        val windowsConfig = windowsResourceConfigService.getTypeConfig(workspaceCreate.windowsType!!)
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

        val windowsZone = windowsResourceConfigService.getZoneConfig(workspaceCreate.windowsZone!!)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_CONFIG_NOT_FIND.errorCode,
                params = arrayOf(workspaceCreate.windowsZone!!)
            )
        if (windowsZone.available == false) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WINDOWS_RESOURCE_NOT_AVAILABLE.errorCode,
                params = arrayOf(workspaceCreate.windowsZone!!)
            )
        }

        windowsGpuCheck(workspaceCreate, userId)
        workspaceCommon.checkWorkspaceAvailability(userId, mountType, WorkspaceOwnerType.PERSONAL)
        val resourceCount = startCloudResourceCountCheck(workspaceCreate.windowsType!!, workspaceCreate.windowsZone!!)
        if (resourceCount < 1) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DESKTOP_RESOURCES_INSUFFICIENT.errorCode,
                params = arrayOf(resourceCount.toString())
            )
        }

        logger.info("createWorkspace|mountType|$mountType")
        val workspaceName = generateWorkspaceName(userId)
        val workspace = Workspace(
            workspaceId = null,
            workspaceName = workspaceName,
            projectId = projectId,
            createUserId = userId,
            hostName = "",
            workspaceMountType = mountType,
            workspaceSystemType = systemType,
            ownerType = WorkspaceOwnerType.PERSONAL,
            gpu = windowsConfig.gpu,
            cpu = windowsConfig.cpu,
            memory = windowsConfig.memory,
            disk = windowsConfig.workspaceDisk(),
            winConfigId = windowsConfig.id?.toInt()
        )

        doPreparing(workspace)

        val bizId = MDC.get(TraceTag.BIZID)
        // 发送给k8s
        dispatcher.dispatch(
            WorkspaceCreateEvent(
                userId = userId,
                traceId = bizId,
                workspaceName = workspace.workspaceName,
                repositoryUrl = "",
                branch = "",
                devFilePath = workspace.devFilePath,
                devFile = Devfile(
                    version = "",
                    zoneId = windowsZone.zoneShortName,
                    machineType = windowsConfig.size
                ),
                settingEnvs = remoteDevSettingDao.fetchOneSetting(dslContext, userId).envsForVariable,
                bkTicket = bkTicket,
                projectId = projectId,
                mountType = mountType
            )
        )

        return workspace
    }

    private fun windowsGpuCheck(workspaceCreate: WorkspaceCreate, userId: String) {
        whiteListService.checkRunsOnOs(
            key = RedisKeys.REDIS_RUNS_ON_OS_KEY,
            runsOnKey = WorkspaceSystemType.WINDOWS_GPU.name,
            currentOs = workspaceCreate.currentOS
        )

        whiteListService.windowsNumberLimit(
            userId = userId,
            value = workspaceDao.countUserWorkspace(
                dslContext = dslContext,
                userId = userId,
                unionShared = false,
                status = setOf(WorkspaceStatus.RUNNING, WorkspaceStatus.PREPARING, WorkspaceStatus.STARTING),
                systemType = WorkspaceSystemType.WINDOWS_GPU
            )
        )
    }

    private fun startCloudResourceCountCheck(type: String, zone: String) =
        workspaceCommon.syncStartCloudResourceList().count {
            it.status == 11 &&
                it.machineType == type &&
                it.zoneId.replace(Regex("\\d+"), "") == zone &&
                it.locked != true
        }

    private fun doPreparing(workspace: Workspace) {
        val userInfo = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(workspace.createUserId)
        }.onFailure { logger.warn("get user ${workspace.createUserId} info error|${it.message}") }
            .getOrElse { null }?.data

        workspaceDao.createWorkspace(
            workspace = workspace,
            workspaceStatus = WorkspaceStatus.PREPARING,
            bgName = userInfo?.bgName,
            deptName = userInfo?.deptName,
            centerName = userInfo?.centerName,
            groupName = userInfo?.groupName,
            dslContext = dslContext,
            projectName = workspace.projectId ?: "",
            businessLineNmae = userInfo?.businessLineName
        )
    }

    private fun generateWorkspaceName(userId: String): String {
        val subUserId = if (userId.length > Constansts.subUserIdLimitLen) {
            userId.substring(0 until Constansts.subUserIdLimitLen)
        } else {
            userId
        }
        return subUserId.replace(Regex("[@_]"), "-") +
            "-${UUIDUtil.generate().takeLast(Constansts.workspaceNameSuffixLimitLen)}"
    }

    // 判断用户定义的镜像是否在默认镜像白名单列表中
    fun isImageInDefaultList(image: String?, whitelist: Set<String>): Boolean {
        if (image.isNullOrBlank()) return false
        whitelist.forEach { cidr ->
            if (image.contains(cidr)) return true
        }
        return false
    }
}
