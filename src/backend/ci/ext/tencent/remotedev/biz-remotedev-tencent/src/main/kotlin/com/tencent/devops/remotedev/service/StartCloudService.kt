package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.api.service.ServiceTXPipelineResource
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.CheckoutPipelineParameter
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_CHECKOUT_TEMPLATE_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class StartCloudService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val workspaceDao: WorkspaceDao,
    private val remoteDevSettingDao: RemoteDevSettingDao,
    private val cacheService: RedisCacheService
) {
    private val executorService = Executors.newFixedThreadPool(20)

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }

    fun afterStartCloudInit(userId: String, workspaceName: String?, agentId: String): Boolean {
        logger.info("userId($userId) after start cloud init|$workspaceName|$agentId")
        val workspace = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            userId = userId,
            mountType = WorkspaceMountType.START,
            status = WorkspaceStatus.RUNNING
        ) ?: kotlin.run {
            logger.warn("user($userId) not have running start cloud workspace, return false.")
            return false
        }

        if (workspaceName != null && workspace.name != workspaceName) {
            logger.warn("inconsistent workspace |${workspace.name}|$workspaceName")
            return false
        }

        val templateVersionId = cacheService.get(REDIS_CHECKOUT_TEMPLATE_ID) ?: kotlin.run {
            logger.error("REDIS_CHECKOUT_TEMPLATE_ID empty !!")
            return false
        }
        val projectId = remoteDevSettingDao.fetchAnySetting(dslContext, workspace.creator).projectId

        executorService.submit<Unit> {
            val agentInfo = client.get(ServiceThirdPartyAgentResource::class).getAgentDetail(
                userId = userId,
                projectId = projectId,
                agentHashId = agentId
            ).data ?: kotlin.run {
                logger.warn("project($projectId) not have agent for id($agentId), return.")
                return@submit
            }

            logger.info("get agent |$projectId|$agentId|${agentInfo.displayName}")

            val parameter = JsonUtil.anyTo(
                CheckoutPipelineParameter(
                    nodeDisplayName = agentInfo.displayName,
                    gitUrl = workspace.url,
                    gitBranch = workspace.branch,
                    gitSavePath = ""
                ),
                object : TypeReference<Map<String, String>>() {}
            )

            logger.info("load parameter |$parameter")

            val res = client.get(ServiceTXPipelineResource::class).runPipelineWithTemplate(
                userId = userId,
                projectId = projectId,
                templateVersionId = templateVersionId.toLong(),
                parameters = parameter
            )
            logger.info("start build ${res.data?.id}")
        }
        return true
    }
}
