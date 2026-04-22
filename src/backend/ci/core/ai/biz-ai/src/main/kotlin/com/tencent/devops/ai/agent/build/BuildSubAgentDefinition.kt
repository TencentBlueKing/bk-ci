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

package com.tencent.devops.ai.agent.build

import com.tencent.devops.ai.agent.CommonTools
import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.common.client.Client
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 流水线构建子智能体定义。
 */
@Component
class BuildSubAgentDefinition @Autowired constructor(
    private val client: Client
) : SubAgentDefinition {

    override fun toolName(): String = "build_agent"

    override fun description(): String =
        "流水线构建智能体，负责流水线查询、构建触发与管理、构建日志分析。" +
                "当用户询问流水线信息、触发/停止/重试构建、" +
                "查看构建历史/详情/状态/变量、" +
                "分析构建错误日志等相关问题时使用。"

    override fun defaultSysPrompt(): String = buildOperationGuideMarkdown()

    override fun createAgent(
        model: Model,
        userId: String,
        toolkit: Toolkit,
        hooks: List<Hook>,
        sysPrompt: String,
        chatContext: ChatContextDTO,
        autoContextConfig: AutoContextConfig
    ): ReActAgent {
        toolkit.registerTool(CommonTools(client) { userId })
        toolkit.registerTool(BuildTools(client) { userId })

        val projectId = chatContext.projectId
        val pipelineId = chatContext.pipelineId
        val buildId = chatContext.buildId

        val resolvedPrompt = sysPrompt
            .replace("{{userId}}", userId)
            .replace("{{projectId}}", projectId ?: "未知")
            .replace("{{pipelineId}}", pipelineId ?: "未知")
            .replace("{{buildId}}", buildId ?: "未知")

        return ReActAgent.builder()
            .name("流水线构建助手")
            .sysPrompt(resolvedPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }
}
