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

package com.tencent.devops.ai.service

import com.tencent.devops.ai.dao.AiAgentStageDao
import com.tencent.devops.ai.pojo.AiAgentStageMetadata.SessionStatus
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** AI Agent 阶段追踪服务，集中封装阶段记录的创建和结束。 */
@Service
class AiAgentStageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val aiAgentStageDao: AiAgentStageDao
) {

    fun createStage(
        id: String,
        sessionId: String,
        agentName: String,
        stageType: String,
        toolName: String? = null,
        toolCallId: String? = null,
        inputBrief: String? = null
    ): AiAgentStageCreateResult {
        val nextIndex = aiAgentStageDao.getMaxIndex(
            dslContext = dslContext,
            sessionId = sessionId
        ) + 1
        aiAgentStageDao.create(
            dslContext = dslContext,
            id = id,
            sessionId = sessionId,
            agentName = agentName,
            stageIndex = nextIndex,
            stageType = stageType,
            toolName = toolName,
            toolCallId = toolCallId,
            inputBrief = inputBrief
        )
        return AiAgentStageCreateResult(
            id = id,
            stageIndex = nextIndex
        )
    }

    fun finishStage(
        id: String,
        durationMs: Long,
        sessionStatus: String = SessionStatus.SUCCESS.value,
        outputBrief: String? = null
    ) {
        aiAgentStageDao.finish(
            dslContext = dslContext,
            id = id,
            durationMs = durationMs,
            sessionStatus = sessionStatus,
            outputBrief = outputBrief
        )
    }

    fun recordCompletedStage(
        id: String,
        sessionId: String,
        agentName: String,
        stageType: String,
        toolName: String? = null,
        toolCallId: String? = null,
        inputBrief: String? = null,
        durationMs: Long,
        sessionStatus: String = SessionStatus.SUCCESS.value,
        outputBrief: String? = null
    ): AiAgentStageCreateResult {
        val result = createStage(
            id = id,
            sessionId = sessionId,
            agentName = agentName,
            stageType = stageType,
            toolName = toolName,
            toolCallId = toolCallId,
            inputBrief = inputBrief
        )
        finishStage(
            id = id,
            durationMs = durationMs,
            sessionStatus = sessionStatus,
            outputBrief = outputBrief
        )
        return result
    }

    fun timeoutBySession(sessionId: String): Int {
        return aiAgentStageDao.timeoutBySession(
            dslContext = dslContext,
            sessionId = sessionId
        )
    }

    fun cancelBySession(sessionId: String): Int {
        return aiAgentStageDao.cancelBySession(
            dslContext = dslContext,
            sessionId = sessionId
        )
    }
}

data class AiAgentStageCreateResult(
    val id: String,
    val stageIndex: Int
)
