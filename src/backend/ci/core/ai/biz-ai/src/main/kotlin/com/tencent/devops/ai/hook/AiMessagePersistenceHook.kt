package com.tencent.devops.ai.hook

import com.tencent.devops.ai.dao.AiMessageDao
import com.tencent.devops.ai.dao.AiSessionDao
import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.common.api.util.UUIDUtil
import io.agentscope.core.hook.Hook
import io.agentscope.core.hook.HookEvent
import io.agentscope.core.hook.PostCallEvent
import io.agentscope.core.hook.PreCallEvent
import io.agentscope.core.message.MsgRole
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * 将用户消息（PreCallEvent）和助手回复（PostCallEvent）
 * 持久化到 T_AI_MESSAGE 表。
 * 消息持久化仅在 Supervisor（主智能体）触发时执行，
 * 子智能体的事件正常透传但不落库，避免重复记录。
 */
@Component
class AiMessagePersistenceHook @Autowired constructor(
    private val dslContext: DSLContext,
    private val aiMessageDao: AiMessageDao,
    private val aiSessionDao: AiSessionDao,
    private val sessionContext: AgentSessionContext
) : Hook {

    @Suppress("UNCHECKED_CAST")
    override fun <T : HookEvent> onEvent(event: T): Mono<T> {
        if (event.agent.name != SUPERVISOR_NAME) {
            return Mono.just(event)
        }
        return when (event) {
            is PreCallEvent -> handlePreCall(event) as Mono<T>

            is PostCallEvent -> handlePostCall(event) as Mono<T>

            else -> Mono.just(event)
        }
    }

    override fun priority(): Int = PRIORITY

    /**
     * 智能体调用前：提取用户消息并异步持久化到数据库。
     */
    private fun handlePreCall(
        event: PreCallEvent
    ): Mono<PreCallEvent> {
        val sessionId = sessionContext.getThreadIdByAgent(event.agent)
            ?: return Mono.just(event)
        val userMsgs = event.inputMessages
            ?.filter { it.role == MsgRole.USER }
            ?: return Mono.just(event)
        if (userMsgs.isEmpty()) {
            return Mono.just(event)
        }
        logger.info("[Hook] PreCall: sessionId={}, userMessages={}", sessionId, userMsgs.size)
        return Mono.fromRunnable<Void> {
            userMsgs.forEach { msg ->
                persistMessage(
                    sessionId, msg.role.name,
                    msg.textContent
                )
            }
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
    }

    /**
     * 智能体调用后：持久化助手回复并更新会话时间戳。
     */
    private fun handlePostCall(
        event: PostCallEvent
    ): Mono<PostCallEvent> {
        if (event.agent.name != SUPERVISOR_NAME) {
            return Mono.just(event)
        }
        val sessionId = sessionContext.getThreadIdByAgent(event.agent)
            ?: return Mono.just(event)
        val content = event.finalMessage?.textContent
        if (content.isNullOrBlank()) {
            return Mono.just(event)
        }
        logger.info(
            "[Hook] PostCall: sessionId={}, responseLength={}",
            sessionId, content.length
        )
        return Mono.fromRunnable<Void> {
            persistMessage(sessionId, ROLE_ASSISTANT, content)
            aiSessionDao.updateTime(dslContext, sessionId)
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
    }

    /**
     * 将单条消息写入数据库，自动分配递增序号。
     * 空白内容会被静默跳过，写入失败仅记录告警不中断流程。
     */
    private fun persistMessage(
        sessionId: String,
        role: String,
        content: String?,
        extraData: String? = null
    ) {
        if (content.isNullOrBlank()) return
        try {
            val nextIndex = aiMessageDao.getMaxIndex(
                dslContext, sessionId
            ) + 1
            val msgId = UUIDUtil.generate()
            aiMessageDao.create(
                dslContext, msgId, sessionId,
                role, content, nextIndex, extraData
            )
            logger.info(
                "[Hook] Persisted {}: sessionId={}, " +
                        "index={}, len={}, extraDataLen={}",
                role, sessionId, nextIndex, content.length,
                extraData?.length ?: 0
            )
        } catch (e: Exception) {
            logger.warn(
                "[Hook] Failed to persist {} " +
                        "for session {}: {}",
                role, sessionId, e.message, e
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiMessagePersistenceHook::class.java)
        private const val PRIORITY = 200
        private const val ROLE_ASSISTANT = "ASSISTANT"
        private const val SUPERVISOR_NAME = "BkCI-Supervisor"
    }
}
