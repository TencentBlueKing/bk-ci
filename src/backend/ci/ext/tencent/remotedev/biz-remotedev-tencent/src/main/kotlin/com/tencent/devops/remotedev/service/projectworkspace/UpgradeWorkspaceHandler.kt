package com.tencent.devops.remotedev.service.projectworkspace

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceStartCloudResource
import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.EnvStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.mq.WorkspaceOperateEvent
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.OpHistoryCopyWriting
import com.tencent.devops.remotedev.pojo.WebSocketActionType
import com.tencent.devops.remotedev.pojo.WorkSpaceCacheInfo
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.event.UpdateEventType
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import com.tencent.devops.remotedev.service.redis.RedisCallLimit
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CALL_LIMIT_KEY_PREFIX
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class UpgradeWorkspaceHandler @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val permissionService: PermissionService,
    private val tCloudCfsService: TCloudCfsService,
    private val gitProxyTGitService: GitProxyTGitService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val notifyControl: NotifyControl,
    private val deliverControl: DeliverControl,
    private val workspaceCommon: WorkspaceCommon,
    private val dispatcher: RemoteDevDispatcher
) {
    @ActionAuditRecord(
        actionId = ActionId.CGS_UPGRADE_VM,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CGS,
            instanceNames = "#workspaceName",
            instanceIds = "#workspaceName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.CGS_UPGRADE_VM
    )
    fun upgradeWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        rebuildReq: WorkspaceUpgradeReq
    ): WorkspaceResponse {
        logger.info("$userId upgrade project $projectId workspace $workspaceName")
        if (!permissionService.hasOwnerPermission(
                userId = userId,
                workspaceName = workspaceName,
                projectId = projectId
            ) && !permissionService.hasUserManager(userId, projectId)
        ) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You do not have permission to upgrade $workspaceName")
            )
        }

        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        RedisCallLimit(
            redisOperation = redisOperation,
            lockKey = "$REDIS_CALL_LIMIT_KEY_PREFIX:workspace:$workspaceName",
            expiredTimeInSeconds = expiredTimeInSeconds
        ).tryLock().use {
            // TODO：116140983 要不要校验动作
            if (workspaceCommon.notOk2doNextAction(workspace)) {
                logger.info("${workspace.workspaceName} is ${workspace.status}, return error.")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_STATUS_CHANGE_FAIL.errorCode,
                    params = arrayOf(
                        workspace.workspaceName,
                        "status is already ${workspace.status}, can't upgrade now"
                    )
                )
            }
            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.UPGRADE,
                // TODO: 116140983 看看OP状态是否同步 NOT_FIRST_START
                actionMessage = workspaceCommon.getOpHistory(OpHistoryCopyWriting.NOT_FIRST_START)
            )

            workspaceDao.updateWorkspaceStatus(
                dslContext = dslContext,
                workspaceName = workspaceName,
                status = WorkspaceStatus.UPGRADING
            )

            workspaceOpHistoryDao.createWorkspaceHistory(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = userId,
                action = WorkspaceAction.UPGRADE,
                actionMessage = String.format(
                    workspaceCommon.getOpHistory(OpHistoryCopyWriting.ACTION_CHANGE),
                    workspace.status,
                    WorkspaceStatus.UPGRADING
                )
            )

            // TODO: 116140983 获取真正的创建人看看传递是否合适
            val owner = workspaceSharedDao.fetchWorkspaceSharedInfo(
                dslContext = dslContext,
                workspaceName = workspaceName,
                sharedUsers = null,
                assignType = WorkspaceShared.AssignType.OWNER
            ).firstOrNull()?.sharedUser ?: userId

            dispatcher.dispatch(
                WorkspaceOperateEvent(
                    userId = owner,
                    traceId = MDC.get(TraceTag.BIZID) ?: TraceTag.buildBiz(),
                    type = UpdateEventType.UPGRADE,
                    workspaceName = workspaceName,
                    mountType = WorkspaceMountType.START,
                    machineType = rebuildReq.machineType,
                    gameId = null,
                    projectId = projectId
                )
            )

            notifyControl.dispatchWebsocketPushEvent(
                userId = userId,
                workspaceName = workspaceName,
                workspaceHost = null,
                errorMsg = null,
                type = WebSocketActionType.WORKSPACE_UPGRADE,
                status = true,
                action = WorkspaceAction.UPGRADING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START,
                ownerType = WorkspaceOwnerType.PROJECT,
                projectId = projectId
            )

            return WorkspaceResponse(
                workspaceName = workspaceName,
                workspaceHost = "",
                status = WorkspaceAction.UPGRADING,
                systemType = WorkspaceSystemType.WINDOWS_GPU,
                workspaceMountType = WorkspaceMountType.START
            )
        }
    }

    fun upgradeVm(
        userId: String,
        workspaceName: String,
        uid: String
    ): Boolean {
        val taskInfo = kotlin.runCatching {
            client.get(ServiceStartCloudResource::class).getTaskInfoByUid(uid).data!!
        }.onFailure {
            logger.warn("upgradeVm not find uid $uid")
            return false
        }.getOrThrow()
        val envId = taskInfo.vmCreateResp?.envId ?: kotlin.run {
            logger.warn("upgradeVm check envId fail $taskInfo")
            return false
        }
        val workspaceInfo = kotlin.runCatching {
            client.get(ServiceStartCloudResource::class).getWorkspaceInfoByEid(envId).data!!
        }.onFailure {
            logger.warn("upgradeVm not find uid $uid")
            return false
        }.getOrThrow()

        logger.info("upgradeVm get vm info $workspaceInfo")

        // 检查vm状态，如果不是正常的，中止自动升级。转失败状态
        if (workspaceInfo.status != EnvStatusEnum.running) {
            logger.warn("upgradeVm vm not running, fix failed.")
            return false
        }

        val cgsIp = taskInfo.vmCreateResp?.cgsIp
        val vm = workspaceCommon.syncStartCloudResourceList().find { it.cgsId == cgsIp } ?: kotlin.run {
            logger.warn("upgradeVm not find $cgsIp")
            return false
        }

        val oldWs = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName) ?: kotlin.run {
            logger.error("upgradeVm fetchWorkspace $workspaceName null")
            return false
        }
        val projectId = oldWs.projectId
        val checkOwnerType = oldWs.ownerType
        val (gameId, appId) = workspaceCommon.getGameIdAndAppId(projectId, checkOwnerType)

        val mountType = WorkspaceMountType.START
        val systemType = WorkspaceSystemType.WINDOWS_GPU
        val windowsConfig = windowsResourceConfigService.getTypeConfig(vm.machineType) ?: kotlin.run {
            logger.warn("upgradeVm not find machineType ${vm.machineType}")
            return false
        }
        val ws = Workspace(
            workspaceId = null,
            workspaceName = workspaceName,
            projectId = projectId,
            createUserId = userId,
            hostName = cgsIp,
            workspaceMountType = mountType,
            workspaceSystemType = systemType,
            ownerType = checkOwnerType,
            gpu = windowsConfig.gpu,
            cpu = windowsConfig.cpu,
            memory = windowsConfig.memory,
            disk = windowsConfig.workspaceDisk(),
            winConfigId = windowsConfig.id?.toInt(),
            zoneId = vm.zoneId.replace(Regex("\\d+"), "")
        )
        // WorkspaceDao 修改
        when (checkOwnerType) {
            WorkspaceOwnerType.PROJECT -> {
                // 删除记录。新的工作空间会复用原先的name
                workspaceDao.deleteWorkspace(workspaceName, dslContext)
                val projectInfo = kotlin.runCatching {
                    client.get(ServiceProjectResource::class).get(projectId)
                }.onFailure {
                    logger.warn("upgradeVm get project $projectId info error|${it.message}")
                }.getOrElse { null }?.data ?: kotlin.run {
                    logger.warn("upgradeVm not find projectInfo $projectId")
                    return false
                }
                workspaceDao.createWorkspace(
                    workspace = ws,
                    workspaceStatus = WorkspaceStatus.DELIVERING,
                    bgName = projectInfo.bgName,
                    deptName = projectInfo.deptName,
                    centerName = projectInfo.centerName,
                    groupName = null,
                    dslContext = dslContext,
                    projectName = projectInfo.projectName,
                    businessLineNmae = projectInfo.businessLineName
                )
            }

            WorkspaceOwnerType.PERSONAL -> {
                // 删除记录。新的工作空间会复用原先的name
                workspaceDao.deleteWorkspace(workspaceName, dslContext)
                val userInfo = kotlin.runCatching {
                    client.get(ServiceTxUserResource::class).get(userId)
                }.onFailure {
                    logger.warn("get user $userId info error|${it.message}")
                }.getOrElse { null }?.data
                workspaceDao.createWorkspace(
                    workspace = ws,
                    workspaceStatus = WorkspaceStatus.DELIVERING,
                    bgName = userInfo?.bgName,
                    deptName = userInfo?.deptName,
                    centerName = userInfo?.centerName,
                    groupName = userInfo?.groupName,
                    dslContext = dslContext,
                    projectName = ws.projectId ?: "",
                    businessLineNmae = userInfo?.businessLineName
                )
            }
        }

        // workspaceDetail 修改
        val oldDetail = workspaceCommon.getWorkspaceDetail(workspaceName)
        workspaceDao.saveOrUpdateWorkspaceDetail(
            dslContext = dslContext,
            workspaceName = workspaceName,
            detail = JsonUtil.toJson(
                WorkSpaceCacheInfo(
                    sshKey = "",
                    environmentHost = cgsIp ?: "",
                    hostIP = cgsIp ?: "",
                    environmentIP = cgsIp ?: "",
                    clusterId = "",
                    namespace = workspaceInfo.namespace,
                    curLaunchId = appId.toInt(),
                    regionId = workspaceInfo.regionId
                )
            )
        )

        // workspaceWindows 修改
        workspaceWindowsDao.updateWindowsResourceId(
            dslContext = dslContext,
            workspaceName = workspaceName,
            resourceId = taskInfo.vmCreateResp?.resourceId,
            hostIp = cgsIp,
            macAddress = taskInfo.vmCreateResp?.macAddress
        )

        // 安全初始化
        deliverControl.safeInitialization(projectId = projectId, userId = userId, workspaceName = workspaceName)

        // 创建成功时给 cmdb 添加字段方便监控检索
        val ip = cgsIp?.substringAfter(".")
        if (!ip.isNullOrBlank()) {
            workspaceCommon.updateHostMonitor(
                workspaceName = workspaceName,
                props = workspaceCommon.genWorkspaceCCInfo(
                    projectId = projectId,
                    workspaceName = oldWs.displayName.ifBlank { ws.workspaceName },
                    owner = null
                ),
                type = systemType
            )

            // 创建成功后做异步设置
            workspaceCommon.makeDiskMount(ip, userId)

            // 给有cfs的机器绑定权限组
            tCloudCfsService.addOrRemoveCfsPermissionRule(projectId, ip, false)

            // 关联tgit相关
            gitProxyTGitService.addOrRemoveAclIp(projectId, ip, false)
        }

        // 同时要删去老的 ip 的一些相关信息
        val hostIdSub = oldDetail?.environmentIP?.split(".")
        val oldIp = hostIdSub?.subList(1, hostIdSub.size)?.joinToString(separator = ".")
        if (!oldIp.isNullOrBlank()) {
            if (oldDetail.regionId != null) {
                workspaceCommon.updateHostMonitor(
                    regionId = oldDetail.regionId,
                    ip = oldIp,
                    props = mapOf("devx_meta" to ""),
                    type = systemType
                )

                workspaceCommon.updateHostMonitor(
                    regionId = oldDetail.regionId,
                    ip = oldIp,
                    props = mapOf("bk_host_name" to "VM-${hostIdSub.joinToString("-")}"),
                    type = systemType
                )
            }

            // 删除cfs的权限组规则
            tCloudCfsService.addOrRemoveCfsPermissionRule(projectId, oldIp, true)

            // 关联tgit相关
            gitProxyTGitService.addOrRemoveAclIp(projectId, oldIp, true)
        }

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeWorkspaceHandler::class.java)
        private val expiredTimeInSeconds = TimeUnit.MINUTES.toSeconds(2)
    }
}