package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.op.OpOpUpdateCCHostDataAction
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class DesktopWorkspaceService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val bkccService: BKCCService,
    private val bkBaseService: BKBaseService,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceWindowsDao: WorkspaceWindowsDao
) {

    fun updateCCHost(
        data: OpUpdateCCHostData
    ): Boolean {
        when (data.action) {
            OpOpUpdateCCHostDataAction.UPDATE -> {
                val records = workspaceDao.fetchAllUsedWindows(dslContext)
                records.forEach { workspaceName ->
                    val ws = workspaceDao.fetchAnyWorkspace(
                        dslContext = dslContext, workspaceName = workspaceName
                    ) ?: return@forEach
                    val owner = workspaceSharedDao.fetchWorkspaceSharedInfo(
                        dslContext = dslContext,
                        workspaceName = workspaceName,
                        assignType = WorkspaceShared.AssignType.OWNER
                    ).firstOrNull()?.sharedUser
                    kotlin.runCatching {
                        logger.info("start update dimension $workspaceName|owner=$owner")
                        workspaceCommon.reportWorkspaceDimension(
                            workspaceName = workspaceName,
                            projectId = ws.projectId,
                            displayName = ws.displayName.ifBlank { workspaceName },
                            owner = owner,
                            type = ws.workspaceSystemType
                        )
                    }.onFailure {
                        logger.warn("updateCCHost fail ${it.message}", it)
                    }
                }
                return true
            }

            OpOpUpdateCCHostDataAction.DELETE -> {
                if (data.host.isNullOrEmpty()) {
                    return false
                }
                val items = data.host!!.mapNotNull { hostIdStr ->
                    val hostId = hostIdStr.toLongOrNull()
                    if (hostId == null) {
                        logger.warn("updateCCHost DELETE invalid hostId|$hostIdStr")
                        null
                    } else {
                        HostExtraDimensionItem(hostId = hostId, dimensions = emptyList())
                    }
                }
                if (items.isEmpty()) {
                    return false
                }
                return bkBaseService.updateHostExtraDimensions(items)
            }
        }
        return false
    }

    /**
     * 按项目刷存量云桌面监控维度到 bkbase。
     * 逻辑：未删除且有拥有者的 Windows 实例，分批上报。
     */
    fun refreshProjectWorkspaceDimension(projectId: String): Boolean {
        logger.info("refreshProjectWorkspaceDimension start|projectId=$projectId")
        val workspaces = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            projectIds = setOf(projectId),
            notStatus = setOf(WorkspaceStatus.DELETED)
        )
        if (workspaces.isEmpty()) {
            logger.info("refreshProjectWorkspaceDimension empty|projectId=$projectId")
            return true
        }

        val workspaceNames = workspaces.map { it.workspaceName }.toSet()
        val ownerMap = workspaceSharedDao.batchFetchWorkspaceSharedInfo(dslContext, workspaceNames)
            .filter { it.type == WorkspaceShared.AssignType.OWNER }
            .associate { it.workspaceName to it.sharedUser }

        val candidates = workspaces.filter { ws ->
            !ownerMap[ws.workspaceName].isNullOrBlank() &&
                ws.regionId != null &&
                !ws.hostIp.isNullOrBlank()
        }
        if (candidates.isEmpty()) {
            logger.info(
                "refreshProjectWorkspaceDimension no owned workspace|projectId=$projectId"
            )
            return true
        }

        val items = mutableListOf<HostExtraDimensionItem>()
        candidates.groupBy { it.regionId!! }.forEach { (regionId, list) ->
            val ipToWorkspace = list.mapNotNull { ws ->
                val ip = ws.hostIp?.substringAfter(".")
                if (ip.isNullOrBlank()) {
                    null
                } else {
                    ip to ws
                }
            }.toMap()
            if (ipToWorkspace.isEmpty()) {
                return@forEach
            }
            val hostIdMap = bkccService.fetchHostIds(regionId, ipToWorkspace.keys)
            ipToWorkspace.forEach { (ip, ws) ->
                val hostId = hostIdMap[ip]
                if (hostId == null) {
                    logger.warn(
                        "refreshProjectWorkspaceDimension skip|" +
                            "workspace=${ws.workspaceName}|regionId=$regionId|ip=$ip"
                    )
                    return@forEach
                }
                val owner = ownerMap[ws.workspaceName] ?: return@forEach
                items.add(
                    HostExtraDimensionItem(
                        hostId = hostId,
                        dimensions = listOf(
                            workspaceCommon.genWorkspaceDimension(
                                projectId = ws.projectId,
                                workspaceName = ws.displayName.ifBlank { ws.workspaceName },
                                owner = owner
                            )
                        )
                    )
                )
            }
        }

        if (items.isEmpty()) {
            logger.warn(
                "refreshProjectWorkspaceDimension no hostId resolved|projectId=$projectId"
            )
            return false
        }
        val success = bkBaseService.updateHostExtraDimensions(items)
        logger.info(
            "refreshProjectWorkspaceDimension done|projectId={}|size={}|success={}",
            projectId, items.size, success
        )
        return success
    }

    fun checkWorkspaceProject(projectId: String, ip: String): Boolean {
        return workspaceWindowsDao.countProjectIp(dslContext, projectId, ip) > 0
    }

    fun checkUserIpPermission(user: String, ip: String): Boolean {
        return workspaceWindowsDao.countUserIp(dslContext, user, ip) > 0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DesktopWorkspaceService::class.java)
    }
}
