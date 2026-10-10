/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.ai.agent.codecc

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
 * CodeCC 代码检查子智能体定义。
 *
 * 此处只注册平台通用工具。CodeCC MCP 需由运营配置绑定到 `codecc_agent`，
 * 再由 SubAgentFactory 按 bindAgent 自动注入。
 */
@Component
class CodeccSubAgentDefinition @Autowired constructor(
    private val client: Client
) : SubAgentDefinition {

    override fun toolName(): String = TOOL_NAME

    override fun description(): String =
        "CodeCC 代码检查智能体，负责任务创建与扫描、告警查询与修复、" +
            "规则查找与加入规则集、扫描报告生成。" +
            "当用户需要创建 CodeCC 任务、触发或查看扫描、查询或修复告警、" +
            "查找规则或将规则加入规则集、生成扫描报告时使用。"

    override fun defaultSysPrompt(): String = codeccOperationGuideMarkdown()

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

        val resolvedPrompt = resolveCodeccPrompt(
            sysPrompt = "$sysPrompt\n\n${codeccMandatorySafetyGuardrails()}",
            userId = userId,
            chatContext = chatContext
        )

        return ReActAgent.builder()
            .name("CodeCC 代码检查助手")
            .sysPrompt(resolvedPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }

    companion object {
        internal const val TOOL_NAME = "codecc_agent"
    }
}

/**
 * 用强类型上下文补齐核心变量，并允许 CodeCC 页面通过 rawPairs 传入 taskId 等扩展变量。
 */
internal fun resolveCodeccPrompt(
    sysPrompt: String,
    userId: String,
    chatContext: ChatContextDTO
): String {
    val rawVariables = chatContext.rawPairs
        .filter { (key, _) -> SAFE_CONTEXT_KEYS.any { it.equals(key.trim(), ignoreCase = true) } }
        .associate { (key, value) -> key.trim() to sanitizeContextValue(value) }
    val variables = linkedMapOf(
        "userId" to userId.contextValue(),
        "projectId" to chatContext.projectId.contextValue(
            rawVariables.valueOf("projectId", "project_id")
        ),
        "pipelineId" to chatContext.pipelineId.contextValue(
            rawVariables.valueOf("pipelineId", "pipeline_id")
        ),
        "buildId" to chatContext.buildId.contextValue(
            rawVariables.valueOf("buildId", "build_id")
        ),
        "taskId" to rawVariables.valueOf(
            "taskId",
            "codeccTaskId",
            "task_id",
            "codecc_task_id"
        ).contextValue()
    )
    variables["context_block"] = buildCodeccContextBlock(variables)
    return PLACEHOLDER_PATTERN.replace(sysPrompt) { match ->
        variables[match.groupValues[1]] ?: match.value
    }
}

private fun String?.contextValue(fallback: String? = null): String {
    return this?.let(::sanitizeContextValue)?.takeIf { it.isNotEmpty() }
        ?: fallback?.let(::sanitizeContextValue)?.takeIf { it.isNotEmpty() }
        ?: UNKNOWN_CONTEXT
}

private fun Map<String, String>.valueOf(vararg aliases: String): String? {
    return entries.lastOrNull { (key, value) ->
        value.isNotBlank() && aliases.any { alias -> key.equals(alias, ignoreCase = true) }
    }?.value
}

private fun buildCodeccContextBlock(variables: Map<String, String>): String = listOf(
    "当前环境信息：",
    "- 当前用户：${variables.getValue("userId")}",
    "- 当前项目：${variables.getValue("projectId")}",
    "- 当前流水线：${variables.getValue("pipelineId")}",
    "- 当前构建：${variables.getValue("buildId")}",
    "- 当前 CodeCC 任务：${variables.getValue("taskId")}"
).joinToString("\n")

private fun sanitizeContextValue(value: String): String =
    value.replace(Regex("[\\r\\n\\u0000-\\u001F]"), " ").trim().take(MAX_CONTEXT_VALUE_LENGTH)

private const val UNKNOWN_CONTEXT = "未知"
private const val MAX_CONTEXT_VALUE_LENGTH = 256
private val PLACEHOLDER_PATTERN = Regex(
    """\{\{(userId|projectId|pipelineId|buildId|taskId|context_block)\}\}"""
)
private val SAFE_CONTEXT_KEYS = setOf(
    "projectId",
    "project_id",
    "pipelineId",
    "pipeline_id",
    "buildId",
    "build_id",
    "taskId",
    "task_id",
    "codeccTaskId",
    "codecc_task_id"
)
