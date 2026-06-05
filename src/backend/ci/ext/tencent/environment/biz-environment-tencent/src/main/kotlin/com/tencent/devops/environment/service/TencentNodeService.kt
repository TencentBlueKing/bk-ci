package com.tencent.devops.environment.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.imate.ImateListItem
import com.tencent.devops.environment.pojo.imate.ImateOriginEngine
import com.tencent.devops.environment.pojo.imate.ImportImageNodeData
import com.tencent.devops.environment.service.thirdpartyagent.BatchInstallAgentService
import com.tencent.devops.support.api.service.ServiceIMateResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TencentNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeService: NodeService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val batchInstallAgentService: BatchInstallAgentService
) {
    @Value("\${environment.batch-install.aes-key}")
    private val batchInstallAesKey = ""

    fun updateCreateNodeDisplay(
        userId: String,
        projectId: String,
        workspaceName: String,
        displayName: String
    ): Boolean {
        val record =
            thirdPartyAgentDao.getAgentByWorkspaceName(dslContext, projectId, listOf(workspaceName)).firstOrNull()
                ?: return false
        nodeService.updateDisplayName(userId, projectId, HashUtil.encodeLongId(record.nodeId), displayName)
        return true
    }

    fun getUserImateList(
        userId: String,
        projectId: String
    ): List<ImateListItem> {
        // 只能导入自己创建的
        val imateList =
            client.get(ServiceIMateResource::class).queryUserRobots(userId).data?.filter { it.username == userId }
                ?: return emptyList()
        val installedAgents =
            thirdPartyAgentDao.getAgentByWorkspaceName(dslContext, projectId, imateList.map { it.clientUuid }.toList())
                .filter { (it.status != AgentStatus.UN_IMPORT.status || it.status != AgentStatus.UN_IMPORT_OK.status) }
                .map { it.createWorkspaceName }
        return imateList.filter { it.clientUuid !in installedAgents }.map {
            ImateListItem(
                name = it.botName,
                deviceId = it.clientUuid,
                ip = it.envId ?: it.deviceName,
                // 暂时只有linux，未来有了再加，他们的接口没字段
                os = OS.LINUX,
                engine = ImateOriginEngine.toEngine(it.clientType),
                status = it.status
            )
        }
    }

    fun batchImportImateNodes(
        userId: String,
        projectId: String,
        data: ImportImageNodeData
    ): Boolean {
        data.agentList.forEach { imate ->
            // 校验是否有权限
            if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            if (!(client.get(ServiceIMateResource::class).queryUserRobots(userId).data?.filter { it.username == userId }
                    ?.any { it.clientUuid == imate.deviceId } ?: false)) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            // 生成节点,如果用说明没导入成功，再次导入
            if (thirdPartyAgentDao.getAgentByWorkspaceName(
                    dslContext = dslContext,
                    projectId = projectId,
                    workspaceNames = listOf(imate.deviceId)
                ).firstOrNull() == null
            ) {
                // 暂时只有linux，未来有了再加，他们的接口没字段
                batchInstallAgentService.genNewAgent(
                    projectId = projectId,
                    userId = userId,
                    os = data.os,
                    zoneName = data.zoneName,
                    agentType = AgentType.CREATE,
                    createWorkspaceName = imate.deviceId
                )
            }
            // 生成临时1小时TOKEN用来导入鉴权
            val tokenData = "${projectId}:${imate.deviceId};$userId;${Instant.now().plusSeconds(3600).toEpochMilli()}"
            val token = AESUtil.encrypt(batchInstallAesKey, tokenData)
            // TODO: 调用imate的接口安装并传递token
            logger.info("batchImportImateNodes token $token") // TODO: 仅测试，过后删
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TencentNodeService::class.java)
    }
}