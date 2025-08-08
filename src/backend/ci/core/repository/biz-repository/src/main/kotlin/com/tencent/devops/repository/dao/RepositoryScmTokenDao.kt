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

import com.tencent.devops.model.repository.tables.TRepositoryScmToken
import com.tencent.devops.model.repository.tables.records.TRepositoryScmTokenRecord
import com.tencent.devops.repository.pojo.oauth.RepositoryScmToken
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RepositoryScmTokenDao {
    fun getToken(
        dslContext: DSLContext,
        userId: String,
        scmCode: String,
        appType: String
    ): TRepositoryScmTokenRecord? {
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            return dslContext.selectFrom(this)
                .where(
                    USER_ID.eq(userId)
                        .and(SCM_CODE.eq(scmCode))
                        .and(APP_TYPE.eq(appType))
                )
                .fetchOne()
        }
    }

    fun saveAccessToken(dslContext: DSLContext, scmToken: RepositoryScmToken): Int {
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                USER_ID,
                SCM_CODE,
                APP_TYPE,
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                EXPIRES_IN,
                CREATE_TIME,
                UPDATE_TIME,
                OPERATOR
            )
                .values(
                    scmToken.userId,
                    scmToken.scmCode,
                    scmToken.appType,
                    scmToken.accessToken,
                    scmToken.refreshToken,
                    scmToken.expiresIn,
                    now,
                    now,
                    scmToken.operator
                )
                .onDuplicateKeyUpdate()
                .set(ACCESS_TOKEN, scmToken.accessToken)
                .set(REFRESH_TOKEN, scmToken.refreshToken)
                .set(EXPIRES_IN, scmToken.expiresIn)
                .set(UPDATE_TIME, LocalDateTime.now())
                .let {
                    if (scmToken.operator.isNotBlank()) {
                        it.set(OPERATOR, scmToken.operator)
                    } else {
                        it
                    }
                }
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, userId: String, scmCode: String) {
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            dslContext.deleteFrom(this)
                .where(USER_ID.eq(userId))
                .and(SCM_CODE.eq(scmCode))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, operator: String, scmCode: String): Result<TRepositoryScmTokenRecord> {
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            return dslContext.selectFrom(this)
                    .where(OPERATOR.eq(operator))
                    .and(SCM_CODE.eq(scmCode))
                    .fetch()
        }
    }

    fun getTokenByOperator(
        dslContext: DSLContext,
        operator: String,
        scmCode: String,
        appType: String
    ): TRepositoryScmTokenRecord? {
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            return dslContext.selectFrom(this)
                    .where(
                        OPERATOR.eq(operator)
                                .and(SCM_CODE.eq(scmCode))
                                .and(APP_TYPE.eq(appType))
                    )
                    .orderBy(CREATE_TIME.desc())
                    .limit(1)
                    .fetchOne()
        }
    }
}
