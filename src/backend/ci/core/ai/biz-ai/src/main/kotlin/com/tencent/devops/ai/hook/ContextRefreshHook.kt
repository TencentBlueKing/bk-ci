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
import com.tencent.devops.ai.context.ContextMarker
import io.agentscope.core.hook.Hook
import io.agentscope.core.hook.HookEvent
import io.agentscope.core.hook.PreReasoningEvent
import io.agentscope.core.message.Msg
import io.agentscope.core.message.MsgRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * 在 Supervisor 每次推理前，将 system 消息中的上下文标记区间
 * 替换为最新的上下文
 */
@Component
class ContextRefreshHook @Autowired constructor(
    private val sessionContext: AgentSessionContext
) : Hook {

    @Suppress("UNCHECKED_CAST")
    override fun <T : HookEvent> onEvent(event: T): Mono<T> {
        if (event is PreReasoningEvent && event.agent.name == SUPERVISOR_NAME) {
            refreshContext(event)
        }
        return Mono.just(event)
    }

    override fun priority(): Int = PRIORITY

    private fun refreshContext(event: PreReasoningEvent) {
        val threadId = sessionContext.getThreadIdByAgent(event.agent) ?: return
        val latestContext = sessionContext.getChatContext(threadId) ?: return
        val userId = sessionContext.getUserId(threadId) ?: "unknown"

        // 构建新的 context block（带标记区间）
        val newBlock = buildContextBlock(userId, latestContext.rawPairs)

        // 遍历 inputMessages，找到 role=system 且包含标记区间的消息，用正则替换
        val updated = event.inputMessages.map { msg ->
            if (msg.role == MsgRole.SYSTEM &&
                msg.textContent?.contains(ContextMarker.START) == true
            ) {
                val updatedText = ContextMarker.PATTERN.replace(
                    msg.textContent!!, newBlock
                )
                Msg.builder()
                    .id(msg.id)
                    .name(msg.name)
                    .role(msg.role)
                    .textContent(updatedText)
                    .metadata(msg.metadata)
                    .timestamp(msg.timestamp)
                    .build()
            } else {
                msg
            }
        }
        event.inputMessages = updated

        logger.debug(
            "[ContextRefresh] Replaced context block: threadId={}, userId={}",
            threadId, userId
        )
    }

    private fun buildContextBlock(
        userId: String,
        rawPairs: List<Pair<String, String>>
    ): String {
        val lines = mutableListOf("当前环境信息：")
        lines.add("- 当前用户：$userId")
        rawPairs.forEach { (desc, value) ->
            lines.add("- $desc：$value")
        }
        val content = lines.joinToString("\n")
        return "${ContextMarker.START}\n$content\n${ContextMarker.END}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ContextRefreshHook::class.java)

        /** 在 AutoContextHook(0) 之后、AgentStageTimingHook(50) 之前执行 */
        private const val PRIORITY = 5

        private const val SUPERVISOR_NAME = "BkCI-Supervisor"
    }
}
