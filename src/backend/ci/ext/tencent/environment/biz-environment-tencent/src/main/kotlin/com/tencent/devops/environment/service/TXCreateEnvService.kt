package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.service.thirdpartyagent.BatchInstallAgentService
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Primary
class TXCreateEnvService @Autowired constructor(
    private val client: Client,
    private val batchInstallAgentService: BatchInstallAgentService
) : CreateEnvService() {

    @Value("\${environment.batch-install.aes-key}")
    private val batchInstallAesKey = ""

    override fun fetchUserWorkspaceId(projectId: String, userId: String): List<String> {
        return client.get(ServiceRemoteDevResource::class).getWorkspaceListNew(
            userId = userId,
            projectId = projectId,
            page = null,
            pageSize = null,
            search = WorkspaceSearch()
        ).data?.records?.map { it.workspaceName } ?: emptyList()
    }

    override fun getWorkspaceDisplayName(userId: String, projectId: String, workspaceId: String?): String? {
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            userId, projectId, workspaceId ?: return null
        ).data?.displayName
    }

    override fun addCreateNode(token: String, deviceId: String, userId: String, os: OS): String {

        val (_, _, errMsg) = verifyTempToken(token, deviceId, userId)
        if (errMsg != null) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                defaultMessage = errMsg
            )
        }
        // TODO: 通过deviceId获取用户和项目信息，这里要考虑下权限相关的
        return batchInstallAgentService.genCreateAgentInstallScript(
            userId = TODO(),
            projectId = TODO(),
            workspaceName = deviceId,
            os = os
        )

    }


    // 校验临时token
    private fun verifyTempToken(token: String, deviceId: String, userId: String): Triple<String, String, String?> {
        val decodeSub = AESUtil.decrypt(batchInstallAesKey, token).split(";")
        if (decodeSub.size < 3) {
            return Triple("", "", "token verify error")
        }

        if (decodeSub[0] != deviceId || decodeSub[1] != userId) {
            return Triple("", "", "token's deviceId or user not find")
        }

        if (decodeSub[2].toLong() <= LocalDateTime.now().timestampmilli()) {
            return Triple("", "", "token is expired")
        }

        return Triple(decodeSub[0], decodeSub[1], null)
    }
}