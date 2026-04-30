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

import com.tencent.devops.model.ai.tables.TAiAgentState
import com.tencent.devops.model.ai.tables.records.TAiAgentStateRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 智能体状态 DAO，对应 T_AI_AGENT_STATE 表，
 * 持久化智能体的对话记忆。
 */
@Repository
class AiAgentStateDao {

    fun upsert(
        dslContext: DSLContext,
        sessionId: String,
        stateKey: String,
        itemIndex: Int,
        stateData: String
    ) {
        val now = LocalDateTime.now()
        dslContext.transaction { config ->
            val ctx = DSL.using(config)
            with(TAiAgentState.T_AI_AGENT_STATE) {
                ctx.deleteFrom(this)
                    .where(SESSION_ID.eq(sessionId))
                    .and(STATE_KEY.eq(stateKey))
                    .and(ITEM_INDEX.eq(itemIndex))
                    .execute()

                ctx.insertInto(
                    this,
                    SESSION_ID, STATE_KEY, ITEM_INDEX,
                    STATE_DATA, CREATED_TIME, UPDATED_TIME
                ).values(
                    sessionId, stateKey, itemIndex,
                    stateData, now, now
                ).execute()
            }
        }
    }

    fun deleteBySessionAndKey(
        dslContext: DSLContext,
        sessionId: String,
        stateKey: String
    ): Int {
        with(TAiAgentState.T_AI_AGENT_STATE) {
            return dslContext.deleteFrom(this)
                .where(SESSION_ID.eq(sessionId))
                .and(STATE_KEY.eq(stateKey))
                .execute()
        }
    }

    fun batchInsert(
        dslContext: DSLContext,
        sessionId: String,
        stateKey: String,
        stateDataList: List<String>
    ) {
        if (stateDataList.isEmpty()) return
        val now = LocalDateTime.now()
        with(TAiAgentState.T_AI_AGENT_STATE) {
            var insert = dslContext.insertInto(
                this,
                SESSION_ID, STATE_KEY, ITEM_INDEX,
                STATE_DATA, CREATED_TIME, UPDATED_TIME
            )
            stateDataList.forEachIndexed { index, data ->
                insert = insert.values(
                    sessionId, stateKey, index,
                    data, now, now
                )
            }
            insert.execute()
        }
    }

    fun getBySessionKeyAndIndex(
        dslContext: DSLContext,
        sessionId: String,
        stateKey: String,
        itemIndex: Int
    ): TAiAgentStateRecord? {
        with(TAiAgentState.T_AI_AGENT_STATE) {
            return dslContext.selectFrom(this)
                .where(SESSION_ID.eq(sessionId))
                .and(STATE_KEY.eq(stateKey))
                .and(ITEM_INDEX.eq(itemIndex))
                .orderBy(CREATED_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun listBySessionAndKey(
        dslContext: DSLContext,
        sessionId: String,
        stateKey: String
    ): Result<TAiAgentStateRecord> {
        with(TAiAgentState.T_AI_AGENT_STATE) {
            return dslContext.selectFrom(this)
                .where(SESSION_ID.eq(sessionId))
                .and(STATE_KEY.eq(stateKey))
                .orderBy(ITEM_INDEX.asc())
                .fetch()
        }
    }

    fun countBySession(
        dslContext: DSLContext,
        sessionId: String
    ): Int {
        with(TAiAgentState.T_AI_AGENT_STATE) {
            return dslContext.selectCount()
                .from(this)
                .where(SESSION_ID.eq(sessionId))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun deleteBySession(
        dslContext: DSLContext,
        sessionId: String
    ): Int {
        with(TAiAgentState.T_AI_AGENT_STATE) {
            return dslContext.deleteFrom(this)
                .where(SESSION_ID.eq(sessionId))
                .execute()
        }
    }

    fun listDistinctSessionIds(
        dslContext: DSLContext
    ): List<String> {
        with(TAiAgentState.T_AI_AGENT_STATE) {
            return dslContext.selectDistinct(SESSION_ID)
                .from(this)
                .fetch(SESSION_ID)
        }
    }
}
