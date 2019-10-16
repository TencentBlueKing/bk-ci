package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TToken
import com.tencent.devops.model.experience.tables.records.TTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TokenDao {
    fun create(dslContext: DSLContext, experienceId: Long, userId: String, token: String, expireTime: LocalDateTime): Long {
        val now = LocalDateTime.now()
        with(TToken.T_TOKEN) {
            val record = dslContext.insertInto(this,
                    EXPERIENCE_ID,
                    USER_ID,
                    TOKEN,
                    EXPIRE_TIME,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    experienceId,
                    userId,
                    token,
                    expireTime,
                    now,
                    now)
                    .returning(ID)
                    .fetchOne()
            return record.id
        }
    }

    fun batchCreate(dslContext: DSLContext, experienceId: Long, expireTime: LocalDateTime, userIdAndTokenList: List<Pair<String, String>>) {
        val now = LocalDateTime.now()
        with(TToken.T_TOKEN) {
            val recordList = userIdAndTokenList.map {
                TTokenRecord(0L, experienceId, it.first, it.second, expireTime, now, now)
            }
            dslContext.batchInsert(recordList).execute()
        }
    }

    fun getOrNull(dslContext: DSLContext, token: String): TTokenRecord? {
        with(TToken.T_TOKEN) {
            return dslContext.selectFrom(this)
                    .where(TOKEN.eq(token))
                    .fetchOne()
        }
    }
}