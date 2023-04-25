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

import com.tencent.devops.model.experience.tables.TExperienceSubscribe
import com.tencent.devops.model.experience.tables.records.TExperienceSubscribeRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperiencePushSubscribeDao {
    fun createSubscription(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        bundle: String,
        platform: String
    ): Long {
        val now = LocalDateTime.now()
        with(TExperienceSubscribe.T_EXPERIENCE_SUBSCRIBE) {
            return dslContext.insertInto(
                this,
                USER_ID,
                PROJECT_ID,
                BUNDLE_IDENTIFIER,
                PLATFORM,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                userId,
                projectId,
                bundle,
                platform,
                now,
                now
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun deleteSubscription(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        bundle: String,
        platform: String
    ) {
        with(TExperienceSubscribe.T_EXPERIENCE_SUBSCRIBE) {
            dslContext.deleteFrom(this)
                .where(USER_ID.eq(userId))
                .and(PROJECT_ID.eq(projectId))
                .and(BUNDLE_IDENTIFIER.eq(bundle))
                .and(PLATFORM.eq(platform))
                .execute()
        }
    }

    fun getSubscription(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        bundle: String,
        platform: String
    ): TExperienceSubscribeRecord? {
        with(TExperienceSubscribe.T_EXPERIENCE_SUBSCRIBE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUNDLE_IDENTIFIER.eq(bundle))
                .and(PLATFORM.eq(platform))
                .and(USER_ID.eq(userId))
                .fetchOne()
        }
    }

    fun listSubscription(
        dslContext: DSLContext,
        projectId: String,
        bundle: String,
        platform: String
    ): Result<TExperienceSubscribeRecord> {
        with(TExperienceSubscribe.T_EXPERIENCE_SUBSCRIBE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUNDLE_IDENTIFIER.eq(bundle))
                .and(PLATFORM.eq(platform))
                .fetch()
        }
    }

    fun listByUserId(
        dslContext: DSLContext,
        userId: String,
        limit: Int
    ): Result<TExperienceSubscribeRecord> {
        with(TExperienceSubscribe.T_EXPERIENCE_SUBSCRIBE) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .orderBy(UPDATE_TIME.desc())
                .limit(limit)
                .fetch()
        }
    }
}
