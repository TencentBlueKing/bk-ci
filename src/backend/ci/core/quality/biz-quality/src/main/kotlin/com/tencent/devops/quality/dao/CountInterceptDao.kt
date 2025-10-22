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

package com.tencent.devops.quality.dao

import com.tencent.devops.model.quality.tables.TCountIntercept
import com.tencent.devops.model.quality.tables.records.TCountInterceptRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository@Suppress("ALL")
class CountInterceptDao {
    fun list(
        dslContext: DSLContext,
        projectId: String,
        start: LocalDate,
        end: LocalDate
    ): Result<TCountInterceptRecord> {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(DATE.ge(start))
                .and(DATE.le(end))
                .orderBy(DATE.desc())
                .fetch()
        }
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, date: LocalDate): TCountInterceptRecord? {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(DATE.eq(date))
                .fetchOne()
        }
    }

    fun create(dslContext: DSLContext, projectId: String, date: LocalDate, count: Int): Long {
        val now = LocalDateTime.now()
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                DATE,
                COUNT,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                date,
                count,
                now,
                now
            )
                .returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun plusCount(dslContext: DSLContext, id: Long) {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            dslContext.update(this)
                .set(COUNT, COUNT + 1)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun plusRuleInterceptCount(dslContext: DSLContext, id: Long, count: Int = 1) {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            dslContext.update(this)
                .set(RULE_INTERCEPT_COUNT, RULE_INTERCEPT_COUNT + count)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun count(dslContext: DSLContext, projectId: String): Long {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.select(COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun countRuleIntercept(dslContext: DSLContext, projectId: String): Long {
        with(TCountIntercept.T_COUNT_INTERCEPT) {
            return dslContext.select(RULE_INTERCEPT_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }
}
