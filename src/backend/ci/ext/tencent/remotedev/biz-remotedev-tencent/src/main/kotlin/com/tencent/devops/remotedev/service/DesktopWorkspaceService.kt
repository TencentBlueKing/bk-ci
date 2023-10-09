package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.FetchRemoteDevData
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.op.OpOpUpdateCCHostDataAction
import com.tencent.devops.remotedev.pojo.op.OpOpUpdateCCHostDataScope
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminData
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminItem
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DesktopWorkspaceService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao,
    private val bkccService: BKCCService,
    private val workspaceCommon: WorkspaceCommon
) {

    fun fetchOwnerAndAdmin(
        data: FetchOwnerAndAdminData
    ): Map<String, FetchOwnerAndAdminItem> {
        // 查询云研发项目管理员
        val projectAndAdmins = client.get(ServiceTxUserResource::class).getRemoteDevAdmin(
            FetchRemoteDevData(
                projectIds = data.projectIds.toSet()
            )
        ).data ?: return emptyMap()

        val res = mutableMapOf<String, FetchOwnerAndAdminItem>()
        projectAndAdmins.forEach { (projectId, admins) ->
            res[projectId] = FetchOwnerAndAdminItem(
                admin = admins,
                owner = mutableSetOf()
            )
        }

        workspaceDao.fetchWorkspaceWithOwner(
            dslContext = dslContext,
            status = WorkspaceStatus.RUNNING,
            mountType = WorkspaceMountType.START,
            projectIds = projectAndAdmins.keys,
            ip = null,
            assignType = WorkspaceShared.AssignType.OWNER
        )?.forEach {
            val owner = it["SHARED_USER"] as? String ?: it["CREATOR"] as String

            val projectId = it["PROJECT_ID"] as String

            res[projectId]?.owner?.add(owner)
        }

        return res
    }

    fun updateCCHost(
        data: OpUpdateCCHostData
    ): Boolean {
        when (data.scope) {
            OpOpUpdateCCHostDataScope.ALL -> {
                val projectAndIps = mutableMapOf<String, MutableSet<String>>()
                val records = workspaceDao.fetchWinWorkspaceIp(dslContext)
                records.forEach { (projectId, ip) ->
                    if (ip.isNullOrEmpty()) {
                        return@forEach
                    }
                    if (projectAndIps[projectId] == null) {
                        projectAndIps[projectId] = mutableSetOf(ip)
                    } else {
                        projectAndIps[projectId]!!.add(ip)
                    }
                }

                logger.debug("updateCCHost projectAndIps {}", projectAndIps)

                projectAndIps.forEach { (projectId, ips) ->
                    if (data.action == OpOpUpdateCCHostDataAction.DELETE) {
                        bkccService.updateHost(ips, mapOf("devx_meta" to ""))
                    } else {
                        bkccService.updateHost(ips, workspaceCommon.genWorkspaceCCInfo(projectId))
                    }
                }

                return true
            }

            OpOpUpdateCCHostDataScope.PART -> {
                if (data.host.isNullOrEmpty() || data.projectId.isNullOrBlank()) {
                    return false
                }

                if (data.action == OpOpUpdateCCHostDataAction.DELETE) {
                    bkccService.updateHost(data.host!!, mapOf("devx_meta" to ""))
                } else {
                    bkccService.updateHost(data.host!!, workspaceCommon.genWorkspaceCCInfo(data.projectId!!))
                }

                return true
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DesktopWorkspaceService::class.java)
    }
}
