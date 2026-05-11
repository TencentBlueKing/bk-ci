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

import com.tencent.devops.ai.pojo.AiAgentStageMetadata.SessionStatus
import com.tencent.devops.model.ai.tables.TAiAgentStage
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 智能体执行阶段 DAO，对应 T_AI_AGENT_STAGE 表，
 * 记录推理、工具调用、整轮调用、上下文压缩与错误等执行追踪。
 */
@Repository
class AiAgentStageDao {

    fun create(
        dslContext: DSLContext,
        id: String,
        sessionId: String,
        agentName: String,
        stageIndex: Int,
        stageType: String,
        toolName: String? = null,
        toolCallId: String? = null,
        inputBrief: String? = null
    ) {
        val now = LocalDateTime.now()
        with(TAiAgentStage.T_AI_AGENT_STAGE) {
            dslContext.insertInto(
                this,
                ID, SESSION_ID, AGENT_NAME,
                STAGE_INDEX, STAGE_TYPE,
                TOOL_NAME, TOOL_CALL_ID,
                STATUS, DURATION_MS, INPUT_BRIEF,
                CREATED_TIME, UPDATED_TIME
            ).values(
                id, sessionId, agentName,
                stageIndex, stageType,
                toolName, toolCallId,
                SessionStatus.RUNNING.value, -1L,
                inputBrief?.take(MAX_BRIEF),
                now, now
            ).execute()
        }
    }

    fun getMaxIndex(
        dslContext: DSLContext,
        sessionId: String
    ): Int {
        with(TAiAgentStage.T_AI_AGENT_STAGE) {
            return dslContext.select(
                org.jooq.impl.DSL.max(STAGE_INDEX)
            ).from(this)
                .where(SESSION_ID.eq(sessionId))
                .fetchOne(0, Int::class.java) ?: -1
        }
    }

    fun finish(
        dslContext: DSLContext,
        id: String,
        durationMs: Long,
        sessionStatus: String = SessionStatus.SUCCESS.value,
        outputBrief: String? = null
    ) {
        with(TAiAgentStage.T_AI_AGENT_STAGE) {
            dslContext.update(this)
                .set(STATUS, sessionStatus)
                .set(DURATION_MS, durationMs)
                .set(
                    OUTPUT_BRIEF,
                    outputBrief?.take(MAX_BRIEF)
                )
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    /**
     * 将超过指定时间仍处于 RUNNING 的记录批量标记为 TIMEOUT。
     * @return 受影响行数
     */
    fun timeoutStaleRecords(
        dslContext: DSLContext,
        staleMinutes: Long
    ): Int {
        val now = LocalDateTime.now()
        val cutoff = now.minusMinutes(staleMinutes)
        with(TAiAgentStage.T_AI_AGENT_STAGE) {
            return dslContext.update(this)
                .set(STATUS, SessionStatus.TIMEOUT.value)
                .set(DURATION_MS, durationSinceCreated())
                .set(UPDATED_TIME, now)
                .where(STATUS.eq(SessionStatus.RUNNING.value))
                .and(CREATED_TIME.lt(cutoff))
                .execute()
        }
    }

    /**
     * 用户主动停止时，将指定 session 所有 RUNNING 记录标记为 CANCELLED，
     * 并根据 CREATED_TIME 计算已运行时长写入 DURATION_MS。
     * @return 受影响行数
     */
    fun cancelBySession(
        dslContext: DSLContext,
        sessionId: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAiAgentStage.T_AI_AGENT_STAGE) {
            return dslContext.update(this)
                .set(STATUS, SessionStatus.CANCELLED.value)
                .set(DURATION_MS, durationSinceCreated())
                .set(UPDATED_TIME, now)
                .where(SESSION_ID.eq(sessionId))
                .and(STATUS.eq(SessionStatus.RUNNING.value))
                .execute()
        }
    }

    /**
     * LLM 超时时，将指定 session 所有 RUNNING 记录标记为 TIMEOUT。
     * @return 受影响行数
     */
    fun timeoutBySession(
        dslContext: DSLContext,
        sessionId: String
    ): Int {
        val now = LocalDateTime.now()
        with(TAiAgentStage.T_AI_AGENT_STAGE) {
            return dslContext.update(this)
                .set(STATUS, SessionStatus.TIMEOUT.value)
                .set(DURATION_MS, durationSinceCreated())
                .set(UPDATED_TIME, now)
                .where(SESSION_ID.eq(sessionId))
                .and(STATUS.eq(SessionStatus.RUNNING.value))
                .execute()
        }
    }

    private fun durationSinceCreated() = DSL.field(
        "TIMESTAMPDIFF(SECOND, {0}, NOW()) * 1000",
        Long::class.java,
        TAiAgentStage.T_AI_AGENT_STAGE.CREATED_TIME
    )

    companion object {
        private const val MAX_BRIEF = 1024
    }
}
