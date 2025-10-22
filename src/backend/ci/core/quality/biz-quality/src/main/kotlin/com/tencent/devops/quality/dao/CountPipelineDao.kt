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

import com.tencent.devops.model.quality.tables.TCountPipeline
import com.tencent.devops.model.quality.tables.records.TCountPipelineRecord
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Repository@Suppress("ALL")
class CountPipelineDao {
    fun list(dslContext: DSLContext, projectId: String, pipelineIds: Set<String>): Result<TCountPipelineRecord>? {
        return with(TCountPipeline.T_COUNT_PIPELINE) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .fetch()
        }
    }

    fun listByCount(
        dslContext: DSLContext,
        projectId: String,
        offset: Int,
        limit: Int,
        isDesc: Boolean = true
    ): Result<Record2<String, BigDecimal>> {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            val sql = dslContext.select(PIPELINE_ID, COUNT.sum().`as`("allCount"))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .groupBy(PROJECT_ID, PIPELINE_ID)
            if (isDesc) sql.orderBy(DSL.field("allCount").desc()) else sql.orderBy(DSL.field("allCount"))
            return sql.offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun listByInterceptCount(
        dslContext: DSLContext,
        projectId: String,
        isDesc: Boolean = true
    ): Result<Record2<String, BigDecimal>> {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            val sql = dslContext.select(PIPELINE_ID, INTERCEPT_COUNT.sum().`as`("allCount"))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .groupBy(PROJECT_ID, PIPELINE_ID)
            if (isDesc) sql.orderBy(DSL.field("allCount").desc()) else sql.orderBy(DSL.field("allCount"))
            return sql.fetch()
        }
    }

    fun getOrNull(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        date: LocalDate
    ): TCountPipelineRecord? {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(DATE.eq(date))
                .fetchOne()
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        date: LocalDate,
        count: Int,
        lastInterceptTime: LocalDateTime
    ): Long {
        val now = LocalDateTime.now()
        with(TCountPipeline.T_COUNT_PIPELINE) {
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                DATE,
                COUNT,
                LAST_INTERCEPT_TIME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                pipelineId,
                date,
                count,
                lastInterceptTime,
                now,
                now
            )
                .returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun plusCount(dslContext: DSLContext, id: Long, lastInterceptTime: LocalDateTime) {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            dslContext.update(this)
                .set(COUNT, COUNT + 1)
                .set(LAST_INTERCEPT_TIME, lastInterceptTime)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun count(dslContext: DSLContext, projectId: String): Long {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            return dslContext.select(PIPELINE_ID.countDistinct())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun plusInterceptCount(dslContext: DSLContext, id: Long) {
        with(TCountPipeline.T_COUNT_PIPELINE) {
            dslContext.update(this)
                .set(INTERCEPT_COUNT, INTERCEPT_COUNT + 1)
                .where(ID.eq(id))
                .execute()
        }
    }
}
