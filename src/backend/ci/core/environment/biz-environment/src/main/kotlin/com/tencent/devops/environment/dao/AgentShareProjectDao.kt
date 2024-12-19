package com.tencent.devops.environment.dao

import com.tencent.devops.common.db.utils.fetchCount
import com.tencent.devops.common.db.utils.fetchCountFix
import com.tencent.devops.model.environment.tables.TAgentShareProject
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.jooq.Select

@Repository
class AgentShareProjectDao {
    fun selectSharedAgentCount(
        dslContext: DSLContext,
        agentId: Long,
        sharedProjectId: String
    ): Int {
        with(TAgentShareProject.T_AGENT_SHARE_PROJECT) {
            return dslContext.fetchCountFix(
                dslContext.selectFrom(this)
                    .where(AGENT_ID.eq(agentId))
                    .and(SHARED_PROJECT_ID.eq(sharedProjectId))
            )
        }
    }

    fun add(
        dslContext: DSLContext,
        agentId: Long,
        mainProjectId: String,
        sharedProjectId: String,
        creator: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAgentShareProject.T_AGENT_SHARE_PROJECT) {
            return dslContext.insertInto(
                this,
                AGENT_ID,
                MAIN_PROJECT_ID,
                SHARED_PROJECT_ID,
                CREATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                agentId,
                mainProjectId,
                sharedProjectId,
                creator,
                now,
                now
            ).onDuplicateKeyUpdate().set(CREATOR, creator).set(UPDATE_TIME, now).execute()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        agentId: Long,
        mainProjectId: String,
        sharedProjectIds: List<String>,
        creator: String
    ) {
        val now = LocalDateTime.now()
        with(TAgentShareProject.T_AGENT_SHARE_PROJECT) {
            dslContext.insertInto(
                this,
                AGENT_ID,
                MAIN_PROJECT_ID,
                SHARED_PROJECT_ID,
                CREATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).also { insertSql ->
                sharedProjectIds.forEach { projectId ->
                    insertSql.values(
                        agentId,
                        mainProjectId,
                        projectId,
                        creator,
                        now,
                        now
                    ).onDuplicateKeyUpdate()
                        .set(CREATOR, creator)
                        .set(UPDATE_TIME, now)
                }
            }.execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        agentId: Long,
        mainProjectId: String,
        sharedProjectId: String
    ): Int {
        with(TAgentShareProject.T_AGENT_SHARE_PROJECT) {
            return dslContext.deleteFrom(this)
                .where(AGENT_ID.eq(agentId))
                .and(MAIN_PROJECT_ID.eq(mainProjectId))
                .and(SHARED_PROJECT_ID.eq(sharedProjectId))
                .execute()
        }
    }
}
