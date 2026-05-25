package com.tencent.devops.ai.service

import com.tencent.devops.ai.constant.AiMessageCode
import com.tencent.devops.ai.dao.UserLlmConfigDao
import com.tencent.devops.ai.pojo.UserLlmConfigInfo
import com.tencent.devops.ai.pojo.UserLlmConfigUpsertRequest
import com.tencent.devops.ai.properties.AiLlmModelProperties
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.model.ai.tables.TAiUserLlmConfig
import com.tencent.devops.model.ai.tables.records.TAiUserLlmConfigRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class UserLlmConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dao: UserLlmConfigDao,
    @Value("\${aes.aesKey}")
    private val aesKey: String = ""
) {

    fun get(userId: String): UserLlmConfigInfo? {
        return dao.getByUserId(dslContext, userId)?.toInfo()
    }

    fun upsert(
        userId: String,
        request: UserLlmConfigUpsertRequest
    ): UserLlmConfigInfo {
        val existing = dao.getByUserId(dslContext, userId)
        val encryptedApiKey = resolveEncryptedSecret(
            incomingValue = request.apiKey,
            existingValue = existing?.apiKey ?: ""
        )
        val encryptedBkAppSecret = resolveEncryptedSecret(
            incomingValue = request.bkAppSecret,
            existingValue = existing?.bkAppSecret ?: ""
        )
        validateRequest(
            request = request,
            encryptedApiKey = encryptedApiKey,
            encryptedBkAppSecret = encryptedBkAppSecret
        )
        val now = java.time.LocalDateTime.now()
        val record = dslContext.newRecord(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG).apply {
            this.userId = userId
            baseUrl = request.baseUrl.trim()
            modelName = request.modelName.trim()
            apiKey = encryptedApiKey
            bkAppCode = request.bkAppCode.trim()
            bkAppSecret = encryptedBkAppSecret
            enabled = request.enabled
            connectTimeoutSeconds = request.connectTimeoutSeconds
            readTimeoutSeconds = request.readTimeoutSeconds
            writeTimeoutSeconds = request.writeTimeoutSeconds
            executionTimeoutSeconds = request.executionTimeoutSeconds
            maxAttempts = request.maxAttempts
            initialBackoffSeconds = request.initialBackoffSeconds
            maxBackoffSeconds = request.maxBackoffSeconds
            backoffMultiplier = request.backoffMultiplier
            createdTime = existing?.createdTime ?: now
            updatedTime = now
        }
        dao.upsert(dslContext, record)
        logger.info(
            "[UserLlmConfig] Upserted config: userId={}, modelName={}, enabled={}",
            userId,
            request.modelName,
            request.enabled
        )
        return get(userId) ?: throw ErrorCodeException(
            errorCode = AiMessageCode.USER_LLM_CONFIG_SAVE_FAILED,
            defaultMessage = "Failed to save user LLM config"
        )
    }

    fun delete(userId: String): Boolean {
        logger.info("[UserLlmConfig] Deleting config: userId={}", userId)
        return dao.delete(dslContext, userId) > 0
    }

    fun getEnabledModel(userId: String): AiLlmModelProperties? {
        val config = dao.getByUserId(dslContext, userId) ?: return null
        if (config.enabled != true) return null
        return AiLlmModelProperties(
            id = "user-$userId",
            baseUrl = config.baseUrl,
            modelName = config.modelName,
            apiKey = decryptIfPresent(config.apiKey ?: ""),
            bkAppCode = config.bkAppCode,
            bkAppSecret = decryptIfPresent(config.bkAppSecret ?: ""),
            connectTimeoutSeconds = config.connectTimeoutSeconds,
            readTimeoutSeconds = config.readTimeoutSeconds,
            writeTimeoutSeconds = config.writeTimeoutSeconds,
            executionTimeoutSeconds = config.executionTimeoutSeconds,
            maxAttempts = config.maxAttempts,
            initialBackoffSeconds = config.initialBackoffSeconds,
            maxBackoffSeconds = config.maxBackoffSeconds,
            backoffMultiplier = config.backoffMultiplier
        )
    }

    private fun validateRequest(
        request: UserLlmConfigUpsertRequest,
        encryptedApiKey: String,
        encryptedBkAppSecret: String
    ) {
        if (request.baseUrl.isBlank() || request.modelName.isBlank()) {
            throw ErrorCodeException(
                errorCode = AiMessageCode.USER_LLM_CONFIG_INVALID,
                defaultMessage = "baseUrl and modelName are required"
            )
        }
        val useStandardMode = encryptedApiKey.isNotBlank()
        val useBkGateway = request.bkAppCode.isNotBlank() || encryptedBkAppSecret.isNotBlank()
        if (useStandardMode == useBkGateway) {
            throw ErrorCodeException(
                errorCode = AiMessageCode.USER_LLM_CONFIG_INVALID,
                defaultMessage = "Configure exactly one auth mode: apiKey or bkAppCode + bkAppSecret"
            )
        }
        if (useBkGateway && (request.bkAppCode.isBlank() || encryptedBkAppSecret.isBlank())) {
            throw ErrorCodeException(
                errorCode = AiMessageCode.USER_LLM_CONFIG_INVALID,
                defaultMessage = "bkAppCode and bkAppSecret must both be configured"
            )
        }
    }

    private fun resolveEncryptedSecret(
        incomingValue: String?,
        existingValue: String
    ): String {
        return when (incomingValue) {
            null -> existingValue
            "" -> ""
            else -> encrypt(incomingValue)
        }
    }

    private fun encrypt(value: String): String {
        requireAesKey()
        return AESUtil.encrypt(aesKey, value)
    }

    private fun decryptIfPresent(value: String): String {
        if (value.isBlank()) return ""
        requireAesKey()
        return AESUtil.decrypt(aesKey, value)
    }

    private fun requireAesKey() {
        if (aesKey.isBlank()) {
            throw ErrorCodeException(
                errorCode = AiMessageCode.USER_LLM_CONFIG_AES_KEY_MISSING,
                defaultMessage = "config[aes.ai] is not found"
            )
        }
    }

    private fun TAiUserLlmConfigRecord.toInfo(): UserLlmConfigInfo {
        return UserLlmConfigInfo(
            userId = userId,
            baseUrl = baseUrl,
            modelName = modelName,
            hasApiKey = !apiKey.isNullOrBlank(),
            bkAppCode = bkAppCode,
            hasBkAppSecret = !bkAppSecret.isNullOrBlank(),
            enabled = enabled == true,
            connectTimeoutSeconds = connectTimeoutSeconds,
            readTimeoutSeconds = readTimeoutSeconds,
            writeTimeoutSeconds = writeTimeoutSeconds,
            executionTimeoutSeconds = executionTimeoutSeconds,
            maxAttempts = maxAttempts,
            initialBackoffSeconds = initialBackoffSeconds,
            maxBackoffSeconds = maxBackoffSeconds,
            backoffMultiplier = backoffMultiplier,
            createdTime = createdTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
            updatedTime = updatedTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserLlmConfigService::class.java)
    }
}
