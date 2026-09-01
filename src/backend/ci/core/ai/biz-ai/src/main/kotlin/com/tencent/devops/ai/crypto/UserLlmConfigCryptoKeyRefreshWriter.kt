package com.tencent.devops.ai.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.ai.tables.TAiUserLlmConfig
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class UserLlmConfigCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val userLlmConfigCryptoHelper: UserLlmConfigCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "user-llm-config"

    private val currentKeySha = userLlmConfigCryptoHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            dslContext.select(USER_ID, API_KEY, BK_APP_SECRET, AES_KEY_SHA)
                .from(this)
                .where(hasEncryptedSecret())
                .and(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val configRow = row as UserLlmConfigCryptoKeyRefreshRow
        with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            dslContext.update(this)
                .set(API_KEY, refreshIfPresent(configRow.apiKey))
                .set(BK_APP_SECRET, refreshIfPresent(configRow.bkAppSecret))
                .set(AES_KEY_SHA, currentKeySha)
                .where(USER_ID.eq(configRow.userId))
                .execute()
        }
    }

    override fun fetchMissingKeyShaBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            dslContext.select(USER_ID, API_KEY, BK_APP_SECRET, AES_KEY_SHA)
                .from(this)
                .where(hasEncryptedSecret())
                .and(AES_KEY_SHA.isNull)
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateAesKeySha(row: CryptoKeyRefreshRow) {
        val configRow = row as UserLlmConfigCryptoKeyRefreshRow
        with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            dslContext.update(this)
                .set(AES_KEY_SHA, currentKeySha)
                .where(USER_ID.eq(configRow.userId))
                .execute()
        }
    }

    private fun TAiUserLlmConfig.hasEncryptedSecret() =
        API_KEY.isNotNull.and(API_KEY.ne("")).or(
            BK_APP_SECRET.isNotNull.and(BK_APP_SECRET.ne(""))
        )

    private fun refreshIfPresent(content: String?): String? {
        return if (content.isNullOrBlank()) {
            content
        } else {
            userLlmConfigCryptoHelper.refreshSm4OrAes(content)
        }
    }

    private fun toRow(record: Record): UserLlmConfigCryptoKeyRefreshRow {
        return with(TAiUserLlmConfig.T_AI_USER_LLM_CONFIG) {
            UserLlmConfigCryptoKeyRefreshRow(
                userId = record.get(USER_ID),
                apiKey = record.get(API_KEY),
                bkAppSecret = record.get(BK_APP_SECRET),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class UserLlmConfigCryptoKeyRefreshRow(
    val userId: String,
    val apiKey: String?,
    val bkAppSecret: String?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "user-llm-config:$userId"

    override fun keySha(): String? = aesKeySha
}
