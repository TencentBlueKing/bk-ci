package com.tencent.devops.ai.session

import com.tencent.devops.ai.context.AgentSessionContext
import io.agentscope.core.agent.Agent
import io.agentscope.core.agui.AguiException
import io.agentscope.core.agui.processor.AgentResolver
import io.agentscope.core.agui.registry.AguiAgentRegistry
import io.agentscope.core.session.Session
import io.agentscope.core.state.StateModule
import io.agentscope.spring.boot.agui.common.ThreadSessionManager
import org.slf4j.LoggerFactory

/**
 * 自定义 AgentResolver，整合 ThreadSessionManager（内存热缓存）与
 * Session（MySQL 冷存储），负责解析/创建 Agent 实例和持久化状态。
 */
class PersistentAgentResolver(
    private val registry: AguiAgentRegistry,
    private val sessionManager: ThreadSessionManager,
    private val session: Session,
    private val serverSideMemory: Boolean,
    private val sessionContext: AgentSessionContext
) : AgentResolver {

    /** 根据 agentId 和 threadId 解析或创建 Agent 实例，并从 DB 恢复状态。 */
    override fun resolveAgent(
        agentId: String,
        threadId: String?
    ): Agent {
        logger.debug(
            "[PersistentResolver] resolveAgent called: " +
                "agentId={}, threadId={}, " +
                "serverSideMemory={}, thread={}",
            agentId, threadId,
            serverSideMemory, Thread.currentThread().name
        )

        if (!serverSideMemory || threadId.isNullOrBlank()) {
            logger.debug(
                "[PersistentResolver] Stateless mode: " +
                    "serverSideMemory={}, threadId={}",
                serverSideMemory, threadId
            )
            return registry.getAgent(agentId).orElseThrow {
                AguiException.AgentNotFoundException(agentId)
            }
        }

        val hadSession = sessionManager.getSession(threadId).isPresent
        val agent = sessionManager.getOrCreateAgent(
            threadId, agentId
        ) {
            logger.info(
                "[PersistentResolver] Creating new agent: " +
                    "agentId={}, threadId={}",
                agentId, threadId
            )
            registry.getAgent(agentId).orElseThrow {
                AguiException.AgentNotFoundException(agentId)
            }
        }

        sessionContext.registerAgentThread(agent, threadId)
        val userId = sessionContext.getUserId(threadId)
        logger.info(
            "[PersistentResolver] resolveAgent: " +
                "threadId={}, agentId={}, " +
                "hadSession={}, userId={}, thread={}",
            threadId, agentId, hadSession,
            userId, Thread.currentThread().name
        )

        if (!hadSession && agent is StateModule) {
            val loaded = agent.loadIfExists(session, threadId)
            logger.info(
                "[PersistentResolver] State recovery: " +
                    "threadId={}, loadedFromDb={}",
                threadId, loaded
            )
        } else {
            logger.debug(
                "[PersistentResolver] Reusing cached: " +
                    "threadId={}, agentClass={}",
                threadId, agent.javaClass.simpleName
            )
        }

        return agent
    }

    override fun hasMemory(threadId: String?): Boolean {
        if (!serverSideMemory || threadId.isNullOrBlank()) {
            return false
        }
        if (sessionManager.hasMemory(threadId)) {
            return true
        }
        return session.exists(
            io.agentscope.core.state.SimpleSessionKey.of(threadId)
        )
    }

    /** 将指定 threadId 对应的 Agent 状态持久化到 MySQL。 */
    fun persistAgent(threadId: String) {
        val threadSession = sessionManager.getSession(threadId)
        if (threadSession.isEmpty) {
            logger.debug(
                "[PersistentResolver] persistAgent: " +
                    "no session for threadId={}",
                threadId
            )
            return
        }
        val agent = threadSession.get().agent
        if (agent !is StateModule) {
            logger.warn(
                "[PersistentResolver] persistAgent: " +
                    "agent does not implement StateModule, " +
                    "skip persistence, threadId={}",
                threadId
            )
            return
        }
        agent.saveTo(session, threadId)
        logger.info(
            "[PersistentResolver] Saved agent state: " +
                "threadId={}",
            threadId
        )
    }

    /**
     * 获取指定 threadId 对应的已缓存 Agent 实例（只读，不创建新实例）。
     * 用于在新请求发起前检查 Agent 是否仍在执行中。
     *
     * @return 已缓存的 Agent 实例，无缓存时返回 null
     */
    fun getSessionAgent(threadId: String): Agent? {
        return sessionManager.getSession(threadId).map { it.agent }.orElse(null)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            PersistentAgentResolver::class.java
        )
    }
}
