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

package com.tencent.devops.ai.dao

import com.tencent.devops.model.ai.tables.TAiRunEvent
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

/**
 * AI 运行事件 DAO，对应 T_AI_RUN_EVENT 表。
 *
 * 用于 AG-UI 事件的异步持久化与回放查询。
 * 事件数据为 [AguiEventEncoder] 编码后的 SSE 字符串，用完即删。
 *
 * 注意：使用 INSERT IGNORE 实现幂等写入，
 * UNIQUE(THREAD_ID, RUN_ID, EVENT_INDEX) 保证多实例并发安全。
 */
@Repository
class AiRunEventDao {

    fun create(
        dslContext: DSLContext,
        threadId: String,
        runId: String,
        eventIndex: Int,
        eventData: String
    ) {
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            dslContext.insertInto(
                this,
                THREAD_ID, RUN_ID, EVENT_INDEX, EVENT_DATA
            ).values(
                threadId, runId, eventIndex, eventData
            ).onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        events: List<EventRecord>
    ) {
        if (events.isEmpty()) return
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            var insert = dslContext.insertInto(
                this,
                THREAD_ID, RUN_ID, EVENT_INDEX, EVENT_DATA
            )
            events.forEach { e ->
                insert = insert.values(
                    e.threadId, e.runId, e.eventIndex, e.eventData
                )
            }
            insert.onDuplicateKeyIgnore().execute()
        }
    }

    /**
     * 按事件序号升序查询指定 run 的所有事件数据。
     *
     * @return 按 EVENT_INDEX 排序的 SSE 字符串列表
     */
    fun listByRun(
        dslContext: DSLContext,
        threadId: String,
        runId: String
    ): List<String> {
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            return dslContext.select(EVENT_DATA)
                .from(this)
                .where(THREAD_ID.eq(threadId))
                .and(RUN_ID.eq(runId))
                .orderBy(EVENT_INDEX.asc())
                .fetch(EVENT_DATA)
        }
    }

    /**
     * 查询 EVENT_INDEX 大于 afterIndex 的增量事件。
     *
     * @return Pair<事件数据列表, 本批次最大 eventIndex>，列表为空时 maxIndex 返回 afterIndex
     */
    fun listIncrementalByRun(
        dslContext: DSLContext,
        threadId: String,
        afterIndex: Int
    ): Pair<List<String>, Int> {
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            val records = dslContext.select(EVENT_INDEX, EVENT_DATA)
                .from(this)
                .where(THREAD_ID.eq(threadId))
                .and(EVENT_INDEX.gt(afterIndex))
                .orderBy(EVENT_INDEX.asc())
                .fetch()
            if (records.isEmpty()) {
                return emptyList<String>() to afterIndex
            }
            val events = records.map { it.get(EVENT_DATA) }
            val maxIndex = records.last().get(EVENT_INDEX)
            return events to maxIndex
        }
    }

    /**
     * 检查指定 run 是否有事件记录（用于判断 run 是否存在于 DB）。
     */
    fun existsByRun(
        dslContext: DSLContext,
        threadId: String,
        runId: String? = null
    ): Boolean {
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            val query = dslContext.selectCount()
                .from(this)
                .where(THREAD_ID.eq(threadId))
            if (!runId.isNullOrBlank()) {
                query.and(RUN_ID.eq(runId))
            }
            return (query.fetchOne(0, Int::class.java) ?: 0) > 0
        }
    }

    /**
     * 删除指定 run 的所有事件数据。
     */
    fun deleteByRun(
        dslContext: DSLContext,
        threadId: String,
        runId: String
    ): Int {
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            return dslContext.deleteFrom(this)
                .where(THREAD_ID.eq(threadId))
                .and(RUN_ID.eq(runId))
                .execute()
        }
    }

    /**
     * 清理超过指定小时数的僵尸事件（兜底防护）。
     */
    fun cleanupStaleEvents(
        dslContext: DSLContext,
        hours: Int = STALE_HOURS
    ): Int {
        with(TAiRunEvent.T_AI_RUN_EVENT) {
            return dslContext.deleteFrom(this)
                .where(
                    CREATED_TIME.lt(
                        DSL.currentLocalDateTime().minus(
                            DSL.`val`(hours).mul(DSL.`val`(3600))
                        )
                    )
                )
                .execute()
        }
    }

    data class EventRecord(
        val threadId: String,
        val runId: String,
        val eventIndex: Int,
        val eventData: String
    )

    companion object {
        /** 僵尸事件清理阈值（小时） */
        private const val STALE_HOURS = 2
    }
}
