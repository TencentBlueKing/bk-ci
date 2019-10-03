package com.tencent.devops.repository.dao

import com.tencent.devops.model.repository.tables.TRepositoryGtiToken
import com.tencent.devops.model.repository.tables.records.TRepositoryGtiTokenRecord
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GitTokenDao {
    fun getAccessToken(dslContext: DSLContext, userId: String): TRepositoryGtiTokenRecord? {
        with(TRepositoryGtiToken.T_REPOSITORY_GTI_TOKEN) {
            return dslContext.selectFrom(this)
                    .where(USER_ID.eq(userId))
                    .fetchOne()
        }
    }

    fun saveAccessToken(dslContext: DSLContext, userId: String, token: GitToken): Int {
        with(TRepositoryGtiToken.T_REPOSITORY_GTI_TOKEN) {
            return dslContext.insertInto(this,
                    USER_ID,
                    ACCESS_TOKEN,
                    REFRESH_TOKEN,
                    TOKEN_TYPE,
                    EXPIRES_IN
            )
                    .values(
                            userId,
                            token.accessToken,
                            token.refreshToken,
                            token.tokenType,
                            token.expiresIn
                    )
                    .onDuplicateKeyUpdate()
                    .set(ACCESS_TOKEN, token.accessToken)
                    .set(REFRESH_TOKEN, token.refreshToken)
                    .set(TOKEN_TYPE, token.tokenType)
                    .set(EXPIRES_IN, token.expiresIn)
                    .execute()
        }
    }

    fun deleteToken(dslContext: DSLContext, userId: String): Int {
        with(TRepositoryGtiToken.T_REPOSITORY_GTI_TOKEN) {
            return dslContext.deleteFrom(this)
                    .where(USER_ID.eq(userId))
                    .execute()
        }
    }
}