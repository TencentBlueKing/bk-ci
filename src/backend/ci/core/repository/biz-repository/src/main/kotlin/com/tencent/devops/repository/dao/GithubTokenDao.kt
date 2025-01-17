/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.devops.model.repository.tables.TRepositoryGithubToken
import com.tencent.devops.model.repository.tables.records.TRepositoryGithubTokenRecord
import com.tencent.devops.repository.pojo.oauth.GithubTokenType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GithubTokenDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        accessToken: String,
        tokenType: String,
        scope: String,
        githubTokenType: GithubTokenType = GithubTokenType.GITHUB_APP,
        operator: String
    ) {
        val now = LocalDateTime.now()
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.insertInto(
                this,
                USER_ID,
                ACCESS_TOKEN,
                TOKEN_TYPE,
                SCOPE,
                CREATE_TIME,
                UPDATE_TIME,
                TYPE,
                OPERATOR
            ).values(
                userId,
                accessToken,
                tokenType,
                scope,
                now,
                now,
                githubTokenType.name,
                operator
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        userId: String,
        accessToken: String,
        tokenType: String,
        scope: String,
        githubTokenType: GithubTokenType = GithubTokenType.GITHUB_APP,
        operator: String
    ) {
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.update(this)
                .set(TOKEN_TYPE, tokenType)
                .set(ACCESS_TOKEN, accessToken)
                .set(SCOPE, scope)
                .let {
                    if (operator.isNotBlank()) {
                        it.set(OPERATOR, operator)
                    }
                    it
                }
                .where(USER_ID.eq(userId)).and(TYPE.eq(githubTokenType.name))
                .execute()
        }
    }

    fun getOrNull(
        dslContext: DSLContext,
        userId: String,
        githubTokenType: GithubTokenType?
    ): TRepositoryGithubTokenRecord? {
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            return getByOperator(
                dslContext = dslContext,
                operator = userId,
                githubTokenType = githubTokenType
            ) ?: dslContext.selectFrom(this).where(USER_ID.eq(userId))
                .let {
                    if (githubTokenType != null) {
                        it.and(TYPE.eq(githubTokenType.name))
                    } else it
                }
                .fetchOne()
        }
    }

    fun getByOperator(
        dslContext: DSLContext,
        operator: String,
        githubTokenType: GithubTokenType?
    ): TRepositoryGithubTokenRecord? {
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            return dslContext.selectFrom(this)
                .where(OPERATOR.eq(operator))
                .let {
                    if (githubTokenType != null) {
                        it.and(TYPE.eq(githubTokenType.name))
                    } else it
                }
                .orderBy(CREATE_TIME.desc())
                .fetch()
                .firstOrNull()
        }
    }

    /**
     * 删除token
     * @param operator 蓝盾平台操作人用户名
     */
    fun delete(
        dslContext: DSLContext,
        operator: String
    ) {
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.deleteFrom(this).where(OPERATOR.eq(operator))
        }
    }
}
