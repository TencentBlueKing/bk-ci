package com.tencent.devops.artifactory.dao

import com.tencent.devops.model.artifactory.tables.TToken
import com.tencent.devops.model.artifactory.tables.records.TTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TokenDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        artifactoryType: String,
        path: String,
        token: String,
        expireTime: LocalDateTime
    ): Long {
        val now = LocalDateTime.now()
        with(TToken.T_TOKEN) {
            val record = dslContext.insertInto(this,
                    USER_ID,
                    PROJECT_ID,
                    ARTIFACTORY_TYPE,
                    PATH,
                    TOKEN,
                    EXPIRE_TIME,
                    CREATE_TIME,
                    UPDATE_TIME
            ).values(
                    userId,
                    projectId,
                    artifactoryType,
                    path,
                    token,
                    expireTime,
                    now,
                    now)
                    .returning(ID)
                    .fetchOne()
            return record.id
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