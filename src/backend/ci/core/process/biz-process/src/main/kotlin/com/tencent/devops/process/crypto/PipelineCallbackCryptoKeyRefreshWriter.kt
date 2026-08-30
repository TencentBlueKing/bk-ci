package com.tencent.devops.process.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.process.tables.TPipelineCallback
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class PipelineCallbackCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val pipelineCallbackCryptoHelper: PipelineCallbackCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "pipeline-callback"

    private val currentKeySha = pipelineCallbackCryptoHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            dslContext.select(PROJECT_ID, PIPELINE_ID, NAME, SECRET_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(SECRET_TOKEN.isNotNull)
                .and(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val callbackRow = row as PipelineCallbackTokenCryptoKeyRefreshRow
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            dslContext.update(this)
                .set(
                    SECRET_TOKEN,
                    callbackRow.secretToken?.let(pipelineCallbackCryptoHelper::refreshSm4OrAes)
                )
                .set(AES_KEY_SHA, currentKeySha)
                .where(PROJECT_ID.eq(callbackRow.projectId))
                .and(PIPELINE_ID.eq(callbackRow.pipelineId))
                .and(NAME.eq(callbackRow.name))
                .execute()
        }
    }

    override fun fetchMissingKeyShaBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            dslContext.select(PROJECT_ID, PIPELINE_ID, NAME, SECRET_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(SECRET_TOKEN.isNotNull)
                .and(AES_KEY_SHA.isNull)
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateAesKeySha(row: CryptoKeyRefreshRow) {
        val callbackRow = row as PipelineCallbackTokenCryptoKeyRefreshRow
        with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            dslContext.update(this)
                .set(AES_KEY_SHA, currentKeySha)
                .where(PROJECT_ID.eq(callbackRow.projectId))
                .and(PIPELINE_ID.eq(callbackRow.pipelineId))
                .and(NAME.eq(callbackRow.name))
                .execute()
        }
    }

    private fun toRow(record: Record): PipelineCallbackTokenCryptoKeyRefreshRow {
        return with(TPipelineCallback.T_PIPELINE_CALLBACK) {
            PipelineCallbackTokenCryptoKeyRefreshRow(
                projectId = record.get(PROJECT_ID),
                pipelineId = record.get(PIPELINE_ID),
                name = record.get(NAME),
                secretToken = record.get(SECRET_TOKEN),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class PipelineCallbackTokenCryptoKeyRefreshRow(
    val projectId: String,
    val pipelineId: String,
    val name: String,
    val secretToken: String?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "pipeline-callback:$projectId:$pipelineId:$name"

    override fun keySha(): String? = aesKeySha
}
