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

package com.tencent.devops.ai.service

import com.tencent.devops.ai.agent.SubAgentDefinition
import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.ai.context.AiChatContext
import com.tencent.devops.ai.pojo.AgentInfo
import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.pojo.ServiceAgentRunRequest
import com.tencent.devops.ai.pojo.ServiceAgentRunResponse
import com.tencent.devops.ai.util.AiErrorMessageTranslator
import com.tencent.devops.ai.util.SseEventWriter
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.ai.constant.AiMessageCode
import io.agentscope.core.agui.encoder.AguiEventEncoder
import io.agentscope.core.agui.event.AguiEvent
import io.agentscope.core.agui.model.AguiMessage
import io.agentscope.core.agui.model.RunAgentInput
import io.agentscope.core.agui.processor.AguiRequestProcessor
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.Disposable
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * AI 智能体调用服务。
 */
@Service
class AiAgentInvocationService @Autowired constructor(
    private val processor: AguiRequestProcessor,
    private val subAgentDefinitions: List<SubAgentDefinition>,
    private val sessionContext: AgentSessionContext
) {

    fun listAgents(): List<AgentInfo> {
        return subAgentDefinitions.map { definition ->
            AgentInfo(
                name = definition.toolName(),
                description = definition.description()
            )
        }
    }

    fun invokeAgent(
        agentName: String,
        userId: String,
        request: ServiceAgentRunRequest
    ): ServiceAgentRunResponse {
        subAgentDefinitions.find {
            it.toolName() == agentName
        } ?: throw ErrorCodeException(
            errorCode = AiMessageCode.AGENT_NOT_FOUND,
            defaultMessage = "Agent not found: $agentName",
            params = arrayOf(agentName)
        )

        val threadId = UUID.randomUUID().toString()
        val runId = UUID.randomUUID().toString()

        logger.info("[AgentInvocation] Invoking agent: name={}, userId={}, threadId={}", agentName, userId, threadId)

        try {
            setupContext(userId, threadId, request.chatContext)

            val input = buildRunAgentInput(
                threadId = threadId,
                runId = runId,
                message = request.message
            )

            val result = processor.process(input, null, agentName)
            val content = collectEvents(
                result.events(), threadId, runId
            )

            logger.info(
                "[AgentInvocation] Agent completed: name={}, userId={},contentLength={}",
                agentName, userId, content.length
            )

            return ServiceAgentRunResponse(
                content = content,
                agentName = agentName
            )
        } finally {
            cleanupContext(threadId)
        }
    }

    fun streamAgent(
        agentName: String,
        userId: String,
        request: ServiceAgentRunRequest,
        output: ChunkedOutput<String>
    ) {
        subAgentDefinitions.find {
            it.toolName() == agentName
        } ?: throw ErrorCodeException(
            errorCode = AiMessageCode.AGENT_NOT_FOUND,
            defaultMessage = "Agent not found: $agentName",
            params = arrayOf(agentName)
        )

        val threadId = UUID.randomUUID().toString()
        val runId = UUID.randomUUID().toString()

        logger.info(
            "[AgentInvocation] Streaming agent: name={}, " +
                    "userId={}, threadId={}", agentName, userId, threadId
        )

        try {
            setupContext(userId, threadId, request.chatContext)

            val input = buildRunAgentInput(
                threadId = threadId,
                runId = runId,
                message = request.message
            )

            val result = processor.process(input, null, agentName)
            val encoder = AguiEventEncoder()
            val latch = CountDownLatch(1)

            result.events().subscribe(
                { event ->
                    try {
                        output.write(encoder.encode(event))
                    } catch (e: Exception) {
                        logger.info(
                            "[AgentInvocation] Client disconnected: " +
                                    "threadId={}",
                            threadId
                        )
                    }
                },
                { error ->
                    logger.error(
                        "[AgentInvocation] Stream error: " +
                                "threadId={}, error={}",
                        threadId, error.message
                    )
                    SseEventWriter.writeErrorAndFinish(
                        output, threadId, runId,
                        AiErrorMessageTranslator.toFriendlyMessage(error)
                    )
                    latch.countDown()
                },
                {
                    logger.info(
                        "[AgentInvocation] Stream completed: " +
                                "threadId={}, runId={}",
                        threadId, runId
                    )
                    latch.countDown()
                }
            )

            val completed = latch.await(
                RUN_TIMEOUT_MINUTES, TimeUnit.MINUTES
            )
            if (!completed) {
                logger.warn(
                    "[AgentInvocation] Stream timeout: " +
                            "threadId={}",
                    threadId
                )
                SseEventWriter.writeErrorAndFinish(
                    output, threadId, runId,
                    "Agent invocation timeout after ${RUN_TIMEOUT_MINUTES}min"
                )
            }
        } finally {
            cleanupContext(threadId)
        }
    }

    private fun setupContext(
        userId: String,
        threadId: String,
        chatContext: ChatContextDTO?
    ) {
        AiChatContext.setContext(chatContext ?: ChatContextDTO())
        AiChatContext.setThreadId(threadId)
        sessionContext.bindContext(threadId, userId, AiChatContext.getContext())
    }

    private fun cleanupContext(threadId: String) {
        sessionContext.evictAll(threadId)
        AiChatContext.clear()
    }

    private fun buildRunAgentInput(
        threadId: String,
        runId: String,
        message: String
    ): RunAgentInput {
        return RunAgentInput.builder()
            .threadId(threadId)
            .runId(runId)
            .messages(
                listOf(
                    AguiMessage.userMessage(
                        UUID.randomUUID().toString(),
                        message
                    )
                )
            )
            .build()
    }

    /**
     * 订阅事件流，从 [AguiEvent.TextMessageContent] 事件中
     * 收集文本 delta，拼接为最终响应。
     *
     * 超时或出错时通过 [Disposable.dispose] 取消订阅，
     * 避免底层 Agent 继续占用资源。
     */
    private fun collectEvents(
        events: reactor.core.publisher.Flux<AguiEvent>,
        threadId: String,
        runId: String
    ): String {
        val textBuilder = StringBuilder()
        val latch = CountDownLatch(1)
        var streamError: Throwable? = null

        val disposable: Disposable = events.subscribe(
            { event ->
                when (event) {
                    is AguiEvent.TextMessageStart -> {
                        textBuilder.clear()
                    }

                    is AguiEvent.TextMessageContent -> {
                        textBuilder.append(event.delta ?: "")
                    }

                    else -> { /* ignore reasoning, tool calls, etc. */
                    }
                }
            },
            { error ->
                logger.error(
                    "[AgentInvocation] Stream error: " +
                            "threadId={}, error={}",
                    threadId, error.message
                )
                streamError = error
                latch.countDown()
            },
            {
                logger.info(
                    "[AgentInvocation] Stream completed: " +
                            "threadId={}, runId={}",
                    threadId, runId
                )
                latch.countDown()
            }
        )

        try {
            val completed = latch.await(
                RUN_TIMEOUT_MINUTES, TimeUnit.MINUTES
            )
            if (!completed) {
                logger.warn(
                    "[AgentInvocation] Timeout: threadId={}",
                    threadId
                )
                throw ErrorCodeException(
                    errorCode = AiMessageCode.AGENT_RUN_TIMEOUT,
                    defaultMessage = "Agent run timed out"
                )
            }
            streamError?.let { error ->
                throw ErrorCodeException(
                    errorCode = AiMessageCode.AGENT_RUN_FAILED,
                    defaultMessage = "Agent run failed: " +
                            "${error.message}"
                )
            }
        } finally {
            if (!disposable.isDisposed) {
                disposable.dispose()
            }
        }

        return textBuilder.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiAgentInvocationService::class.java
        )
        private const val RUN_TIMEOUT_MINUTES = 5L
    }
}
