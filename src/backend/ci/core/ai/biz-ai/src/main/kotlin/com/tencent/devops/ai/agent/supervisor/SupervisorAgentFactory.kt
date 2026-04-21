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

package com.tencent.devops.ai.agent.supervisor

import com.tencent.devops.ai.agent.CommonTools
import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.agent.SubAgentFactory
import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.ai.context.AiChatContext
import com.tencent.devops.ai.context.ContextMarker
import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.service.AgentSysPromptService
import com.tencent.devops.common.client.Client
import io.agentscope.core.ReActAgent
import io.agentscope.core.agent.EventType
import io.agentscope.core.agent.StreamOptions
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.OpenAIChatModel
import io.agentscope.core.tool.Toolkit
import io.agentscope.core.tool.subagent.SubAgentConfig
import org.slf4j.LoggerFactory

/**
 * Supervisor 智能体工厂，负责创建主控智能体实例。
 *
 * 组装流程：注册所有子智能体为工具、加载 MCP 服务端、
 * 挂载 Skill 与 Hook、解析系统提示词模板。
 */
class SupervisorAgentFactory(
    private val model: OpenAIChatModel,
    private val autoContextConfig: AutoContextConfig,
    private val sessionContext: AgentSessionContext,
    private val sysPromptService: AgentSysPromptService,
    private val subAgentFactory: SubAgentFactory,
    private val subAgents: List<SubAgentDefinition>,
    private val client: Client
) {

    /**
     * 创建完整配置的 Supervisor 智能体。
     *
     * 按顺序执行：解析用户身份 → 构建模板变量 →
     * 组装工具集（含子智能体和 MCP）→ 加载 Skill →
     * 拼装系统提示词 → 构建 [ReActAgent] 实例。
     */
    fun create(): ReActAgent {
        val userId = resolveUserId()
        val chatContext = AiChatContext.getContext()
        val boundAgents = subAgents.filter { it.bindToSupervisor() }
        val variables = buildVariables(userId, chatContext, boundAgents)
        val sysPrompt = sysPromptService.buildSysPrompt(
            agentName = SUPERVISOR_BIND_KEY,
            defaultPrompt = DEFAULT_SUPERVISOR_PROMPT,
            variables = variables
        )
        logger.info(
            "[Supervisor] Creating agent: thread={}, " +
                    "userId={}, context={}, " +
                    "boundAgents={}/{}",
            Thread.currentThread().name, userId, chatContext,
            boundAgents.size, subAgents.size
        )
        val toolkit = buildToolkit(
            userId = userId,
            boundAgents = boundAgents
        )
        val skillBox = subAgentFactory.buildSkillBox(
            userId = userId,
            bindAgent = SUPERVISOR_BIND_KEY,
            toolkit = toolkit
        )
        val allHooks = subAgentFactory.buildHooks(skillBox, includeAutoContext = true)

        logger.info(
            "[Supervisor] Agent created: name={}, " +
                    "boundSubAgents={}, hooks={}, skills={}",
            SUPERVISOR_NAME, boundAgents.size,
            allHooks.size, skillBox.allSkillIds.size
        )

        return ReActAgent.builder()
            .name(SUPERVISOR_NAME)
            .sysPrompt(sysPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .maxIters(MAX_ITERS)
            .hooks(allHooks)
            .build()
    }

    private fun resolveUserId(): String {
        val threadId = AiChatContext.getThreadId() ?: return "unknown"
        return sessionContext.getUserId(threadId) ?: "unknown"
    }

    /**
     * 组装工具集：加载 Supervisor 级 MCP 工具，
     * 并将所有 [SubAgentDefinition] 注册为子智能体工具。
     *
     * supplier lambda 会被 SubAgentTool 在构造期和每次调用时重复执行，
     * 因此通过闭包捕获 threadId 和 sessionContext 来保证异步线程上
     * 也能正确注册 agent → threadId 映射。
     */
    private fun buildToolkit(
        userId: String,
        boundAgents: List<SubAgentDefinition>
    ): Toolkit {
        val toolkit = subAgentFactory.loadMcpClients(
            userId, SUPERVISOR_BIND_KEY
        )
        toolkit.registerTool(CommonTools(client) { userId })

        val capturedThreadId = AiChatContext.getThreadId()
        boundAgents.forEach { definition ->
            val subAgentConfig = SubAgentConfig.builder()
                .toolName(definition.toolName())
                .description(definition.description())
                .forwardEvents(true)
                .streamOptions(
                    StreamOptions.builder()
                        .eventTypes(EventType.ALL)
                        .includeReasoningChunk(true)
                        .includeReasoningResult(false)
                        .includeActingChunk(true)
                        .build()
                )
                .build()
            toolkit.registration().subAgent(
                {
                    // 实时获取最新上下文，降级用闭包捕获的旧值
                    val latestContext = capturedThreadId?.let {
                        sessionContext.getChatContext(it)
                    } ?: ChatContextDTO()
                    val latestUserId = capturedThreadId?.let {
                        sessionContext.getUserId(it)
                    } ?: userId

                    // 用最新上下文重新构建 variables
                    val latestVars = buildDefaultVariables(
                        latestUserId, latestContext
                    )

                    val subAgent = subAgentFactory.createAgent(
                        definition = definition,
                        model = model,
                        userId = latestUserId,
                        chatContext = latestContext,
                        variables = latestVars
                    )
                    capturedThreadId?.let { threadId ->
                        sessionContext.registerAgentThread(
                            subAgent, threadId
                        )
                    }
                    subAgent
                },
                subAgentConfig
            ).apply()
        }
        return toolkit
    }

    /**
     * 构建提示词模板变量，合并内置变量与前端上下文。
     */
    private fun buildVariables(
        userId: String,
        context: ChatContextDTO,
        boundAgents: List<SubAgentDefinition>
    ): Map<String, String> {
        val vars = mutableMapOf<String, String>()
        vars["user_id"] = userId
        vars["agent_list"] = if (boundAgents.isEmpty()) {
            "当前没有注册专家智能体，请直接回答用户问题。"
        } else {
            boundAgents.joinToString("\n") { agent ->
                "- ${agent.toolName()}: ${agent.description()}"
            }
        }
        vars["context_block"] = buildContextBlock(
            userId, context.rawPairs
        )
        context.rawPairs.forEach { (key, value) ->
            vars[key] = value
        }
        return vars
    }

    private fun buildContextBlock(
        userId: String,
        context: List<Pair<String, String>>
    ): String {
        val lines = mutableListOf("当前环境信息：")
        lines.add("- 当前用户：$userId")
        context.forEach { (desc, value) ->
            lines.add("- $desc：$value")
        }
        val content = lines.joinToString("\n")
        return "${ContextMarker.START}\n$content\n${ContextMarker.END}"
    }

    /**
     * 为子智能体 Supplier lambda 构建基础变量（不含 agent_list / context_block）。
     */
    private fun buildDefaultVariables(
        userId: String,
        context: ChatContextDTO
    ): Map<String, String> {
        val vars = mutableMapOf<String, String>()
        vars["user_id"] = userId
        context.rawPairs.forEach { (key, value) ->
            vars[key] = value
        }
        return vars
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SupervisorAgentFactory::class.java)
        private const val SUPERVISOR_NAME = "BkCI-Supervisor"
        private const val SUPERVISOR_BIND_KEY = "supervisor"
        private const val MAX_ITERS = 10

        @Suppress("MaxLineLength")
        private val DEFAULT_SUPERVISOR_PROMPT = """
        |你是蓝盾 DevOps 平台的 AI 助手。
        |
        |{{context_block}}
        |
        |⚠️ **关键规则1: 蓝盾智能助手仅回答持续集成领域和蓝盾产品相关的问题 **
        |⚠️ **关键规则2：上下文优先**
        |- `<!-- CONTEXT_START --><!-- CONTEXT_END -->`中包含用户当前所在的项目、流水线等实时环境信息。
        |- **每次回答前，必须先读取本轮上下文中的项目 ID、项目名称等字段，以此为准。**
        |- 禁止沿用历史对话中的项目信息。如果上下文中的项目与历史对话不一致，以上下文为准，不需要向用户确认。
        |- 如果上下文为空或缺少关键字段，主动询问用户当前所在项目。
        |
        |你拥有两类工具：
        |
        |一、iWiki 文档搜索（直接调用，不经过子智能体）
        |这些工具可搜索蓝盾官方文档（iWiki DevOps 空间）。
        |使用步骤：
        |1. 调用 getSpaceInfoByKey(space_key="DevOps") 获取数字 space_id
        |2. 用 aiSearchDocument(space_id=<数字ID>, query="问题关键词") 语义搜索
        |3. 若aiSearchDocument接口找不到数据，可以结合searchDocument接口来查询
        |4. 如需文档详情，用 getDocument 获取全文
        |注意：space_id 必须是数字，不能传字符串 "DevOps"。
        |
        |二、专家子智能体（工具名以 call_ 开头）
        |{{agent_list}}
        |
        |决策原则：
        |1. 收到问题后，优先用 iWiki 搜索相关文档
        |2. 搜到有用内容则直接回答，注明文档来源
        |3. 搜不到或需要执行操作（如加权限、触发构建）时，转给对应的子智能体处理
        |4. 通用常识问题无需搜索，直接回答
        |5. 始终用中文回复
        """.trimMargin()
    }
}
