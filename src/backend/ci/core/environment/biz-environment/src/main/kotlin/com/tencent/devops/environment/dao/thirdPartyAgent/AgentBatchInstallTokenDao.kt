package com.tencent.devops.environment.dao.thirdPartyAgent

import com.tencent.devops.model.environment.tables.TAgentBatchInstallToken
import com.tencent.devops.model.environment.tables.records.TAgentBatchInstallTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AgentBatchInstallTokenDao {
    fun createOrUpdateToken(
        dslContext: DSLContext,
        projectId: String,
        userId: String,
        token: String,
        createTime: LocalDateTime,
        expireTime: LocalDateTime
    ) {
        with(TAgentBatchInstallToken.T_AGENT_BATCH_INSTALL_TOKEN) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                USER_ID,
                TOKEN,
                CREATED_TIME,
                EXPIRED_TIME
            ).values(
                projectId,
                userId,
                token,
                createTime,
                expireTime
            ).onDuplicateKeyUpdate()
                .set(TOKEN, token)
                .set(CREATED_TIME, createTime)
                .set(EXPIRED_TIME, expireTime)
                .execute()
        }
    }

    fun deleteToken(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ) {
        with(TAgentBatchInstallToken.T_AGENT_BATCH_INSTALL_TOKEN) {
            dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(USER_ID.eq(userId)).execute()
        }
    }

    fun getToken(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): TAgentBatchInstallTokenRecord? {
        with(TAgentBatchInstallToken.T_AGENT_BATCH_INSTALL_TOKEN) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(USER_ID.eq(userId)).fetchAny()
        }
    }
}
