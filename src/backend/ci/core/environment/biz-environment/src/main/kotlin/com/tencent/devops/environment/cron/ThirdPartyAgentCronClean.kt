package com.tencent.devops.environment.cron

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentDisableInfo
import com.tencent.devops.environment.model.AgentDisableType
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ThirdPartyAgentCronClean @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val actionDao: ThirdPartyAgentActionDao,
    private val agentDao: ThirdPartyAgentDao,
    private val tokenService: ClientTokenService
) {
    // 清理超过100的Action数据
    @Scheduled(cron = "0 28 3 * * ?")
    fun cronDeleteActionRecord() {
        val agentIds = actionDao.fetchAgentIdByGtCount(dslContext, 100)
        agentIds.forEach { agentId ->
            logger.info("cronDeleteActionRecord start clean agent $agentId")

            val id = actionDao.getIndexId(dslContext, agentId, 1, 99) ?: run {
                logger.info("cronDeleteActionRecord agent $agentId index 100 is null")
                return@forEach
            }
            logger.info("cronDeleteActionRecord agent $agentId will clean less than id $id")

            val count = actionDao.deleteOldActionById(dslContext, agentId, id)
            logger.info("cronDeleteActionRecord agent $agentId cleaned $count")

            Thread.sleep(500)
        }
    }

    // 将禁用项目的和两个月没有构建任务的Agent标记为禁用
    @Scheduled(cron = "0 28 2 * * ?")
    fun fetchAndDisableAgents() {
        // 对已经发送过通知但是还没有禁用的做筛选
        val now = LocalDateTime.now()
        val needDisAgents = agentDao.fetchNeedDisabledAgent(dslContext, now.timestamp())
        needDisAgents.chunked(100).forEach { chunk ->
            val noBuildAgents = fetchNoBuildAgents(chunk.map { it.first }.toSet(), now.minusMonths(2).timestamp())
            agentDao.batchUpdateAgent(dslContext, noBuildAgents, AgentStatus.DISABLED)
        }

        // 寻找还没有发送通知的做禁用预告
        val max = redisOperation.get(MAX_TO_DISABLE_AGENT)?.toInt()
        val agents = agentDao.fetchAgents(
            dslContext = dslContext,
            status = setOf(AgentStatus.IMPORT_OK),
            hasDisableInfo = false,
            limit = if (max != null) {
                PageUtil.convertPageSizeToSQLLimit(1, max)
            } else {
                null
            }
        ).associateBy { it.id }
        agents.keys.chunked(100).forEach { chunk ->
            val noBuildAgents = fetchNoBuildAgents(chunk.toSet(), now.minusMonths(2).timestamp())
            noBuildAgents.forEach noBuildFor@{ agentId ->
                val agent = agents[agentId] ?: return@noBuildFor
                agentDao.updateAgentDisableInfo(
                    dslContext, agentId, AgentDisableInfo(
                        type = AgentDisableType.AGENT_IDLE_DISABLED,
                        time = now.plusDays(7).timestamp()
                    )
                )
                val request = SendNotifyMessageTemplateRequest(
                    templateCode = "THIRDPART_AGENT_DISABLE_NOTIFY",
                    receivers = getNotifyReceivers(agent),
                    titleParams = mapOf(
                        "projectId" to agent.projectId,
                        "agentId" to HashUtil.encodeLongId(agentId)
                    ),
                    bodyParams = mapOf(
                        "url" to "${HomeHostUtil.innerServerHost()}/console/environment/${agent.projectId}/" +
                                "nodeDetail/${HashUtil.encodeLongId(agent.nodeId)}"
                    )
                )
                try {
                    client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
                } catch (e: Exception) {
                    logger.error("fetchAndDisableAgents send notify err", e)
                    return@noBuildFor
                }
            }
        }
    }

    private fun fetchNoBuildAgents(agentIds: Set<Long>, time: Long): Set<Long> {
        val agents = try {
            client.get(ServiceAgentResource::class).fetchAgentBuildByTime(
                time = time,
                agentIds = agentIds.map { HashUtil.encodeLongId(it) }.toSet()
            ).data
        } catch (e: Exception) {
            logger.error("fetchNoBuildAgents err", e)
            null
        }?.map { HashUtil.decodeIdToLong(it) }?.toSet() ?: return emptySet()

        return agentIds.subtract(agents)
    }

    private fun getNotifyReceivers(agentRecord: TEnvironmentThirdpartyAgentRecord): MutableSet<String> {
        val users = mutableSetOf(agentRecord.createdUser)
        val nodeHashId = HashUtil.encodeLongId(agentRecord.nodeId)
        val authUsers = kotlin.runCatching {
            client.get(ServiceResourceMemberResource::class).getResourceGroupMembers(
                token = tokenService.getSystemToken(),
                projectCode = agentRecord.projectId,
                resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value,
                resourceCode = nodeHashId
            ).data
        }.onFailure {
            logger.warn("getNotifyReceivers ${agentRecord.projectId}|$nodeHashId")
        }.getOrNull() ?: emptyList()
        users.addAll(authUsers)
        return users.toMutableSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentCronClean::class.java)
        private const val MAX_TO_DISABLE_AGENT = "environment:thirdparty:goagent:maxdisable"
    }
}
