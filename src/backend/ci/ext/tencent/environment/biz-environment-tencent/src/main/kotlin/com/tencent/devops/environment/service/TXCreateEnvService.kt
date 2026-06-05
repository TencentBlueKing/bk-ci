package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.service.thirdpartyagent.DownloadAgentInstallService
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64

@Service
@Primary
class TXCreateEnvService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val downloadAgentInstallService: DownloadAgentInstallService
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

    override fun genCreateNodeInstallScript(
        token: String,
        deviceId: String,
        userId: String
    ): Response {
        val (projectId, errMsg) = verifyTempToken(token, deviceId, userId)
        if (errMsg != null) {
            logger.warn("genCreateNodeInstallScript $deviceId|$userId token $token check error $errMsg")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                defaultMessage = errMsg
            )
        }
        val record = thirdPartyAgentDao.getAgentByWorkspaceName(
            dslContext = dslContext,
            projectId = projectId,
            workspaceNames = listOf(deviceId)
        ).firstOrNull()
        if (record == null) {
            logger.error("addCreateNode no found agent $projectId|$deviceId|$userId")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(deviceId)
            )
        }

        return downloadAgentInstallService.downloadInstallScript(
            agentId = HashUtil.encodeLongId(record.id),
            loginName = null,
            loginPassword = null,
            installType = null
        )
    }

    // 校验临时token "projectId;deviceId;userId;time"
    private fun verifyTempToken(token: String, deviceId: String, userId: String): Pair<String, String?> {
        val realToken = Base64.getUrlDecoder().decode(token)
        val decodeSub = AESUtil.decrypt(batchInstallAesKey, realToken).toString(charset("UTF-8")).split(";")
        if (decodeSub.size < 4) {
            return Pair("", "token verify error")
        }

        if (decodeSub[1] != deviceId || decodeSub[2] != userId) {
            return Pair("", "token's deviceId or user not find")
        }

        if (decodeSub[3].toLong() <= LocalDateTime.now().timestampmilli()) {
            return Pair("", "token is expired")
        }

        return Pair(decodeSub[0], null)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TXCreateEnvService::class.java)
    }
}