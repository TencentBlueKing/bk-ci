package com.tencent.devops.store.common.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.store.tables.TStoreSensitiveConf
import com.tencent.devops.store.pojo.common.enums.FieldTypeEnum
import org.jooq.Condition
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

    override fun fetchBatch(limit: Int, filters: Map<String, String>): List<CryptoKeyRefreshRow> {
        return with(TStoreSensitiveConf.T_STORE_SENSITIVE_CONF) {
            dslContext.select(ID, STORE_CODE, FIELD_VALUE, AES_KEY_SHA)
                .from(this)
                .where(refreshCondition(filters))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    private fun TStoreSensitiveConf.refreshCondition(filters: Map<String, String>): List<Condition> {
        val conditions = mutableListOf(
            FIELD_TYPE.eq(FieldTypeEnum.BACKEND.name),
            AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha))
        )
        filters[SensitiveConfCryptoKeyRefreshRow::id.name]
            ?.takeIf { it.isNotBlank() }
            ?.let { conditions.add(ID.eq(it)) }
        filters[SensitiveConfCryptoKeyRefreshRow::storeCode.name]
            ?.takeIf { it.isNotBlank() }
            ?.let { conditions.add(STORE_CODE.eq(it)) }
        return conditions
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
                storeCode = record.get(STORE_CODE),
                fieldValue = record.get(FIELD_VALUE),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class SensitiveConfCryptoKeyRefreshRow(
    val id: String,
    val storeCode: String,
    val fieldValue: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "store-sensitive-conf:$storeCode:$id"

    override fun keySha(): String? = aesKeySha
}
