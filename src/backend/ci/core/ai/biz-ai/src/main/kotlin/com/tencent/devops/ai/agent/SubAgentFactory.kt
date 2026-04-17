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

package com.tencent.devops.ai.agent

import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.service.AgentSysPromptService
import com.tencent.devops.ai.service.AiMcpServerService
import com.tencent.devops.ai.service.AiSkillService
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.hook.PendingToolRecoveryHook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextHook
import io.agentscope.core.model.Model
import io.agentscope.core.skill.SkillBox
import io.agentscope.core.skill.SkillHook
import io.agentscope.core.tool.Toolkit
import io.agentscope.core.tool.ToolkitConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 子智能体工厂，负责创建独立运行的子智能体实例。
 *
 * 封装 MCP 加载、Skill 构建、Hook 组装、系统提示词解析等通用流程，
 * 供 [com.tencent.devops.ai.agent.supervisor.SupervisorAgentFactory]
 * 和服务间调用共同复用。
 */
@Component
class SubAgentFactory @Autowired constructor(
    private val mcpServerService: AiMcpServerService,
    private val skillService: AiSkillService,
    private val sysPromptService: AgentSysPromptService,
    private val autoContextConfig: AutoContextConfig,
    private val hooks: List<Hook>
) {

    /**
     * 创建独立运行的子智能体实例（不作为 Supervisor 的子工具）。
     *
     * @param definition 子智能体定义
     * @param model LLM 模型
     * @param userId 当前操作用户
     * @param chatContext 聊天上下文
     * @param variables 提示词模板变量，为 null 时自动构建基础变量
     */
    fun createAgent(
        definition: SubAgentDefinition,
        model: Model,
        userId: String,
        chatContext: ChatContextDTO = ChatContextDTO(),
        variables: Map<String, String>? = null
    ): ReActAgent {
        val resolvedVars = variables ?: buildDefaultVariables(userId, chatContext)

        val toolkit = loadMcpClients(userId, definition.toolName())
        val skillBox = buildSkillBox(toolkit, userId, definition.toolName())
        val agentHooks = buildHooks(skillBox, includeAutoContext = true)

        val sysPrompt =  sysPromptService.buildSysPrompt(
            agentName = definition.toolName(),
            defaultPrompt = definition.defaultSysPrompt(),
            variables = resolvedVars
        )

        logger.info(
            "[SubAgentFactory] Creating standalone agent: " +
                "name={}, userId={}, skills={}",
            definition.toolName(), userId,
            skillBox.allSkillIds.size
        )

        return definition.createAgent(
            model = model,
            userId = userId,
            toolkit = toolkit,
            hooks = agentHooks,
            sysPrompt = sysPrompt,
            chatContext = chatContext,
            autoContextConfig = autoContextConfig
        )
    }

    fun loadMcpClients(
        userId: String,
        bindAgent: String
    ): Toolkit {
        val toolkit = Toolkit(
            ToolkitConfig.builder().parallel(true).build()
        )
        val configs = mcpServerService.getMergedConfigs(
            userId, bindAgent
        )
        if (configs.isEmpty()) return toolkit
        logger.info(
            "[SubAgentFactory] Loading {} MCP(s) for " +
                "bindAgent={}, userId={}",
            configs.size, bindAgent, userId
        )
        configs.forEach { config ->
            try {
                val client = mcpServerService.createClient(config)
                toolkit.registerMcpClient(client).block()
                logger.info(
                    "[SubAgentFactory] Registered MCP: {} -> {}",
                    config.serverName, bindAgent
                )
            } catch (e: Exception) {
                logger.error(
                    "[SubAgentFactory] Failed to load MCP: " +
                        "name={}, bindAgent={}, error={}",
                    config.serverName, bindAgent, e.message
                )
            }
        }
        return toolkit
    }

    fun buildSkillBox(
        toolkit: Toolkit,
        userId: String,
        bindAgent: String
    ): SkillBox {
        val skillBox = SkillBox(toolkit)
        val skills = skillService.getMergedSkills(userId, bindAgent)
        if (skills.isEmpty()) return skillBox

        logger.info(
            "[SubAgentFactory] Loading {} skill(s) for " +
                "bindAgent={}, userId={}",
            skills.size, bindAgent, userId
        )
        skills.forEach { info ->
            skillBox.registerSkill(skillService.toAgentSkill(info))
        }
        skillBox.registerSkillLoadTool()
        return skillBox
    }

    fun buildHooks(
        skillBox: SkillBox,
        // 打开后，开启上下文压缩
        includeAutoContext: Boolean
    ): List<Hook> {
        val result = hooks.toMutableList()
        result.add(PendingToolRecoveryHook())
        if (includeAutoContext) {
            result.add(AutoContextHook())
        }
        if (skillBox.allSkillIds.isNotEmpty()) {
            result.add(SkillHook(skillBox))
        }
        return result
    }

    fun resolvePrompt(
        agentName: String,
        variables: Map<String, String>
    ): String? {
        return sysPromptService.getPromptTemplate(
            agentName = agentName
        )?.let {
            sysPromptService.resolveTemplate(it, variables)
        }
    }

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
        private val logger = LoggerFactory.getLogger(
            SubAgentFactory::class.java
        )
    }
}
