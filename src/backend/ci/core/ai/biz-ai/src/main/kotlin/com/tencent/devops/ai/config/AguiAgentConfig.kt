package com.tencent.devops.ai.config

import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.agent.SubAgentFactory
import com.tencent.devops.ai.context.AiChatContext
import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.ai.session.PersistentAgentResolver
import io.agentscope.core.ReActAgent
import io.agentscope.core.agui.adapter.AguiAdapterConfig
import io.agentscope.core.agui.processor.AguiRequestProcessor
import io.agentscope.core.agui.registry.AguiAgentRegistry
import io.agentscope.core.model.OpenAIChatModel
import io.agentscope.core.session.Session
import io.agentscope.spring.boot.agui.common.AguiAgentRegistryCustomizer
import io.agentscope.spring.boot.agui.common.AguiProperties
import io.agentscope.spring.boot.agui.common.ThreadSessionManager
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Supplier

/**
 * AG-UI 智能体配置类，
 * 注册 AguiRequestProcessor 和 PersistentAgentResolver。
 *
 * 除了注册 Supervisor（"default"）外，还为每个
 * [SubAgentDefinition] 注册独立的 agentId，
 * 使其可被服务间调用直接使用。
 */
@Configuration
class AguiAgentConfig {

    @Bean
    fun aguiAgentRegistryCustomizer(
        supervisorAgentFactory: Supplier<ReActAgent>,
        subAgentDefinitions: List<SubAgentDefinition>,
        subAgentFactory: SubAgentFactory,
        openAIChatModel: OpenAIChatModel,
        sessionContext: AgentSessionContext
    ): AguiAgentRegistryCustomizer {
        logger.info(
            "[AguiConfig] Registering Supervisor factory " +
                    "with agentId='{}'",
            AGENT_ID
        )
        return AguiAgentRegistryCustomizer { registry ->
            registry.registerFactory(AGENT_ID) {
                logger.info(
                    "[AguiConfig] Factory invoked, " +
                            "creating agent instance, thread={}",
                    Thread.currentThread().name
                )
                supervisorAgentFactory.get()
            }

            subAgentDefinitions.forEach { definition ->
                val agentId = definition.toolName()
                require(agentId != AGENT_ID) {
                    "SubAgentDefinition toolName '$agentId' " +
                        "conflicts with Supervisor agentId"
                }
                registry.registerFactory(agentId) {
                    val threadId = AiChatContext.getThreadId()
                    val userId = threadId?.let {
                        sessionContext.getUserId(it)
                    } ?: "unknown"
                    val chatContext = AiChatContext.getContext()
                    logger.info(
                        "[AguiConfig] Creating standalone " +
                                "sub-agent: agentId={}, userId={}",
                        agentId, userId
                    )
                    val agent = subAgentFactory.createAgent(
                        definition = definition,
                        model = openAIChatModel,
                        userId = userId,
                        chatContext = chatContext
                    )
                    threadId?.let {
                        sessionContext.registerAgentThread(
                            agent, it
                        )
                    }
                    agent
                }
                logger.info(
                    "[AguiConfig] Registered sub-agent " +
                            "factory: agentId='{}'", agentId
                )
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun threadSessionManager(
        props: AguiProperties
    ): ThreadSessionManager {
        return ThreadSessionManager(
            props.maxThreadSessions,
            props.sessionTimeoutMinutes
        )
    }

    @Bean
    fun persistentAgentResolver(
        registry: AguiAgentRegistry,
        sessionManager: ThreadSessionManager,
        session: Session,
        props: AguiProperties,
        sessionContext: AgentSessionContext
    ): PersistentAgentResolver {
        logger.info(
            "[AguiConfig] PersistentAgentResolver created " +
                    "with serverSideMemory={}, " +
                    "sessionBackend={}",
            props.isServerSideMemory,
            session.javaClass.simpleName
        )
        return PersistentAgentResolver(
            registry = registry,
            sessionManager = sessionManager,
            session = session,
            serverSideMemory = props.isServerSideMemory,
            sessionContext = sessionContext
        )
    }

    @Bean
    fun aguiRequestProcessor(
        resolver: PersistentAgentResolver,
        props: AguiProperties
    ): AguiRequestProcessor {
        val config = AguiAdapterConfig.builder()
            .toolMergeMode(props.defaultToolMergeMode)
            .runTimeout(props.runTimeout)
            .emitStateEvents(props.isEmitStateEvents)
            .emitToolCallArgs(props.isEmitToolCallArgs)
            .enableReasoning(props.isEnableReasoning)
            .defaultAgentId(props.defaultAgentId)
            .build()

        return AguiRequestProcessor.builder()
            .agentResolver(resolver)
            .config(config)
            .build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AguiAgentConfig::class.java
        )
        private const val AGENT_ID = "default"
    }
}
