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

import io.agentscope.core.hook.Hook
import io.agentscope.core.hook.HookEvent
import io.agentscope.core.hook.PreActingEvent
import io.agentscope.core.message.ToolUseBlock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * 工具入参清洗 Hook：在工具执行前将 LLM 传入的空白字符串参数替换为 null。
 *
 * **背景：** LLM 对可选参数经常传 `""` 而非不传（null），
 * 后端接口收到空字符串后会尝试解析导致报错（如 `"action() does not exist"`）。
 */
@Component
class ToolInputSanitizeHook : Hook {

    @Suppress("UNCHECKED_CAST")
    override fun <T : HookEvent> onEvent(event: T): Mono<T> {
        if (event is PreActingEvent) {
            sanitize(event)
        }
        return Mono.just(event)
    }

    override fun priority(): Int = PRIORITY

    private fun sanitize(event: PreActingEvent) {
        val original = event.toolUse.input
        if (original.isEmpty()) return

        var changed = false
        val sanitized = original.mapValues { (_, v) ->
            if (v is String && v.isBlank()) {
                changed = true
                null
            } else {
                v
            }
        }

        if (changed) {
            logger.debug(
                "[Sanitize] Blanks → null | tool={} | before={} | after={}",
                event.toolUse.name, original, sanitized
            )
            event.toolUse = ToolUseBlock(
                event.toolUse.id,
                event.toolUse.name,
                sanitized,
                event.toolUse.content,
                event.toolUse.metadata
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ToolInputSanitizeHook::class.java)

        /** 优先级高于 AgentStageTimingHook(50)，确保清洗在计时之前完成 */
        private const val PRIORITY = 10
    }
}
