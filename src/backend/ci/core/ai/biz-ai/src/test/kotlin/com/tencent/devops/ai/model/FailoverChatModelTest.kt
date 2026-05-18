package com.tencent.devops.ai.model

import io.agentscope.core.message.Msg
import io.agentscope.core.model.ChatResponse
import io.agentscope.core.model.GenerateOptions
import io.agentscope.core.model.Model
import io.agentscope.core.model.ToolSchema
import io.agentscope.core.model.exception.AuthenticationException
import io.agentscope.core.model.exception.BadRequestException
import io.agentscope.core.model.exception.RateLimitException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux

class FailoverChatModelTest {

    private val errorClassifier = AiErrorClassifier()

    @Test
    fun `should fallback to next model when previous model fails with retryable error`() {
        val first = FakeModel(
            name = "primary",
            response = Flux.error(RateLimitException("rate limited", null, null))
        )
        val second = FakeModel(
            name = "backup",
            response = Flux.just(ChatResponse.builder().build())
        )

        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(id = "primary", model = first),
                FailoverModelCandidate(id = "backup", model = second)
            ),
            errorClassifier = errorClassifier
        )

        model.stream(
            mutableListOf<Msg>(),
            mutableListOf<ToolSchema>(),
            GenerateOptions.builder().build()
        ).collectList().block()

        assertEquals(1, first.invocations)
        assertEquals(1, second.invocations)
    }

    @Test
    fun `should throw last error when all retryable models fail`() {
        val primaryError = RateLimitException("rate limited", null, null)
        val backupError = IllegalArgumentException("backup down")
        val first = FakeModel("primary", Flux.error(primaryError))
        val second = FakeModel("backup", Flux.error(backupError))

        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(id = "primary", model = first),
                FailoverModelCandidate(id = "backup", model = second)
            ),
            errorClassifier = errorClassifier
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            model.stream(
                mutableListOf<Msg>(),
                mutableListOf<ToolSchema>(),
                GenerateOptions.builder().build()
            ).collectList().block()
        }

        assertEquals("backup down", error.message)
        assertEquals(1, first.invocations)
        assertEquals(1, second.invocations)
    }

    @Test
    fun `should fail-fast on 4xx client error and not switch to next candidate`() {
        val first = FakeModel(
            name = "primary",
            response = Flux.error(BadRequestException("invalid prompt", null, null))
        )
        val second = FakeModel(
            name = "backup",
            response = Flux.just(ChatResponse.builder().build())
        )

        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(id = "primary", model = first),
                FailoverModelCandidate(id = "backup", model = second)
            ),
            errorClassifier = errorClassifier
        )

        val error = assertThrows(BadRequestException::class.java) {
            model.stream(
                mutableListOf<Msg>(),
                mutableListOf<ToolSchema>(),
                GenerateOptions.builder().build()
            ).collectList().block()
        }

        assertEquals("invalid prompt", error.message)
        assertEquals(1, first.invocations)
        assertEquals(0, second.invocations)
    }

    @Test
    fun `should stop at second candidate when mid-chain hits non-retryable error`() {
        val first = FakeModel(
            name = "primary",
            response = Flux.error(RateLimitException("rate limited", null, null))
        )
        val second = FakeModel(
            name = "secondary",
            response = Flux.error(AuthenticationException("unauthorized", null, null))
        )
        val third = FakeModel(
            name = "tertiary",
            response = Flux.just(ChatResponse.builder().build())
        )

        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(id = "primary", model = first),
                FailoverModelCandidate(id = "secondary", model = second),
                FailoverModelCandidate(id = "tertiary", model = third)
            ),
            errorClassifier = errorClassifier
        )

        val error = assertThrows(AuthenticationException::class.java) {
            model.stream(
                mutableListOf<Msg>(),
                mutableListOf<ToolSchema>(),
                GenerateOptions.builder().build()
            ).collectList().block()
        }

        assertEquals("unauthorized", error.message)
        assertEquals(1, first.invocations)
        assertEquals(1, second.invocations)
        assertEquals(0, third.invocations)
    }

    @Test
    fun `should stop calling later candidates once one succeeds`() {
        val first = FakeModel(
            name = "primary",
            response = Flux.error(RateLimitException("rate limited", null, null))
        )
        val second = FakeModel(
            name = "secondary",
            response = Flux.just(ChatResponse.builder().build())
        )
        val third = FakeModel(
            name = "tertiary",
            response = Flux.error(RateLimitException("should not be invoked", null, null))
        )

        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(id = "primary", model = first),
                FailoverModelCandidate(id = "secondary", model = second),
                FailoverModelCandidate(id = "tertiary", model = third)
            ),
            errorClassifier = errorClassifier
        )

        val responses = model.stream(
            mutableListOf<Msg>(),
            mutableListOf<ToolSchema>(),
            GenerateOptions.builder().build()
        ).collectList().block()

        assertTrue(responses != null && responses.isNotEmpty())
        assertEquals(1, first.invocations)
        assertEquals(1, second.invocations)
        assertEquals(0, third.invocations)
    }

    private class FakeModel(
        private val name: String,
        private val response: Flux<ChatResponse>
    ) : Model {
        var invocations: Int = 0

        override fun stream(
            messages: MutableList<Msg>?,
            toolSchemas: MutableList<ToolSchema>?,
            options: GenerateOptions?
        ): Flux<ChatResponse> {
            invocations++
            return response
        }

        override fun getModelName(): String = name
    }
}
