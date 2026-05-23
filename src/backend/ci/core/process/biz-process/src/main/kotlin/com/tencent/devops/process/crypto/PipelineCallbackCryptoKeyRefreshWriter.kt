package com.tencent.devops.process.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.process.tables.TProjectPipelineCallback
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class PipelineCallbackCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val pipelineCallbackCryptoHelper: PipelineCallbackCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "project-pipeline-callback"

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.select(ID, SECRET_PARAM, AES_KEY_SHA)
                .from(this)
                .where(SECRET_PARAM.isNotNull)
                .and(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(pipelineCallbackCryptoHelper.currentKeySha())))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val callbackRow = row as PipelineCallbackCryptoKeyRefreshRow
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.update(this)
                .set(SECRET_PARAM, pipelineCallbackCryptoHelper.refreshSm4OrAes(callbackRow.secretParam))
                .set(AES_KEY_SHA, pipelineCallbackCryptoHelper.currentKeySha())
                .where(ID.eq(callbackRow.id))
                .execute()
        }
    }

    private fun toRow(record: Record): PipelineCallbackCryptoKeyRefreshRow {
        return with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            PipelineCallbackCryptoKeyRefreshRow(
                id = record.get(ID),
                secretParam = record.get(SECRET_PARAM),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class PipelineCallbackCryptoKeyRefreshRow(
    val id: Long,
    val secretParam: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "project-pipeline-callback:$id"

    override fun keySha(): String? = aesKeySha
}
