package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusData
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusEnum
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusResp
import com.tencent.devops.remotedev.pojo.windows.ComputerUserData
import com.tencent.devops.remotedev.pojo.windows.ComputerUserEnum
import com.tencent.devops.remotedev.service.startcloud.StartCloudClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartWorkspaceService @Autowired constructor(
    private val startCloudClient: StartCloudClient,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao
) {
    fun computerStatus(
        userId: String,
        projectId: String
    ): ComputerStatusResp {
        // 获取这个项目下所有的工作空间
        val csgIds = mutableSetOf<String>()
        workspaceDao.fetchWinWorkspaceIpAndRegId(dslContext, projectId).forEach { (_, cgsId, _) ->
            if (cgsId.isNullOrBlank()) {
                return@forEach
            }
            csgIds.add(cgsId)
        }

        // 获取状态信息
        val resp = startCloudClient.computerStatus(userId, csgIds)
            ?: return ComputerStatusResp(0, emptyList(), emptyList())

        // 拼接仪表信息
        var statusNormalValue = 0
        var statusAbnormalValue = 0
        var statusShutdownValue = 0
        var userLoginValue = 0
        var userLogoutValue = 0
        resp.forEach {
            when (it.state) {
                0 -> statusAbnormalValue++
                1 -> statusNormalValue++
                2 -> statusShutdownValue++
            }
            if (!it.userInfos.isNullOrEmpty()) {
                userLoginValue++
            } else {
                userLogoutValue++
            }
        }
        return ComputerStatusResp(
            count = resp.size,
            status = listOf(
                ComputerStatusData(statusNormalValue, ComputerStatusEnum.NORMAL),
                ComputerStatusData(statusAbnormalValue, ComputerStatusEnum.ABNORMAL),
                ComputerStatusData(statusShutdownValue, ComputerStatusEnum.SHUTDOWN)
            ),
            users = listOf(
                ComputerUserData(userLoginValue, ComputerUserEnum.LOGIN),
                ComputerUserData(userLogoutValue, ComputerUserEnum.LOGOUT)
            )
        )
    }
}