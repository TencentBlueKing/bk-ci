package com.tencent.devops.ai.model

import io.agentscope.core.message.Msg
import io.agentscope.core.model.ChatResponse
import io.agentscope.core.model.GenerateOptions
import io.agentscope.core.model.Model
import io.agentscope.core.model.ToolSchema
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

data class FailoverModelCandidate(
    val id: String,
    val model: Model
)

class FailoverChatModel(
    private val candidates: List<FailoverModelCandidate>,
    private val errorClassifier: AiErrorClassifier
) : Model {

    override fun stream(
        messages: MutableList<Msg>?,
        toolSchemas: MutableList<ToolSchema>?,
        options: GenerateOptions?
    ): Flux<ChatResponse> {
        require(candidates.isNotEmpty()) {
            "FailoverChatModel requires at least one candidate"
        }
        return streamWithCandidate(
            candidateIndex = 0,
            messages = messages,
            toolSchemas = toolSchemas,
            options = options
        )
    }

    override fun getModelName(): String {
        return candidates.joinToString(separator = ",") {
            it.model.getModelName()
        }
    }

    private fun streamWithCandidate(
        candidateIndex: Int,
        messages: MutableList<Msg>?,
        toolSchemas: MutableList<ToolSchema>?,
        options: GenerateOptions?
    ): Flux<ChatResponse> {
        val candidate = candidates[candidateIndex]
        return Flux.defer {
            logger.info(
                "[LLM-Failover] attempting candidate {}/{}: {} ({})",
                candidateIndex + 1,
                candidates.size,
                candidate.id,
                candidate.model.getModelName()
            )
            candidate.model.stream(messages, toolSchemas, options)
                .doOnComplete {
                    logger.info(
                        "[LLM-Failover] candidate completed: current={} ({}), position={}/{}",
                        candidate.id,
                        candidate.model.getModelName(),
                        candidateIndex + 1,
                        candidates.size
                    )
                }
                .onErrorResume { error ->
                    handleCandidateError(
                        error = error,
                        candidateIndex = candidateIndex,
                        candidate = candidate,
                        messages = messages,
                        toolSchemas = toolSchemas,
                        options = options
                    )
                }
        }
    }

    private fun handleCandidateError(
        error: Throwable,
        candidateIndex: Int,
        candidate: FailoverModelCandidate,
        messages: MutableList<Msg>?,
        toolSchemas: MutableList<ToolSchema>?,
        options: GenerateOptions?
    ): Flux<ChatResponse> {
        val nextIndex = candidateIndex + 1
        val isLast = nextIndex >= candidates.size
        // 永久性客户端错误（4xx）切到下一个模型也注定失败，立即 fail-fast，
        // 避免浪费用户等待时间并保留原始错误归因。最后一个候选无论可重试与否都已无处可切，
        // 维持原有「全失败抛出」语义。
        if (!isLast && !errorClassifier.isRetryable(error)) {
            logger.warn(
                "[LLM-Failover] non-retryable error, fail-fast: current={} ({}), " +
                    "position={}/{}, reasonType={}, reason={} | cause: {}",
                candidate.id,
                candidate.model.getModelName(),
                candidateIndex + 1,
                candidates.size,
                error.javaClass.simpleName,
                error.message,
                errorClassifier.describeCauseChain(error)
            )
            return Flux.error(error)
        }
        if (isLast) {
            logger.error(
                "[LLM-Failover] all candidates failed, lastCandidate={} ({}), " +
                    "position={}/{}, reasonType={}, reason={}",
                candidate.id,
                candidate.model.getModelName(),
                candidateIndex + 1,
                candidates.size,
                error.javaClass.simpleName,
                error.message,
                error
            )
            return Flux.error(error)
        }
        val nextCandidate = candidates[nextIndex]
        logger.warn(
            "[LLM-Failover] candidate failed, switching: current={} ({}), " +
                "next={} ({}), currentPosition={}/{}, reasonType={}, reason={}",
            candidate.id,
            candidate.model.getModelName(),
            nextCandidate.id,
            nextCandidate.model.getModelName(),
            candidateIndex + 1,
            candidates.size,
            error.javaClass.simpleName,
            error.message
        )
        return streamWithCandidate(
            candidateIndex = nextIndex,
            messages = messages,
            toolSchemas = toolSchemas,
            options = options
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FailoverChatModel::class.java)
    }
}
