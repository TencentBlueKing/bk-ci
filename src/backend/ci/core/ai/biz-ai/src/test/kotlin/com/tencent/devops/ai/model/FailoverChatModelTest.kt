package com.tencent.devops.ai.model

import io.agentscope.core.message.Msg
import io.agentscope.core.model.ChatResponse
import io.agentscope.core.model.GenerateOptions
import io.agentscope.core.model.Model
import io.agentscope.core.model.ToolSchema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux

class FailoverChatModelTest {

    @Test
    fun `should fallback to next model when previous model fails`() {
        val first = FakeModel(
            name = "primary",
            response = Flux.error(IllegalStateException("primary down"))
        )
        val second = FakeModel(
            name = "backup",
            response = Flux.just(ChatResponse.builder().build())
        )

        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(id = "primary", model = first),
                FailoverModelCandidate(id = "backup", model = second)
            )
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
    fun `should throw last error when all models fail`() {
        val primaryError = IllegalStateException("primary down")
        val backupError = IllegalArgumentException("backup down")
        val model = FailoverChatModel(
            candidates = listOf(
                FailoverModelCandidate(
                    id = "primary",
                    model = FakeModel("primary", Flux.error(primaryError))
                ),
                FailoverModelCandidate(
                    id = "backup",
                    model = FakeModel("backup", Flux.error(backupError))
                )
            )
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            model.stream(
                mutableListOf<Msg>(),
                mutableListOf<ToolSchema>(),
                GenerateOptions.builder().build()
            ).collectList().block()
        }

        assertEquals("backup down", error.message)
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
