package com.tencent.devops.remotedev.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.start.StartMessageDataType
import com.tencent.devops.remotedev.pojo.startcloud.StartMessageRegisterCondition
import com.tencent.devops.remotedev.pojo.startcloud.StartMessageRegisterData
import com.tencent.devops.remotedev.pojo.startcloud.StartMessageRegisterReq
import com.tencent.devops.remotedev.pojo.startcloud.StartMessageRegisterUserStrategy
import com.tencent.devops.remotedev.pojo.startcloud.StartMessageType
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusData
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusEnum
import com.tencent.devops.remotedev.pojo.windows.ComputerStatusResp
import com.tencent.devops.remotedev.pojo.windows.ComputerUserData
import com.tencent.devops.remotedev.pojo.windows.ComputerUserEnum
import com.tencent.devops.remotedev.service.client.StartCloudClient
import java.util.Base64
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StartWorkspaceService @Autowired constructor(
    private val startCloudClient: StartCloudClient,
    private val dslContext: DSLContext,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val client: Client,
    val checkTokenService: ClientTokenService
) {
    fun computerStatus(
        projectId: String?,
        cgsIds: MutableSet<String> = mutableSetOf()
    ): ComputerStatusResp {
        // 获取这个项目下所有的工作空间
        if (projectId != null) {
            workspaceJoinDao.fetchWindowsWorkspacesSimple(
                dslContext = dslContext,
                projectId = projectId,
                checkField = listOf(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP),
                notStatus = listOf(WorkspaceStatus.DELETED, WorkspaceStatus.UNUSED)
            ).forEach {
                if (it.hostIp.isNullOrBlank()) {
                    return@forEach
                }
                cgsIds.add(it.hostIp!!)
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

    fun checkIpUsers(
        ip: String,
        users: Set<String>
    ): Boolean {
        val record = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext, sip = ip, notStatus = listOf(
                WorkspaceStatus.PREPARING,
                WorkspaceStatus.DELETED,
                WorkspaceStatus.DELIVERING_FAILED
            ),
            checkField = listOf(TWorkspace.T_WORKSPACE.PROJECT_ID)
        ).ifEmpty {
            logger.warn("$ip checkIpUsers not found")
            return false
        }
        if (record.size > 1) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REMOTEDEV_CLIENT_IP_DUPLICATE_ERROR.errorCode,
                params = arrayOf(ip)
            )
        }
        // 获取当前项目下的所有用户做过滤
        // 先使用云桌面做判断
        val projectId = record.first().projectId
        val currUsers = workspaceJoinDao.fetchProjectSharedUser(dslContext, setOf(projectId))
        val subUsers = users.subtract(currUsers)
        if (subUsers.isEmpty()) {
            return true
        }
        // 再使用项目人员做判断
        subUsers.forEach { user ->
            val check = client.get(ServiceProjectAuthResource::class).isProjectUser(
                token = checkTokenService.getSystemToken(),
                userId = user,
                projectCode = projectId
            ).data == true
            if (!check) {
                logger.warn("$ip checkIpUsers error $user")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.REMOTEDEV_CLIENT_IP_NO_PERM_ERROR.errorCode,
                    params = arrayOf(ip, user)
                )
            }
        }
        return true
    }

    fun sendMessage(
        operator: String,
        userIdList: Set<String>,
        dataType: StartMessageDataType,
        data: String,
        messageStartTime: Long,
        messageEndTime: Long
    ) {
        val id = UUIDUtil.generate()
        val dataStr = Base64.getEncoder().encodeToString(
            JsonUtil.getObjectMapper(false).writeValueAsBytes(
                StartMessageRegisterData(
                    dataType.value, data
                )
            )
        )
        startCloudClient.messageRegister(
            StartMessageRegisterReq(
                operator = operator,
                orderId = id,
                user = StartMessageRegisterUserStrategy(userIdList, null, null),
                condition = StartMessageRegisterCondition(
                    type = StartMessageType.INSTANT.value,
                    startTime = messageStartTime,
                    endTime = messageEndTime
                ),
                data = dataStr
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StartWorkspaceService::class.java)
    }
}
