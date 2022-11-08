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

import com.tencent.devops.model.experience.tables.TExperienceDownload
import com.tencent.devops.model.experience.tables.records.TExperienceDownloadRecord
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceDownloadDao {
    fun create(dslContext: DSLContext, experienceId: Long, userId: String): Long {
        val now = LocalDateTime.now()
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            val record = dslContext.insertInto(
                this,
                EXPERIENCE_ID,
                USER_ID,
                TIMES,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                experienceId,
                userId,
                1,
                now,
                now
            )
                .returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun plusTimes(dslContext: DSLContext, id: Long) {
        val now = LocalDateTime.now()
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            dslContext.update(this)
                .set(TIMES, TIMES + 1)
                .set(UPDATE_TIME, now)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getOrNull(dslContext: DSLContext, experienceId: Long, userId: String): TExperienceDownloadRecord? {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.selectFrom(this)
                .where(EXPERIENCE_ID.eq(experienceId))
                .and(USER_ID.eq(userId))
                .fetchOne()
        }
    }

    fun listByExperienceId(
        dslContext: DSLContext,
        experienceId: Long,
        offset: Int,
        limit: Int
    ): Result<TExperienceDownloadRecord> {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.selectFrom(this)
                .where(EXPERIENCE_ID.eq(experienceId))
                .orderBy(UPDATE_TIME.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun count(dslContext: DSLContext, experienceId: Long): Long {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.selectCount()
                .from(this)
                .where(EXPERIENCE_ID.eq(experienceId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun sumTimes(dslContext: DSLContext, experienceId: Long): Long {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext.select(DSL.sum(TIMES))
                .from(this)
                .where(EXPERIENCE_ID.eq(experienceId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun distinctExperienceIdByUserId(
        dslContext: DSLContext,
        userId: String,
        limit: Int
    ): Result<Record2<Long, LocalDateTime>>? {
        with(TExperienceDownload.T_EXPERIENCE_DOWNLOAD) {
            return dslContext
                .select(EXPERIENCE_ID, UPDATE_TIME)
                .from(this)
                .where(USER_ID.eq(userId))
                .orderBy(UPDATE_TIME.desc())
                .limit(limit)
                .fetch()
        }
    }
}
