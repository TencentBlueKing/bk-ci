package com.tencent.devops.ai.service

import com.tencent.devops.ai.config.AiModelFactory
import com.tencent.devops.ai.model.FailoverChatModel
import com.tencent.devops.ai.model.FailoverModelCandidate
import com.tencent.devops.ai.properties.AiLlmModelProperties
import com.tencent.devops.ai.properties.AiLlmProperties
import io.agentscope.core.model.Model
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class AiModelSource {
    PLATFORM,
    USER
}

data class ResolvedAiModel(
    val model: Model,
    val source: AiModelSource,
    val identifier: String
)

@Service
class AiModelResolver(
    private val properties: AiLlmProperties,
    private val modelFactory: AiModelFactory,
    private val userLlmConfigService: UserLlmConfigService
) {
    private val platformModel by lazy { buildPlatformModel() }

    fun resolve(userId: String?): ResolvedAiModel {
        if (!userId.isNullOrBlank()) {
            val userModelConfig = userLlmConfigService.getEnabledModel(userId)
            if (userModelConfig != null) {
                return buildUserModelWithPlatformFallback(userId, userModelConfig)
            }
        }
        return platformModel
    }

    private fun buildUserModelWithPlatformFallback(
        userId: String,
        userModelConfig: AiLlmModelProperties
    ): ResolvedAiModel {
        val fallback = platformModel
        val chainId = "${userModelConfig.id} -> ${fallback.identifier}"
        logger.info(
            "[AiModelResolver] Using user model with platform fallback: userId={}, modelId={}, " +
                "modelName={}, fallback={}, retryMode=single-attempt-before-switch",
            userId,
            userModelConfig.id,
            userModelConfig.modelName,
            fallback.identifier
        )
        return ResolvedAiModel(
            model = FailoverChatModel(
                listOf(
                    FailoverModelCandidate(
                        id = userModelConfig.id,
                        model = modelFactory.createSingleAttempt(userModelConfig)
                    ),
                    FailoverModelCandidate(
                        id = "platform:${fallback.identifier}",
                        model = fallback.model
                    )
                )
            ),
            source = AiModelSource.USER,
            identifier = chainId
        )
    }

    private fun buildPlatformModel(): ResolvedAiModel {
        val platformModels = properties.enabledPlatformModels()
        require(platformModels.isNotEmpty()) {
            "No enabled AI platform LLM models configured"
        }
        if (platformModels.size == 1) {
            val single = platformModels.single()
            logger.info(
                "[AiModelResolver] Using single platform model: modelId={}, modelName={}",
                single.id,
                single.modelName
            )
            return ResolvedAiModel(
                model = modelFactory.create(single),
                source = AiModelSource.PLATFORM,
                identifier = single.id
            )
        }
        val candidates = platformModels.map { config ->
            FailoverModelCandidate(
                id = config.id,
                model = modelFactory.createSingleAttempt(config)
            )
        }
        val chainId = platformModels.joinToString(separator = " -> ") { it.id }
        logger.info(
            "[AiModelResolver] Using platform failover chain: {}, retryMode=single-attempt-before-switch",
            chainId
        )
        return ResolvedAiModel(
            model = FailoverChatModel(candidates),
            source = AiModelSource.PLATFORM,
            identifier = chainId
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiModelResolver::class.java)
    }
}
