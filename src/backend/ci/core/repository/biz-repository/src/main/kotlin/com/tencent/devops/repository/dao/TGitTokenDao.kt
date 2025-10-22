/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.dao

import com.tencent.devops.model.repository.tables.TRepositoryTgitToken
import com.tencent.devops.model.repository.tables.records.TRepositoryTgitTokenRecord
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TGitTokenDao {
    fun getAccessToken(dslContext: DSLContext, userId: String): TRepositoryTgitTokenRecord? {
        with(TRepositoryTgitToken.T_REPOSITORY_TGIT_TOKEN) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .fetchOne()
        }
    }

    fun saveAccessToken(dslContext: DSLContext, userId: String, oauthUserId: String, token: GitToken): Int {
        with(TRepositoryTgitToken.T_REPOSITORY_TGIT_TOKEN) {
            return dslContext.insertInto(
                this,
                USER_ID,
                OAUTH_USER_ID,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                TOKEN_TYPE,
                EXPIRES_IN,
                CREATE_TIME
            )
                .values(
                    userId,
                    oauthUserId,
                    token.accessToken,
                    token.refreshToken,
                    token.tokenType,
                    token.expiresIn,
                    LocalDateTime.now()
                )
                .onDuplicateKeyUpdate()
                .set(ACCESS_TOKEN, token.accessToken)
                .set(REFRESH_TOKEN, token.refreshToken)
                .set(OAUTH_USER_ID, oauthUserId)
                .set(TOKEN_TYPE, token.tokenType)
                .set(EXPIRES_IN, token.expiresIn)
                .set(CREATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun deleteToken(dslContext: DSLContext, userId: String): Int {
        with(TRepositoryTgitToken.T_REPOSITORY_TGIT_TOKEN) {
            return dslContext.deleteFrom(this)
                .where(USER_ID.eq(userId))
                .execute()
        }
    }
}
