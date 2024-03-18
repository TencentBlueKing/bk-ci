package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusData
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusEnum
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusResp
import com.tencent.devops.remotedev.pojo.windows.ComputerUserData
import com.tencent.devops.remotedev.pojo.windows.ComputerUserEnum
import com.tencent.devops.remotedev.service.client.StartCloudClient
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StartWorkspaceService @Autowired constructor(
    private val startCloudClient: StartCloudClient,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao
) {
    fun computerStatus(
        projectId: String?,
        cgsIds: MutableSet<String> = mutableSetOf()
    ): ComputerStatusResp {
        // 获取这个项目下所有的工作空间
        if (projectId != null) {
            workspaceDao.fetchWinWorkspaceIpAndRegId(dslContext, projectId).forEach { (_, cgsId, _) ->
                if (cgsId.isNullOrBlank()) {
                    return@forEach
                }
                cgsIds.add(cgsId)
            }
        }
        if (cgsIds.isEmpty()) {
            return ComputerStatusResp(0, emptyList(), emptyList())
        }

        // 获取状态信息
        val resp = startCloudClient.computerStatus(cgsIds)
            ?: return ComputerStatusResp(0, emptyList(), emptyList())

        // 拼接仪表信息
        val statusResMap = mutableMapOf<ComputerStatusEnum, ComputerStatusData>()
        val userResMap = mutableMapOf(
            ComputerUserEnum.LOGIN to ComputerUserData(0, mutableMapOf(), ComputerUserEnum.LOGIN),
            ComputerUserEnum.LOGOUT to ComputerUserData(0, null, ComputerUserEnum.LOGOUT)
        )
        resp.forEach {
            if (!it.userInfos.isNullOrEmpty()) {
                userResMap[ComputerUserEnum.LOGIN]!!.value++
                userResMap[ComputerUserEnum.LOGIN]!!.names!![it.cgsId] = it.userInfos.map { user -> user.account }
            } else {
                userResMap[ComputerUserEnum.LOGOUT]!!.value++
            }

            val status = ComputerStatusEnum.getEnumFromStatus(it.state)
            if (status == null) {
                logger.warn("computerStatus $projectId get unknown state ${it.state}")
                return@forEach
            }
            if (statusResMap[status] == null) {
                statusResMap[status] = ComputerStatusData(1, status, status.message)
            } else {
                statusResMap[status]!!.value++
            }
        }
        return ComputerStatusResp(
            count = resp.size,
            status = statusResMap.values.toList(),
            users = userResMap.values.toList()
        )
    }

    fun loginUsers(
        cgsIds: Set<String>
    ): Map<String, List<String>> {
        return kotlin.runCatching {
            computerStatus(
                null,
                cgsIds.toMutableSet()
            ).users.find { it.type == ComputerUserEnum.LOGIN }?.names
        }.getOrNull() ?: emptyMap()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartWorkspaceService::class.java)
    }
}
