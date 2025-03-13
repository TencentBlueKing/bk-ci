package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.WorkspaceShared
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
    private val bkccService: BKCCService,
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
                        val props = workspaceCommon.genWorkspaceCCInfo(
                            projectId = ws.projectId,
                            workspaceName = ws.displayName.ifBlank { workspaceName },
                            owner = owner
                        )
                        logger.info("start update $workspaceName|$props")
                        workspaceCommon.updateHostMonitor(
                            workspaceName = workspaceName,
                            props = props,
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
                bkccService.updateHost(data.host!!, mapOf("devx_meta" to ""))
                return true
            }
        }
        return false
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
