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

import com.tencent.devops.model.experience.tables.TExperienceGroupOuter
import com.tencent.devops.model.experience.tables.records.TExperienceGroupOuterRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceGroupOuterDao {
    fun create(
        dslContext: DSLContext,
        groupId: Long,
        outer: String
    ) {
        val now = LocalDateTime.now()
        with(TExperienceGroupOuter.T_EXPERIENCE_GROUP_OUTER) {
            dslContext.insertInto(
                this,
                GROUP_ID,
                OUTER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                groupId,
                outer,
                now,
                now
            ).onConflictDoNothing().execute()
        }
    }

    fun deleteByGroupId(dslContext: DSLContext, groupId: Long) {
        with(TExperienceGroupOuter.T_EXPERIENCE_GROUP_OUTER) {
            dslContext.delete(this)
                .where(GROUP_ID.eq(groupId))
                .execute()
        }
    }

    fun listGroupIdsByUserId(dslContext: DSLContext, outer: String): Result<Record1<Long>> {
        return with(TExperienceGroupOuter.T_EXPERIENCE_GROUP_OUTER) {
            dslContext.select(GROUP_ID)
                .from(this)
                .where(OUTER.eq(outer))
                .fetch()
        }
    }

    fun listByGroupIds(dslContext: DSLContext, groupIds: Set<Long>): Result<TExperienceGroupOuterRecord> {
        return with(TExperienceGroupOuter.T_EXPERIENCE_GROUP_OUTER) {
            dslContext.selectFrom(this)
                .where(GROUP_ID.`in`(groupIds))
                .fetch()
        }
    }
}
