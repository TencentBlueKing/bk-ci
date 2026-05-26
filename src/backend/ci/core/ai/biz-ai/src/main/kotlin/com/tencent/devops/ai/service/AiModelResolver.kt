package com.tencent.devops.ai.service

import com.tencent.devops.ai.config.AiModelFactory
import com.tencent.devops.ai.model.AiErrorClassifier
import com.tencent.devops.ai.model.FailoverChatModel
import com.tencent.devops.ai.model.FailoverModelCandidate
import com.tencent.devops.ai.properties.AiLlmModelProperties
import com.tencent.devops.ai.properties.AiLlmProperties
import io.agentscope.core.model.Model
import kotlin.random.Random
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
    private val userLlmConfigService: UserLlmConfigService,
    private val errorClassifier: AiErrorClassifier,
    private val random: Random = Random.Default
) {
    /**
     * 平台模型实例缓存：modelId → 已构建的底层 [Model]（含 HTTP 客户端）。
     * 进程内只构建一次，每次 resolve 时复用，避免反复创建 HTTP 客户端；
     * 调用顺序由每次 resolve 时新一次的 [AiLlmProperties.enabledPlatformModels] 决定。
     */
    private val platformModelCache: Map<String, Model> by lazy { buildPlatformModelCache() }

    private val isSinglePlatformModel: Boolean get() = platformModelCache.size == 1

    fun resolve(userId: String?): ResolvedAiModel {
        if (!userId.isNullOrBlank()) {
            val userModelConfig = userLlmConfigService.getEnabledModel(userId)
            if (userModelConfig != null) {
                return buildUserModelWithPlatformFallback(userId, userModelConfig)
            }
        }
        return buildPlatformChain()
    }

    private fun buildUserModelWithPlatformFallback(
        userId: String,
        userModelConfig: AiLlmModelProperties
    ): ResolvedAiModel {
        val fallback = buildPlatformChain()
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
                candidates = listOf(
                    FailoverModelCandidate(
                        id = userModelConfig.id,
                        model = modelFactory.createSingleAttempt(userModelConfig)
                    ),
                    FailoverModelCandidate(
                        id = "platform:${fallback.identifier}",
                        model = fallback.model
                    )
                ),
                errorClassifier = errorClassifier
            ),
            source = AiModelSource.USER,
            identifier = chainId
        )
    }

    private fun buildPlatformChain(): ResolvedAiModel {
        val cache = platformModelCache
        if (isSinglePlatformModel) {
            val (id, model) = cache.entries.single()
            logger.info(
                "[AiModelResolver] Using single platform model: modelId={}",
                id
            )
            return ResolvedAiModel(
                model = model,
                source = AiModelSource.PLATFORM,
                identifier = id
            )
        }
        val orderedConfigs = properties.enabledPlatformModels(random)
        val orderedCandidates = orderedConfigs.map { config ->
            FailoverModelCandidate(
                id = config.id,
                model = cache.getValue(config.id)
            )
        }
        val chainId = orderedCandidates.joinToString(separator = " -> ") { it.id }
        logger.info(
            "[AiModelResolver] Resolved platform failover chain: {}, retryMode=single-attempt-before-switch",
            chainId
        )
        return ResolvedAiModel(
            model = FailoverChatModel(
                candidates = orderedCandidates,
                errorClassifier = errorClassifier
            ),
            source = AiModelSource.PLATFORM,
            identifier = chainId
        )
    }

    private fun buildPlatformModelCache(): Map<String, Model> {
        val platformModels = properties.enabledPlatformModels(random)
        require(platformModels.isNotEmpty()) {
            "No enabled AI platform LLM models configured"
        }
        val isSingle = platformModels.size == 1
        val cache = LinkedHashMap<String, Model>(platformModels.size)
        platformModels.forEach { config ->
            cache[config.id] = if (isSingle) {
                modelFactory.create(config)
            } else {
                modelFactory.createSingleAttempt(config)
            }
        }
        logger.info(
            "[AiModelResolver] Pre-built platform model cache: ids={}, mode={}",
            cache.keys,
            if (isSingle) "single-platform-model" else "platform-failover-candidates"
        )
        return cache
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiModelResolver::class.java)
    }
}
