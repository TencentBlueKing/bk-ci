package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceP2pConnect
import com.tencent.devops.model.experience.tables.records.TExperienceP2pConnectRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 *
 */
@Repository
class ExperienceP2pConnectDao {
    /**
     * 新增联系人
     */
    fun create(
        dslContext: DSLContext,
        recordId: Long,
        sender: String,
        receiver: String
    ) {
        with(TExperienceP2pConnect.T_EXPERIENCE_P2P_CONNECT) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this)
                .set(RECORD_ID, recordId)
                .set(SENDER, sender)
                .set(RECEIVER, receiver)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    /**
     * 获取最近联系人
     */
    fun listRecentByUserId(
        dslContext: DSLContext,
        userId: String,
        recentDay: Long,
        limit: Long
    ): List<TExperienceP2pConnectRecord> {
        with(TExperienceP2pConnect.T_EXPERIENCE_P2P_CONNECT) {
            return dslContext.selectFrom(this)
                .where(SENDER.eq(userId).or(RECEIVER.eq(userId)))
                .and(CREATE_TIME.greaterThan(LocalDateTime.now().minusDays(recentDay)))
                .orderBy(CREATE_TIME.desc())
                .limit(limit)
                .fetch()
        }
    }
}