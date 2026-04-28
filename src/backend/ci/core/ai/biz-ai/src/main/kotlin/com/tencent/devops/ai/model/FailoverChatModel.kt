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
    private val candidates: List<FailoverModelCandidate>
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
                    val nextIndex = candidateIndex + 1
                    if (nextIndex >= candidates.size) {
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
                        Flux.error(error)
                    } else {
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
                        streamWithCandidate(
                            candidateIndex = nextIndex,
                            messages = messages,
                            toolSchemas = toolSchemas,
                            options = options
                        )
                    }
                }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FailoverChatModel::class.java)
    }
}
