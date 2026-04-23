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

package com.tencent.devops.ai.agent.textpolish

import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.pojo.ChatContextDTO
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.springframework.stereotype.Component

/**
 * 文本润色子智能体，对输入文本进行润色改写。
 *
 * 典型场景：流水线描述润色、文档内容改写、通知文案优化等。
 */
@Component
class TextPolishSubAgentDefinition : SubAgentDefinition {

    override fun toolName(): String = "text_polish"

    override fun description(): String =
        "文本润色智能体，对输入文本进行润色改写，" +
            "使表达更流畅、专业、易读"

    override fun bindToSupervisor(): Boolean = false

    override fun defaultSysPrompt(): String = DEFAULT_SYS_PROMPT

    override fun createAgent(
        model: Model,
        userId: String,
        toolkit: Toolkit,
        hooks: List<Hook>,
        sysPrompt: String,
        chatContext: ChatContextDTO,
        autoContextConfig: AutoContextConfig
    ): ReActAgent {
        return ReActAgent.builder()
            .name("TextPolish")
            .sysPrompt(sysPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .maxIters(1)
            .build()
    }

    companion object {
        @Suppress("MaxLineLength")
        private val DEFAULT_SYS_PROMPT = """
            |你是一位专业的文本润色助手。
            |你的任务是对用户提供的文本进行润色改写，使其表达更流畅、专业、易读。
            |
            |## 润色原则
            |
            |1. **保留原意**：不改变原文的核心含义和关键信息
            |2. **提升表达**：优化用词、句式，使语言更自然流畅
            |3. **修正错误**：纠正错别字、语法错误、标点符号问题
            |4. **统一风格**：保持全文语气和风格的一致性
            |5. **精简冗余**：删除多余的词句，避免啰嗦
            |6. **增强可读性**：适当调整段落结构，使逻辑更清晰
            |
            |## 输出要求
            |
            |- 直接输出润色后的文本，不要加额外的解释说明
            |- 使用与原文相同的语言
            |- 如果原文已经很好，可以只做微调
        """.trimMargin()
    }
}
