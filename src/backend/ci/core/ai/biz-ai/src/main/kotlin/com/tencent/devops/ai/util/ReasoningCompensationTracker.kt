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

package com.tencent.devops.ai.util

import io.agentscope.core.agui.event.AguiEvent
import java.util.UUID

/**
 * 跟踪单次 run 的事件流，检测 reasoning-only 场景并生成补偿事件。
 *
 * 当 AgentScope 1.0.11 的 AguiAgentAdapter 将 final answer 的内容
 * 全部以 ThinkingBlock（REASONING_MESSAGE）发出而缺少 TextBlock（TEXT_MESSAGE）时，
 * 本类从已收集的 reasoning delta 中提取文本，构建补偿的 TEXT_MESSAGE 三件套事件。
 *
 * TODO: 升级 AgentScope 到 1.0.12+ 后可移除此补偿逻辑
 */
class ReasoningCompensationTracker(
    private val threadId: String,
    private val runId: String
) {
    private var hasTextMessage = false
    private var hasReasoningMessage = false

    /**
     * 只保留最后一段 reasoning 的内容。
     * 在多轮 ReAct 中，中间轮的 reasoning 包含工具调用决策，
     * 每次新的 REASONING_MESSAGE_START 到来时清空 buffer，
     * 确保补偿时只使用 final answer 轮的文本。
     */
    private val lastReasoningBuffer = StringBuilder()

    fun track(event: AguiEvent) {
        when (event) {
            is AguiEvent.TextMessageStart,
            is AguiEvent.TextMessageContent -> {
                hasTextMessage = true
            }
            is AguiEvent.ReasoningMessageStart -> {
                hasReasoningMessage = true
                lastReasoningBuffer.clear()
            }
            is AguiEvent.ReasoningMessageContent -> {
                hasReasoningMessage = true
                lastReasoningBuffer.append(event.delta)
            }
            else -> {}
        }
    }

    fun needsCompensation(): Boolean = hasReasoningMessage && !hasTextMessage && lastReasoningBuffer.isNotEmpty()

    fun buildCompensationEvents(): List<AguiEvent> {
        val msgId = UUID.randomUUID().toString()
        val text = lastReasoningBuffer.toString()
        return listOf(
            AguiEvent.TextMessageStart(
                threadId, runId, msgId, "assistant"
            ),
            AguiEvent.TextMessageContent(
                threadId, runId, msgId, text
            ),
            AguiEvent.TextMessageEnd(threadId, runId, msgId)
        )
    }
}
