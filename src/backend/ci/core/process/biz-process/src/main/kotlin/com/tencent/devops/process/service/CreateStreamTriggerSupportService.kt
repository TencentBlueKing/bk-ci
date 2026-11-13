package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.pojo.EnvData
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_IP
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_NAME
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.service.pipeline.RuntimeResourceResolver
import com.tencent.devops.process.pojo.WorkspaceBaseInfo
import com.tencent.devops.process.utils.NODE_AGENT_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 创作流流水线服务类
 */
@Service
class CreateStreamTriggerSupportService constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val runtimeResourceResolver: RuntimeResourceResolver
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
        projectId: String?,
        workspaceName: String,
        userId: String
    ): List<EnvData> {
        return try {
            val result = client.get(ServiceEnvironmentResource::class).fetchAllNodeEnvListByWorkspace(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = userId,
                noCheckPerm = true
            ).data
            logger.info("fetch all node env list|$projectId|$workspaceName")
            result
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
        logger.info("get env node list|$projectId|$envHashId|$userId")
        return runtimeResourceResolver.listEnvAgentHashIds(
            projectId = projectId,
            userId = userId,
            envHashId = envHashId
        )
    }

    /**
     * 列出创作流可执行的构建机。
     *
     * 单体运行资源（数字分身 / 云桌面）固定一台，环境资源列出环境下的全部节点。
     */
    fun getRuntimeResourceNodeList(
        pipelineInfo: PipelineInfo,
        pipelineSetting: PipelineSetting,
        userId: String
    ): List<String>? {
        if (pipelineInfo.channelCode != ChannelCode.CREATIVE_STREAM) return null
        val projectId = pipelineInfo.projectId
        val resource = pipelineSetting.runtimeResource() ?: run {
            logger.warn("pipeline[${pipelineInfo.pipelineId}] runtime resource not configured")
            return null
        }
        return if (resource.group) {
            resource.envHashId?.let {
                runtimeResourceResolver.listEnvAgentHashIds(projectId, userId, it)
            }.orEmpty()
        } else {
            listOfNotNull(
                runtimeResourceResolver.resolveAgentHashId(projectId, userId, resource)
            )
        }
    }

    fun externalWebhookStartParams(
        pipelineInfo: PipelineInfo,
        userId: String
    ): MutableMap<String, String> {
        val pipelineId = pipelineInfo.pipelineId
        val projectId = pipelineInfo.projectId
        val externalStartParams = mutableMapOf<String, String>()
        if (pipelineInfo.channelCode != ChannelCode.CREATIVE_STREAM) {
            return externalStartParams
        }
        // 创作流场景下才需要查询 setting 及对应的环境节点列表
        pipelineRepositoryService.getSetting(
            projectId = projectId,
            pipelineId = pipelineId
        )?.let {
            val resource = it.runtimeResource()
            val list = getRuntimeResourceNodeList(
                pipelineInfo = pipelineInfo,
                pipelineSetting = it,
                userId = userId
            ) ?: listOf()
            logger.info(
                "creative stream pipeline[$pipelineId] resolved nodes from resource[$resource]: $list"
            )
            if (list.isEmpty()) {
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_CREATIVE_STREAM_ENV_NODE_IS_EMPTY,
                        params = arrayOf(resource?.resourceId ?: "")
                    )
                )
            }
            // 环境资源现阶段取环境下的第一个节点，后续根据实际需求进行调整
            externalStartParams.putAll(
                creativeStreamParams(
                    projectId = projectId,
                    agentHashId = list.first(),
                    userId = userId
                )
            )
        }
        return externalStartParams
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CreateStreamTriggerSupportService::class.java)
    }
}