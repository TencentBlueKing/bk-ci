package com.tencent.devops.gpt.service.model

import com.tencent.devops.gpt.service.config.HunYuanConfig
import dev.ai4j.openai4j.OpenAiClient
import dev.ai4j.openai4j.chat.ChatCompletionChoice
import dev.ai4j.openai4j.chat.ChatCompletionRequest
import dev.ai4j.openai4j.chat.ChatCompletionResponse
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.internal.Utils
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.Tokenizer
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.chat.TokenCountEstimator
import dev.langchain4j.model.chat.listener.ChatModelErrorContext
import dev.langchain4j.model.chat.listener.ChatModelListener
import dev.langchain4j.model.chat.listener.ChatModelRequestContext
import dev.langchain4j.model.chat.listener.ChatModelResponseContext
import dev.langchain4j.model.openai.InternalOpenAiHelper
import dev.langchain4j.model.openai.OpenAiStreamingResponseBuilder
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.model.output.Response
import java.net.Proxy
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HunYuanStreamingChatModel(
    private val client: OpenAiClient,
    private val modelName: String,
    private val temperature: Double,
    private val topP: Double?,
    private val stop: List<String>?,
    private val maxTokens: Int?,
    private val presencePenalty: Double?,
    private val frequencyPenalty: Double?,
    private val logitBias: Map<String, Int>?,
    private val responseFormat: String?,
    private val seed: Int?,
    private var user: String?,
    private val tokenizer: Tokenizer?,
    private val listeners: List<ChatModelListener>
) : StreamingChatLanguageModel, TokenCountEstimator {

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
            .writeTimeout(timeout ?: Duration.ofSeconds(60L))
            .proxy(proxy)
            .logRequests(logRequests)
            .logStreamingResponses(logResponses)
            .userAgent("langchain4j-openai")
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
        tokenizer = tokenizer ?: OpenAiTokenizer(),
        listeners = listeners ?: emptyList()
    )

    fun modelName(): String {
        return this.modelName
    }

    override fun generate(messages: List<ChatMessage>, handler: StreamingResponseHandler<AiMessage>) {
        this.generate(messages, null, null, handler)
    }

    override fun generate(
        messages: List<ChatMessage>,
        toolSpecifications: List<ToolSpecification>,
        handler: StreamingResponseHandler<AiMessage>
    ) {
        this.generate(messages, toolSpecifications, null, handler)
    }

    override fun generate(
        messages: List<ChatMessage>,
        toolSpecification: ToolSpecification,
        handler: StreamingResponseHandler<AiMessage>
    ) {
        this.generate(messages, null, toolSpecification, handler)
    }

    private fun generate(
        messages: List<ChatMessage>,
        toolSpecifications: List<ToolSpecification>?,
        toolThatMustBeExecuted: ToolSpecification?,
        handler: StreamingResponseHandler<AiMessage>
    ) {
        val requestBuilder = ChatCompletionRequest.builder().stream(true).model(this.modelName)
            .messages(InternalOpenAiHelper.toOpenAiMessages(messages)).temperature(this.temperature).topP(this.topP)
            .stop(this.stop).maxTokens(this.maxTokens).presencePenalty(this.presencePenalty)
            .frequencyPenalty(this.frequencyPenalty).logitBias(this.logitBias).responseFormat(this.responseFormat)
            .seed(this.seed).user(this.user)
        if (toolThatMustBeExecuted != null) {
            requestBuilder.tools(InternalOpenAiHelper.toTools(listOf(toolThatMustBeExecuted)))
            requestBuilder.toolChoice(toolThatMustBeExecuted.name())
        } else if (!Utils.isNullOrEmpty(toolSpecifications)) {
            requestBuilder.tools(InternalOpenAiHelper.toTools(toolSpecifications))
        }

        val request = requestBuilder.build()
        val modelListenerRequest = ChatHelper.createModelListenerRequest(request, messages, toolSpecifications)
        val attributes: Map<Any, Any> = ConcurrentHashMap()
        val requestContext = ChatModelRequestContext(modelListenerRequest, attributes)
        listeners.forEach(Consumer { listener: ChatModelListener ->
            try {
                listener.onRequest(requestContext)
            } catch (e: Exception) {
                log.warn("Exception while calling model listener", e)
            }
        })
        val inputTokenCount = this.countInputTokens(messages, toolSpecifications, toolThatMustBeExecuted)
        val responseBuilder = OpenAiStreamingResponseBuilder(inputTokenCount)
        val responseId: AtomicReference<String> = AtomicReference<String>()
        val responseModel: AtomicReference<String> = AtomicReference<String>()
        client.chatCompletion(request).onPartialResponse { partialResponse: ChatCompletionResponse ->
            responseBuilder.append(partialResponse)
            handle(partialResponse, handler)
            if (!Utils.isNullOrBlank(partialResponse.id())) {
                responseId.set(partialResponse.id())
            }
            if (!Utils.isNullOrBlank(partialResponse.model())) {
                responseModel.set(partialResponse.model())
            }
        }.onComplete {
            val response = this.createResponse(responseBuilder, toolThatMustBeExecuted)
            val modelListenerResponse =
                ChatHelper.createModelListenerResponse(responseId.get(), responseModel.get(), response)
            val responseContext = ChatModelResponseContext(modelListenerResponse, modelListenerRequest, attributes)
            listeners.forEach(Consumer { listener: ChatModelListener ->
                try {
                    listener.onResponse(responseContext)
                } catch (e: Exception) {
                    log.warn("Exception while calling model listener", e)
                }
            })
            handler.onComplete(response)
        }.onError { error: Throwable ->
            val response = this.createResponse(responseBuilder, toolThatMustBeExecuted)
            val modelListenerPartialResponse =
                ChatHelper.createModelListenerResponse(responseId.get(), responseModel.get(), response)
            val errorContext =
                ChatModelErrorContext(error, modelListenerRequest, modelListenerPartialResponse, attributes)
            listeners.forEach(Consumer { listener: ChatModelListener ->
                try {
                    listener.onError(errorContext)
                } catch (e: Exception) {
                    log.warn("Exception while calling model listener", e)
                }
            })
            handler.onError(error)
        }.execute()
    }

    private fun createResponse(
        responseBuilder: OpenAiStreamingResponseBuilder,
        toolThatMustBeExecuted: ToolSpecification?
    ): Response<AiMessage> {
        val response = responseBuilder.build(this.tokenizer, toolThatMustBeExecuted != null)
        return response
    }

    private fun countInputTokens(
        messages: List<ChatMessage>,
        toolSpecifications: List<ToolSpecification>?,
        toolThatMustBeExecuted: ToolSpecification?
    ): Int {
        var inputTokenCount = tokenizer!!.estimateTokenCountInMessages(messages)
        if (toolThatMustBeExecuted != null) {
            inputTokenCount += tokenizer.estimateTokenCountInForcefulToolSpecification(toolThatMustBeExecuted)
        } else if (!Utils.isNullOrEmpty(toolSpecifications)) {
            inputTokenCount += tokenizer.estimateTokenCountInToolSpecifications(toolSpecifications)
        }

        return inputTokenCount
    }

    override fun estimateTokenCount(messages: List<ChatMessage>): Int {
        return tokenizer!!.estimateTokenCountInMessages(messages)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HunYuanStreamingChatModel::class.java)
        private fun handle(partialResponse: ChatCompletionResponse, handler: StreamingResponseHandler<AiMessage>) {
            val choices = partialResponse.choices()
            if (choices != null && choices.isNotEmpty()) {
                val delta = (choices[0] as ChatCompletionChoice).delta()
                val content = delta.content()
                if (content != null) {
                    handler.onNext(content)
                }
            }
        }
    }
}
