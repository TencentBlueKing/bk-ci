package com.tencent.devops.ai.agent.external

import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.agent.external.caller.ExternalAgentCaller
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import com.tencent.devops.ai.service.ExternalAgentService
import io.agentscope.core.ReActAgent
import io.agentscope.core.hook.Hook
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.model.Model
import io.agentscope.core.tool.Toolkit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 外部智能体子智能体定义，动态注册用户配置的第三方智能体。
 *
 * 创建时从 [ExternalAgentService] 加载用户已启用的外部智能体配置，
 * 为每个配置创建 [ExternalAgentTool] 并注册到工具集中，
 * 支持 Knot、蓝鲸智能体等多个平台。
 */
@Component
class ExternalAgentSubAgentDefinition @Autowired constructor(
    private val externalAgentService: ExternalAgentService,
    private val callers: List<ExternalAgentCaller>
) : SubAgentDefinition {

    override fun toolName(): String = "external_agent"

    override fun description(): String =
        "调用用户自定义的外部智能体（支持 Knot、蓝鲸智能体等多个平台），" +
                "用于将问题委派给用户在外部平台创建的专业智能体处理"

    override fun defaultSysPrompt(): String = DEFAULT_SYS_PROMPT

    override fun bindToSupervisor(): Boolean = false

    override fun createAgent(
        model: Model,
        userId: String,
        toolkit: Toolkit,
        hooks: List<Hook>,
        sysPrompt: String,
        chatContext: ChatContextDTO,
        autoContextConfig: AutoContextConfig
    ): ReActAgent {
        val callerMap = callers.associateBy { it.platform() }

        val configs = externalAgentService.listEnabled(userId)
        logger.info(
            "[ExternalAgent] Creating with {} agents " +
                    "for userId={}",
            configs.size, userId
        )

        configs.forEach { config ->
            val caller = callerMap[config.platform]
            if (caller == null) {
                logger.warn(
                    "[ExternalAgent] No caller for platform: " +
                            "{}, skipping agent: {}",
                    config.platform, config.agentName
                )
                return@forEach
            }
            val tool = ExternalAgentTool(config, caller)
            toolkit.registerTool(tool)
            logger.info(
                "[ExternalAgent] Registered tool: {} " +
                        "(platform={})",
                config.agentName, config.platform
            )
        }

        val prompt = resolvePrompt(sysPrompt, configs)

        return ReActAgent.builder()
            .name("ExternalAgent")
            .sysPrompt(prompt)
            .model(model)
            .toolkit(toolkit)
            .memory(AutoContextMemory(autoContextConfig, model))
            .hooks(hooks)
            .build()
    }

    private fun resolvePrompt(
        prompt: String,
        configs: List<ExternalAgentInfo>
    ): String {
        val agentList = if (configs.isEmpty()) {
            "当前没有注册外部智能体。如果用户询问的问题需要外部智能体处理，" +
                    "请告知用户需要先在设置中注册外部智能体。"
        } else {
            configs.joinToString("\n") { config ->
                "- ${config.agentName} [${config.platform}]: " +
                        config.description
            }
        }

        return prompt.replace("{{external_agent_list}}", agentList)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            ExternalAgentSubAgentDefinition::class.java
        )
        private const val DEFAULT_SYS_PROMPT =
            "你是一个外部智能体调度器。" +
                    "你可以调用用户注册的外部智能体来处理专业领域问题。\n\n" +
                    "可用的外部智能体：\n{{external_agent_list}}\n\n" +
                    "工作原则：\n" +
                    "1. 根据用户问题的领域选择最合适的外部智能体\n" +
                    "2. 将用户的问题清晰地传达给外部智能体\n" +
                    "3. 如果有多个相关的智能体，选择最匹配的那个\n" +
                    "4. 将外部智能体的回答整理后返回给用户\n" +
                    "5. 如果外部智能体无法回答，如实告知"
    }
}
