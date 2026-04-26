package com.tencent.devops.ai.config

import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.agent.SubAgentFactory
import com.tencent.devops.ai.agent.supervisor.SupervisorAgentFactory
import com.tencent.devops.ai.service.AgentSysPromptService
import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.common.client.Client
import io.agentscope.core.ReActAgent
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.model.OpenAIChatModel
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Supplier

/** Supervisor 智能体配置类，创建 SupervisorAgentFactory Bean。 */
@Configuration
class AiSupervisorConfig {

    @Bean
    fun supervisorAgentFactory(
        openAIChatModel: OpenAIChatModel,
        autoContextConfig: AutoContextConfig,
        sessionContext: AgentSessionContext,
        sysPromptService: AgentSysPromptService,
        subAgentFactory: SubAgentFactory,
        subAgentDefinitions: List<SubAgentDefinition>,
        client: Client
    ): Supplier<ReActAgent> {
        val factory = SupervisorAgentFactory(
            model = openAIChatModel,
            autoContextConfig = autoContextConfig,
            sessionContext = sessionContext,
            sysPromptService = sysPromptService,
            subAgentFactory = subAgentFactory,
            subAgents = subAgentDefinitions,
            client = client
        )

        logger.info(
            "[Supervisor] Factory initialized with {} " +
                    "sub-agents",
            subAgentDefinitions.size
        )

        return Supplier { factory.create() }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiSupervisorConfig::class.java
        )
    }
}
