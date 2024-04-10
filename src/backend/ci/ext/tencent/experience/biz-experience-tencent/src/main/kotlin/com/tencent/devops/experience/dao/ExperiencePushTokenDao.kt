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

package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperiencePushToken
import com.tencent.devops.model.experience.tables.records.TExperiencePushTokenRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperiencePushTokenDao {
    fun getByUserId(
        dslContext: DSLContext,
        userId: String
    ): TExperiencePushTokenRecord? {
        with(TExperiencePushToken.T_EXPERIENCE_PUSH_TOKEN) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .fetchOne()
        }
    }

    fun getByToken(
        dslContext: DSLContext,
        token: String
    ): TExperiencePushTokenRecord? {
        with(TExperiencePushToken.T_EXPERIENCE_PUSH_TOKEN) {
            return dslContext.selectFrom(this)
                .where(TOKEN.eq(token))
                .fetchOne()
        }
    }

    fun updateUserToken(
        dslContext: DSLContext,
        userId: String,
        token: String,
        platform: String
    ): Boolean {
        val now = LocalDateTime.now()
        return with(TExperiencePushToken.T_EXPERIENCE_PUSH_TOKEN) {
            dslContext.update(this)
                .set(TOKEN, token)
                .set(UPDATE_TIME, now)
                .set(PLATFORM, platform)
                .where(USER_ID.eq(userId))
                .execute() == 1
        }
    }

    fun createUserToken(
        dslContext: DSLContext,
        userId: String,
        token: String,
        platform: String
    ): Long {
        val now = LocalDateTime.now()
        with(TExperiencePushToken.T_EXPERIENCE_PUSH_TOKEN) {
            return dslContext.insertInto(
                this,
                USER_ID,
                TOKEN,
                PLATFORM,
                UPDATE_TIME,
                CREATE_TIME
            ).values(
                userId,
                token,
                platform,
                now,
                now
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun deleteUserToken(
        dslContext: DSLContext,
        userId: String,
        token: String
    ) {
        with(TExperiencePushToken.T_EXPERIENCE_PUSH_TOKEN) {
            dslContext.deleteFrom(this)
                .where(USER_ID.eq(userId))
                .and(TOKEN.eq(token))
                .execute()
        }
    }
}
