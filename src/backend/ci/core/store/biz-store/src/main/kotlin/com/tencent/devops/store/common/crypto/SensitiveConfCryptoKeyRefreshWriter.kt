package com.tencent.devops.store.common.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.store.tables.TStoreSensitiveConf
import com.tencent.devops.store.pojo.common.enums.FieldTypeEnum
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class SensitiveConfCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val storeCryptoHelper: StoreCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "store-sensitive-conf"

    private val currentKeySha = storeCryptoHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            dslContext.select(ID, FIELD_VALUE, AES_KEY_SHA)
                .from(this)
                .where(FIELD_TYPE.eq(FieldTypeEnum.BACKEND.name))
                .and(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val sensitiveConfRow = row as SensitiveConfCryptoKeyRefreshRow
        with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            dslContext.update(this)
                .set(FIELD_VALUE, storeCryptoHelper.refreshSm4OrAes(sensitiveConfRow.fieldValue))
                .set(AES_KEY_SHA, currentKeySha)
                .where(ID.eq(sensitiveConfRow.id))
                .execute()
        }
    }

    private fun toRow(record: Record): SensitiveConfCryptoKeyRefreshRow {
        return with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            SensitiveConfCryptoKeyRefreshRow(
                id = record.get(ID),
                fieldValue = record.get(FIELD_VALUE),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class SensitiveConfCryptoKeyRefreshRow(
    val id: String,
    val fieldValue: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "store-sensitive-conf:$id"

    override fun keySha(): String? = aesKeySha
}
