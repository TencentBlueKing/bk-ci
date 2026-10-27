package com.tencent.devops.process.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.process.tables.TProjectPipelineCallback
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class ProjectPipelineCallbackCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val pipelineCallbackCryptoHelper: PipelineCallbackCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "project-pipeline-callback"

    private val currentKeySha = pipelineCallbackCryptoHelper.currentKeySha()

    override fun fetchBatch(limit: Int, filters: Map<String, String>): List<CryptoKeyRefreshRow> {
        return with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.select(ID, PROJECT_ID, SECRET_PARAM, AES_KEY_SHA)
                .from(this)
                .where(refreshCondition(filters))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val callbackRow = row as PipelineCallbackCryptoKeyRefreshRow
        with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            dslContext.update(this)
                .set(
                    SECRET_PARAM,
                    pipelineCallbackCryptoHelper.refreshSm4OrAes(callbackRow.secretParam)
                )
                .set(AES_KEY_SHA, currentKeySha)
                .where(ID.eq(callbackRow.id))
                .execute()
        }
    }

    private fun TProjectPipelineCallback.refreshCondition(filters: Map<String, String>): List<Condition> {
        val conditions = mutableListOf(
            SECRET_PARAM.isNotNull,
            AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha))
        )
        filters[PipelineCallbackCryptoKeyRefreshRow::projectId.name]
            ?.takeIf { it.isNotBlank() }
            ?.let { conditions.add(PROJECT_ID.eq(it)) }
        filters[PipelineCallbackCryptoKeyRefreshRow::id.name]
            ?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()
            ?.let { conditions.add(ID.eq(it)) }
        return conditions
    }

    private fun toRow(record: Record): PipelineCallbackCryptoKeyRefreshRow {
        return with(TProjectPipelineCallback.T_PROJECT_PIPELINE_CALLBACK) {
            PipelineCallbackCryptoKeyRefreshRow(
                id = record.get(ID),
                projectId = record.get(PROJECT_ID),
                secretParam = record.get(SECRET_PARAM),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class PipelineCallbackCryptoKeyRefreshRow(
    val id: Long,
    val projectId: String,
    val secretParam: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "project-pipeline-callback:$id"

    override fun keySha(): String? = aesKeySha
}
