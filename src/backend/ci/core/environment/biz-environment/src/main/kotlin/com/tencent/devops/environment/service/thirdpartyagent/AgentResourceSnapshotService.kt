package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.environment.dao.thirdpartyagent.AgentResourceSnapshotDao
import com.tencent.devops.environment.pojo.AgentResourceSnapshot
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgent.T_ENVIRONMENT_THIRDPARTY_AGENT
import com.tencent.devops.model.environment.tables.TAgentResourceSnapshot.T_AGENT_RESOURCE_SNAPSHOT
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AgentResourceSnapshotService @Autowired constructor(
    private val context: DSLContext,
    private val dao: AgentResourceSnapshotDao
) {
    fun report(projectId: String, agentHashId: String, secret: String, data: String) {
        try {
            context.transaction { configuration ->
                val tx = DSL.using(configuration)
                val agent = with(T_ENVIRONMENT_THIRDPARTY_AGENT) {
                    tx.selectFrom(this).where(ID.eq(HashUtil.decodeIdToLong(agentHashId)))
                        .and(PROJECT_ID.eq(projectId)).forUpdate().fetchOne()
                } ?: throw PermissionForbiddenException("Agent does not exist")
                if (SecurityUtil.decrypt(agent.secretKey) != secret) {
                    throw PermissionForbiddenException("Invalid agent credentials")
                }
                if (agent.status != AgentStatus.IMPORT_OK.status) return@transaction
                // Keep the established telemetry pipeline available when optional snapshot parsing/storage fails.
                try {
                    dao.save(tx, projectId, agent.id, AgentResourceParser.parse(data, agent.agentInstallPath))
                } catch (e: Exception) {
                    logger.warn("Cannot save resource snapshot for agent ${agent.id}", e)
                }
            }
        } catch (e: DataAccessException) {
            // Snapshot availability must not break the established core/ext monitoring pipeline.
            logger.warn("Resource snapshot storage unavailable for agent $agentHashId", e)
        }
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    fun cleanExpired() {
        // Bounded and indexed; safe for multiple instances. Read-time expiry remains authoritative.
        with(T_AGENT_RESOURCE_SNAPSHOT) {
            context.deleteFrom(this)
                .where(UPDATED_TIME.lt(System.currentTimeMillis() - AgentResourceSnapshot.MAX_AGE_MILLIS))
                .limit(1000).execute()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentResourceSnapshotService::class.java)
    }
}
