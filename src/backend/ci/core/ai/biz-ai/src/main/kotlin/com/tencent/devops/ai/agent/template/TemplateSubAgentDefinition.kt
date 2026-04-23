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

package com.tencent.devops.ai.agent.template

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
 * 流水线模板管理子智能体定义。
 */
@Component
class TemplateSubAgentDefinition @Autowired constructor(
    private val client: Client
) : SubAgentDefinition {

    override fun toolName(): String = "template_agent"

    override fun description(): String =
        "流水线模板管理智能体，负责模板查询、创建、更新、实例化、实例升级、" +
                "从模板创建独立流水线、删除模板/版本等全生命周期操作。" +
                "当用户询问流水线模板信息、查看模板列表/详情、" +
                "基于已有模板创建新模板、更新模板编排、" +
                "从模板创建独立流水线、从模板批量创建流水线实例、" +
                "升级模板实例到新版本、查看模板实例状态、" +
                "查看流水线编排或启动参数、" +
                "删除模板或模板版本等相关问题时使用。"

    override fun defaultSysPrompt(): String = templateOperationGuideMarkdown()

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
        toolkit.registerTool(TemplateTools(client) { userId })

        val projectId = chatContext.projectId

        val resolvedPrompt = sysPrompt
            .replace("{{userId}}", userId)
            .replace("{{projectId}}", projectId ?: "未知")

        return ReActAgent.builder()
            .name("流水线模板助手")
            .sysPrompt(resolvedPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }
}
