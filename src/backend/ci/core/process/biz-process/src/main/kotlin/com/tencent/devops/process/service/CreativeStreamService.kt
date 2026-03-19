package com.tencent.devops.process.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.pojo.EnvData
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_IP
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_NAME
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.WorkspaceBaseInfo
import com.tencent.devops.process.utils.NODE_AGENT_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 创作流流水线服务类
 */
@Service
class CreativeStreamService constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    fun listEventSubscriber(
        eventSource: String,
        eventType: String,
        eventCode: String
    ) = pipelineEventSubscriptionDao.listEventSubscriber(
        dslContext = dslContext,
        eventType = eventType,
        eventSource = eventSource,
        eventCode = eventCode
    )

    /**
     * 获取节点列表
     */
    fun fetchAllNodeEnvList(
        projectId: String,
        workspaceName: String,
        userId: String
    ): List<EnvData> {
        return try {
            client.get(ServiceEnvironmentResource::class).fetchAllNodeEnvList(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = userId,
                noCheckPerm = true
            ).data
        } catch (ignored: Exception) {
            logger.warn("get env list failed|$projectId|$workspaceName", ignored)
            null
        } ?: listOf()
    }

    /**
     * 获取流水线权限代持人
     */
    fun getPipelineOAuthUser(
        projectId: String,
        pipelineId: String
    ) = try {
        pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId)
    } catch (ignored: Exception) {
        logger.warn("get pipeline oauth user failed", ignored)
        null
    }

    /**
     * 创作流启动参数
     */
    fun creativeStreamParams(
        projectId: String,
        agentHashId: String,
        userId: String
    ): Map<String, String> {
        val params = mutableMapOf(NODE_AGENT_ID to agentHashId)
        getWorkspaceInfo(
            projectId = projectId,
            agentHashId = agentHashId,
            userId = userId
        )?.let {
            params[CI_NODE_ID] = it.workspaceName ?: ""
            params[CI_NODE_NAME] = it.displayName ?: ""
            params[CI_NODE_IP] = it.innerIp ?: ""
        }
        return params
    }

    fun creativeStreamBuildParameters(
        projectId: String,
        pipelineId: String,
        paramMap: Map<String, String>,
        userId: String
    ): Map<String, BuildParameters> {
        val startBuildParameters = mutableMapOf<String, BuildParameters>()
        paramMap[NODE_AGENT_ID]?.let {
            if (it.isBlank()) return@let
            creativeStreamParams(
                projectId = projectId,
                agentHashId = it,
                userId = pipelineRepositoryService.getPipelineOauthUser(
                    projectId = projectId,
                    pipelineId = pipelineId
                ) ?: userId
            ).mapValues { entry ->
                startBuildParameters[entry.key] = BuildParameters(
                    key = entry.key,
                    value = entry.value,
                    readOnly = true,
                    valueType = BuildFormPropertyType.STRING
                )
            }
        }
        return startBuildParameters
    }

    /**
     * 获取云桌面信息
     */
    open fun getWorkspaceInfo(
        projectId: String,
        agentHashId: String,
        userId: String
    ): WorkspaceBaseInfo? = null

    /**
     * 获取云桌面信息
     */
    open fun getWorkspaceInfoByName(
        projectId: String,
        workspaceName: String,
        userId: String
    ): WorkspaceBaseInfo? = null

    fun getEnvNodeList(
        userId: String,
        projectId: String,
        envHashId: String
    ): List<String> {
        return try {
            client.get(ServiceEnvironmentResource::class).listNodesByEnvIdsNew(
                projectId = projectId,
                envHashIds = listOf(envHashId),
                userId = userId,
                page = -1
            ).data?.records
        } catch (ignored: Exception) {
            logger.warn("get env node list failed", ignored)
            listOf()
        }?.map { it.agentHashId ?: "" } ?: listOf()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CreativeStreamService::class.java)
    }
}