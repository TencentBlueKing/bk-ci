package com.tencent.devops.gpt.service.model

import com.tencent.devops.gpt.service.config.HunYuanConfig
import dev.ai4j.openai4j.OpenAiClient
import dev.ai4j.openai4j.OpenAiHttpException
import dev.ai4j.openai4j.chat.ChatCompletionChoice
import dev.ai4j.openai4j.chat.ChatCompletionRequest
import dev.ai4j.openai4j.chat.ChatCompletionResponse
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.internal.RetryUtils
import dev.langchain4j.model.Tokenizer
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.TokenCountEstimator
import dev.langchain4j.model.chat.listener.ChatModelErrorContext
import dev.langchain4j.model.chat.listener.ChatModelListener
import dev.langchain4j.model.chat.listener.ChatModelRequestContext
import dev.langchain4j.model.chat.listener.ChatModelResponseContext
import dev.langchain4j.model.openai.InternalOpenAiHelper
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.model.output.Response
import java.net.Proxy
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HunYuanChatModel(
    private val client: OpenAiClient,
    private val modelName: String?,
    private val temperature: Double,
    private val topP: Double?,
    private val stop: List<String>?,
    private val maxTokens: Int?,
    private val presencePenalty: Double?,
    private val frequencyPenalty: Double?,
    private val logitBias: Map<String, Int>?,
    private val responseFormat: String?,
    private val seed: Int?,
    private val user: String?,
    private val maxRetries: Int,
    private val tokenizer: Tokenizer,
    private val listeners: List<ChatModelListener>
) : ChatLanguageModel, TokenCountEstimator {

    constructor(hunYuanConfig: HunYuanConfig) : this(
        url = hunYuanConfig.url,
        customHeaders = hunYuanConfig.headers
    )

    constructor(
        url: String,
        temperature: Double? = null,
        topP: Double? = null,
        stop: List<String>? = null,
        maxTokens: Int? = null,
        presencePenalty: Double? = null,
        frequencyPenalty: Double? = null,
        logitBias: Map<String, Int>? = null,
        responseFormat: String? = null,
        seed: Int? = null,
        user: String? = null,
        timeout: Duration? = null,
        maxRetries: Int? = null,
        proxy: Proxy? = null,
        logRequests: Boolean? = null,
        logResponses: Boolean? = null,
        tokenizer: Tokenizer? = null,
        customHeaders: Map<String, String>? = null,
        listeners: List<ChatModelListener>? = null
    ) : this(
        client = OpenAiClient.builder()
            .baseUrl(url)
            .openAiApiKey("*")
            .callTimeout(timeout ?: Duration.ofSeconds(60L))
            .connectTimeout(timeout ?: Duration.ofSeconds(60L))
            .readTimeout(timeout ?: Duration.ofSeconds(60L))
            .writeTimeout(timeout ?: Duration.ofSeconds(60L)).proxy(proxy)
            .logRequests(logRequests).logResponses(logResponses).userAgent("langchain4j-openai")
            .customHeaders(customHeaders).build(),
        modelName = "hunyuan",
        temperature = temperature ?: 0.7,
        topP = topP,
        stop = stop,
        maxTokens = maxTokens,
        presencePenalty = presencePenalty,
        frequencyPenalty = frequencyPenalty,
        logitBias = logitBias,
        responseFormat = responseFormat,
        seed = seed,
        user = user,
        maxRetries = maxRetries ?: 3,
        tokenizer = tokenizer ?: OpenAiTokenizer(),
        listeners = listeners ?: emptyList()
    )

    fun modelName(): String? {
        return this.modelName
    }

    override fun generate(messages: List<ChatMessage>): Response<AiMessage> {
        return this.generate(messages, null, null)
    }

    override fun generate(
        messages: List<ChatMessage>,
        toolSpecifications: List<ToolSpecification>
    ): Response<AiMessage> {
        return this.generate(messages, toolSpecifications, null)
    }

    override fun generate(messages: List<ChatMessage>, toolSpecification: ToolSpecification): Response<AiMessage> {
        return this.generate(messages, listOf(toolSpecification), toolSpecification)
    }

    private fun generate(
        messages: List<ChatMessage>,
        toolSpecifications: List<ToolSpecification>?,
        toolThatMustBeExecuted: ToolSpecification?
    ): Response<AiMessage> {
        val requestBuilder = ChatCompletionRequest.builder().model(this.modelName)
            .messages(InternalOpenAiHelper.toOpenAiMessages(messages)).temperature(this.temperature).topP(this.topP)
            .stop(this.stop).maxTokens(this.maxTokens).presencePenalty(this.presencePenalty)
            .frequencyPenalty(this.frequencyPenalty).logitBias(this.logitBias).responseFormat(this.responseFormat)
            .seed(this.seed).user(this.user)
        if (!toolSpecifications.isNullOrEmpty()) {
            requestBuilder.tools(InternalOpenAiHelper.toTools(toolSpecifications))
        }

        if (toolThatMustBeExecuted != null) {
            requestBuilder.toolChoice(toolThatMustBeExecuted.name())
        }

        val request = requestBuilder.build()
        val modelListenerRequest = ChatHelper.createModelListenerRequest(request, messages, toolSpecifications)
        val attributes: Map<Any, Any> = ConcurrentHashMap()
        val requestContext = ChatModelRequestContext(modelListenerRequest, attributes)
        listeners.forEach(Consumer { listener: ChatModelListener ->
            try {
                listener.onRequest(requestContext)
            } catch (ignore: Exception) {
                log.warn("Exception while calling model listener", ignore)
            }
        })

        try {
            val chatCompletionResponse = RetryUtils.withRetry(
                { client.chatCompletion(request).execute() as ChatCompletionResponse },
                maxRetries
            ) as ChatCompletionResponse
            val response = Response.from(
                InternalOpenAiHelper.aiMessageFrom(chatCompletionResponse),
                InternalOpenAiHelper.tokenUsageFrom(chatCompletionResponse.usage()),
                InternalOpenAiHelper.finishReasonFrom(
                    (chatCompletionResponse.choices()[0] as ChatCompletionChoice).finishReason()
                )
            )
            val modelListenerResponse = ChatHelper.createModelListenerResponse(
                chatCompletionResponse.id(),
                chatCompletionResponse.model(),
                response
            )
            val responseContext = ChatModelResponseContext(modelListenerResponse, modelListenerRequest, attributes)
            listeners.forEach(Consumer { listener: ChatModelListener ->
                try {
                    listener.onResponse(responseContext)
                } catch (ignore: Exception) {
                    log.warn("Exception while calling model listener", ignore)
                }
            })
            return response
        } catch (e: RuntimeException) {
            val error = if (e.cause is OpenAiHttpException) {
                e.cause
            } else {
                e
            }

            val errorContext = ChatModelErrorContext(
                error, modelListenerRequest, null, attributes
            )
            listeners.forEach(Consumer { listener: ChatModelListener ->
                try {
                    listener.onError(errorContext)
                } catch (ignore: Exception) {
                    log.warn("Exception while calling model listener", ignore)
                }
            })
            throw e
        }
    }

    override fun estimateTokenCount(messages: List<ChatMessage>): Int {
        return tokenizer.estimateTokenCountInMessages(messages)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HunYuanChatModel::class.java)
    }
}
