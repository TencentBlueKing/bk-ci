package com.tencent.devops.gpt.service.hunyuan

import dev.ai4j.openai4j.chat.ChatCompletionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.listener.ChatModelRequest
import dev.langchain4j.model.chat.listener.ChatModelResponse
import dev.langchain4j.model.output.Response

object HunYuanChatHelper {
    fun createModelListenerRequest(
        request: ChatCompletionRequest,
        messages: List<ChatMessage>,
        toolSpecifications: List<ToolSpecification>?
    ): ChatModelRequest {
        return ChatModelRequest.builder()
            .model(request.model())
            .temperature(request.temperature())
            .topP(request.topP())
            .maxTokens(request.maxTokens())
            .messages(messages)
            .toolSpecifications(toolSpecifications)
            .build()
    }

    fun createModelListenerResponse(
        responseId: String,
        responseModel: String,
        response: Response<AiMessage>?
    ): ChatModelResponse? {
        if (response == null) {
            return null
        }

        return ChatModelResponse.builder()
            .id(responseId)
            .model(responseModel)
            .tokenUsage(response.tokenUsage())
            .finishReason(response.finishReason())
            .aiMessage(response.content())
            .build()
    }
}
