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

package com.tencent.devops.ai.agent.knowledge

import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.agent.SubAgentDefinition
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.springframework.stereotype.Component

/**
 * 知识库子智能体定义，提供知识检索和问答能力。
 *
 * 通过 Supervisor 工厂自动加载绑定的 MCP 知识平台工具
 * （如 iWiki、Knot 等），无需在此处手动注册工具。
 */
@Component
class KnowledgeSubAgentDefinition : SubAgentDefinition {

    override fun toolName(): String = "knowledge_agent"

    override fun description(): String =
        "知识库检索智能体，从 iWiki、Knot 等平台的 MCP 服务搜索文档和知识库内容，" +
                "用于回答与蓝盾 DevOps、内部文档、技术方案等相关的问题"

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
            .name("KnowledgeAgent")
            .sysPrompt(sysPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }

    companion object {
        private const val DEFAULT_SYS_PROMPT =
            "你是一个知识库检索智能体，" +
                "负责从用户配置的 MCP 知识平台（如 Knot 等）中搜索文档。\n\n" +
                "工作原则：\n" +
                "1. 收到问题后，使用可用的搜索工具查找相关文档\n" +
                "2. 优先使用语义搜索类工具获取精准结果\n" +
                "3. 如果需要更多细节，可以获取文档全文\n" +
                "4. 根据每个工具的参数说明正确传参\n" +
                "5. 回答时注明信息来源（文档标题或链接）\n" +
                "6. 如果没有找到相关文档，如实告知"
    }
}
