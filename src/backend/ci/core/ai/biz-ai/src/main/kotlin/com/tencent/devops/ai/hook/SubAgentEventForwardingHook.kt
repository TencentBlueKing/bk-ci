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

package com.tencent.devops.ai.hook

import com.tencent.devops.ai.context.AgentSessionContext
import io.agentscope.core.agent.Event
import io.agentscope.core.agent.EventType
import io.agentscope.core.agui.event.AguiEvent
import io.agentscope.core.hook.ActingChunkEvent
import io.agentscope.core.hook.Hook
import io.agentscope.core.hook.HookEvent
import io.agentscope.core.message.TextBlock
import io.agentscope.core.message.ToolUseBlock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * 将子智能体的执行事件实时转发为 AG-UI CUSTOM 事件，
 * 使前端能看到子智能体的思考和工具调用过程。
 *
 * 工作原理：
 * 1. SubAgentConfig.forwardEvents(true) 使子智能体的事件
 *    通过 ToolEmitter 触发 ActingChunkEvent
 * 2. 本 Hook 拦截 ActingChunkEvent，提取子智能体事件数据
 * 3. 转换为 AguiEvent.Custom 并推送到 per-request Sink
 * 4. AiChatService 将 Sink 的 Flux 与主事件流合并
 */
@Component
class SubAgentEventForwardingHook @Autowired constructor(
    private val sessionContext: AgentSessionContext
) : Hook {

    @Suppress("UNCHECKED_CAST")
    override fun <T : HookEvent> onEvent(event: T): Mono<T> {
        if (event !is ActingChunkEvent) {
            return Mono.just(event)
        }
        val metadata = event.chunk.metadata ?: return Mono.just(event)
        if (!metadata.containsKey(META_SUBAGENT_EVENT)) {
            return Mono.just(event)
        }

        val sinkInfo = sessionContext.getSinkByAgent(event.agent) ?: return Mono.just(event)

        val subEvent = metadata[META_SUBAGENT_EVENT] as? Event ?: return Mono.just(event)
        val agentName = metadata[META_SUBAGENT_NAME] as? String ?: "SubAgent"

        val eventData = buildEventData(subEvent, agentName) ?: return Mono.just(event)

        val customEvent = AguiEvent.Custom(
            sinkInfo.threadId,
            sinkInfo.runId,
            CUSTOM_EVENT_NAME,
            eventData
        )
        sinkInfo.sink.tryEmitNext(customEvent)

        return Mono.just(event)
    }

    override fun priority(): Int = PRIORITY

    /**
     * 根据子智能体事件类型构建事件数据，返回 null 表示跳过该事件。
     *
     * @param subEvent 子智能体原始事件
     * @param agentName 子智能体名称，用于前端展示
     */
    private fun buildEventData(
        subEvent: Event,
        agentName: String
    ): Map<String, Any?>? {
        val msg = subEvent.message ?: return null

        when (subEvent.type) {
            EventType.REASONING -> {
                val textParts = msg.getContentBlocks(
                    TextBlock::class.java
                )?.mapNotNull { it.text }
                val content = textParts
                    ?.joinToString("")
                    ?.takeIf { it.isNotEmpty() }
                val toolUses = msg.getContentBlocks(
                    ToolUseBlock::class.java
                )?.map {
                    mapOf(
                        "toolName" to it.name,
                        "toolCallId" to it.id
                    )
                }?.takeIf { it.isNotEmpty() }
                if (content == null && toolUses == null) {
                    return null
                }
                val data = mutableMapOf<String, Any?>(
                    "agentName" to agentName,
                    "eventType" to subEvent.type.name,
                    "isLast" to subEvent.isLast
                )
                content?.let { data["content"] = it }
                toolUses?.let { data["toolCalls"] = it }
                return data
            }

            EventType.TOOL_RESULT -> {
                val text = msg.textContent
                if (text.isNullOrBlank()) return null
                return mapOf(
                    "agentName" to agentName,
                    "eventType" to subEvent.type.name,
                    "isLast" to subEvent.isLast,
                    "content" to text.take(MAX_CONTENT)
                )
            }

            else -> {
                val text = msg.textContent
                // 最终回复（isLast=true）只保留结束信号，不携带 content，
                // 主智能体会负责转述，避免前端重复展示
                if (subEvent.isLast) {
                    return mapOf(
                        "agentName" to agentName,
                        "eventType" to subEvent.type.name,
                        "isLast" to true
                    )
                }
                if (text.isNullOrBlank()) return null
                return mapOf(
                    "agentName" to agentName,
                    "eventType" to subEvent.type.name,
                    "isLast" to subEvent.isLast,
                    "content" to text.take(MAX_CONTENT)
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            SubAgentEventForwardingHook::class.java
        )
        private const val PRIORITY = 50
        private const val META_SUBAGENT_EVENT = "subagent_event"
        private const val META_SUBAGENT_NAME = "subagent_name"
        private const val CUSTOM_EVENT_NAME = "subagent_event"
        private const val MAX_CONTENT = 2000
    }
}
