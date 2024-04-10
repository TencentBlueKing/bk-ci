package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.FetchRemoteDevData
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.op.OpOpUpdateCCHostDataAction
import com.tencent.devops.remotedev.pojo.op.OpOpUpdateCCHostDataScope
import com.tencent.devops.remotedev.pojo.op.OpUpdateCCHostData
import com.tencent.devops.remotedev.pojo.windows.FetchOwnerAndAdminData
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
    private val bkccService: BKCCService,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceWindowsDao: WorkspaceWindowsDao
) {

    fun fetchOwnerAndAdmin(
        data: FetchOwnerAndAdminData
    ): Set<String> {
        // 查询云研发项目管理员
        val projectAndAdmins = client.get(ServiceTxUserResource::class).getRemoteDevAdmin(
            FetchRemoteDevData(
                projectIds = data.projectIds.toSet()
            )
        ).data ?: return emptySet()

        val res = mutableSetOf<String>()
        projectAndAdmins.forEach { (projectId, admins) ->
            if (admins.isNullOrEmpty()) {
                return@forEach
            }
            res.addAll(admins)
        }

        return res
    }

    fun updateCCHost(
        data: OpUpdateCCHostData
    ): Boolean {
        when (data.scope) {
            OpOpUpdateCCHostDataScope.ALL -> {
                val projectAndRegIdAndIps = mutableMapOf<String, MutableMap<Int, MutableSet<String>>>()
                val records = workspaceDao.fetchWinWorkspaceIpAndRegId(dslContext, null)
                records.forEach { (projectId, hostIp, regId) ->
                    if (regId == null) {
                        return@forEach
                    }
                    if (hostIp.isNullOrEmpty()) {
                        return@forEach
                    }

                    val hostIdSub = hostIp.split(".")
                    val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")

                    if (projectAndRegIdAndIps[projectId] == null) {
                        projectAndRegIdAndIps[projectId] = mutableMapOf(regId to mutableSetOf(ip))
                    } else if (projectAndRegIdAndIps[projectId]!![regId] == null) {
                        projectAndRegIdAndIps[projectId]!![regId] = mutableSetOf(ip)
                    } else {
                        projectAndRegIdAndIps[projectId]!![regId]!!.add(ip)
                    }
                }

                logger.debug("updateCCHost projectAndRegIdAndIps {}", projectAndRegIdAndIps)

                projectAndRegIdAndIps.forEach { (projectId, regAndIps) ->
                    try {
                        regAndIps.forEach { (regId, ips) ->
                            if (data.action == OpOpUpdateCCHostDataAction.DELETE) {
                                bkccService.updateHostMonitor(regId, null, ips, mapOf("devx_meta" to ""))
                            } else {
                                bkccService.updateHostMonitor(
                                    regionId = regId,
                                    workspaceName = null,
                                    ips = ips,
                                    props = workspaceCommon.genWorkspaceCCInfo(projectId)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("updateCCHost {} {} request cc api error", projectId, regAndIps, e)
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
